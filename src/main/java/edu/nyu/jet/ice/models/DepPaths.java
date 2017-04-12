package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import edu.nyu.jet.aceJet.*;
import edu.nyu.jet.Control;
import edu.nyu.jet.parser.DepTransformer;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.pat.Pat;
import edu.nyu.jet.pat.PatternSet;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;
import edu.nyu.jet.tipster.Span;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Collect a list of all dependency paths connecting two named entity mentions.
 */

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

    // skip first x sentences at the beginning. 0: keep all; 1: skip first sentence
    public static final int SKIPPED_SENTENCES_AT_BEGINNING = 0;

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
     * counts the number of instances of each dependency path connecting
     * two entity mentions in a set of files.  Invoked by <br>
     *
     * DepPaths      propsFile docList inputDir inputSuffix outputFile
     * 
     * <br>
     * propsFile     Jet properties file
     * docList       file containing list of documents to be processed, 1 per line
     * inputDir      directory containing files to be processed
     * inputSuffix   file extension to be added to document name to obtain name of input file
     * outputFile    file to contain counts of dependency relations
     * typeOutputFile    file to contain counts of dependency relations
     * sourceDictFile file to save source sentences for given dependency paths
     * cacheDir (optional) cache of preprocessed files
     */

    public static void main(String[] args) throws IOException {
        relationTypeCounts.clear();
        relationInstanceCounts.clear();
        sourceDict.clear();
        linearizationDict.clear();
        DepTransformer transformer = new DepTransformer("yes");
        transformer.setUsePrepositionTransformation(false);
        if (args.length != 7 && args.length != 8) {
            System.err.println("DepPaths requires 7 or 8 arguments:");
            System.err.println("  propsFile docList inputDir inputSuffix outputFile");
            System.exit(1);
        }
        String propsFile = args[0];
        String docList = args[1];
        inputDir = args[2];
        String inputSuffix = args[3];
        String outputFile = args[4];
        String typeOutputFile = args[5];
        // String sourceDictFile = args[6];
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
                System.out.println("Collecting dependency paths in document " + docCount + ": " + inputFile);
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

                IcePreprocessor.fetchAnnotations (cacheDir, inputDir, inputFile);
		// maintain two dependency trees:
		//   relations is original parse, for generating repr (English phrase)
		//   transformedRelations is transformed parse, for generating path
                SyntacticRelationSet relations = IcePreprocessor.loadSyntacticRelationSet();
                SyntacticRelationSet transformedRelations = transformer.transform(
                        relations.deepCopy(), doc.fullSpan());
                relations.addInverses();
                transformedRelations.addInverses();
                IcePreprocessor.loadPOS(doc);
                IcePreprocessor.loadENAMEX(doc);
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
//            PrintWriter sourceDictWriter = new PrintWriter(new FileWriter(sourceDictFile));
            for (String r : relationInstanceCounts.keySet()) {
                writer.println(relationInstanceCounts.get(r) + "\t" + r);
            }
            for (String r : relationTypeCounts.keySet()) {
                typeWriter.println(relationTypeCounts.get(r) + "\t" + r);
//                sourceDictWriter.println(relationTypeCounts.get(r) + "\t" + r + " ||| " + sourceDict.get(r));
                relationReprWriter.println(r + ":::" + linearizationDict.get(r) + ":::" + sourceDict.get(r));
            }
            writer.close();
            typeWriter.close();
            relationReprWriter.close();
//            sourceDictWriter.close();

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

    /**
     *  generates dependency paths between all pairs of ENAMEXs appearing
     *  in the same sentence of document 'doc'.  These paths are then
     *  recorded by 'recordPaths'.  
     */

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

            if (sentCount == SKIPPED_SENTENCES_AT_BEGINNING) continue;
            if (sentence.end() - sentence.start() > MAX_ALLOWABLE_SENTLENGTH_FOR_DEPPATH) continue;
            String sentText = doc.text(sentence);
            if (sentText.contains("(") || sentText.contains(")") || sentText.contains("[") || sentText.contains("]") ||
                    sentText.contains("{") || sentText.contains("}") || sentText.contains("\"")) {
                continue;
            }

            List<Annotation> localNames = new ArrayList<Annotation>();
            List<Span> localHeadSpans = new ArrayList<Span>();
            for (Annotation name : names) {
                if (annotationInSentence(name, sentence)) {
                    localNames.add(name);
                    // do not use 'findTerminalHead' till it is improved
                    // localHeadSpans.add(IcePreprocessor.findTermHead(doc, name, relations).span()); >>> check
                    localHeadSpans.add(name.span());
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
                    // find and record dep path from head of m1 to head of m2
                    DepPath path = buildSyntacticPathOnSpans(h1.start(), h2.start(),
                            localNames.get(i).span(), localNames.get(j).span(),
                            relations, localHeadSpans);
                    DepPath transPath = buildSyntacticPathOnSpans(h1.start(), h2.start(),
                            localNames.get(i).span(), localNames.get(j).span(),
                            transformedRelations, localHeadSpans);
                    recordPath(doc, sentence, relations, localNames.get(i), path,  transPath, localNames.get(j));
                }
            }
        }
    }

    private static boolean mentionInSentence(AceEntityMention mention, Annotation sentence) {
        return mention.jetExtent.start() >= sentence.start() &&
                mention.jetExtent.end() <= sentence.end();
    }

    public static boolean annotationInSentence(Annotation mention, Annotation sentence) {
        return mention.start() >= sentence.start() &&
                mention.end() <= sentence.end();
    }

    /**
     *  Records dependency path 'path' from 'mention1' to 'mention2' as follows:
     *  <ul>
     *  <li> with its actual arguments, in 'relationInstanceCounts'            </li>
     *  <li> with its argument types, in 'relationTypeCounts'                  </li>
     *  <li> as a mapping from paths to the linearized form of the path,
     *       in linearizationDict                                              </li>
     *  <li> as a mapping from paths to an example sentence containing that path,
     *       in sourceDict                                                     </li>
     *  </ul>
     */

    static void recordPath(Document doc, Annotation sentence, SyntacticRelationSet relations,
                           Annotation mention1, DepPath path, DepPath transPath, Annotation mention2) {

        if (transPath == null || path == null) return;
        DepPath regularizedPath = depPathRegularizer.regularize(transPath);

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
        String source = pathText(doc, sentence, mention1, mention2);

        count(relationInstanceCounts, m1Val + " -- " + regularizedPath + " -- " + m2Val);

        String type1 = mention1.get("TYPE") != null ? (String) mention1.get("TYPE") : "OTHER";
        String type2 = mention2.get("TYPE") != null ? (String) mention2.get("TYPE") : "OTHER";
        if (type1.equals(type2) && !type1.equals("OTHER")) {
            type1 += "(1)";
            type2 += "(2)";
        }

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
        String linearizedPath = path.linearize(doc, relations, type1, type2, false);
        if (!linearizationDict.containsKey(fullPath)) {
            linearizationDict.put(fullPath, linearizedPath);
        }
    }

    /**
     * returns the syntactic path from the anchor to an argument. path is not allowed if
     * one of localMentions is on the path (but not at the beginning or end of the path)
     * @param  fromPosn   start of path
     * @param  toPosn     end of path
     * @param  arg1       span of first arg
     * @param  arg2       span of second arg
     * @param  relations  dependency relations
     * @param  localSpans list of spans of entities in this sentence
     */

    public static DepPath buildSyntacticPathOnSpans (int fromPosn, int toPosn,
            Span arg1, Span arg2, SyntacticRelationSet relations, List<Span> localSpans) {
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

    /**
     *  returns text of 'sentence' with 'mention1' and 'mention2' enclosed in brackets.
     *  This is the form in which examples of relations are presented to the user.
     */

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
