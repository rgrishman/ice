package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import edu.nyu.jet.aceJet.*;
import edu.nyu.jet.Control;
import edu.nyu.jet.parser.DepTransformer;
import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.events.*;
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
 *  Collect a list of all dependency paths connecting two named entity mentions.  This is
 *  run as a separate process which reads and analyzes a corpus of documents, finally 
 *  writing a set of files in the cache directory for the corpus.
 *. <p>
 *  The dependency paths are affected by the entity sets currently defined, and so
 *  this operation may be performed frequently. Two measures are taken to speed it up:<ul>
 *  <li> 
 *  - the dependency parse is computed once and saved
 *  <li>
 *  - the dependency paths are recomputed only for documents containing newly tagged entities
 *    (decribed further below).
 */

public class DepPaths {

    final static Logger logger = LoggerFactory.getLogger(DepPaths.class);
    public static final int MAX_ALLOWABLE_SENTLENGTH_FOR_DEPPATH = 600;
    public static final int MAX_INTERVENING_MENTIONS = 3;
    public static int maxInterveningMentions = Nice.iceIntegerProperty
	("Ice.DepPaths.maxInterveningMentions", MAX_INTERVENING_MENTIONS);
    public static final int MIN_RELATION_TYPE_FREQ = 1;
    public static int minRelationTypeFreq = Nice.iceIntegerProperty
	("Ice.DepPaths.minRelationTypeFreq", MIN_RELATION_TYPE_FREQ);

    static Map<String, Integer> relationTypeCounts = new TreeMap<String, Integer>();
    static Map<String, Integer> relationInstanceCounts = new TreeMap<String, Integer>();
    static Map<String, Integer> eventTypeCount = new TreeMap<String, Integer>();
    static Map<String, Integer> eventInstanceCounts = new TreeMap<String, Integer>();
    static Map<String, String> sourceDict = new TreeMap<String, String>();
    static Map<String, String> linearizationDict = new TreeMap<String, String>();
    static Map<String, String> eventLinearizationDict = new TreeMap<String, String>();

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

    static BufferedReader logReader;
    static PrintWriter logWriter;

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
        eventTypeCount.clear();
        eventInstanceCounts.clear();
        sourceDict.clear();
        linearizationDict.clear();
        eventLinearizationDict.clear();
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

	File log = new File(FileNameSchema.getDepPathsLogFileName(Ice.selectedCorpusName));
	File priorlog = new File(FileNameSchema.getDepPathsPriorLogFileName(Ice.selectedCorpusName));
	boolean priorLogExists = log.exists();
	if (priorLogExists) {
	    log.renameTo(priorlog);
	    logReader = new BufferedReader ( new FileReader (priorlog));
	}
	logWriter = new PrintWriter (new FileWriter(log));
	dpidReset(); 
	resetTreeCollector();

        // initialize Jet

        System.out.println("Starting Jet DepCounter ...");
        JetTest.initializeFromConfig(Nice.locateFile(propsFile));
        PatternSet patternSet = IcePreprocessor.loadPatternSet(Nice.locateFile(
                JetTest.getConfig("Jet.dataPath") + File.separator + JetTest.getConfig("Pattern.quantifierFileName")));
        // load ACE type dictionary
        EDTtype.readTypeDict();
        // ACE mode (provides additional antecedents ...)
        Resolve.ACE = true;
        Properties props = new Properties();
        props.load(new FileReader(Nice.locateFile(propsFile)));

        String docName;
        int docCount = 0;

        BufferedReader docListReader = new BufferedReader(new FileReader(docList));
        boolean isCanceled = false;
        while ((docName = docListReader.readLine()) != null) {
	    docCount++;
	    processDocument(docName, inputSuffix,docCount, priorLogExists, cacheDir, transformer);
	    if (progressMonitor != null) {
		progressMonitor.setProgress(docCount);
		progressMonitor.setNote(docCount + " files processed");
		
		if (progressMonitor.isCanceled()) {
		    isCanceled = true;
		    System.err.println("Relation path collection canceled.");
		    break;
		}
	    }
	}
	if (!isCanceled) {
	    writePaths(outputFile, typeOutputFile, Ice.selectedCorpusName);
	    if (Ice.iceProperties.getProperty("Ice.processEvents") != null)
		writeTrees(Ice.selectedCorpusName);
	}
    }

    public static void processDocument (String docName, String inputSuffix, int docCount, 
	    boolean priorLogExists, String cacheDir, DepTransformer transformer) {
	try {
	    if ("*".equals(inputSuffix.trim())) {
		inputFile = docName;
	    } else {
		inputFile = docName + "." + inputSuffix;
	    }
	    System.out.println("Collecting dependency paths in document " + docCount + ": " + inputFile);
	    if (priorLogExists) dpidRead(logReader);
	    boolean clean = dpidIsClean();
	    if (priorLogExists & clean & (Ice.iceProperties.getProperty("Ice.cacheLDPs") != null)) {
		System.out.println("using previously computed paths");
	    } else {
		dpidReset(); 
		ExternalDocument doc = new ExternalDocument("sgml", inputDir, inputFile);
		doc.setAllTags(true);
		doc.open();
		Ace.monocase = Ace.allLowerCase(doc);
		doc.stretchAll();
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
		doc.relations = transformedRelations;
	    }
	    dpidUpdateCorpusCounts();
	    dpidWrite(logWriter);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void writePaths (String outputFile, String typeOutputFile) throws IOException {
	/*
	String corpusName = Ice.selectedCorpusName;
	    writer = new PrintWriter (new FileWriter (FileNameSchema.getEventsFileName(corpusName)));
	    typeWriter = new PrintWriter (new FileWriter (FileNameSchema.getEventTypesFileName(corpusName)));
	String relationReprFile = outputFile.substring(0, outputFile.length() - 1) + "Repr";
	PrintWriter relationReprWriter = new PrintWriter(new FileWriter(relationReprFile));
	// PrintWriter sourceDictWriter = new PrintWriter(new FileWriter(sourceDictFile));
	for (String r : relationInstanceCounts.keySet()) {
	    writer.println(relationInstanceCounts.get(r) + "\t" + r);
            }
	for (String r : relationTypeCounts.keySet()) {
	    int count = relationTypeCounts.get(r);
	    if (count >= MIN_RELATION_TYPE_FREQ) {
		typeWriter.println(relationTypeCounts.get(r) + "\t" + r);
		relationReprWriter.println(r + ":::" + linearizationDict.get(r) + ":::" + sourceDict.get(r));
	    }
	}
	writer.close();
	typeWriter.close();
	relationReprWriter.close();
	//            sourceDictWriter.close();
	*/
	logWriter.close();
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
	    Annotation.sortByStartPosition(localNames);
            for (int i = 0; i < localNames.size(); i++) {
                for (int j = 0; j < localNames.size(); j++) {
                    if (i == j) continue;
		    if (Math.abs(i - j) > MAX_INTERVENING_MENTIONS) continue;
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
	    if (Ice.iceProperties.getProperty("Ice.processEvents") != null)
		collectTreesInSentence (doc, sentence, relations, transformedRelations);
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

        pathsWithArguments.add(m1Val + " -- " + regularizedPath + " -- " + m2Val);

        String type1 = mention1.get("TYPE") != null ? (String) mention1.get("TYPE") : "OTHER";
        String type2 = mention2.get("TYPE") != null ? (String) mention2.get("TYPE") : "OTHER";
        if (type1.equals(type2) && !type1.equals("OTHER")) {
            type1 += "(1)";
            type2 += "(2)";
        }

        String fullPath = type1 + " -- " + regularizedPath + " -- " + type2;
        pathsWithTypes.add(fullPath);
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
        String linearizedPath = path.linearize(doc, relations, type1, type2, false);
        linearizedPath = DepTreeMap.normalizeRepr(linearizedPath);
        sources.add(fullPath + " ==> " + source);
        linearizedPaths.add(fullPath + " --> " + linearizedPath);
    }

    /**
     * Returns the shortest path from 'fromPosn' to 'toPosn' in the dependency tree. 
     * A path is not allowed if one of localMentions is on the path 
     * (but not at the beginning or end of the path).
     * 
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
            logger.trace("from = {}", from);
            SyntacticRelationSet fromSet = relations.getRelationsFrom(from.intValue());
            logger.trace("fromSet = {}", fromSet);
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
                logger.trace("to = {}", to);
                // if 'to' is target
                if (r.type.equals("-1")) {
                    logger.warn("WARNING: Label error!");
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
	
//------------------------- T r e e s   and   e v e n t s

    public static void resetTreeCollector () {
	treesWithTypes.clear();
	treesWithArguments.clear();
	linearizedTrees.clear();
    }

    public static String treeText (Document doc, Annotation sentence, int posn, SyntacticRelationSet relations) {
        return doc.normalizedText(sentence);
    }

    public static void collectTreesInSentence (Document doc, Annotation sentence,
	    SyntacticRelationSet relations, SyntacticRelationSet transformedRelations) {
	List<IceTree> rawTrees = IceTree.extract(doc, sentence.span(), transformedRelations);
	for (IceTree tree : rawTrees) {
	    IceTree lemmaTree = tree.lemmatize();
	    IceTree typeTree = lemmaTree.keySignature();
	    treesWithTypes.add(typeTree.core());
	    treesWithArguments.add(tree.core());
	    String source = treeText (doc, sentence, tree.getTriggerPosn(), relations);
	    sources.add(typeTree + " ==> " + source);
        String repr = tree.linearize();
        repr = DepTreeMap.normalizeRepr(repr);
	    linearizedTrees.add(typeTree + " --> " + repr);
	}
    }
    public static void writePaths (String outputFile, String typeOutputFile, String corpusName) {
	try {
	     PrintWriter pathWriter = new PrintWriter (new FileWriter (outputFile));
	     PrintWriter pathTypeWriter = new PrintWriter (new FileWriter (typeOutputFile));
	    PrintWriter pathReprWriter = new PrintWriter (new FileWriter (FileNameSchema.getRelationReprFileName(corpusName)));
	    for (String r : relationInstanceCounts.keySet()) {
		pathWriter.println(relationInstanceCounts.get(r) + "\t" + r);
	    }
	    for (String r : relationTypeCounts.keySet()) {
		pathTypeWriter.println(relationTypeCounts.get(r) + "\t" + r);
		pathReprWriter.println(r + ":::" + linearizationDict.get(r) + ":::" + sourceDict.get(r));
	    }
	    pathWriter.close();
	    pathTypeWriter.close();
	    pathReprWriter.close();
	} catch (IOException e) {
	    System.out.println("IOException writing event trees");
	    System.out.println(e);
	}
    }

    public static void writeTrees(String corpusName) {
	try {
	    PrintWriter eventWriter = new PrintWriter (new FileWriter (FileNameSchema.getEventsFileName(corpusName)));
	    PrintWriter eventTypeWriter = new PrintWriter (new FileWriter (FileNameSchema.getEventTypesFileName(corpusName)));
	    PrintWriter eventReprWriter = new PrintWriter (new FileWriter (FileNameSchema.getEventReprFileName(corpusName)));
	    for (String r : eventInstanceCounts.keySet()) {
		eventWriter.println(eventInstanceCounts.get(r) + "\t" + r);
	    }
	    for (String r : eventTypeCount.keySet()) {
		eventTypeWriter.println(eventTypeCount.get(r) + "\t" + r);
		eventReprWriter.println(r + ":::" + eventLinearizationDict.get(r) + ":::" + sourceDict.get(r));
	    }
	    eventWriter.close();
	    eventTypeWriter.close();
	    eventReprWriter.close();
	} catch (IOException e) {
	    System.out.println("IOException writing event trees");
	    System.out.println(e);
	}
    }

//------- DepPathsInDocument ---- D e p e n d e n c y   P a t h   C a c h i n g

   // The following code implements Dependency Path caching:  if dependency paths have
   // been previously generated for a document and the document does not contain any
   // any words which are members of an entity set, the previosly generated paths
   // are used.

   // Note that the initial run of DepPaths ill be quite slow as the cache is 
   // being filled.

   // The cache is maintained as a file 'DepPathsLog' stored in the
   // cache subdirectory for a corpus.  When DepPaths is invoked on a corpus,
   // "DepPathsLog' is renamed 'DepPathPriorLog' and a new 'DepPathsLog'
   // is created.

    static List<String> pathsWithArguments;
    static List<String> pathsWithTypes;
    static List<String> treesWithArguments;
    static List<String> treesWithTypes;
    static List<String> sources;
    static List<String> linearizedPaths;
    static List<String> linearizedTrees;
    static Set<String> toks;

    static public void dpidReset() {
	pathsWithArguments = new ArrayList<String>();
	pathsWithTypes = new ArrayList<String>();
	treesWithArguments = new ArrayList<String>();
	treesWithTypes = new ArrayList<String>();
	sources = new ArrayList<String>();
	linearizedPaths = new ArrayList<String>();
	linearizedTrees = new ArrayList<String>();
	toks = new HashSet<String>();
    }

    /**
      *  Reads the information associated with one document 
      *  from the PriorLog
      */

    static public void dpidRead(BufferedReader reader) throws IOException {
	dpidReset();
	String line;
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    toks.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    pathsWithTypes.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    pathsWithArguments.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    treesWithTypes.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    treesWithArguments.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    sources.add(line);
	}
	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    linearizedPaths.add(line);
	}

	while ((line = reader.readLine()) != null) {
	    if (line.equals("<o>")) break;
	    linearizedTrees.add(line);
	}
    }

    /**
      * A document is clean if it contains no words which are members of an entity set.
      */

    static public boolean dpidIsClean () {
	for (IceEntitySet es : Ice.entitySets.values()) {
	    for (String w : es.getNouns()) {
		if (toks.contains(w.toLowerCase())) {
		    return false;
		}
	    }
	}
	return true;
    }

    static public void dpidUpdateCorpusCounts () {

	if (pathsWithTypes != null)
	    for (String s : pathsWithTypes)
		count(relationTypeCounts, s);
	if (pathsWithArguments != null)
	    for (String s : pathsWithArguments)
		count(relationInstanceCounts, s);
	if (treesWithTypes != null)
	    for (String s : treesWithTypes)
		count(eventTypeCount, s);
	if (treesWithArguments != null)
	    for (String s : treesWithArguments)
		count(eventInstanceCounts, s);
	if (sources != null) {
	    for (String s : sources) {
		String[] ss = s.split(" ==> ");
		if (ss.length == 2) {
		    if (!sourceDict.containsKey(ss[0])) {
			sourceDict.put(ss[0], ss[1]);
		    }
		} else {
		    logger.warn("Error in LDP cache.");
		}
	    }
	}
	if (linearizedPaths != null) {
	    for (String s : linearizedPaths) {
		String[] ss = s.split(" --> ");
		if (ss.length == 2) {
		    if (!linearizationDict.containsKey(ss[0])) {
			linearizationDict.put(ss[0], ss[1]);
		    }
		} else {
		    logger.warn("Error in LDP cache.");
		}
	    }
	}
	if (linearizedTrees != null) {
	    for (String s : linearizedTrees) {
		String[] ss = s.split(" --> ");
		if (ss.length == 2) {
		    if (!eventLinearizationDict.containsKey(ss[0])) {
			eventLinearizationDict.put(ss[0], ss[1]);
		    }
		} else {
		    logger.warn("Error in LDP cache.");
		}
	    }
	}
    }

    /**
      * Writes the information associated with a document to
      * the DepPathsLog.
      */

    static public void dpidWrite(PrintWriter logWriter) {
	if (toks != null)
	    for (String s : toks)
		logWriter.println(s);
	logWriter.println("<o>");
	if (pathsWithTypes != null)
	    for (String s : pathsWithTypes)
		logWriter.println(s);
	logWriter.println("<o>");
	if (pathsWithArguments != null)
	    for (String s : pathsWithArguments)
		logWriter.println(s);
	logWriter.println("<o>");
	if (treesWithTypes != null)
	    for (String s : treesWithTypes)
		logWriter.println(s);
	logWriter.println("<o>");
	if (treesWithArguments != null)
	    for (String s : treesWithArguments)
		logWriter.println(s);
	logWriter.println("<o>");
	if (sources != null)
	    for (String s : sources)
		logWriter.println(s);
	logWriter.println("<o>");
	if (linearizedPaths != null)
	    for (String s : linearizedPaths)
		logWriter.println(s);
	logWriter.println("<o>");
	if (linearizedTrees != null)
	    for (String s : linearizedTrees)
		logWriter.println(s);
	logWriter.println("<o>");
    }
}

