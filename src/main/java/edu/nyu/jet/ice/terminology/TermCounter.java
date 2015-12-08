package edu.nyu.jet.ice.terminology;

import AceJet.Ace;
import Jet.Control;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.AnnotationStartComparator;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import Jet.JetTest;
import Jet.Pat.Pat;
import Jet.Refres.Resolve;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import Jet.Tipster.Span;

import java.io.*;
import java.util.*;

/**
 * Count terms based on NP
 *
 * @author yhe
 * @version 1.0
 */
public class TermCounter extends Thread {
    private Map<String, Integer> docCounter = new HashMap<String, Integer>();
    private Map<String, Integer> freqCount = new HashMap<String, Integer>();
    private Map<String, List<Integer>> rawCount = new HashMap<String, List<Integer>>();
    private ProgressMonitorI progressMonitor = null;
    private static String[] stopWordsArr = new String[]{
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with", "from"};
    private static Set<String> stopWords = new HashSet<String>();
    static {
        stopWords.addAll(Arrays.asList(stopWordsArr));
    }

    public void setRunnerProperties(RunnerProperties runnerProperties) {
        this.runnerProperties = runnerProperties;
    }

    private RunnerProperties runnerProperties = null;

    public TermCounter(ProgressMonitorI monitor) {
        this.progressMonitor = monitor;
    }

    public void resetCounters() {
        docCounter.clear();
        freqCount.clear();
    }

    public void count(String propsFile,
                      List<String> docList,
                      String inputDir,
                      String inputSuffix,
                      String outputFile) throws IOException {
        if (progressMonitor != null) {
            progressMonitor.setNote("Loading Jet models...");
            progressMonitor.setProgress(1);
        }
        try {
            Thread.sleep(500);
            if (progressMonitor != null) {
                progressMonitor.setProgress(2);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        resetCounters();
        JetTest.initializeFromConfig(propsFile);
        // load ACE type dictionary
        AceJet.EDTtype.readTypeDict();
        // turn off traces
        Pat.trace = false;
        Resolve.trace = false;
        // ACE mode (provides additional antecedents ...)
        Resolve.ACE = true;
        int docCount = 0;
        //        DictionaryBasedNamedEntityPostprocessor nePostprocessor =
        //                new DictionaryBasedNamedEntityPostprocessor(propsFile);
        for (String docName : docList) {
            if (progressMonitor != null && progressMonitor.isCanceled()) {
                System.err.println("Term counter canceled.");
                break;
            }
            docCount++;
            try {
                String inputFile;
                if ("*".equals(inputSuffix)) {
                    inputFile = docName;
                } else {
                    inputFile = docName + "." + inputSuffix;
                }
                System.out.println("\nCounting terms in document " + docCount + ": " + inputFile);
                ExternalDocument doc = new ExternalDocument("sgml", inputDir, inputFile);
                doc.setAllTags(true);
                doc.open();
                // process document
                Ace.monocase = Ace.allLowerCase(doc);
                Control.processDocument(doc, null, false, docCount);
                addDocument(doc, FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName), inputDir, inputFile);
                if (progressMonitor != null) {
                    progressMonitor.setProgress(docCount);
                    progressMonitor.setNote(docCount + " files processed");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        writeOutputFile(outputFile, docCount);
    }

    /**
     * Write absolute word frequencies in each document.
     *
     * Currently document level count information is not used. We record this information because the
     * term extraction algorithms in FUSE would use such information. e.g., some algorithms will prefer
     * terms that are evenly distributed in the foreground corpus. We wanted to migrate the FUSE algorithms
     * here in future.
     *
     * @param outputFile
     * @param docCount
     * @throws IOException
     */
    public void writeOutputFile(String outputFile, int docCount) throws IOException{
        PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
        pw.println("# DOC_COUNT\n# TERM DOC_FREQS");
        pw.println(docCount);
        for (String s : freqCount.keySet()) {
            pw.print(String.format("%s", s));
            List<Integer> rawCounts = rawCount.get(s);
            for (Integer i : rawCounts) {
                pw.print("\t" + i);
            }
            pw.println();
        }
        pw.close();
    }

    /**
     * Add document by loading count information from cache
     *
     * @param doc
     * @param cacheDir
     * @param inputDir
     * @param inputFile
     */
    public void addDocument(Document doc, String cacheDir, String inputDir, String inputFile) {

        Map<String, Integer> localCount = new HashMap<String, Integer>();

            try {
                Map<String, Integer> loadedCount = IcePreprocessor.loadNPs(cacheDir, inputDir, inputFile);
                for (String k : loadedCount.keySet()) {
                    localCount.put(k + "/nn", loadedCount.get(k));
                }
                IcePreprocessor.loadENAMEX(doc, cacheDir, inputDir, inputFile);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names != null) {
            for (Annotation name : names) {
                String term = doc.text(name).trim().replaceAll("\\s+", " ") + "/nnp";
                if (term.length() <= 5 ||
                        (term.length() > 0 && !Character.isLetter(term.charAt(0)))) continue;
                if (!localCount.containsKey(term)) {
                    localCount.put(term, 0);
                }
                localCount.put(term, localCount.get(term) + 1);
            }
        }
        for (String localTerm : localCount.keySet()) {
            if (!docCounter.containsKey(localTerm)) {
                docCounter.put(localTerm, 0);
            }
            docCounter.put(localTerm, docCounter.get(localTerm) + 1);
            if (!freqCount.containsKey(localTerm)) {
                freqCount.put(localTerm, 0);
            }
            freqCount.put(localTerm, freqCount.get(localTerm) + localCount.get(localTerm));
            if (!rawCount.containsKey(localTerm)) {
                rawCount.put(localTerm, new ArrayList<Integer>());
            }
            rawCount.get(localTerm).add(localCount.get(localTerm));
        }
    }

    public static List<String> extractPossibleTerms(Document doc, Span s) {
        List<String> terms = new ArrayList<String>();
        boolean isENAMEX = (doc.annotationsOfType("ENAMEX", s) != null);
        if (isENAMEX) {
            return terms;
        }
        List<Annotation> tokens = doc.annotationsOfType("token", s);
        Collections.sort(tokens, new AnnotationStartComparator());
        int i = tokens.size() - 1;
        StringBuilder termBuilder = new StringBuilder();
        while(i > -1) {
            Annotation token = tokens.get(i);
            String pos = getPOS(doc, token);
            String tokenString = doc.text(token).replace("\\s+", " ").trim();
            if ((pos.equals("n") || pos.startsWith("adj"))
                    && tokenString.length() > 1
                    && !stopWords.contains(tokenString)) {
                termBuilder.insert(0, tokenString + " ");
                if (!tokenString.startsWith("'")) {
                     terms.add(termBuilder.toString().trim());
                }
                i--;
            }
            else {
                break;
            }
        }
        return terms;
    }

    private static String getPOS(Document doc, Annotation token) {
        String pos = "NN";
        List<Annotation> posAnns = doc.annotationsOfType("constit", token.span());
        if (posAnns != null) {
            pos = (String)posAnns.get(0).get("cat");
        }
        return pos;
    }

    public static TermCounter prepareRun(String propsFile,
                           List<String> docList,
                           String inputDir,
                           String inputSuffix,
                           String outputFile,
                           ProgressMonitorI progressMonitor) {
        TermCounter result = new TermCounter(progressMonitor);
        result.setRunnerProperties(new RunnerProperties(propsFile, docList, inputDir, inputSuffix, outputFile));
        return result;
    }

    @Override
    public void run() {
        if (runnerProperties != null) {
            try {
                count(runnerProperties.propsFile,
                        runnerProperties.docList,
                        runnerProperties.inputDir,
                        runnerProperties.inputSuffix,
                        runnerProperties.outputFile);
            }
            catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        runnerProperties = null;
    }


}

class RunnerProperties {
    String propsFile;
    List<String> docList;
    String inputDir;
    String inputSuffix;
    String outputFile;

    RunnerProperties(String propsFile,
                     List<String> docList,
                     String inputDir,
                     String inputSuffix,
                     String outputFile) {
        this.propsFile = propsFile;
        this.docList = docList;
        this.inputDir = inputDir;
        this.inputSuffix = inputSuffix;
        this.outputFile = outputFile;
    }
}
