package Jet.IceModels;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import AceJet.*;
import Jet.Control;
import Jet.IceUI.Ice;
import Jet.IceUtils.FileNameSchema;
import Jet.IceUtils.IceUtils;
import Jet.IceUtils.ProgressMonitorI;
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
import Jet.Terminology.TermCounter;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import Jet.Tipster.Span;
import Jet.Zoner.SentenceSet;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

/**
 * collect a list of all dependency paths connecting two named entity mentions
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
    String propsFile;
    String docList;
    String inputSuffix;
    String cacheDir;

    private static final int MAX_MENTIONS_IN_SENTENCE = 50;

    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    ProgressMonitorI progressMonitor = null;

    public IcePreprocessor(String inputDir, String propsFile, String docList, String inputSuffix, String cacheDir) {
        this.inputDir = inputDir;
        this.propsFile = propsFile;
        this.docList = docList;
        this.inputSuffix = inputSuffix;
        this.cacheDir = cacheDir;
    }

    /**
     * counts the number of instances of each dependency triple in a set
     * of files.  Invoked by <br>
     * DepCounter  propsFile docList inputDir inputSuffix outputFile
     * <p/>
     * propsFile     Jet properties file
     * docList       file containing list of documents to be processed, 1 per line
     * inputDir      directory containing files to be processed
     * inputSuffix   file extension to be added to document name to obtain name of input file
     * outputFile    file to contain counts of dependency relations
     * typeOutputFile    file to contain counts of dependency relations
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
        IcePreprocessor icePreprocessor = new IcePreprocessor(inputDir,
                propsFile, docList, inputSuffix, cacheDir);

        icePreprocessor.processFiles();
    }

    private void processFiles() {
        System.out.println("Starting Jet Preprocessor ...");
        if (progressMonitor != null) {
            progressMonitor.setProgress(1);
            progressMonitor.setNote("Loading Jet models...");
        }

        File cacheDirFile = new File(cacheDir);
        cacheDirFile.mkdirs();
        try {
            FileUtils.copyFile(new File(JetTest.getConfig("Jet.dataPath") + File.separator + "apf.v5.1.1.dtd"),
                    new File(cacheDir + File.separator + "apf.v5.1.1.dtd"));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //String cacheRepository = FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName);
        // initialize Jet

        JetTest.initializeFromConfig(propsFile);
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
            while ((docName = docListReader.readLine()) != null) {
                docCount++;
                String inputFile;
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
                                new FileWriter(cacheFileName(cacheDir, inputDir, inputFile) + ".ace"))), doc);
                // ---------------
                // invoke parser on 'doc' and accumulate counts
                SyntacticRelationSet relations = null;
                try {
                    saveENAMEX(doc, cacheFileName(cacheDir, inputDir, inputFile) + ".names");
                    savePOS(doc, cacheFileName(cacheDir, inputDir, inputFile) + ".pos");
                    saveJetExtents(aceDoc, cacheFileName(cacheDir, inputDir, inputFile) + ".jetExtents");
                    saveNPs(doc, cacheFileName(cacheDir, inputDir, inputFile) + ".nps");
                    doc.removeAnnotationsOfType("ENAMEX");
                    doc.removeAnnotationsOfType("entity");
                    relations = DepParser.parseDocument(doc);
                    saveSyntacticRelationSet(relations, cacheFileName(cacheDir, inputDir, inputFile) + ".dep");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (relations == null) {
                    continue;
                }
                // relations.addInverses();
                // start writing ENAMEX and Syntactic Relation Set cache
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PatternSet loadPatternSet(String fileName) throws IOException {
        PatternCollection pc = new PatternCollection();
        pc.readPatternCollection(new FileReader(fileName));
        pc.makePatternGraph();
        return pc.getPatternSet("quantifiers");
    }

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
        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".nps";
        Map<String, Integer> localCount = new HashMap<String, Integer>();
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String term = parts[0];
            int count = Integer.valueOf(parts[1]);

            localCount.put(term, count);
        }
        br.close();
        return localCount;

    }

    public static Annotation findTermHead(Document doc, Annotation term, SyntacticRelationSet relations) {
        List<Annotation> tokens = doc.annotationsOfType("token", term.span());
        if (tokens == null || tokens.size() == 0) {
            return null;
        }
        int chosen = -1;
        for (int i = tokens.size() - 1; i > -1; i--) {
            List<Annotation> posAnn = doc.annotationsOfType("tagger", tokens.get(i).span());
            if (posAnn != null && posAnn.size() > 0 && posAnn.get(0).get("cat") != null &&
                    ((String)posAnn.get(0).get("cat")).startsWith("NN")) {
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
        // Debug only
        // System.err.println(doc.text(term) + "=> " + doc.text(tokens.get(chosen)));

        return tokens.get(chosen);
    }

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
        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".jetExtents";
        Map<String, Span> jetExtentsMap = new HashMap<String, Span>();
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String id = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end   = Integer.valueOf(parts[2]);
            jetExtentsMap.put(id, new Span(start, end));
        }
        br.close();
        return jetExtentsMap;
    }

    public static void saveENAMEX(Document doc, String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names != null) {
            for (Annotation name : names) {
                pw.println(String.format("%s\t%d\t%d", name.get("TYPE"), name.start(), name.end()));
            }
        }
        pw.close();
    }

    public static void loadENAMEX(Document doc, String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".names";
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        List<Annotation> existingNames = doc.annotationsOfType("ENAMEX");
        existingNames = existingNames == null ? new ArrayList<Annotation>() : existingNames;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String type = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end = Integer.valueOf(parts[2]);
            Annotation newAnn = new Annotation("ENAMEX", new Span(start, end), new FeatureSet("TYPE", type));
            Annotation lowercasedAnn = new Annotation("enamex", new Span(start, end), new FeatureSet("TYPE", type));

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

    public static void loadENAMEX(Document doc, String cacheDir, String inputDir, String inputFile,
                                  PatternSet patternSet) throws IOException {


        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".names";
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        List<Annotation> existingNames = doc.annotationsOfType("ENAMEX");
        existingNames = existingNames == null ? new ArrayList<Annotation>() : existingNames;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String type = parts[0];
            int start = Integer.valueOf(parts[1]);
            int end = Integer.valueOf(parts[2]);
            Annotation newAnn = new Annotation("ENAMEX", new Span(start, end), new FeatureSet("TYPE", type));
            // Annotation lowercasedAnn = new Annotation("enamex", new Span(start, end), new FeatureSet("TYPE", type));

            boolean conflict = false;
            for (Annotation existingAnn : existingNames) {
                if (isCrossed(newAnn, existingAnn)) {
                    conflict = true;
                }
            }
            if (!conflict) {
                doc.addAnnotation(newAnn);
                // doc.addAnnotation(lowercasedAnn);
            }
        }
        br.close();
        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names != null) {
            for (Annotation name : names) {
                String type = (String)name.get("TYPE");
                if (type != null) {
                    Annotation lowercasedAnn = new Annotation("enamex",
                            new Span(name.start(), name.end()),
                            new FeatureSet("TYPE", type));
                    doc.addAnnotation(lowercasedAnn);
                }
            }
        }
        List<Annotation> sentences = doc.annotationsOfType("sentence");
        if (sentences != null) {
            for (Annotation sentence : sentences) {
                patternSet.apply(doc, sentence.span());
            }
        }
        List<Annotation> qnames = doc.annotationsOfType("qenamex");
        if (qnames != null) {
            for (Annotation qname :qnames) {
                Annotation name = (Annotation)qname.get("name");
                if (name != null) {
                    String type = (String) name.get("TYPE");
                    doc.removeAnnotation(name);
                    List<Annotation> upperENAMEX = doc.annotationsAt(name.start());
                    for (Annotation enamex : upperENAMEX) {
                        if (enamex.end() == name.end()) {
                            doc.removeAnnotation(enamex);
                        }
                    }
                    if (type != null) {
                        doc.addAnnotation(new Annotation("ENAMEX", qname.span(),
                                new FeatureSet("TYPE", type)));
                    }
                    // System.err.println("Merged:" + doc.text(qname));
                }
                else {
                    // System.err.println("Skipped:" + doc.text(qname));
                }
            }
        }
    }

    public static void loadAdditionalMentions(Document doc,
                                      String cacheDir,
                                      String inputDir,
                                      String inputFile) throws IOException {
        String aceFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".ace";
        String txtFileName = inputDir + File.separator + inputFile;
        // String jetExtentsFileName = cacheFileName(cacheDir, inputDir, inputFile);
        Map<String, Span> jetExtentsMap = loadJetExtents(cacheDir, inputDir, inputFile);
        AceDocument aceDocument = new AceDocument(txtFileName, aceFileName);
        if (aceDocument.entities != null) {
            for (AceEntity aceEntity : aceDocument.entities) {
                String val = "";
                if (aceEntity.names != null && aceEntity.names.size() > 0) {
                    val = aceEntity.names.get(0).text;
                }
                else {
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

    public static void tagAdditionalMentions(Document doc,
                                              AceDocument aceDocument) {
        if (aceDocument.entities != null) {
            for (AceEntity aceEntity : aceDocument.entities) {
                String val = "";
                if (aceEntity.names != null && aceEntity.names.size() > 0) {
                    val = aceEntity.names.get(0).text;
                }
                else {
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

    public static void loadPOS(Document doc, String cacheDir, String inputDir, String inputFile) throws IOException {
        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".pos";
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            String cat = parts[0];
            int start  = Integer.valueOf(parts[1]);
            int end    = Integer.valueOf(parts[2]);
            doc.addAnnotation(new Annotation("tagger", new Span(start, end), new FeatureSet("cat", cat)));
        }
        br.close();
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
        String inputFileName = cacheFileName(cacheDir, inputDir, inputFile) + ".dep";
        SyntacticRelationSet relations = new SyntacticRelationSet();
        BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        relations.read(br);
        br.close();
        return relations;
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


    @Override
    public void run() {
        processFiles();
    }
}
