package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import AceJet.*;
import Jet.Control;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import Jet.JetTest;
import Jet.Lex.Stemmer;
import Jet.Lisp.FeatureSet;
import Jet.Parser.DepParser;
import Jet.Parser.SyntacticRelation;
import Jet.Parser.SyntacticRelationSet;
import Jet.Pat.Pat;
import Jet.Pat.PatternCollection;
import Jet.Pat.PatternSet;
import Jet.Refres.Resolve;
import edu.nyu.jet.ice.terminology.TermCounter;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import Jet.Tipster.Span;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

/**
 * computes and saves a variety of NLP features of a corpus.
 * Invoked whenever a new corpus is added to ICE. Saves <ul>
 * <li> ENAMEX tags for each document </li>
 * <li> POS tags for each document </li>
 * <li> the extent of each entity mention in a document </li>
 * <li> the count of each possible term in each document </li>
 * <li> dependency parse of each document </li>
 * <li> aggregate term counts over the corpus </li>
 * <li> dependency paths over the corpus </li>
 * </ul>
 * All this information is stored as files within the cache directory.
 */

public class IcePreprocessor extends Thread {

    static Stemmer stemmer = new Stemmer().getDefaultStemmer();

    static HashMap<String, String> aceTypeMap = new HashMap<String, String>();

    {
        aceTypeMap.put("PER", "PERSON");
        aceTypeMap.put("ORG", "ORGANIZATION");
        aceTypeMap.put("GPE", "GPE");
        aceTypeMap.put("LOC", "LOCATION");
        aceTypeMap.put("WEA", "WEAPON");
        aceTypeMap.put("FAC", "FACILITY");
        aceTypeMap.put("VEH", "VEHICLE");
    }

	String corpusName;
	Corpus corpus;
    String inputDir;
    String propsFile;
    String docList;
    String inputSuffix;
    String cacheDir;

    private static final int MAX_MENTIONS_IN_SENTENCE = 50;

    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    ProgressMonitorI progressMonitor = null;


    /**
     * creates an IcePreprocessor.
     *
     * @param inputDir    directory containing files to be processed
     * @param propsFile   Jet properties file
     * @param docList     file containing list of documents to be processed, 1 per line
     * @param inputSuffix file extension to be added to document name to obtain name of input file
     * @param cacheDir    cache directory for the given corpus
     */

    public IcePreprocessor(String corpusName, String propsFile, String inputSuffix) {
		this.corpusName = corpusName;
		this.corpus = Ice.corpora.get(corpusName);
        this.inputDir = corpus.getDirectory();
        this.propsFile = propsFile;
		this.inputSuffix = inputSuffix;
        this.docList = FileNameSchema.getDocListFileName(corpusName);
        this.cacheDir = FileNameSchema.getPreprocessCacheDir(corpusName);

    }

    public IcePreprocessor(String inputDir, String propsFile, String docList, String inputSuffix, String cacheDir) {
		this.corpusName = FileNameSchema.getCorpusNameFromDocList(docList);
		this.corpus = Ice.corpora.get(corpusName);
        this.inputDir = inputDir;
        this.propsFile = propsFile;
        this.docList = docList;
        this.inputSuffix = inputSuffix;
        this.cacheDir = cacheDir;
    }

	/**
     * command-line version of IcePreprocessor, invoked by <br>
     * IcePreprocessor  propsFile docList inputDir inputSuffix outputFile
     * <br>
     * propsFile     Jet properties file
     * docList       file containing list of documents to be processed, 1 per line
     * inputDir      directory containing files to be processed
     * inputSuffix   file extension to be added to document name to obtain name of input file
     * cacheDir      cache directory of the given corpus (can usu. be obtained by FileNameSchema.getPreprocessCacheDir())
     */

    public static void main(String[] args) throws IOException {

        if (args.length != 5) {
            System.err.println("IcePreprocessor requires 5 arguments:");
            System.err.println("  propsFile docList inputDir inputSuffix cacheDir");
            System.exit(-1);
        }
        String propsFile = args[0];
        String docList = args[1];
        String inputDir = args[2];
        String inputSuffix = args[3];
        String cacheDir = args[4];

		String corpusName = FileNameSchema.getCorpusNameFromDocList(docList);

        IcePreprocessor icePreprocessor = new IcePreprocessor(corpusName, propsFile, inputSuffix);

        icePreprocessor.processFiles();
    }


    /**
     * preprocess all the files in a corpus.
     */
    private void processFiles() {

        System.out.println("Starting Jet Preprocessor ...");
        if (progressMonitor != null) {
            progressMonitor.setProgress(1);
            progressMonitor.setNote("Loading Jet models...");
        }

        File cacheDirFile = new File(cacheDir);
        cacheDirFile.mkdirs();

        // initialize Jet
        JetTest.initializeFromConfig(propsFile);

        try {
            FileUtils.copyFile(new File(JetTest.getConfig("Jet.dataPath") + File.separator + "apf.v5.1.1.dtd"),
                    new File(cacheDir + File.separator + "apf.v5.1.1.dtd"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // load ACE type dictionary
        EDTtype.readTypeDict();
        // turn off traces
        Pat.trace = false;
        Resolve.trace = false;
        // ACE mode (provides additional antecedents ...)
        Resolve.ACE = true;

        int docCount = 0;
        String docName;
        if (progressMonitor != null) {
            progressMonitor.setProgress(5);
            progressMonitor.setNote("Loading Jet models... done.");
        }
        try {
            BufferedReader docListReader = new BufferedReader(new FileReader(docList));
            boolean isCanceled = false;
            docCount = 0;
            System.out.println();
            while ((docName = docListReader.readLine()) != null) {
                docCount++;
                try {
                    String inputFile;
                    if ("*".equals(inputSuffix.trim())) {
                        inputFile = docName;
                    } else {
                        inputFile = docName + "." + inputSuffix;
                    }
                    System.out.println(String.format("[Corpus:%s]", corpusName)
                            + " Processing document " + docCount + ": " + inputFile);
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
                    Ace.tagReciprocalRelations(doc);
                    String docId = Ace.getDocId(doc);
                    if (docId == null)
                        docId = docName;
                    // create empty Ace document
                    String sourceType = "text";
                    AceDocument aceDoc =
                            new AceDocument(inputFile, sourceType, docId, doc.text());
                    // build entities
                    Ace.buildAceEntities(doc, docId, aceDoc);
                    aceDoc.write(new PrintWriter(
                            new BufferedWriter(
                                    new FileWriter(getAceFileName(cacheDir, inputDir, inputFile)))), doc);
                    // ---------------
                    // IcePreprocessor.tagAdditionalMentions(doc, aceDoc);
                    saveENAMEX(doc, getNamesFileName(cacheDir, inputDir, inputFile));
                    savePOS(doc, getPosFileName(cacheDir, inputDir, inputFile));
                    saveJetExtents(aceDoc, getJetExtentsFileName(cacheDir, inputDir, inputFile));
                    saveNPs(doc, getNpsFileName(cacheDir, inputDir, inputFile));
                    doc.removeAnnotationsOfType("ENAMEX");
                    doc.removeAnnotationsOfType("entity");
                    SyntacticRelationSet relations = doc.relations;
                    saveSyntacticRelationSet(relations, getDepFileName(cacheDir, inputDir, inputFile));

                    if (relations == null) {
                        continue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (progressMonitor != null) {
                    progressMonitor.setProgress(docCount + 5);
                    progressMonitor.setNote(docCount + " files processed");
                    if (progressMonitor.isCanceled()) {
                        isCanceled = true;
                        System.err.println("Relation path collection canceled.");
                        break;
                    }
                }
            }
			System.out.println("Finished processing documents");
            // Do word count now
            if (corpus != null) {
                if (progressMonitor != null) {
                    progressMonitor.setNote("Counting words and relations...");
                }
                if (countWords(isCanceled)) return;
                System.err.println("Finding dependency paths...");
                if (!isCanceled && (progressMonitor == null || !progressMonitor.isCanceled())) {
                    RelationFinder finder = new RelationFinder(
							FileNameSchema.getDocListFileName(corpusName),
							corpus.getDirectory(),
							corpus.getFilter(),
                            FileNameSchema.getRelationsFileName(corpusName),
                            FileNameSchema.getRelationTypesFileName(corpusName), null,
                            corpus.getNumberOfDocs(),
                            null);
                    finder.run();
                    corpus.setRelationTypesFileName(FileNameSchema.getRelationTypesFileName(corpusName));
                    corpus.setRelationInstanceFileName(FileNameSchema.getRelationsFileName(corpusName));
                }
                if (progressMonitor != null && !progressMonitor.isCanceled()) {
                    progressMonitor.setProgress(progressMonitor.getMaximum());
                }
            } else {
				System.err.println("Corpus is null!");
			}
			System.out.println("Finished word count and relation finding");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean countWords(boolean isCanceled) {
		return countWords(Ice.selectedCorpusName, false);

	}

	public static boolean countWords(String corpusName, boolean isCanceled) {
        String[] docFileNames = null;
		Corpus corpus = Ice.corpora.get(corpusName);
        try {
            docFileNames = IceUtils.readLines(FileNameSchema.getDocListFileName(corpusName));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return true;
        }
        System.err.println("Counting words...");
        if (!isCanceled) {
            // && (progressMonitor == null || !progressMonitor.isCanceled())
            String wordCountFileName = FileNameSchema.getWordCountFileName(corpusName);
            TermCounter counter = TermCounter.prepareRun(
			  "onomaprops",
			  Arrays.asList(docFileNames),
			  corpus.getDirectory(),
			  corpus.getFilter(),
			  wordCountFileName,
			  null);
            counter.run();
            corpus.setWordCountFileName(FileNameSchema.getWordCountFileName(corpusName));
        }
        return false;
    }


    private void createNewFileList(List<String> newFileNames, String newDirName, String docListFileName) throws IOException {
        //Ice.selectedCorpus.directory = newDirName;
        PrintWriter fileListWriter = new PrintWriter(new FileWriter(docListFileName));
        for (String fileName : newFileNames) {
            if (!"*".equals(inputSuffix.trim())) {
                if (fileName.length() - inputSuffix.length() - 1 < 0) {
                    continue;
                }
                fileName =
                        fileName.substring(0, fileName.length() - inputSuffix.length() - 1);
            }
            fileListWriter.println(fileName);
        }

        fileListWriter.close();
    }

    public static PatternSet loadPatternSet(String fileName) throws IOException {
        PatternCollection pc = new PatternCollection();
        pc.readPatternCollection(new FileReader(fileName));
        pc.makePatternGraph();
        return pc.getPatternSet("quantifiers");
    }

    /**
     * part of preprocessing:  compute the local count of each potential term
     * (head of NP with preceding nouns and adjectives) in document <CODE>doc</CODE>
     * and save the result in file <CODE>fileName</CODE>.  File format:
     * one term per line.
     */
    public static void saveNPs(Document doc, String fileName) throws IOException {
        List<Annotation> nps = doc.annotationsOfType("ng");
        Map<String, Integer> localCount = new HashMap<String, Integer>();
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        if (nps != null) {
            for (Annotation np : nps) {
                List<String> termsFound = TermCounter.extractPossibleTerms(doc, np.span());
                for (String term : termsFound) {
                    if (!localCount.containsKey(term)) {
                        localCount.put(term, 0);
                    }
                    localCount.put(term, localCount.get(term) + 1);
                }
            }
        }
        for (String k : localCount.keySet()) {
            pw.println(k.trim() + "\t" + localCount.get(k));
        }
        pw.close();
    }

    public static Map<String, Integer> loadNPs(String cacheDir, String inputDir, String inputFile) throws IOException {
		System.out.println("loadNPs(" + cacheDir + ", " + inputDir + ", " + inputFile);
        String inputFileName = getNpsFileName(cacheDir, inputDir, inputFile);
        Map<String, Integer> localCount = new HashMap<String, Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFileName));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\\t");
				String term = parts[0];
				int count = Integer.valueOf(parts[1]);
				
				localCount.put(term, count);
			}
			br.close();
		} catch (IOException e) {
			System.err.println("Unable to read NpsFile " + inputFileName + ": " + e.getMessage());
			throw(e);
		}
        return localCount;

    }

    public static String getNpsFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".nps";
    }

    public static Annotation findTermHead(Document doc, Annotation term, SyntacticRelationSet relations) {
        return findTermHead(doc, term.span(), relations);
    }

    public static Annotation findTermHead(Document doc, Span term, SyntacticRelationSet relations) {
        List<Annotation> tokens = doc.annotationsOfType("token", term);
        if (tokens == null || tokens.size() == 0) {
            List<Annotation> startAtAnn = doc.annotationsAt(term.start(), "token");
            if (startAtAnn == null || startAtAnn.size() == 0) {
                return null;
            } else {
                return startAtAnn.get(0);
            }
        }
        int chosen = -1;
        for (int i = tokens.size() - 1; i > -1; i--) {
            List<Annotation> posAnn = doc.annotationsOfType("tagger", tokens.get(i).span());
            if (posAnn != null && posAnn.size() > 0 && posAnn.get(0).get("cat") != null &&
                    ((String) posAnn.get(0).get("cat")).startsWith("NN")) {
                SyntacticRelation sourceRelation = relations.getRelationTo(tokens.get(i).start());
                if (sourceRelation == null ||
                        sourceRelation.type.endsWith("-1") ||
                        sourceRelation.sourcePosn < term.start() &&
                                sourceRelation.sourcePosn > term.end()) {
                    chosen = i;
                    break;
                }
            }
        }
        if (chosen < 0) {
            chosen = tokens.size() - 1;
        }

        return tokens.get(chosen);
    }

    /**
     * part of preprocessing:  save every Ace entity mention in <CODE>aceDocument</CODE>
     * along with its extent.  Format:  one mention per line, mention id + start + end.
     */

    public static void saveJetExtents(AceDocument aceDocument, String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        List<AceEntity> entities = aceDocument.entities;
        if (entities != null) {
            for (AceEntity entity : entities) {
                for (AceEntityMention mention : entity.mentions) {
                    pw.println(
                            String.format("%s\t%d\t%d", mention.id, mention.jetHead.start(), mention.jetHead.end()));
                }
            }
        }
        pw.close();
    }

    public static Map<String, Span> loadJetExtents(String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = getJetExtentsFileName(cacheDir, inputDir, inputFile);
        Map<String, Span> jetExtentsMap = new HashMap<String, Span>();
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String id = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end = Integer.valueOf(parts[2]);
            jetExtentsMap.put(id, new Span(start, end));
        }
        br.close();
        return jetExtentsMap;
    }

    public static String getJetExtentsFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".jetExtents";
    }


    /**
     * part of preprocessing:  save each name (ENAMEX annotation) in document
     * <CODE>doc</CODE> to file <CODE>fileName</CODE>.  File format:
     * one name per line:  type + start + end
     */

    public static void saveENAMEX(Document doc, String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names != null) {
            for (Annotation name : names) {
                pw.println(String.format("%s\t%d\t%d",
                        name.get("TYPE"), name.start(), name.end()));
            }
        }
        pw.close();
    }

//    public static void saveENAMEX(Document doc, String fileName) throws IOException {
//        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
//        List<Annotation> names = doc.annotationsOfType("ENAMEX");
//        if (names != null) {
//            for (Annotation name : names) {
//                pw.println(String.format("%s\t%d\t%d\t%s\t%s",
//                        name.get("TYPE"), name.start(), name.end(),
//                        name.get("val") != null ? name.get("val") : doc.text(name).replaceAll("\\s+", " ").trim(),
//                        name.get("mType") != null ? name.get("mType") : name.get("TYPE")));
//            }
//        }
//        pw.close();
//    }

    /**
     * regenerate <CODE>ENAMEX</CODE> and generate <CODE>enamex</CODE> annotations from
     * file saved by saveENAMEX.
     */

    public static void loadENAMEX(Document doc, String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = getNamesFileName(cacheDir, inputDir, inputFile);
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        List<Annotation> existingNames = doc.annotationsOfType("ENAMEX");
        existingNames = existingNames == null ? new ArrayList<Annotation>() : existingNames;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            if (parts.length != 5 && parts.length != 3) {
                System.err.println("Format error in ENAMEX cache:" + inputFileName);
                System.err.println("\tline:" + line);
                continue;
            }
            String type = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end = Integer.valueOf(parts[2]);
            String val  = parts.length == 5 ? parts[3].trim() : "UNK";
            String mType = parts.length == 5 ? parts[4].trim() : "UNK";
            Annotation newAnn = new Annotation("ENAMEX", new Span(start, end),
                    new FeatureSet("TYPE", type));
            Annotation lowercasedAnn = new Annotation("enamex", new Span(start, end),
                    new FeatureSet("TYPE", type));

            boolean conflict = false;
            for (Annotation existingAnn : existingNames) {
                if (isCrossed(newAnn, existingAnn)) {
                    conflict = true;
                }
            }
            if (!conflict) {
                doc.addAnnotation(newAnn);
                doc.addAnnotation(lowercasedAnn);
            }
        }
        br.close();
    }

    public static String getNamesFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".names";
    }

    public static void loadAdditionalMentions(Document doc,
                                              String cacheDir,
                                              String inputDir,
                                              String inputFile) throws IOException {
        String aceFileName = getAceFileName(cacheDir, inputDir, inputFile);
        String txtFileName = inputDir + File.separator + inputFile;
        if (!(new File(aceFileName)).exists()) {
            return;
        }

        // String jetExtentsFileName = cacheFileName(cacheDir, inputDir, inputFile);
        Map<String, Span> jetExtentsMap = loadJetExtents(cacheDir, inputDir, inputFile);
        AceDocument aceDocument = new AceDocument(txtFileName, aceFileName);
        if (aceDocument.entities != null) {
            for (AceEntity aceEntity : aceDocument.entities) {
                String val = "";
                if (aceEntity.names != null && aceEntity.names.size() > 0) {
                    val = aceEntity.names.get(0).text.replaceAll("\\s+", " ").trim();
                } else {
                    break;
                }
                if (aceEntity.mentions != null) {
                    for (AceEntityMention mention : aceEntity.mentions) {
                        Span span = jetExtentsMap.get(mention.id);
                        if (doc.annotationsAt(span.start(), "ENAMEX") == null) {
                            doc.addAnnotation(new Annotation("ENAMEX",
                                    span,
                                    new FeatureSet("TYPE", aceEntity.type,
                                            "val", val,
                                            "mType", mention.type)));
                        }
                    }
                }
            }
        }
    }

    /**
     * Add time and number annotations for bootstrapping
     * TIMEX will be annotated as <ENAMEX TYPE="TIME"/>
     * NUMBER will be annotated as <ENAMEX TYPE="NUMBER"/>
     *
     * @param doc
     * @param cacheDir
     * @param inputDir
     * @param inputFile
     * @throws IOException
     */
    public static void addNumberAndTime(Document doc,
                                         String cacheDir,
                                         String inputDir,
                                         String inputFile) throws IOException {
        String txtFileName = inputDir + File.separator + inputFile;
        if (!(new File(txtFileName)).exists()) {
            return;
        }
        List<Annotation> existingNames = doc.annotationsOfType("ENAMEX");
        if (existingNames == null) {
            existingNames = new ArrayList<Annotation>();
        }

        // Add TIMEX
        List<Annotation> timeExps = doc.annotationsOfType("TIMEX2");
        if (timeExps != null) {
            for (Annotation timeExp : timeExps) {
                if (!isCrossedWithList(timeExp, existingNames)) {
                    Annotation timeAnn = new Annotation("ENAMEX",
                            new Span(timeExp.start(), timeExp.end()),
                            new FeatureSet("TYPE", "TIME",
                                    "val", timeExp.get("VAL"),
                                    "mType", "TIME"));
                    doc.addAnnotation(timeAnn);
                    existingNames.add(timeAnn);
                }
            }
        }

        // Now add numbers
        List<Annotation> tokens = doc.annotationsOfType("token");
        if (tokens != null) {
            for (Annotation token : tokens) {
                 if (token.get("intvalue") != null &&
                         !isCrossedWithList(token, existingNames)) {
                     Annotation numberAnn = new Annotation("ENAMEX",
                             new Span(token.start(), token.end()),
                             new FeatureSet("TYPE", "NUMBER",
                                     "val", token.get("intValue"),
                                     "mType", "NUMBER"));
                     doc.addAnnotation(numberAnn);
                 }
            }
        }


    }

    public static String getAceFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".ace";
    }

    public static void tagAdditionalMentions(Document doc,
                                             AceDocument aceDocument) {
        if (aceDocument.entities != null) {
            for (AceEntity aceEntity : aceDocument.entities) {
                String val = "";
                if (aceEntity.names != null && aceEntity.names.size() > 0) {
                    val = aceEntity.names.get(0).text;
                } else {
                    break;
                }
                if (aceEntity.mentions != null) {
                    for (AceEntityMention mention : aceEntity.mentions) {
                        Span span = mention.getJetHead();
                        if (doc.annotationsAt(span.start(), "ENAMEX") == null) {
                            doc.addAnnotation(new Annotation("ENAMEX",
                                    span,
                                    new FeatureSet("TYPE", aceEntity.type,
                                            "val", val,
                                            "mType", mention.type)));
                        }
                    }
                }
            }
        }
    }

    public static boolean isCrossed(Annotation ann1, Annotation ann2) {
        if (ann1.start() >= ann2.start() && ann1.start() < ann2.end() ||
                ann1.end() > ann2.start() && ann1.end() <= ann2.end()) {
            return true;
        }
        return false;
    }

    /**
     * part of preprocessing:  save part-of-speech information for <CODE>doc</CODE>
     * to file <CODE>fileName</CODE>.  Format:  one token per line, POS + start + end.
     */
    public static void savePOS(Document doc, String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        List<Annotation> names = doc.annotationsOfType("tagger");
        if (names != null) {
            for (Annotation name : names) {
                pw.println(String.format("%s\t%d\t%d", name.get("cat"), name.start(), name.end()));
            }
        }
        pw.close();
    }

    /**
     * regenerate <CODE>tagger</CODE> annotations from cache file produced by savePOS.
     */
    public static void loadPOS(Document doc, String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = getPosFileName(cacheDir, inputDir, inputFile);
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String cat = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end = Integer.valueOf(parts[2]);
            doc.addAnnotation(new Annotation("tagger", new Span(start, end), new FeatureSet("cat", cat)));
        }
        br.close();
    }

    public static String getPosFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".pos";
    }

    public static void saveSyntacticRelationSet(SyntacticRelationSet relationSet, String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        relationSet.write(pw);
        pw.close();
    }

    public static void appendSyntacticRelationSet(SyntacticRelationSet relationSet, PrintWriter pw, int sid) throws IOException {
        // PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        pw.println("SID=" + sid);
        relationSet.write(pw);
        pw.println();
        // pw.close();
    }

    public static SyntacticRelationSet loadSyntacticRelationSet(String cacheDir,
                                                                String inputDir,
                                                                String inputFile) throws IOException {
        String inputFileName = getDepFileName(cacheDir, inputDir, inputFile);
        SyntacticRelationSet relations = new SyntacticRelationSet();
        if (!(new File(inputFileName).exists())) {
            return relations;
        }
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        relations.read(br);
        br.close();
        return relations;
    }

    public static String getDepFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".dep";
    }

    public static String cacheFileName(String cacheDir,
                                       String inputDir,
                                       String inputFile) {
        return cacheDir + File.separator
                + inputDir.replaceAll("/", "_") + "_"
                + inputFile.replaceAll("/", "_");
    }


    static String normalizeWord(String word, String pos) {
        String np = word.replaceAll("[ \t]+", "_");
        String ret = "";
        String[] tmp;
        tmp = np.split("_ \t");

        if (tmp.length == 0)
            return "";

        if (tmp.length == 1)
            ret = stemmer.getStem(tmp[0].toLowerCase(), pos.toLowerCase());
        else {
            for (int i = 0; i < tmp.length - 1; i++) {
                ret += stemmer.getStem(tmp[i].toLowerCase(), pos.toLowerCase()) + "_";
            }
            ret += stemmer.getStem(tmp[tmp.length - 1].toLowerCase(), pos.toLowerCase());
        }
        //System.out.println("[stemmer] " + ret);
        return ret;
    }

    private static boolean mentionInSentence(AceEntityMention mention, Annotation sentence) {
        return mention.jetExtent.start() >= sentence.start() &&
                mention.jetExtent.end() <= sentence.end();
    }

    static void count(Map<String, Integer> map, String s) {
        Integer n = map.get(s);
        if (n == null) n = 0;
        map.put(s, n + 1);
    }

    public static boolean isCrossedWithList(Annotation ann, List<Annotation> annotationList) {
        for (Annotation annotationInList : annotationList) {
            if (isCrossed(ann, annotationInList)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void run() {
        processFiles();
    }
}
