package edu.nyu.jet.ice.terminology;

import edu.nyu.jet.aceJet.Ace;
import edu.nyu.jet.aceJet.EDTtype;
import edu.nyu.jet.Control;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.AnnotationStartComparator;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.pat.Pat;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.tipster.*;

import java.io.*;
import java.util.*;

/**
 * Count terms in corpus.
 *
 * @author yhe
 * @version 1.0
 */
public class TermCounter extends Thread {

    // docCounter = number of documents in corpus containing this term
    private Map<String, Integer> docCounter = new HashMap<String, Integer>();
    // freqCount = total frequency of this term in corpus
    private Map<String, Integer> freqCount = new HashMap<String, Integer>();
    // rawCount = list of frequency of term in each document containing term
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

    /**
     *  Process corpus and compute aggregate term counts.
     */

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
        EDTtype.readTypeDict();
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
     * Write aggregate term frequencies and frequencies in each document.
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
     * Update aggregate term count information with information on
     * document <CODE>doc</CODE> obtained from cache.  Counts are
     * combined for terms and names
     *
     * @param doc
     * @param cacheDir
     * @param inputDir
     * @param inputFile
     */
    public void addDocument(Document doc, String cacheDir, String inputDir, String inputFile) {

        Map<String, Integer> localCount = new HashMap<String, Integer>();

	// start with counts of terms

            try {
                IcePreprocessor.fetchAnnotations (cacheDir, inputDir, inputFile);
                Map<String, Integer> loadedCount = IcePreprocessor.loadTerms(doc);
                for (String k : loadedCount.keySet()) {
                    localCount.put(k + "/nn", loadedCount.get(k));
                }
                IcePreprocessor.loadENAMEX(doc);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

	// add in counts of names

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

    /**
     *  Computes a list of all possible terms contained within the noun
     *  group with span <CODE>s</CODE> of document <CODE>doc</CODE>,
     *  where a possible term consists of a series of nouns and
     *  adjectives including the head (final token) of the noun group.
     *  Tokens on a stopword list are also excluded, although this
     *  may be redundant.  If the noun group includes a name (ENAMEX), 
     *  an empty list is returned.
     */

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
            if ((pos.equals("NN") || pos.equals("NNS") || pos.startsWith("JJ"))
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

    /**
     *  returns the Penn tag for token <CODE>token</CODE>, or NN
     *  if there is no Penn tag.
     */

    private static String getPOS(Document doc, Annotation token) {
        String pos = "NN";
        List<Annotation> posAnns = doc.annotationsAt(token.start(), "tagger");
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
