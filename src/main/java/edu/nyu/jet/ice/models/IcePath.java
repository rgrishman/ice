package edu.nyu.jet.ice.models;

import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * A path in relation bootstrapping
 *
 * @author yhe
 * @version 1.0
 */
public class IcePath implements Comparable<IcePath> {
    public enum IcePathChoice {
        NO, YES, UNDECIDED
    }
    private String path;
    private String repr;
    private String example;
    private double score;
    public  TObjectDoubleHashMap subScores;
    private IcePathChoice choice;

    public IcePath(String path, String repr, String example, double score, IcePathChoice choice) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = choice;
    }

    public IcePath(String path, String repr, String example, double score) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = IcePathChoice.UNDECIDED;
    }

    public IcePath(String path, String repr, String example, double score, TObjectDoubleHashMap subScores) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = IcePathChoice.UNDECIDED;
        this.subScores = subScores;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRepr() {
        return repr;
    }

    public void setRepr(String repr) {
        this.repr = repr;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public double getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public IcePathChoice getChoice() {
        return choice;
    }

    public void setChoice(IcePathChoice choice) {
        this.choice = choice;
    }

    public int compareTo(IcePath icePath) {
        if (this.score < icePath.score) return 1;
        if (this.score > icePath.score) return -1;
        return 0;
    }

    @Override
    public String toString() {
        if (choice == IcePathChoice.UNDECIDED) {
        return repr;
        }
        else {
            if (choice == IcePathChoice.YES) {
                return repr + " / YES";
            }
            else {
                return repr + " / NO";
            }
        }
    }
}


