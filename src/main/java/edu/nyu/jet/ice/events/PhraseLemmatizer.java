package edu.nyu.jet.ice.events;
 
import java.io.*;
import java.util.*;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.lex.Tokenizer;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.hmm.HMMTagger;

public class PhraseLemmatizer {

    public static void main (String[] args) throws IOException {
         JetTest.tagger = new HMMTagger();
         JetTest.tagger.load("../jet/data/pos_hmm.txt");
        // reads line
        BufferedReader reader = new BufferedReader
            (new InputStreamReader (System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("x"))
                return;
            System.out.println("result :  " + lemmatize(line)); 
        }
    }

    public static String lemmatize (String phrase) {
        Stemmer stemmer = Stemmer.getDefaultStemmer();
        Document doc = new Document(phrase);
        Span span = doc.fullSpan();
        Tokenizer.tokenize (doc, span);
        Vector<Annotation> tokens = doc.annotationsOfType("token");
        StringBuilder sb = new StringBuilder();
        if (JetTest.tagger == null) {
            for (int i = 0; i < tokens.size(); i++) {
                String word = doc.text(tokens.get(i)).trim();
                String stem = stemmer.getStem (word, "?");
                sb.append(stem);
                sb.append(" ");
            }
        } else {
            JetTest.tagger.tagPenn (doc, span);
            Vector<Annotation> posvec = doc.annotationsOfType("constit");
            for (int i = 0; i < tokens.size(); i++) {
                String word = doc.text(tokens.get(i)).trim();
                String pos = (String) posvec.get(i).get("cat");
                String stem = stemmer.getStem (word, pos);
                sb.append(stem);
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
