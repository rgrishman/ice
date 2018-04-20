package edu.nyu.jet.ice.models;

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

    public void generalizeLeft () {
	generalize(0);
    }

    public void generalizeRight () {
	generalize(2);
    }

    private void generalize (int n) {
	// update path
	String[] splitPath = path.split("--");
	if (splitPath.length != 3) return;
	int argPosn = n;
	String arg = splitPath[argPosn];
	int i = arg.indexOf("/");
	if (i < 0) return;
	arg = arg.substring(0, i);
	splitPath[argPosn] = arg;
	path = splitPath[0] + " -- " + splitPath[1] + " -- " +  splitPath[2];
	// update repr
	String[] splitRepr = repr.split(" ");
	if (splitRepr.length < 3) return;
	argPosn = -1;
	if (n == 0) {
	    for (int x = 0; x < splitRepr.length; x++) {
		if (allCaps(splitRepr[x])) {
		    argPosn = x;
		    break;
		}
	    }
	} else {
	    for (int x = splitRepr.length - 1; x >= 0; x--) {
		if (allCaps(splitRepr[x])) {
		    argPosn = x;
		    break;
		}
	    }
	}
	if (argPosn < 0) return;
	arg = splitRepr[argPosn];
	int j = arg.indexOf("/");
	if (j<0) return;
	arg = arg.substring(0, j);
	splitRepr[argPosn] = arg;
	repr = splitRepr[0];
	for (int k = 1; k < splitRepr.length ; k++)
	   repr += " " + splitRepr[k]; 
	DepPathMap depPathMap = DepPathMap.getInstance();
	depPathMap.setMaps (path, repr, example);
    }

    private boolean allCaps (String s) {
	return s.equals(s.toUpperCase());
    }
}


