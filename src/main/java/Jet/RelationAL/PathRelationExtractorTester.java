package Jet.RelationAL;

import opennlp.model.Event;

/**
 * Created by yhe on 2/26/15.
 */
public class PathRelationExtractorTester {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Jet.RelationAL.RelationExtractorTester propsFile ruleFile txtFileList apfFileList");
            System.exit(-1);
        }
        String propsFile = args[0];
        String ruleFile = args[1];
        String txtFileList = args[2];
        String apfFileList = args[3];
        try {
            RelationExtractorTester tester = new RelationExtractorTester();
            tester.loadTestEvents(
                    propsFile, null, txtFileList, apfFileList, new DepPathTypeFeatureExtractor());
            PathRelationExtractor pathRelationExtractor = new PathRelationExtractor();
            pathRelationExtractor.loadRules(ruleFile);
            System.err.println("Initialization finished.");
            for (Event testEvent : tester.testEvents) {
                String goldLabel = testEvent.getOutcome();
                String predictedLabel = pathRelationExtractor.predict(testEvent);
                tester.scorer.addEvent(goldLabel, predictedLabel);
//                System.err.println("Gold:" + goldLabel +
//                        "\tPredicted:" + predictedLabel);
            }
            tester.scorer.print();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
