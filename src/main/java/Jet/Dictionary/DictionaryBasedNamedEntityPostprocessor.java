package Jet.Dictionary;

import Jet.Chunk.TokenClassifier;
import Jet.HMM.HMMNameTagger;
import Jet.HMM.HMMannotator;
import Jet.Lex.Lexicon;
import Jet.Lex.Tokenizer;
import Jet.Lisp.FeatureSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.Span;
import Jet.Zoner.SentenceSplitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * User: yhe
 * Date: 9/23/13
 * Time: 3:01 PM
 */
public class DictionaryBasedNamedEntityPostprocessor extends TokenClassifier {
    protected NamedEntityDictionary dictionary = null;
    protected HMMannotator annotator = null;
    protected Set<String> stopWords = null;
    protected String otherTyphe = "OTHER";
    protected String[] stopArr = new String[]{"mon", "tue", "wed", "thu", "fri", "sat", "sun",
            "monday", "tuesday", "thursday", "wednesday", "friday", "saturday", "sunday",
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "sept", "oct", "nov", "dec",
            "january", "february", "march", "april", "may", "june", "july", "august", "september",
            "october", "november", "december",
            "oh", "wow", "okay", "hello"};
    protected Set<String> personTokensCache = new HashSet<String>();
    protected Set<String> organizationTokensCache = new HashSet<String>();
    protected Set<String> gpeTokensCache = new HashSet<String>();
    protected Map<String, String> abbrCache = new HashMap<String, String>();
    protected Map<String, String> recognizedEntities = new HashMap<String, String>();

    public DictionaryBasedNamedEntityPostprocessor(String jetConfigFile) {
        Properties props = new Properties();
        try {
            props.load(new FileReader(jetConfigFile));
        }
        catch (IOException e) {
            System.err.println("Warning: Error reading config file...");
        }
        String dictFile = props.getProperty("Jet.dataPath") + "/" + props.getProperty("NEPostprocessor.DictFile");
        this.dictionary = new NamedEntityDictionary(dictFile);
        annotator = new HMMannotator(this);
        annotator.setBItag(true);
        annotator.setAnnotateEachToken(false);
        //JetTest.initializeFromConfig(jetConfigFile);

        String tagFile = props.getProperty("Jet.dataPath") + "/" + props.getProperty("Ace.TagFile");
        annotator.readTagTable(tagFile);
        stopWords = new HashSet<String>();
        stopWords = loadDictionary(props.getProperty("Jet.dataPath") + "/" +
                props.getProperty("English.Frequency"), 3000);
        for (String w :stopArr) {
            stopWords.add(w);
        }
    }

    private Set<String> loadDictionary(String fileName, int limit) {
        Set<String> dict = new HashSet<String>();
        try {
            int count = 0;
            BufferedReader r = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split("\t");
                if (++count > limit) break;
                if (Character.isLetter(parts[0].charAt(0))) {
                    dict.add(parts[0].toLowerCase());
                }
            }
            r.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return dict;
    }


    /**
     * tag document <CODE>doc</CODE> for named entities.
     */
    public void tagDocument(Document doc) {
        Vector sentences = doc.annotationsOfType("sentence");
        Iterator is = sentences.iterator();
        while (is.hasNext()) {
            Annotation sentence = (Annotation) is.next();
            Span sentenceSpan = sentence.span();
            tag(doc, sentenceSpan);
            tagUserTypes(doc, sentenceSpan);
        }
    }

    public void tag(Document doc, Span span) {
        annotator.annotateSpan(doc, span);
    }

    public void tagUserTypes(Document doc, Span span) {
        List<Annotation> tokens = doc.annotationsOfType("token", span);
        if (tokens == null) return;
        List<Annotation> nes    = doc.annotationsOfType("ENAMEX", span);
        if (nes == null) nes    = new ArrayList<Annotation>();
        for (Annotation token : tokens) {
            String userType = dictionary.getUserType(doc.text(token.span()).trim());
            if (userType != null) {
                for (Annotation ne : nes) {
                    if (ne.start() <= token.start() && ne.end() >= token.end()) {
                        doc.removeAnnotation(ne);
                    }
                    doc.addAnnotation(new Annotation("ENAMEX",
                            token.span(),
                            new FeatureSet("TYPE", userType.toUpperCase())));
                }
            }
        }
    }

    @Override
    public void train(Document document, Annotation[] annotations, String[] strings) {
        // not used
    }

    @Override
    public void createModel() {
        // not used
    }

    @Override
    public void store(String s) {
        // not used
    }

    @Override
    public void load(String s) {
        // not used
    }

    @Override
    public String[] viterbi(Document document, Annotation[] annotations) {
        StringBuilder builder = new StringBuilder();
        List<String> result = new ArrayList<String>();
        int i = 0;
        for (Annotation ann : annotations) {
            String word = document.text(ann).trim();
            if (!Character.isUpperCase(word.charAt(0))) {
                // dump builder
                boolean isOf = false;
                if (builder.length() > 0) {
                    if ((word.toLowerCase().equals("of") || word.toLowerCase().equals("for") || word.equals("-")) &&
                            ((i + 1 < annotations.length) &&
                                    Character.isUpperCase(document.text(annotations[i + 1]).trim().charAt(0)))) {
                        builder.append(word).append(" ");
                        isOf = true;
                    }
                    else {
                        translate2Tags(builder.toString().trim(), result);
                    }
                }
                if (!isOf) {
                    builder.setLength(0);
                    result.add("other");
                }
            }
            else {
                builder.append(word).append(' ');
            }
            i++;
        }
        if (builder.length() > 0) {
            translate2Tags(builder.toString().trim(), result);
        }

        String[] resultArr = result.toArray(new String[result.size()]);
        removeConflictingAnnotation(document, annotations, resultArr);
        return resultArr;
    }

    private void removeConflictingAnnotation(Document doc, Annotation[] tokens, String[] bioTags) {
        if (tokens.length != bioTags.length) return;
        for (int i = 0; i < tokens.length; i++) {
            if (!bioTags[i].toLowerCase().startsWith("o")) {
                List<Annotation> invalidStarts = doc.annotationsAt(tokens[i].start(), "ENAMEX");
                if (invalidStarts != null) {
                    for (Annotation invalidStart : invalidStarts) {
                        System.err.println("Removing inconsistent annotation:" + doc.text(invalidStart).replaceAll("\\s+", " ").trim());
                        doc.removeAnnotation(invalidStart);
                    }
                }
                List<Annotation> invalidEnds = doc.annotationsEndingAt(tokens[i].end(), "ENAMEX");
                if (invalidEnds != null) {
                    for (Annotation invalidEnd : invalidEnds) {
                        System.err.println("Removing inconsistent annotation:" + doc.text(invalidEnd).replaceAll("\\s+", " ").trim());
                        doc.removeAnnotation(invalidEnd);
                    }
                }
            }
        }
    }

    public void translate2Tags(String buf, List<String> result) {
        String[] bufArr = buf.split(" ");
        if (bufArr.length == 1) {

            String bufArr0 = bufArr[0].trim();
            if (bufArr0.length() == 1 || (bufArr0.length() == 2 && bufArr0.endsWith("."))) {
                result.add("other");
                return;
            }
            if (stopWords.contains(bufArr0.toLowerCase())) {
                result.add("other");
                return;
            }
            if (abbrCache.containsKey(bufArr0)) {
                result.add("B-" + abbrCache.get(bufArr0));
                System.err.println(bufArr0 + ":::" + abbrCache.get(bufArr0));
                return;
            }
            if (personTokensCache.contains(bufArr0)) {
                result.add("B-person");
                System.err.println(bufArr0 + ":::person");
                return;
            }
            if (gpeTokensCache.contains(bufArr0)) {
                result.add("B-gpe");
                System.err.println(bufArr0 + ":::person");
                return;
            }
            if (organizationTokensCache.contains(bufArr0)) {
                result.add("B-organization");
                System.err.println(bufArr0 + ":::organization");
                return;
            }
//            if (dictionary.isName(bufArr0) && ! dictionary.isGPE(bufArr0) && !dictionary.isOrganization(bufArr0)) {
//                result.add("B-person");
//                System.err.println(bufArr0 + ":::person");
//                return;
//            }
//            if (!dictionary.isName(bufArr0) && dictionary.isGPE(bufArr0) && !dictionary.isOrganization(bufArr0)) {
//                result.add("B-gpe");
//                System.err.println(bufArr0 + ":::gpe");
//                return;
//            }
//            if (!dictionary.isName(bufArr0) && !dictionary.isGPE(bufArr0) && dictionary.isOrganization(bufArr0)) {
//                result.add("B-organization");
//                System.err.println(bufArr0 + ":::organization");
//                return;
//            }
            result.add("other");
            return;
//            if (stopWords.contains(bufArr0.toLowerCase()) ||
//                    bufArr0.length() == 1) {
//                result.add("other");
//                return;
//            }
        }
        String type = dictionary.isPerson(buf) ? "person" :
                dictionary.isGPE(buf) ? "gpe" :
                        dictionary.isOrganization(buf) ? "organization": "UNK";

        if (type.equals("UNK") && isPerson(bufArr)) {
            type = "person";
        }
        for (String k : recognizedEntities.keySet()) {
            if (k.startsWith(buf)) {
                type = recognizedEntities.get(k);
            }
        }
        if (!type.equals("UNK") && bufArr.length > 1) {
            for (String w : bufArr) {
                if (type.equals("person")) {
                    personTokensCache.add(w);
                }
                if (type.equals("organization")) {
                    organizationTokensCache.add(w);
                }
                if (type.equals("gpe")) {
                    gpeTokensCache.add(w);
                }
                recognizedEntities.put(buf, type);
            }
            StringBuilder abbrBuilder = new StringBuilder();
            for (String w : bufArr) {
                abbrBuilder.append(w.charAt(0));
            }
            abbrCache.put(abbrBuilder.toString(), type);
        }
        if (!type.equals("UNK")) {
            System.err.println(buf + ":::" + type);
        }
        if (!type.equals("UNK")) {
            for (int i = 0; i < bufArr.length; i++) {
                if (i == 0) {
                    result.add("B-" + type.toLowerCase());
                }
                else {
                    result.add("I-" + type.toLowerCase());
                }
            }
        }
        else {
//            for (int i = 0; i < bufArr.length; i++) {
//                if (i == 0)
//                    result.add("other");
//                else
//                    result.add("other");
//            }
            result.add("other");
            if (bufArr.length > 1) {
                translate2Tags(buf.substring(buf.indexOf(" ") + 1), result);
            }
        }
    }

    public boolean isPerson(String[] pieces) {
        for (String piece : pieces) {
            if (!dictionary.isName(piece) ||
                    stopWords.contains(piece.trim().toLowerCase()))  return false;
        }
        return true;
    }


}
