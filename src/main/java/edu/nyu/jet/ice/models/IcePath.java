package edu.nyu.jet.ice.models;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;
import java.util.*;

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

    static final Logger logger = LoggerFactory.getLogger(IcePath.class);

    public enum IcePathChoice {
        NO, YES, UNDECIDED
    }
    private String path;
    private String repr;
    private String example;
    private double score;
    public  TObjectDoubleHashMap subScores;
    private IcePathChoice choice;

    public IcePath() {
        this.path = "?";
    }

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

    public String getBarePath () {
        String[] pp = getPathString().split("--");
        if (pp.length != 3) {
            logger.error ("Attempting to add invalid path {} to relation", path);
            return null;
        }
        return pp[1].trim();
    }

/*
    public IcePath getFullPath (String arg1, String arg2) {
        if (path.indexOf(" -- ") >= 0) {
            return this;
        } else {
            String fullPath = arg1 + " -- " + path + " -- " + arg2;
            IcePath fullIp = IcePathFactory.getIcePath(fullPath);
            return fullIp;
        }
    }
*/
    public void setPath(String path) {
        this.path = path;
    }

    public String getRepr() {
        if (repr == null)
            return "nullRepr";
            else
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

    public void setScore(double score) {
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
   public boolean equals (Object other) {
       boolean result = false;
       if (other instanceof IcePath) {
           IcePath icePath = (IcePath) other;
           result = this.getPathString().equals(icePath.getPathString());
       }
       return result;
   }
    public String toString() {
        return path + " ["+repr+"] ";
    }

     public IcePath(AnchoredPath ap) {
         path = ap.toString();
     }

     /**
      *  Returns the embedding of the IcePath, which is the
      *  embedding of its English-like phrase (repr).
      */

    public double[] embed () {
        return WordEmbedding.embed(getRepr().split(" "));
    }

    /**
     *  Returns the embedding of a list of IcePaths,
     *  which is the component-by-component sum of 
     *  the embeddings of the constituent IcePaths.
     */

    static public double[] embed (List<IcePath> paths) {
        int dim = WordEmbedding.getDim();
        for (IcePath ip : paths)
            ip.embed();
        double[] result = new double[dim];
        for (int j=0; j < dim; j++) {
            result[j] = paths.get(0).embed()[j];
        }
        for (int i=1; i < paths.size(); i++) {
            for (int j=0; j < dim; j++) {
                result[j] += paths.get(i).embed()[j];
            }
        }
        return result;
    }

}
