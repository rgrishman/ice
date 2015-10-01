package edu.nyu.jet.ice.terminology;

/**
 * Summarizes information related to a term/entity, also supports sorting
 *
 * @author yhe
 * @version 1.0
 */
public class Term implements Comparable<Term>{
    private String text;
    private int positiveDocFreq;
    private int positiveFreq;
    private int negativeDocFreq;
    private int negativeFreq;
    private double score;
    private int[] rawFreq;

    public Term(String text, int positiveDocFreq, int positiveFreq, int negativeDocFreq, int negativeFreq) {
        this.text = text;
        this.positiveDocFreq = positiveDocFreq;
        this.positiveFreq = positiveFreq;
        this.negativeDocFreq = negativeDocFreq;
        this.negativeFreq = negativeFreq;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPositiveDocFreq() {
        return positiveDocFreq;
    }


    public int getPositiveFreq() {
        return positiveFreq;
    }


    public int getNegativeDocFreq() {
        return negativeDocFreq;
    }


    public int getNegativeFreq() {
        return negativeFreq;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int[] getRawFreq() {
        return rawFreq;
    }

    public void setRawFreq(int[] rawFreq) {
        this.rawFreq = rawFreq;
    }

    public int compareTo(Term term) {
        if (this.score - term.score < 0) return -1;
        if (this.score - term.score > 0) return 1;
        return 0;
    }

    @Override
    public String toString() {
        return String.format("%.2f\t%s", score, text);
    }
}
