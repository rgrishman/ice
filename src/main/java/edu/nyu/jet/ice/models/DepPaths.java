package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import AceJet.*;
import Jet.Control;
import Jet.Parser.DepTransformer;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import Jet.JetTest;
import Jet.Parser.SyntacticRelation;
import Jet.Parser.SyntacticRelationSet;
import Jet.Pat.Pat;
import Jet.Pat.PatternSet;
import Jet.Refres.Resolve;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import Jet.Tipster.Span;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * collect a list of all dependency paths connecting two named entity mentions
 */

//  uses cache now


public class DepPaths {

    final static Logger logger = LoggerFactory.getLogger(DepPaths.class);
    public static final int MAX_ALLOWABLE_SENTLENGTH_FOR_DEPPATH = 600;

    static Map<String, Integer> relationTypeCounts = new TreeMap<String, Integer>();
    static Map<String, Integer> relationInstanceCounts = new TreeMap<String, Integer>();
    static Map<String, String> sourceDict = new TreeMap<String, String>();
    static Map<String, String> linearizationDict = new TreeMap<String, String>();

    static Map<String, String> pathRelations = new TreeMap<String, String>();
    static List<Event> depPathEvents = new ArrayList<Event>();

    static PrintWriter writer;
    static PrintWriter typeWriter;

    static String inputDir;
    static String inputFile;

    public static final int MAX_MENTIONS_IN_SENTENCE = 50;

    public static ProgressMonitorI progressMonitor = null;

    public static DepPathRegularizer depPathRegularizer = new DepPathRegularizer();

    public static HashSet<String> disallowedRelations = new HashSet<String>();

    static {
        disallowedRelations.add("advcl");
        disallowedRelations.add("rcmod");
        disallowedRelations.add("advcl-1");
        disallowedRelations.add("rcmod-1");
        disallowedRelations.add("mod");
        disallowedRelations.add("mod-1");
        disallowedRelations.add("conj");
        disallowedRelations.add("conj-1");
    }


    /**
     * counts the number of instances of each dependency triple in a set
     * of files.  Invoked by <br><br>
     * <tt> DepPaths  propsFile docList inputDir inputSuffix outputFile </tt>
     * <br><br>
     * <tt> propsFile     </tt> Jet properties file                                                       <br>
     * <tt> docList       </tt> file containing list of documents to be processed, 1 per line             <br>
     * <tt> inputDir      </tt> directory containing files to be processed                                <br>
     * <tt> inputSuffix   </tt> file extension to be added to document name to obtain name of input file  <br>
     * <tt> outputFile    </tt> file to contain counts of dependency relations                            <br>
     * <tt> typeOutputFile </tt> file to contain counts of dependency relations                           <br>
     * <tt> sourceDictFile </tt> file to save source sentences for given dependency paths                 <br>
     * <tt> cacheDir      </tt> (optional) cache of preprocessed files
     */

    public static void main(String[] args) throws IOException {
        relationTypeCounts.clear();
        relationInstanceCounts.clear();
        sourceDict.clear();
        linearizationDict.clear();
        DepTransformer transformer = new DepTransformer("yes");
        transformer.setUsePrepositionTransformation(false);
        if (args.length != 7 && args.length != 8) {
            System.err.println("DepCounter requires 7 arguments:");
            System.err.println("  propsFile docList inputDir inputSuffix outputFile");
            System.exit(1);
        }
        String propsFile = args[0];
        String docList = args[1];
        inputDir = args[2];
        String inputSuffix = args[3];
        String outputFile = args[4];
        String typeOutputFile = args[5];
        String sourceDictFile = args[6];
        String cacheDir = args.length == 8 ? args[7] :
                FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName);

        // initialize Jet

        System.out.println("Starting Jet DepCounter ...");
        JetTest.initializeFromConfig(propsFile);
        PatternSet patternSet = IcePreprocessor.loadPatternSet(
                JetTest.getConfig("Jet.dataPath") + File.separator +
                        JetTest.getConfig("Pattern.quantifierFileName"));
        // load ACE type dictionary
        EDTtype.readTypeDict();
        // turn off traces
        Pat.trace = false;
        Resolve.trace = false;
        // ACE mode (provides additional antecedents ...)
        Resolve.ACE = true;
        Properties props = new Properties();
        props.load(new FileReader(propsFile));

        String docName;
        int docCount = 0;

        BufferedReader docListReader = new BufferedReader(new FileReader(docList));
        boolean isCanceled = false;
        while ((docName = docListReader.readLine()) != null) {
            docCount++;
            try {
                if ("*".equals(inputSuffix.trim())) {
                    inputFile = docName;
                } else {
                    inputFile = docName + "." + inputSuffix;
                }
                System.out.println("\nProcessing document " + docCount + ": " + inputFile);
                ExternalDocument doc = new ExternalDocument("sgml", inputDir, inputFile);
                doc.setAllTags(true);
                doc.open();
                // process document
                Ace.monocase = Ace.allLowerCase(doc);
                // --------------- code from Ace.java
                doc.stretchAll();
                // process document
                Ace.monocase = Ace.allLowerCase(doc);
                Control.processDocument(doc, null, docCount == -1, docCount);

                SyntacticRelationSet relations = IcePreprocessor.loadSyntacticRelationSet(
                        cacheDir, inputDir, inputFile
                );
                SyntacticRelationSet transformedRelations = transformer.transform(
                        relations.deepCopy(), doc.fullSpan());
                relations = transformedRelations;

                //System.err.println("RELATIONS BEFORE ADDING INVERSE:");
                //System.err.println(relations);
                relations.addInverses();
                //System.err.println("RELATIONS AFTER ADDING INVERSE");
                //System.err.println(relations);
                // IcePreprocessor.loadENAMEX(doc, cacheDir, inputDir, inputFile, patternSet);
                IcePreprocessor.loadPOS(doc, cacheDir, inputDir, inputFile);
                IcePreprocessor.loadENAMEX(doc, cacheDir, inputDir, inputFile);
                IcePreprocessor.loadAdditionalMentions(doc, cacheDir, inputDir, inputFile);
                IcePreprocessor.addNumberAndTime(doc, cacheDir, inputDir, inputFile);
                collectPaths(doc, relations, transformedRelations);
                if (progressMonitor != null) {
                    progressMonitor.setProgress(docCount);
                    progressMonitor.setNote(docCount + " files processed");
                    if (progressMonitor.isCanceled()) {
                        isCanceled = true;
                        System.err.println("Relation path collection canceled.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // *** write counts
        if (!isCanceled) {
            writer = new PrintWriter(new FileWriter(outputFile));
            typeWriter = new PrintWriter(new FileWriter(typeOutputFile));
            String relationReprFile = outputFile.substring(0, outputFile.length() - 1) + "Repr";
            PrintWriter relationReprWriter = new PrintWriter(new FileWriter(relationReprFile));
            PrintWriter sourceDictWriter = new PrintWriter(new FileWriter(sourceDictFile));
            for (String r : relationInstanceCounts.keySet()) {
                writer.println(relationInstanceCounts.get(r) + "\t" + r);
            }
            for (String r : relationTypeCounts.keySet()) {
                typeWriter.println(relationTypeCounts.get(r) + "\t" + r);
                sourceDictWriter.println(relationTypeCounts.get(r) + "\t" + r + " ||| " + sourceDict.get(r));
                relationReprWriter.println(r + ":::" + linearizationDict.get(r) + ":::" + sourceDict.get(r));
            }
            writer.close();
            typeWriter.close();
            relationReprWriter.close();
            sourceDictWriter.close();

        }
    }

    /*Partition POS tag into four types*/
    static String normalizePos(String pos) {
        if (pos.toLowerCase().startsWith("nnp"))
            return "nnp";
        if (pos.toLowerCase().startsWith("nn"))
            return "nn";
        if (pos.toLowerCase().startsWith("vb"))
            return "vb";
        return "o";
    }


    static void collectPaths(Document doc,
                             SyntacticRelationSet relations,
                             SyntacticRelationSet transformedRelations) {

        List<Annotation> jetSentences = doc.annotationsOfType("sentence");
        if (jetSentences == null) return;
        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names == null) {
            return;
        }
        int sentCount = 0;
        for (Annotation sentence : jetSentences) {
            sentCount++;

            // first, block "bad sentences": 1) first sentence, 2) very long sentence
            // (> MAX_ALLOWABLE_SENTLENGTH_FOR_DEPPATH characters);
            // 3) sentence with quotes, and 4) sentence with parenthesis

            if (sentCount == 1) continue;
            if (sentence.end() - sentence.start() > MAX_ALLOWABLE_SENTLENGTH_FOR_DEPPATH) continue;
            String sentText = doc.text(sentence);
            if (sentText.contains("(") || sentText.contains(")") || sentText.contains("[") || sentText.contains("]") ||
                    sentText.contains("{") || sentText.contains("}") || sentText.contains("\"") ||
                    sentText.contains("'")) {
                continue;
            }


            List<Annotation> localNames = new ArrayList<Annotation>();
            List<Span> localHeadSpans = new ArrayList<Span>();
            for (Annotation name : names) {
                if (annotationInSentence(name, sentence)) {
                    localNames.add(name);
                    localHeadSpans.add(
                            IcePreprocessor.findTermHead(doc, name, relations).span());
                }
            }
            if (localNames.size() > MAX_MENTIONS_IN_SENTENCE) {
                System.err.println("Too many mentions in one sentence. Skipped.");
                continue;
            }
            for (int i = 0; i < localNames.size(); i++) {
                for (int j = 0; j < localNames.size(); j++) {
                    if (i == j) continue;
                    Span h1 = localHeadSpans.get(i);
                    Span h2 = localHeadSpans.get(j);
                    // - mention1 precedes mention2
                    if (h1.start() >= h2.start()) continue;
                    // - in same sentence
//                    if (!sentences.inSameSentence(h1, h2)) continue;
                    // find and record dep path from head of m1 to head of m2
                    DepPath path = buildSyntacticPathOnSpans(h1.start(), h2.start(),
                            localNames.get(i).span(), localNames.get(j).span(),
                            relations, localHeadSpans);
//                    String pathText = buildPathStringOnSpans(h1, h2, relations, localHeadSpans);
//                    if (path != null) {
//                        path.setPath(pathText);
//                    }
                    recordPath(doc, sentence, relations, localNames.get(i), path, localNames.get(j));
                }
            }
        }
    }

    private static boolean mentionInSentence(AceEntityMention mention, Annotation sentence) {
        return mention.jetExtent.start() >= sentence.start() &&
                mention.jetExtent.end() <= sentence.end();
    }

    /**
     *  returns true if 'mention' is in 'sentence'.
     */

    public static boolean annotationInSentence(Annotation mention, Annotation sentence) {
        return mention.start() >= sentence.start() &&
                mention.end() <= sentence.end();
    }

    static void recordPath(Document doc, Annotation sentence, SyntacticRelationSet relations,
                           Annotation mention1, DepPath path, Annotation mention2) {

        if (path == null) return;
        DepPath regularizedPath = depPathRegularizer.regularize(path);

        if (regularizedPath.length() > 5) {
            return;
        }
        String m1 = doc.text(mention1).replaceAll("\\s+", " ").trim();
        String m1Val = m1;
        if (mention1.get("mType") != null &&
                mention1.get("mType").equals("PRO")) {
            m1Val = ((String) mention1.get("val")).replaceAll("\\s+", " ").trim();
        }
        String m2 = doc.text(mention2).replaceAll("\\s+", " ").trim();
        String m2Val = m2;
        if (mention2.get("mType") != null &&
                mention2.get("mType").equals("PRO")) {
            m2Val = ((String) mention2.get("val")).replaceAll("\\s+", " ").trim();
        }
        // String source = inputDir + "/" + inputFile + " | " + start + " | " + end;
        String source = pathText(doc, sentence, mention1, mention2);

        count(relationInstanceCounts, m1Val + " -- " + regularizedPath + " -- " + m2Val);

        String type1 = mention1.get("TYPE") != null ? (String) mention1.get("TYPE") : "OTHER";
        String type2 = mention2.get("TYPE") != null ? (String) mention2.get("TYPE") : "OTHER";
        String fullPath = type1 + " -- " + regularizedPath + " -- " + type2;
        count(relationTypeCounts, fullPath);
        // collect events
        // In EntitySetIndexer:
        // Event event = new Event(tokenString, contextList.toArray(new String[contextList.size()]));
        String[] regularizedSegments = regularizedPath.toString().split(":");
        List<String> contextList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; regularizedSegments.length % 2 == 1 && i + 2 < regularizedSegments.length; i += 2) {
            sb.setLength(0);
            sb.append(regularizedSegments[i].trim()).append(":" + regularizedSegments[i + 1].trim()).append(":" + regularizedSegments[i + 2].trim());
            contextList.add(sb.toString());
            Event event = new Event(regularizedPath.toString(), contextList.toArray(new String[contextList.size()]));
            depPathEvents.add(event);
        }
        if (!sourceDict.containsKey(fullPath)) {
            sourceDict.put(fullPath, source);
        }
        String linearizedPath = regularizedPath.linearize(doc, relations, type1, type2);
        if (!linearizationDict.containsKey(fullPath)) {
            linearizationDict.put(fullPath, linearizedPath);
        }
    }

    /**
     * returns the syntactic path from the anchor to an argument. path is not allowed if
     * one of localMentions is on the path (but not at the beginning or end of the path)
     */
    public static DepPath buildSyntacticPathOnSpans
    (int fromPosn, int toPosn,
     Span arg1, Span arg2,
     SyntacticRelationSet relations,
     List<Span> localSpans) {
        Map<Integer, DepPath> path = new HashMap<Integer, DepPath>();
        DepPath p = new DepPath(fromPosn, toPosn, arg1, arg2);
        int variable = 0;
        LinkedList<Integer> todo = new LinkedList<Integer>();
        todo.add(new Integer(fromPosn));
        path.put(new Integer(fromPosn), p);

        while (todo.size() > 0) {
            Integer from = todo.removeFirst();
            logger.trace("from = " + from);
            SyntacticRelationSet fromSet = relations.getRelationsFrom(from.intValue());
            logger.trace("fromSet = " + fromSet);
            for (int ifrom = 0; ifrom < fromSet.size(); ifrom++) {
                SyntacticRelation r = fromSet.get(ifrom);
                if (disallowedRelations.contains(r.type)) {
                    continue;
                }
                Integer to = new Integer(r.targetPosn);
                // avoid loops
                if (path.get(to) != null) continue;
                // disallow mentions
                if (to.intValue() != toPosn &&
                        IceUtils.matchSpan(to, localSpans)) {
                    continue;
                }
                logger.trace("to = " + to);
                // if 'to' is target
                if (r.type.equals("-1")) {
                    System.err.println("WARNING: Label error!");
                }
                if (to.intValue() == toPosn) {
                    logger.trace("TO is an argument");
                    return path.get(from).extend(r);
                    // return (path.get(from) + ":" + r.type).substring(1);
                } else {
                    // 'to' is another node
                    path.put(to, path.get(from).extend(r));
                    // path.put(to, path.get(from) + ":" + r.type + ":" + r.targetWord);
                }
                todo.add(to);
            }
        }
        return null;
    }


    static void count(Map<String, Integer> map, String s) {
        Integer n = map.get(s);
        if (n == null) n = 0;
        map.put(s, n + 1);
    }


    static String pathText(Document doc, Annotation sentence,
                           Annotation mention1, Annotation mention2) {
        int head1start = mention1.start();
        int head1end = mention1.end();
        int head2start = mention2.start();
        int head2end = mention2.end();
        int start = sentence.start();  //mention1.jetExtent.start();
        int end = sentence.end();  //mention2.jetExtent.end();
        int posn = head1start;
        String text = "";
        if (start < head1start) text += doc.normalizedText(new Span(start, head1start));
        text += " [";
        text += doc.normalizedText(new Span(head1start, head1end));
        text += "] ";
        if (head1end < head2start) text += doc.normalizedText(new Span(head1end, head2start));
        text += " [";
        text += doc.normalizedText(new Span(head2start, head2end));
        text += "] ";
        if (head2end < end) text += doc.normalizedText(new Span(head2end, end));
        return text.trim();
    }


}
