package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import edu.nyu.jet.aceJet.*;
import edu.nyu.jet.Control;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.parser.DepParser;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.pat.Pat;
import edu.nyu.jet.pat.PatternCollection;
import edu.nyu.jet.pat.PatternSet;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.ice.terminology.TermCounter;
import edu.nyu.jet.tipster.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Computes and saves a variety of NLP features of a corpus in
 * order to speed up the reanalysis required during entity set
 * and relation development.  Preprocessing is invoked
 * either through the GUI or the command-line interface whenever
 * a new corpus is added to ICE.
 * <p>
 * Preprocessing invokes JET to perform the standard analysis
 * steps (POS tagging, name tagging, dependency parsing, reference
 * resolution).  Preprocessing then saves 
 * <ul>
 * <li> POS tags for each document </li>
 * <li> ENAMEX tags for each document </li>
 * <li> the ACE entities and entity mentions for a document</li>
 * <li> the extent of each entity mention in a document </li>
 * <li> the count of each possible term in each document </li>
 * <li> dependency parse of each document </li>
 * </ul>
 * For a corpus X, this information is saved in directory
 * cache/X/preprocess.  This information does not change over
 * the course of entity set and relation customization.
 * <p>
 * The second stage of processing loads this information, invokes
 * the onoma name tagger to add ENAMEX tags for members of
 * user-created entity sets, adds prenominal mentions of named
 * entities, and adds TIME and NUMBER tags.  It then computes
 * and saves: <ul>
 * <li> aggregate term counts over the corpus </li>
 * <li> dependency paths over the corpus </li>
 * </ul>
 * This information is stored as files within the cache directory.
 * If new entity sets are added, dependency paths can be rapidly
 * recomputed using information computed in the first stage.
 * <p>
 * Annotation Cache:  all information from the first stage (except
 * the ACE entities) is stored in a single file. This information
 * is initially loaded from disk into an <i>annotation cache</i>
 * by method fetchAnnotations;  specific methods (loadPOS,
 * load ENAMEX, ...) then extract information from the cache.
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

    String inputDir;
    String propsFile = "parseprops";
    String docList;
    String inputSuffix;
    String cacheDir;
    static AceDocument aceDoc;

    private static final int MAX_MENTIONS_IN_SENTENCE = 50;

    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    boolean isCanceled = false;

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

    public IcePreprocessor(String inputDir, String propsFile, String docList, String inputSuffix, String cacheDir) {
        this.inputDir = inputDir;
        this.propsFile = "parseprops" ;
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
        String propsFile = "parseprops"; // args[0];
        String docList = args[1];
        String inputDir = args[2];
        String inputSuffix = args[3];
        String cacheDir = args[4];

        IcePreprocessor icePreprocessor = new IcePreprocessor(inputDir,
                propsFile, docList, inputSuffix, cacheDir);
        String selectedCorpusDir = Ice.selectedCorpusName == null ?
                "cache" + File.separator + docList.split(File.separator)[0].split("\\.")[0] :
                FileNameSchema.getCorpusInfoDirectory(Ice.selectedCorpusName);
        icePreprocessor.processFiles(selectedCorpusDir,
                docList.split(File.separator)[0].split("\\.")[0]);
    }

    /**
     * preprocess all the files in a corpus.
     */

    private void processFiles(String selectedCorpusDir, String selectedCorpusName) {

        System.out.println("Starting Jet Preprocessor ...");
        if (progressMonitor != null) {
            progressMonitor.setProgress(1);
            progressMonitor.setNote("Loading Jet models...");
        }

        File cacheDirFile = new File(cacheDir);
        cacheDirFile.mkdirs();

        // initialize Jet
        JetTest.initializeFromConfig(Nice.locateFile("parseprops"));

        try {
            FileUtils.copyFile(Nice.locateFile(JetTest.getConfig("Jet.dataPath") + File.separator + "apf.v5.1.1.dtd"),
                    new File(cacheDir + File.separator + "apf.v5.1.1.dtd"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // load ACE type dictionary
        EDTtype.readTypeDict();
        // ACE mode (provides additional antecedents ...)
        Resolve.ACE = true;

        if (progressMonitor != null) {
            progressMonitor.setProgress(5);
            progressMonitor.setNote("Loading Jet models... done.");
        }

        try {
            BufferedReader docListReader = new BufferedReader(new FileReader(docList));
            boolean isCanceled = false;
            int docCount = 0;
            String docName;
            System.out.println();
            while ((docName = docListReader.readLine()) != null) {
                docCount++;
                String inputFile;
                if ("*".equals(inputSuffix.trim())) {
                    inputFile = docName;
                } else {
                    inputFile = docName + "." + inputSuffix;
                }
                System.out.println(String.format("[Corpus:%s]", Ice.selectedCorpusName)
                        + " Processing document " + docCount + ": " + inputFile);
                ExternalDocument doc = new ExternalDocument("sgml", inputDir, inputFile);
                doc.setAllTags(true);
                doc.open();
                Ace.monocase = Ace.allLowerCase(doc);
                doc.stretchAll();
                Control.processDocument(doc, null, docCount == -1, docCount);
                saveAnnotations(doc, aceDoc, getPosFileName(cacheDir, inputDir, inputFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadAnnotations (Document doc) throws IOException {
        loadPOS (doc);
        loadENAMEX (doc);
        loadTerms (doc);
        loadEntities (doc);
    }

/*

    public void postprocessDocument (Document doc) {
        Ace.tagReciprocalRelations(doc);
        String docId = Ace.getDocId(doc);
        // build entities
        Ace.buildAceEntities(doc, docId, aceDoc);
        aceDoc.write(new PrintWriter
                (new BufferedWriter
                 (new FileWriter
                  (getAceFileName
                   (cacheDir, inputDir, inputFile)))), 
                doc);
        SyntacticRelationSet relations = doc.relations;
        if (relations == null) {
            docId = "";
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
        
            // Do word count now
       if (Ice.selectedCorpus != null) {
           if (progressMonitor != null) {
               progressMonitor.setNote("Counting words and relations...");
           }
           if (countWords(isCanceled)) return;
           System.err.println("Finding dependency paths...");
           if (!isCanceled && (progressMonitor == null || !progressMonitor.isCanceled())) {
               RelationFinder finder = new RelationFinder
                   (Ice.selectedCorpus.docListFileName, 
                  Ice.selectedCorpus.directory,
                    Ice.selectedCorpus.filter, 
                  FileNameSchema.getRelationsFileName
                    (Ice.selectedCorpusName),
                    FileNameSchema.getRelationTypesFileName
                 (Ice.selectedCorpusName), null,
                    Ice.selectedCorpus.numberOfDocs,
                   null);
              finder.run();
               Ice.selectedCorpus.relationTypesFileName =
                   FileNameSchema.getRelationTypesFileName(Ice.selectedCorpus.name);
               Ice.selectedCorpus.relationInstanceFileName =
                   FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);
           }
           if (progressMonitor != null && !progressMonitor.isCanceled()) {
                progressMonitor.setProgress(progressMonitor.getMaximum());
           }
        }  
    }

*/

    /**
     *  Saves POS tags, name tags, terms, dependency parses, entities, and
     *  entity mentions.  This information is saved in a single file;  the first
     *  token in each line of the file indicates the type of information in that line.
     */

     static void saveAnnotations (Document doc, AceDocument aceDoc, String fn) throws IOException {
         PrintWriter pw = new PrintWriter (new FileWriter (fn));
         saveSyntacticRelationSet (doc.relations, pw);
         savePOS (doc, pw);
         saveENAMEX (doc, pw);
         saveTerms (doc, pw);
         saveEntities (doc, pw);
         pw.close();
     }

    /**
     *  Saves POS tags.
     */

    public static void savePOS (Document doc, PrintWriter pw) {
        List<Annotation> names = doc.annotationsOfType("tagger");
        if (names != null) {
            for (Annotation name : names) {
                saveAnnotation(pw, "tagger", name.span(), "cat", (String) name.get("cat"));
            }
        }
    }

    /**
     * Save an entry with no features in the annotation cache.
     */

    public static void saveAnnotation (PrintWriter pw, String type, Span s) {
        pw.println(String.format("%s\t%d\t%d", type, s.start(), s.end()));
    }

    /**
     * Save an entry with one feature in the annotation cache.
     */

    public static void saveAnnotation (PrintWriter pw, String type, Span s, String feat, String value) {
        pw.println(String.format("%s\t%d\t%d\t%s\t%s", type, s.start(), s.end(), feat, value));
    }

    /**
     *  Save all ENAMEX annotations with their TYPE features in the annotation cache.
     */

    public static void saveENAMEX (Document doc, PrintWriter pw) {
       List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names != null) {
            for (Annotation name : names) {
                saveAnnotation(pw, "ENAMEX", name.span(), "TYPE", (String)name.get("TYPE"));
            }
        }
    }

    /**
     * compute the local count of each potential term
     * (head of NP with preceding nouns and adjectives) in document <CODE>doc</CODE>
     * and save the result in PrintWriter <CODE>pw</CODE>.  File format:
     * one term per line.  Names [tagged with ENAMEX] are not included.
     */

    public static void saveTerms(Document doc, PrintWriter pw) throws IOException {
        List<Annotation> nps = doc.annotationsOfType("ng");
        Map<String, Integer> localCount = new HashMap<String, Integer>();
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
            pw.println(String.format("term\t%s\t%d", k, localCount.get(k)));
        }
    }

    /**
     *  Run process to generate aggregate counts of words in a corpus.
     */

    public static boolean countWords(boolean isCanceled) {
        String[] docFileNames = null;
        try {
            docFileNames = IceUtils.readLines(FileNameSchema.getDocListFileName(Ice.selectedCorpus.name));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return true;
        }
        System.err.println("Counting words...");
        if (!isCanceled) {
            // && (progressMonitor == null || !progressMonitor.isCanceled())
            String wordCountFileName = FileNameSchema.getWordCountFileName(Ice.selectedCorpus.name);
            TermCounter counter = TermCounter.prepareRun("onomaprops", // xxx
                    Arrays.asList(docFileNames),
                    Ice.selectedCorpus.directory,
                    Ice.selectedCorpus.filter,
                    wordCountFileName,
                    null);
            counter.run();
            Ice.selectedCorpus.wordCountFileName = FileNameSchema.getWordCountFileName(Ice.selectedCorpus.name);
        }
        return false;
    }


    private void createNewFileList(List<String> newFileNames, String newDirName, String docListFileName) throws IOException {
        //Ice.selectedCorpus.directory = newDirName; xxx`
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

    public static PatternSet loadPatternSet(File file) throws IOException {
        PatternCollection pc = new PatternCollection();
        // pc.readPatternCollection(new FileReader(file));
        pc.makePatternGraph();
        return pc.getPatternSet("quantifiers");
    }

    /**
     *  Load from the annotation cache the counts of all terms in docuemnt
     *  <CODE>doc</CODE>.
     */

    public static Map<String, Integer> loadTerms (Document doc) throws IOException {
        Map<String, Integer> localCount = new HashMap<String, Integer>();
        for (String line : annotationCache) {
            if (line.startsWith("term")) {
                String[] parts = line.split("\\t");
                String term = parts[1];
                int count = Integer.valueOf(parts[2]);
                localCount.put(term, count);
            }
        }
        return localCount;
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
     * Save every Ace entity mention in <CODE>aceDocument</CODE>
     * along with its extent.  Format:  one mention per line, mention id + start + end.
     */

    public static void saveEntities (Document doc, PrintWriter pw) throws IOException {
        Vector<Annotation> entities = (Vector<Annotation>) doc.annotationsOfType("entity");
        if (entities != null) {
            for (Annotation entity : entities) {
                Vector<Annotation> mentions = (Vector<Annotation>) entity.get("mentions");
                for (Annotation mention : mentions) {
                    saveAnnotation (pw, "EntityMention", mention.span());
                }
                saveAnnotation (pw, "Entity", entity.span());
            }
        }
    }

    public static void loadEntities (Document doc) throws IOException {
        Vector<Annotation> mentions = new Vector<Annotation>();
        for (String line : annotationCache) {
            if (line.startsWith("EntityMention")) {
                String[] parts = line.split("\\t");
                int start = Integer.valueOf(parts[1]);
                int end = Integer.valueOf(parts[2]);
                Annotation mention = new Annotation("mention", new Span(start, end), null);
                doc.addAnnotation(mention);
                mentions.add(mention);
            } else if (line.startsWith("Entity")) {
                String[] parts = line.split("\\t");
                int start = Integer.valueOf(parts[1]);
                int end = Integer.valueOf(parts[2]);
                if (mentions.size() > 0)
                    doc.annotate("entity", new Span(start, end), new FeatureSet("mentions", mentions));
                mentions = new Vector<Annotation>();
            }
        }
    }

    /**
     * regenerate <CODE>ENAMEX</CODE> annotations from file saved by saveENAMEX.
     */

    public static void loadENAMEX (Document doc) throws IOException {
        List<Annotation> existingNames = doc.annotationsOfType("ENAMEX");
        existingNames = existingNames == null ? new ArrayList<Annotation>() : existingNames;
        for (String line : annotationCache) {
            if (line.startsWith("ENAMEX")) {
                String[] parts = line.split("\\t");
                if (parts.length != 5 && parts.length != 9) {
                    System.err.println("Format error in ENAMEX cache:");
                    System.err.println("\tline:" + line);
                    continue;
                }
                int start = Integer.valueOf(parts[1]);
                int end = Integer.valueOf(parts[2]);
                String type = parts[4];
                // String val  = parts.length == 6 ? parts[4].trim() : "UNK";  xxx
                // String mType = parts.length == 6 ? parts[5].trim() : "UNK";  xxx
                Annotation newAnn = new Annotation("ENAMEX", new Span(start, end),
                        new FeatureSet("TYPE", type));
                boolean conflict = false;
                for (Annotation existingAnn : existingNames) {
                    if (isCrossed(newAnn, existingAnn)) {
                        conflict = true;
                    }
                }
                if (!conflict) {
                    doc.addAnnotation(newAnn);
                }
            }
        }
    }

    /**
     * Add time, money, and number annotations for bootstrapping
     * TIMEX will be annotated as <ENAMEX TYPE="TIME"/>
     * NUMBER will be annotated as <ENAMEX TYPE="NUMBER"/>
     * MONEY will be annotated as <ENAMEX TYPE="MONEY"/>
     *
     * Limitations:
     * Does not compute true value
     * Does not handle conutry prefix for money (US $)
     * Does not handle decimals (10.3)
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

        // Now add numbers (including money)

        List<Annotation> tokens = doc.annotationsOfType("token");
        if (tokens != null) {
            for (int i = 0; i < tokens.size(); i++) {
		Annotation token = tokens.get(i);
                if (token.get("intvalue") != null) {
		    int value = (int) token.get("intvalue");
		    Span s = token.span();
		    String type = "NUMBER";
		    String prevToken = (i > 0) ?
			doc.text(tokens.get(i - 1)) : "?";
		    if (prevToken.equals("$")) {
			type = "MONEY";
			s.setStart(tokens.get(i - 1).start());
		    }
		    String nextToken = (i < tokens.size() - 1) ?
			doc.text(tokens.get(i + 1)).trim() : "?";
		    if (illions.get(nextToken) != null) {
			value  = illions.get(nextToken);
			i++;
			s.setEnd(tokens.get(i).end());
		    }
		    Annotation numberAnn = new Annotation("ENAMEX",
			s,
			new FeatureSet("TYPE", type,
			    "val", value,
			    "mType", type));
		    if ( !isCrossedWithList(numberAnn, existingNames))
		    doc.addAnnotation(numberAnn);
                 }
            }
        }
	/*
       // this is alternative code which uses Jet 'number' annotations
	JetTest.getNumberAnnotator().annotate(doc);
        List<Annotation> numbers = doc.annotationsOfType("number");
        if (numbers != null) {
            for (Annotation number : numbers) {
                 if ( !isCrossedWithList(number, existingNames)) {
                     Annotation numberAnn = new Annotation("ENAMEX",
                             number.span(),
                             new FeatureSet("TYPE", "NUMBER",
                                     "val", number.get("value"),
                                     "mType", "NUMBER"));
                     doc.addAnnotation(numberAnn);
		     System.out.println("Found number:  " + doc.text(numberAnn) + "   " + numberAnn);
                 }
            }
	} */
    }

    static Map<String, Integer> illions = new HashMap<String, Integer>();
    static {
	illions.put("thousand",1000);
	illions.put("million", 10000);
	illions.put("billion", 100000);
    }

    public static String getAceFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".ace";
    }

    /*
    public static void tagAdditionalMentions(Document doc,
                                             AceDocument aceDoc) {
        if (aceDoc.entities != null) {
            for (AceEntity aceEntity : aceDoc.entities) {
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
    */

    public static boolean isCrossed(Annotation ann1, Annotation ann2) {
        if (ann1.start() >= ann2.start() && ann1.start() < ann2.end() ||
                ann1.end() > ann2.start() && ann1.end() <= ann2.end()) {
            return true;
        }
        return false;
    }

    /**
     *  Loads the annotation cache from file cacheDir/inputDir/inputFile.
     */

    public static void fetchAnnotations (String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = getPosFileName(cacheDir, inputDir, inputFile);
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        annotationCache.clear();
        String line = null;
        while ((line = br.readLine()) != null) {
            annotationCache.add(line);
        }
        br.close();
    }

    private static List<String> annotationCache = new ArrayList<String> ();

    public static String getPosFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheFileName(cacheDir, inputDir, inputFile) + ".pos";
    }

    /**
     * Regenerate <CODE>tagger</CODE> annotations from annotation cache.
     */

    public static void loadPOS (Document doc) {
	DepPaths.toks = new HashSet<String>();
        for (String line : annotationCache) {
            if (line.startsWith("tagger")) {
                String[] parts = line.split("\\t");
                int start = Integer.valueOf(parts[1]);
                int end = Integer.valueOf(parts[2]);
                String cat = parts[3];
                doc.addAnnotation(new Annotation("tagger", new Span(start, end), new FeatureSet("cat", cat)));
		String token = doc.text(new Span(start, end)).trim().toLowerCase();
		DepPaths.toks.add(token);
	    }
        }
    }

    public static void saveSyntacticRelationSet(SyntacticRelationSet relationSet, PrintWriter pw) throws IOException {
        for (SyntacticRelation sr : relationSet)
            pw.println("dep\t" + relationToString(sr));

    }

    private static String relationToString (SyntacticRelation sr) {
        return sr.type + " | " 
               + sr.sourceWord + " | " + sr.sourcePosn + " | " + sr.sourcePos + " | "
               + sr.targetWord + " | " + sr.targetPosn + " | " + sr.targetPos;
    }

    /**
     *  Fetch the dependency information from the annotation cache.  Issues a warning
     *  message if the cache has no dependency information.
     */

    public static SyntacticRelationSet loadSyntacticRelationSet() {
        SyntacticRelationSet relations = new SyntacticRelationSet();
        boolean found = false;
        for (String line : annotationCache) {
            if (! line.startsWith("dep\t")) break;
            line = line.substring(4);
            relations.add(new SyntacticRelation(line));
            found = true;
        }
        if (!found)
            System.out.println("Warning: no dependency information in annotation cache.");
        return relations;
    }

    public static String cacheFileName(String cacheDir,
                                       String inputDir,
                                       String inputFile) {
        String fn = cacheDir + File.separator + 
            inputDir.replaceAll("/", "_").replaceAll("\\\\","_").replaceAll(":","_") 
	    + "_" +
	    inputFile.replaceAll("/", "_").replaceAll("\\\\","_").replaceAll(":","_");
        Map<String, String> map = Ice.selectedCorpus.preprocessCacheMap;
        if (map == null) {
            map = loadPreprocessCacheMap();
        }
        if (! map.isEmpty()) {
            String originalFn = map.get(fn);
            if (originalFn == null) {
                System.err.println ("Error in preprocessCacheMap: no entry for " + fn);
                System.exit(1);
            }
            fn = originalFn;
        }
        return fn;
    }

    public static Map<String, String> loadPreprocessCacheMap () {
        try {
            File f = new File (FileNameSchema.getPreprocessCacheMapFileName(Ice.selectedCorpusName));
            if (f.exists()) {
                Map<String, String> map = new HashMap<String, String>();
                BufferedReader rdr = new BufferedReader(new FileReader(f));
                String line;
                while ((line = rdr.readLine()) != null) {
                    String[] fields = line.split(":");
                    if (fields.length != 2) {
                        System.err.println("Invalid entry in preprocessCacheMap: " + line);
                        continue;
                    }
                    map.put(fields[0], fields[1]);
                }
                return map;
            } else {
                // if no file, return empty map
                return new HashMap<String, String>();
            }
        } catch (IOException e) {
            System.out.println("error loading preprocessCacheMap: " + e);
            return new HashMap<String, String>();
        }
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
        processFiles(Ice.selectedCorpus.directory, Ice.selectedCorpusName);
    }
}
