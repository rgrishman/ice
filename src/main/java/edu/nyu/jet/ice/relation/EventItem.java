package edu.nyu.jet.ice.relation;

import java.util.*;

/**
 * EventItem is a wrapper around the OpenNLP Event class (feature+label for a training instance), to simplify training
 * of supervised relation extraction models
 *
 * Not used in ICE GUI/CLI.
 *
 * @author yhe
 * @version 1.0
 */
public class EventItem implements Comparable<EventItem> {
    public static final String NOT_RELATION_LABEL = "NOT_RELATION";
    public static final String UNKNOWN_LABEL      = "UNDECIDED";
    private String sentence;
    private String path;
    private String type1;
    private String type2;
    private boolean sameNP;
    private List<String> wordsInBetween;
    private String outcome;
    private String predictedOutcome;
    private double score;

    public double getUncertainty() {
        return uncertainty;
    }

    private double uncertainty;

    public EventItem(String sentence, String path, String type1, String type2, boolean sameNP, List<String> wordsInBetween) {
        this.sentence = sentence;
        this.path = path;
        this.type1 = type1;
        this.type2 = type2;
        this.sameNP = sameNP;
        this.wordsInBetween = wordsInBetween;
        this.outcome = UNKNOWN_LABEL;
    }

    public String[] context() {
        List<String> contextList = new ArrayList<String>();
        contextList.add("PATH=" + path.trim().replaceAll("\\s+", "_"));
        contextList.add("sameNP=" + sameNP);
        for (String w : wordsInBetween) {
            contextList.add("wordInBetween=" + w.trim().replaceAll("\\s+", "_"));
        }
        return contextList.toArray(new String[contextList.size()]);
    }

    public static EventItem fromLine(String line) {
        String[] parts = line.trim().split("\\|\\|\\|");
        System.err.println(line);
        System.err.println(parts.length);
        String sentence = parts[0];
        String path     = parts[1];
        String types    = parts[2];
        String sameNP   = parts[3];
        String wordsInSentence = parts[4];
        String[] wordsInBetweenArr  = parts.length == 6 ? parts[5].trim().split(" ") : new String[]{};
        String type1 = types.trim().split("\\+\\+\\+")[0];
        String type2 = types.trim().split("\\+\\+\\+")[1];
        boolean sameNPBool = Boolean.valueOf(sameNP);
        Set<String> wordsInBetweenSet = new TreeSet<String>();
        for (String wordInBetween : wordsInBetweenArr) {
            wordsInBetweenSet.add(wordInBetween);
        }
        List<String> wordsInBetween = new ArrayList(wordsInBetweenSet);
        return new EventItem(sentence.trim(), path.trim(), type1.trim(), type2.trim(), sameNPBool, wordsInBetween);
    }

    public boolean sameTypesAs(String type1, String type2) {
        return (this.type1.equals(type1) && this.type2.equals(type2)) ||
                (this.type1.equals(type2) && this.type2.equals(type1));
    }

    public boolean outcomeUNK() {
        return outcome.equals(UNKNOWN_LABEL);
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType1() {
        return type1;
    }

    public void setType1(String type1) {
        this.type1 = type1;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String type2) {
        this.type2 = type2;
    }

    public boolean isSameNP() {
        return sameNP;
    }

    public void setSameNP(boolean sameNP) {
        this.sameNP = sameNP;
    }

    public List<String> getWordsInBetween() {
        return wordsInBetween;
    }

    public void setWordsInBetween(List<String> wordsInBetween) {
        this.wordsInBetween = wordsInBetween;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @Override
    public String toString() {
        return sentence.trim().replaceAll("\\s+", " ");
    }


    public int compareTo(EventItem eventItem) {
        if (this.uncertainty - eventItem.uncertainty < 0) {
            return -1;
        }
        else if (this.uncertainty - eventItem.uncertainty > 0) {
            return 1;
        }
        else {
            return 0;
        }
    }

    public String getPredictedOutcome() {
        return predictedOutcome;
    }

    public void setPredictedOutcome(String predictedOutcome) {
        this.predictedOutcome = predictedOutcome;
    }

    public void setScore(double score) {
        this.score = score;
        this.uncertainty = Math.abs(0.5 - score);
    }

    public double getScore() {
        return score;
    }
}
