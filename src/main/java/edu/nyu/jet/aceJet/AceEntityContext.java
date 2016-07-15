package edu.nyu.jet.aceJet;

import edu.nyu.jet.Control;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;
import edu.nyu.jet.tipster.Span;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.utils.IceUtils;
import opennlp.model.Event;

import java.io.*;
import java.util.*;

/**
 * Generate dep context vectors for all entities
 *
 * @author yhe
 * @version 1.0
 */
public class AceEntityContext {

    private String[] stopWordsArr = new String[]{
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    };
    private Set<String> stopWords = new HashSet<String>();
    {
        for (String word : stopWordsArr) {
            stopWords.add(word);
        }
    }
    private Stemmer stemmer = Stemmer.getDefaultStemmer();

    private String props;

    public AceEntityContext(String props) {
        super();
        this.props = props;
        JetTest.initializeFromConfig(props);
    }

    public void processDocument(String sourceFile, String aceFile, int docCount, PrintWriter pw) throws IOException {
        ExternalDocument doc = new ExternalDocument("sgml", sourceFile);
        doc.setAllTags(true);
        doc.open();
        Ace.monocase = Ace.allLowerCase(doc);
        Control.processDocument(doc, null, false, docCount);

        AceDocument aceDoc = new AceDocument(sourceFile, aceFile);
        for (AceEntity entity : aceDoc.entities) {
            for (AceEntityMention mention : entity.mentions) {
                int end = mention.head.end();
                if (doc.charAt(end) == '.') end--;

                String fullName = mention.type.equals("PRO") && entity.names.size() > 0 ?
                        entity.names.get(0).text.trim() :
                        doc.text(mention.jetHead);
                // mentionSpans.add(mention.jetHead);
                Annotation mentionSpanAnnotation = new Annotation("aceMentionHead",
                        mention.jetHead,
                        new FeatureSet("val", fullName));
                doc.addAnnotation(mentionSpanAnnotation);
                System.err.println(doc.text(mention.jetHead).replaceAll("\\s+", "_").trim() +
                    "=>" +
                    fullName);
            }
        }
        List<Span> mentionSpans = new ArrayList<Span>();
        doc.stretchAll();
        List<Annotation> aceMentionHeads = doc.annotationsOfType("aceMentionHead");
        for (Annotation mentionHead : aceMentionHeads) {
            mentionSpans.add(mentionHead.span());
        }
        List<Event> contexts = collectContext(doc, mentionSpans);
        for (Event event : contexts) {
            for (String context : event.getContext()) {
                pw.println(event.getOutcome().replaceAll("\\s+", "_").toLowerCase()
                                + " " + context.replaceAll("\\s+", "_").toLowerCase()
                );
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("AceEntityContext props trainingDir docList outputFileName");
            System.exit(-1);
        }
        String propsFile   = args[0];
        String trainingDir = args[1];
        String docListFile = args[2];
        String outputFileName = args[3];
        String[] trainingFiles = IceUtils.readLines(docListFile);
        AceEntityContext aceEntityContext = new AceEntityContext(propsFile);
        PrintWriter pw = new PrintWriter(new FileWriter(outputFileName));
        for (int i = 0; i < trainingFiles.length; i++) {
            String sgmFile = trainingDir + File.separator + trainingFiles[i] + ".sgm";
            String apfFile = trainingDir + File.separator + trainingFiles[i] + ".apf.xml";
            aceEntityContext.processDocument(sgmFile, apfFile, i+1, pw);
        }
        pw.close();
    }

    private List<Event> collectContext(Document doc, List<Span> terms) {
        SyntacticRelationSet s = doc.relations;
        //List<Annotation> terms = doc.annotationsOfType("term", sentence.span());
        if (terms == null) return new ArrayList<Event>();
        List<Event> result = new ArrayList<Event>();
        int missingHead = 0;
        for (int i = 0; i < terms.size(); i++) {
            String tokenString = doc.text(terms.get(i)).replaceAll("\\s+", " ").trim();
            Annotation termHead = IcePreprocessor.findTermHead(doc, terms.get(i), s);
                List<String> contextList = new ArrayList<String>();
            if (termHead == null) {
                missingHead++;
                continue;
            }
                SyntacticRelation syn = s.getRelationTo(termHead.start());
                if (syn != null) {
                    if (!stopWords.contains(syn.sourceWord.trim())) {
//                        contextList.add("wordFrom=" + stemmer.getStem(syn.sourceWord, "NULL"));
                        contextList.add(String.format("%s-1_%s", syn.type, stemmer.getStem(syn.sourceWord.trim(), "NULL")));
                    }
//                    contextList.add("wordFromPOS=" + syn.sourcePos);
//                    contextList.add("fromRelationName=" + syn.type);
                }
//                List<Annotation>  posCands = doc.annotationsAt(termHead.start(), "tagger");
//                if (posCands != null) {
//                    String pos = (String)posCands.get(0).get("cat");
//                    contextList.add("pos=" + pos);
//                }
                SyntacticRelationSet fromSet = s.getRelationsFrom(termHead.start());
                if (fromSet != null) {
                    for (int j = 0; j < fromSet.size(); j++) {
                        SyntacticRelation r = fromSet.get(j);
                        if (!stopWords.contains(r.targetWord.trim())) {
//                            contextList.add("wordTo=" + stemmer.getStem(r.targetWord, "NULL"));
                            contextList.add(String.format("%s_%s", r.type, stemmer.getStem(r.targetWord.trim(), "NULL")));
                        }
//                        contextList.add("wordToPOS=" + r.targetPos);
//                        contextList.add("toRelationName=" + r.type);
                    }
                }
                Event event = new Event(tokenString,
                        contextList.toArray(new String[contextList.size()]));
                if (contextList.size() > 0) {
                    result.add(event);
                }

        }
        System.err.println("Missing head:" + missingHead);
        return result;
    }


}
