package Jet.RelationAL;

import gnu.trove.TObjectIntHashMap;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class RelationExtractorScorer {
    TObjectIntHashMap<String> correctCounter = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> proposedCounter = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> goldCounter = new TObjectIntHashMap<String>();

    public void init() {
        correctCounter = new TObjectIntHashMap<String>();
        proposedCounter = new TObjectIntHashMap<String>();
        goldCounter = new TObjectIntHashMap<String>();
    }

    public void addEvent(String goldLabel, String predictedLabel) {
        if (!goldLabel.equals("NONE")) {
            goldCounter.put(goldLabel, goldCounter.get(goldLabel) + 1);
        }
        if (!predictedLabel.equals("NONE")) {
            proposedCounter.put(predictedLabel, proposedCounter.get(predictedLabel) + 1);
        }
        if (!predictedLabel.equals("NONE") && predictedLabel.equals(goldLabel)) {
            correctCounter.put(predictedLabel, correctCounter.get(predictedLabel) + 1);
        }
    }

    public double precision(String label) {
        return (double)correctCounter.get(label)/proposedCounter.get(label);
    }

    public double recall(String label) {
        return (double)correctCounter.get(label)/goldCounter.get(label);
    }

    public double f1(String label) {
        double p = precision(label);
        double r = recall(label);
        return 2*p*r/(p+r);
    }

    public double precision() {
        int correct = 0;
        int proposed = 0;
        for (Object labelObj : goldCounter.keys()) {
            String label = (String)labelObj;
            correct += correctCounter.get(label);
            proposed += proposedCounter.get(label);
        }
        return (double)correct/proposed;
    }

    public double recall() {
        int correct = 0;
        int gold = 0;
        for (Object labelObj : goldCounter.keys()) {
            String label = (String)labelObj;
            correct += correctCounter.get(label);
            gold += goldCounter.get(label);
        }
        return (double)correct/gold;
    }

    public double f1() {
        double p = precision();
        double r = recall();
        return 2*p*r/(p+r);
    }

    public void print() {
        for (Object labelObj : goldCounter.keys()) {
            String label = (String)labelObj;
            System.out.println(String.format("%s\tP:%.4f\tR:%.4f\tF1:%.4f\tGC:%d\tPC:%d",
                    label,
                    precision(label),
                    recall(label),
                    f1(label),
                    correctCounter.get(label),
                    proposedCounter.get(label)));
        }
        System.out.println(String.format("OVERALL\tP:%.4f\tR:%.4f\tF1:%.4f",
                precision(),
                recall(),
                f1()));
    }

}
