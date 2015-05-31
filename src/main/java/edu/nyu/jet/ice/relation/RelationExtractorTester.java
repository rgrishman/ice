package edu.nyu.jet.ice.relation;

import AceJet.AceDocument;
import Jet.Control;
import Jet.JetTest;
import Jet.Parser.DepParser;
import Jet.Parser.SyntacticRelationSet;
import Jet.Tipster.ExternalDocument;
import opennlp.model.Event;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class RelationExtractorTester extends RelationExtractor {
    private List<AceDocument> testDocs  = new ArrayList<AceDocument>();
    public List<Event> testEvents;
    public RelationExtractorScorer scorer = new RelationExtractorScorer();

    public RelationExtractorTester() {

    }

    public void loadTestEvents(String propsFile,
                               String modelFile,
                               String txtFileList,
                               String apfFileList,
                               RelationFeatureExtractor featureExtractor) throws  IOException {
        if (featureExtractor != null) {
            this.setFeatureExtractor(featureExtractor);
        }

        sgmIdx.clear();
        AceDocument.ace2004 = true;
        AceDocument.ace2005 = true;
        JetTest.initializeFromConfig(propsFile);

        BufferedReader r = new BufferedReader(new FileReader(txtFileList));
        BufferedReader apfr = new BufferedReader(new FileReader(apfFileList));

        String l = null;
        while ((l = r.readLine()) != null) {
            String fileName = l;
            String apfFileName = apfr.readLine();
            AceDocument aceDocument = new AceDocument(fileName, apfFileName);
            testDocs.add(aceDocument);
            sgmIdx.put(aceDocument.sourceFile, fileName);
        }
        r.close();
        apfr.close();
        System.err.println("AceDocuments loaded.");
        testEvents  = new ArrayList<Event>();
        for (AceDocument doc : testDocs) {
            addTestingDocument(doc);
        }
        System.err.println("AceDocuments Analyzed.");
        if (modelFile != null) {
            loadModel(modelFile);
        }
    }

    public RelationExtractorTester(String propsFile,
                             String modelFile,
                             String testFileList,
                             RelationFeatureExtractor featureExtractor) throws IOException {
        super();
        if (featureExtractor != null) {
            this.setFeatureExtractor(featureExtractor);
        }

        sgmIdx.clear();
        AceDocument.ace2004 = false;
        AceDocument.ace2005 = true;
        JetTest.initializeFromConfig(propsFile);

        BufferedReader r = new BufferedReader(new FileReader(testFileList));
        String l = null;
        while ((l = r.readLine()) != null) {
            String fileName = l;
            AceDocument aceDocument = new AceDocument("training/" + fileName,
                    "training/" + fileName.substring(0, fileName.length()-4) + ".apf.xml");
            testDocs.add(aceDocument);
            sgmIdx.put(aceDocument.sourceFile,
                    "training/" + fileName);
        }
        r.close();
        System.err.println("AceDocuments loaded.");
        testEvents  = new ArrayList<Event>();
        for (AceDocument doc : testDocs) {
            addTestingDocument(doc);
        }
        System.err.println("AceDocuments Analyzed.");
        if (modelFile != null) {
            loadModel(modelFile);
        }
    }

    public void addTestingDocument(AceDocument aceDoc) {
        try {
            System.err.println("Document: " + aceDoc.sourceFile);
            ExternalDocument doc = new ExternalDocument("sgml", sgmIdx.get(aceDoc.sourceFile));
            doc.setAllTags(true);
            doc.open();
            Control.processDocument(doc, null, false, 1);
            SyntacticRelationSet depPaths = DepParser.parseDocument(doc);
            depPaths.addInverses();
            collectEvents(doc, aceDoc, depPaths, testEvents);
            System.err.println("Finished processing document: " + aceDoc.sourceFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Jet.RelationAL.RelationExtractorTester propsFile modelFile testFileList");
            System.exit(-1);
        }
        String propsFile = args[0];
        String modelFile = args[1];
        String testFileList = args[2];
        try {
            RelationExtractorTester tester =
                    new RelationExtractorTester(propsFile, modelFile, testFileList, null);
            System.err.println("Initialization finished.");
            for (Event testEvent : tester.testEvents) {
                String goldLabel = testEvent.getOutcome();
                String predictedLabel = tester.predict(testEvent);
                tester.scorer.addEvent(goldLabel, predictedLabel);
                System.err.println("Gold:" + goldLabel +
                        "\tPredicted:" + predictedLabel);
            }
            tester.scorer.print();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }



}
