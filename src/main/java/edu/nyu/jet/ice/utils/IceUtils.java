package edu.nyu.jet.ice.utils;

import edu.nyu.jet.aceJet.AceDocument;
import edu.nyu.jet.aceJet.AceEntityMention;
import edu.nyu.jet.aceJet.AceRelation;
import edu.nyu.jet.aceJet.AceRelationMention;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.parser.DepParser;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.Span;
import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.uicomps.Ice;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class IceUtils {

    public static AceRelationMention findRelation(AceDocument aceDoc,
                                                  AceEntityMention m1,
                                                  AceEntityMention m2) {
        for (AceRelation relation : aceDoc.relations) {
            for (Object rmObj : relation.mentions) {
                AceRelationMention rm  = (AceRelationMention)rmObj;
                if (rm.arg1.equals(m1) && rm.arg2.equals(m2) ||
                        rm.arg1.equals(m2) && rm.arg2.equals(m1)) {
                    return rm;
                }
            }
        }
        return null;
    }

    public static boolean matchSpan(int posn, List<Span> spans) {
        for (Span span : spans) {
            if (span.start() == posn) {
                return true;
            }
        }
        return false;
    }

    public static String propertyKeyForValue(Properties props, String val) {
        for (Object k : props.keySet()) {
            String ks = (String)k;
            if (props.getProperty(ks).equals(val)) {
                return ks;
            }
        }
        return null;
    }

    public static String findAvailablePropertyKey(Properties props, String prefix) {
        for (int i = 1; i <= 256; i++) {
            if (props.getProperty(prefix + i) == null) {
                return prefix + i;
            }
        }
        return null;
    }

    public static String[] findValuesWithPrefix(Properties props, String prefix) {
        List<String> buf = new ArrayList<String>();
        for (int i = 1; i <= 256; i++) {
            if (props.getProperty(prefix + i) != null) {
                buf.add(props.getProperty(prefix + i));
            }
        }
        return buf.toArray(new String[buf.size()]);
    }

    public static void appendFile(String fileName, String content) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
        out.println(content);
        out.close();
    }

    public static void numsort(String inFileName, String outFileName) throws IOException{
        List<NumString> l = new ArrayList<NumString>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(inFileName));
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("\\s");
            double num = 0.0;
            try {
                num = Double.valueOf(parts[0]);
            }
            catch(Exception e) {
                e.printStackTrace();
                continue;
            }
            NumString ns = new NumString(num, line);
            l.add(ns);
        }
        br.close();
        Collections.sort(l);
        PrintWriter pw = new PrintWriter(new FileWriter(outFileName));
        for (NumString ns : l) {
            pw.println(ns);
        }
        pw.close();
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void annotateTerms(Document doc) {
        List<Annotation> onomas = doc.annotationsOfType("onoma");
        if (onomas == null) return;
        for (Annotation onoma : onomas) {
            if (onoma.get("type") != null &&
                    onoma.get("type").equals("term")) {
                doc.addAnnotation(new Annotation("TERM",
                        onoma.span(),
                        new FeatureSet("type", "topic_term")));
            }
        }
    }

    public static String[] readLines(String filename) throws IOException {
        List<String> result = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            result.add(strLine);
        }
        br.close();
        return result.toArray(new String[result.size()]);
    }

    public static void writeLines(String filename, String[] lines) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        for (String line : lines) {
            pw.println(line.trim());
        }
        pw.close();
    }

    public static String readFileAsString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            fileData.append(buf, 0, numRead);
        }
        reader.close();
        return fileData.toString();
    }

    public static SyntacticRelationSet debugParseDocument(Document doc) {
        Vector<Annotation> sentences = doc.annotationsOfType("sentence");
        if (sentences == null || sentences.size() == 0) {
            System.out.println ("DepParser:  no sentences");
            return null;
        }
        SyntacticRelationSet relations = new SyntacticRelationSet();
        for (Annotation sentence : sentences) {
            System.err.println(doc.text(sentence).replace("\\s+", " ").trim());
            Span span = sentence.span();
            DepParser.parseSentence(doc, span, relations);
        }
        return relations;
    }

    public static String splitIntoLine(String input, int maxCharInLine){

        StringTokenizer tok = new StringTokenizer(input, " ");
        StringBuilder output = new StringBuilder(input.length());
        int lineLen = 0;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            while(word.length() > maxCharInLine){
                output.append(word.substring(0, maxCharInLine-lineLen) + "\n");
                word = word.substring(maxCharInLine-lineLen);
                lineLen = 0;
            }

            if (lineLen + word.length() > maxCharInLine) {
                output.append("\n");
                lineLen = 0;
            }
            output.append(word + " ");

            lineLen += word.length() + 1;
        }
        // output.split();
        // return output.toString();
        return output.toString() + "\n";
    }


    public static double innerProduct(double[] v1, double[] v2) {
        double innerProduct = 0.00000001;
        for (int i = 0; i < v1.length; i++) {
            innerProduct += v1[i] * v2[i];
        }
        return innerProduct;
    }

    public static void l2norm(double[] v) {
        double l2sum = 0;
        for (int i = 0; i < v.length; i++) {
            l2sum += v[i] * v[i];
        }
        l2sum = Math.sqrt(l2sum);
        for (int i = 0; i < v.length; i++) {
            v[i] /= l2sum;
        }
    }

    /**
     *
     * @return The number of corpora that have been preprocssed and run word count
     */
    public static int numOfWordCountedCorpora() {
        int count = 0;
        for (Corpus corpus : Ice.corpora.values()) {
            if (corpus.wordCountFileName != null &&
                    (corpus.relationTypeFileName != null &&
                            !corpus.relationTypeFileName.trim().equals("~"))) {
                count++;
            }
        }
        return count;
    }

    /**
     * load a text file in format COUNT\tSTRING
     * @param countFile the count file
     * @return
     */
    public static TObjectIntHashMap loadCountFile(String countFile) throws IOException {
        TObjectIntHashMap result = new TObjectIntHashMap();
        File f = new File(countFile);
        if (!f.exists()) {
            System.err.print(countFile + " does not exist.");
            return result;
        }
        BufferedReader br = new BufferedReader(new FileReader(countFile));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            if (parts.length == 2) {
                int count = Integer.valueOf(parts[0]);
                String content = parts[1].trim();
                result.put(content, count);
            }
        }
        br.close();
        return result;
    }


}

class NumString implements Comparable<NumString> {
    double num;
    String string;

    NumString(double num, String string) {
        this.num = num;
        this.string = string;
    }


    public int compareTo(NumString that) {
        if (this.num < that.num) return 1;
        if (this.num > that.num) return -1;
        return 0;
    }

    @Override
    public String toString() {
        return string;
    }
}
