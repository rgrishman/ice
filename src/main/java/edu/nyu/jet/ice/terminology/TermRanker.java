package edu.nyu.jet.ice.terminology;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.IceUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Ranker for extracted terms.
 *
 * @author yhe
 * @version 1.0
 */
public class TermRanker {
    private List<Term> terms;

    /**
     * Rank multi-word terms using the following score: <br>
     *
     * Score = POSITIVE_FREQ * log(POSITIVE_FREQ) ^ alpha / NEGATIVE_FREQ <br>
     *
     * where POSITIVE_FREQ is the document frequency in the foreground corpus
     * and NEGATIVE_FREQ is the document frequency in the background corpus.
     * alpha can be set in iceprops with the Ice.TermRanker.alpha property
     *
     * @param foregroundCountFile Name of the word count file for the foreground corpus
     * @param backgroundCountFile Name of the word count file for the background corpus
     * @throws IOException
     */
    public TermRanker(String foregroundCountFile, String backgroundCountFile)
            throws IOException {
        String[] positiveWords = IceUtils.readLines(foregroundCountFile);
        String[] negativeWords = IceUtils.readLines(backgroundCountFile);
        //Map<String, Integer> foregroundWordFreq = new HashMap<String, Integer>();
        Map<String, Integer> foregroundDocFreq = new HashMap<String, Integer>();
        //Map<String, Integer> backgroundWordFreq = new HashMap<String, Integer>();
        Map<String, Integer> backgroundDocFreq = new HashMap<String, Integer>();
        int i = 0;
        for (String w : positiveWords) {
            if (i < 3) {
                i++;
                continue;
            }
            String[] parts = w.split("\\t");
            if (parts[0].equals("Contact/nn") ||
                    parts[0].equals("today/nn") ||
                    parts[0].equals("yesterday/nn")) {
                continue;
            }
            foregroundDocFreq.put(parts[0], parts.length - 1);
        }
        i = 0;
        for (String w : negativeWords) {
            if (i < 3) {
                i++;
                continue;
            }
            String[] parts = w.split("\\t");
            backgroundDocFreq.put(parts[0], parts.length - 1);
            i++;
        }
        terms = new ArrayList<Term>();
        double pow = 1.0;
        try {
            pow = Double.valueOf(Ice.iceProperties.getProperty("Ice.TermRanker.alpha"));
            System.err.println("Trying to use alpha: " + pow);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        for (String w : foregroundDocFreq.keySet()) {
//            int negativeWordCount = backgroundWordFreq.containsKey(w) ?
//                    backgroundWordFreq.get(w) + 1 : 1;
            int negativeDocCount = backgroundDocFreq.containsKey(w) ?
                    backgroundDocFreq.get(w) + 1 : 1;
            Term term = new Term(w,
                    foregroundDocFreq.get(w),
                    0,
                    negativeDocCount,
                    0
            );
            term.setScore((double) term.getPositiveDocFreq() *
                    Math.pow(Math.log(term.getPositiveDocFreq()), pow)
                    /
                    term.getNegativeDocFreq());
            terms.add(term);
        }
        Collections.sort(terms);
        Collections.reverse(terms);
    }

    /**
     *  Write a ranked list of terms (top-ranked term first) to file
     *  <CODE>outputFileName</CODE>.
     */

    public void writeRankedList(String outputFileName) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(outputFileName));
        for (Term term : terms) {
            pw.println(term);
        }
        pw.close();
    }

    /**
     *  Rank terms using term count files <CODE>foregroundCountFile</CODE> and
     *  <CODE>backgroundCountFile</CODE>, writing result to <CODE>outputFile</CODE>
     *  and returning a ranked list.
     */

    public static List<Term> rankTerms(String foregroundCountFile,
                                       String backgroundCountFile,
                                       String outputFile) throws IOException {
        TermRanker ranker = new TermRanker(foregroundCountFile, backgroundCountFile);
        ranker.writeRankedList(outputFile);
        return ranker.terms;
    }
}
