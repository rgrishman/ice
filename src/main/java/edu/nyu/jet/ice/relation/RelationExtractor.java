package edu.nyu.jet.ice.relation;

import AceJet.*;
import Jet.Control;
import edu.nyu.jet.ice.models.DepPaths;
import edu.nyu.jet.ice.utils.IceUtils;
import Jet.JetTest;
import Jet.Parser.DepParser;
import Jet.Parser.SyntacticRelationSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.io.GISModelReader;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.FileEventStream;

import java.io.*;
import java.util.*;

/**
 * Created by yhe on 8/31/14.
 */
public class RelationExtractor {
    private List<AceDocument> trainDocs = new ArrayList<AceDocument>();
    protected Map<String, String> sgmIdx  = new TreeMap<String, String>();
    private List<Event> trainEvents;
    private RelationFeatureExtractor featureExtractor = new DepPathSameConstitsFeatureExtractor();
    protected GISModel model;

    public RelationExtractor() {

    }



    public void loadFiles(String propsFile,
                             String trainFileList) throws IOException {
        sgmIdx.clear();
        AceDocument.ace2004 = false;
        AceDocument.ace2005 = true;
        JetTest.initializeFromConfig(propsFile);
        BufferedReader r = new BufferedReader(new FileReader(trainFileList));
        String l = null;
        while ((l = r.readLine()) != null) {
            String fileName = l;
            AceDocument aceDocument = new AceDocument("training/" + fileName + ".sgm",
                    "training/" + fileName + ".apf.xml");
            trainDocs.add(aceDocument);
            sgmIdx.put(aceDocument.sourceFile,
                    "training/" + fileName + ".sgm");
        }
        r.close();



        trainEvents = new ArrayList<Event>();
        for (AceDocument doc : trainDocs) {
            addTrainingDocument(doc);
        }



    }

    public void writeFeatureFile(String featureFileName) throws IOException {
        List<Event> events = trainEvents;
        PrintWriter p = new PrintWriter(new FileWriter(featureFileName));
        for (Event e : events) {
            p.println(FileEventStream.toLine(e).replaceAll("\\n", " ").trim());
        }
        p.close();
    }

    public void addTrainingDocument(AceDocument aceDoc) {
        try {
            ExternalDocument doc = new ExternalDocument("sgml", sgmIdx.get(aceDoc.sourceFile));
            doc.setAllTags(true);
            doc.open();
            Control.processDocument(doc, null, false, 1);
            SyntacticRelationSet depPaths = DepParser.parseDocument(doc);
            depPaths.addInverses();
            collectEvents(doc, aceDoc, depPaths, trainEvents);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void collectEvents (Document doc, AceDocument aceDoc, SyntacticRelationSet relations, List<Event> collection) {
        List<Annotation> jetSentences = doc.annotationsOfType("sentence");
        if (jetSentences == null) return;
        Set<AceEntityMention> allMentions = new HashSet<AceEntityMention>();
        for (AceEntity entity : aceDoc.entities) {
            for (AceEntityMention mention : entity.mentions) {
                allMentions.add(mention);
            }
        }

        int sentCount = 0;
        for (Annotation sentence : jetSentences) {
            sentCount++;

            List<AceEntityMention> localMentions = new ArrayList<AceEntityMention>();
            for (AceEntityMention mention : allMentions) {
                if (mentionInSentence(mention, sentence)) {
                    localMentions.add(mention);
                }
            }
//            System.err.println("Total mentions in sentence:" + localMentions.size());
            if (localMentions.size() > DepPaths.MAX_MENTIONS_IN_SENTENCE) {
                System.err.println("Too many mentions in one sentence. Skipped.");
                continue;
            }
            for (AceEntityMention mention1 : localMentions) {
                for (AceEntityMention mention2 : localMentions) {
                    if (mention1.entity == mention2.entity) continue;
//                    if (mention1.type.startsWith("PRO")) continue;
//                    if (mention2.type.startsWith("PRO")) continue;
                    int h1 = mention1.getJetHead().start();
                    int h2 = mention2.getJetHead().start();
                    // - mention1 precedes mention2
                    if (h1 >= h2) continue;
                    // - in same sentence
//                    if (!sentences.inSameSentence(h1, h2)) continue;
                    // find and record dep path from head of m1 to head of m2
                    //String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, relations, localMentions);
                    AceRelationMention trueMention = IceUtils.findRelation(aceDoc,
                            mention1,
                            mention2);
                    System.err.println("Mention 1:" + doc.text(mention1.jetExtent));
                    System.err.println("Mention 2:" + doc.text(mention2.jetExtent));
                    Event event =
                            featureExtractor.extractFeatures(mention1,
                                    mention2,
                                    trueMention,
                                    sentence,
                                    relations,
                                    localMentions,
                                    aceDoc,
                                    doc);
                    collection.add(event);
                    System.err.println(event);
                }
            }
        }
//        System.err.println("[TIME] " + new Date());
    }

    private static boolean mentionInSentence(AceEntityMention mention, Annotation sentence) {
        return mention.jetExtent.start() >= sentence.start() &&
                mention.jetExtent.end() <= sentence.end();
    }

    public void loadModel(String modelFile) throws IOException {
        model = (GISModel)(new GISModelReader(new File(modelFile))).getModel();
    }

    public String predict(Event e) {
        return model.getBestOutcome(model.eval(e.getContext()));
    }

    public void setFeatureExtractor(RelationFeatureExtractor featureExtractor) {
        this.featureExtractor = featureExtractor;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Train relation extraction model");
            System.err.println("Jet.RelationAL.DepPathRelationExtractor " +
                    "propsFile trainFileList featureFile modelFile");
            System.exit(-1);
        }
        try {
            RelationExtractor extractor = new RelationExtractor();
            extractor.loadFiles(args[0], args[1]);
            String featureFile = args[2];
            String modelFile   = args[3];
            extractor.writeFeatureFile(featureFile);
            EventStream es = new FileEventStream(new File(featureFile));
            GISModel model = GIS.trainL2Model(es, 0, 2);
            File outputFile = new File(modelFile);
            GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
            writer.persist();
//            extractor.loadModel(modelFile);
//            for (Event testEvent : extractor.testEvents) {
//                String goldLabel = testEvent.getOutcome();
//                String predictedLabel = extractor.predict(testEvent);
//                extractor.scorer.addEvent(goldLabel, predictedLabel);
//            }
//            extractor.scorer.print();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

