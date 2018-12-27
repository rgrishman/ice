package edu.nyu.jet.ice.models;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;

import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 *  A path in relation bootstrapping.  ICE generates a ranked list of <code>IcePath</code>s.
 *  These are presented to the user, who can label each as being or not being an
 *  instance of the curent relation.  Each <code>IcePath</code> includes a lexicalized
 *  dependency path ('path'), an English phrase for that path ('repr'), a full-sentence
 *  example of that path ('example'), and a score.  The path includes arguments.
 *
 * @author yhe
 * @version 1.0
 */

public class IcePath implements Comparable<IcePath> {

    Logger logger = LoggerFactory.getLogger(IcePath.class);

    public enum IcePathChoice {
        NO, YES, UNDECIDED
    }
    private String path;
    private String repr;
    private String example;
    private double score;
    public  TObjectDoubleHashMap subScores;
    private IcePathChoice choice;

    public IcePath (String path) {
        this.path = path;
    }

    public IcePath(String path, String repr, String example, double score, IcePathChoice choice) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = choice;
	logger.debug ("created IcePath with path {}", path);
    }

    public IcePath(String path, String repr, String example, double score) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = IcePathChoice.UNDECIDED;
	logger.debug ("created IcePath with path {}", path);
    }

    public IcePath(String path, String repr, String example, double score, TObjectDoubleHashMap subScores) {
        this.path = path;
        this.repr = repr;
        this.example = example;
        this.score = score;
        this.choice = IcePathChoice.UNDECIDED;
        this.subScores = subScores;
	logger.debug ("created IcePath with path {}", path);
    }

    public String getPath() {
        return path;
    }

    public String getPathString() {
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

    public String toString() {
        return path+"["+repr+"]";
    }

     public IcePath(AnchoredPath ap) {
         path = ap.toString();
     }
}

