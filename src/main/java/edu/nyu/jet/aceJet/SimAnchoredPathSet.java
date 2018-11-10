package edu.nyu.jet.aceJet;

import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Find similar paths. Unlike AnchorPathSet, which requires exact match between args,
 * this version uses PathMatcher or WordEmbeddings to find similar paths.
 *
 * @author yhe
 * @version 1.0
 */
public class SimAnchoredPathSet extends AnchoredPathSet {

    private PathMatcher matcher = null;
    private double threshold    = 0.3;
    private boolean useWE = false;

    public SimAnchoredPathSet(String fileName, PathMatcher matcher, double threshold)
            throws IOException {
        super(fileName);
        this.matcher = matcher;
        this.threshold = threshold;
	useWE = WordEmbedding.isLoaded();
    }

    public SimAnchoredPathSet(String fileName) throws IOException {
        super(fileName);
    }

    public List<AnchoredPath> similarPaths(String p) {
	if (useWE) {
	    String[] x = p.split("--");
	    if (x.length > 1) p = x[1].trim();
	}
        List<AnchoredPath> result = new ArrayList<AnchoredPath>();
        for (AnchoredPath path : paths) {
	    double score;
	    if (useWE) {
		score = WordEmbedding.pathSimilarity(p, path.path);
	    } else {
		score = 1 - (matcher.matchPaths("UNK -- " + path.path + " -- UNK",
			    "UNK -- " + p + " -- UNK") / (p.split(":").length + 1));
	    }
            if (score > threshold) {
                System.err.println(path.path + " : " + p + " = " + score);
                result.add(path);
            }
        }
        return result;
    }
}
