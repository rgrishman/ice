package edu.nyu.jet.aceJet;

import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;
import edu.nyu.jet.ice.models.IcePath;

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

    public List<AnchoredPath> similarPaths(IcePath centroid) {
        if (useWE) {
            String[] x = centroid.getPathString().split("--");
            if (x.length > 1) centroid = new IcePath(x[1].trim());
        }
        List<AnchoredPath> result = new ArrayList<AnchoredPath>();
        for (AnchoredPath path : paths) {
            double score = 0.;
            if (useWE) {
                String string1 = centroid.getPathString();
                String string2 = path.toString();
                score = WordEmbedding.pathSimilarity(string1, string2);
            } else {
; //  XXX		score = 1 - (matcher.matchPaths("UNK -- " + path.path + " -- UNK",
//  XXX      "UNK -- " + p + " -- UNK") / (p.getPath().split(":").length + 1));
            }
            if (score > threshold) {
// XXX                System.err.println(path.path + " : " + p + " = " + score);
                result.add(path);
            }
        }
        return result;
    }
}
