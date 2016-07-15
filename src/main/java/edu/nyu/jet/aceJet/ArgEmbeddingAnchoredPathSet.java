package edu.nyu.jet.aceJet;

import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.utils.IceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Find similar paths. Unlike AnchorPathSet, which requires exact match between args,
 * this version uses PathMatcher to find similar paths.
 *
 * @author yhe
 * @version 1.0
 */
public class ArgEmbeddingAnchoredPathSet extends AnchoredPathSet {

    private Map<String, double[]> embedding = null;
    public static final int DIM_SIZE = 200;
    private double threshold    = 0.6;

    public ArgEmbeddingAnchoredPathSet(String fileName,
                                       Map<String, double[]> embedding,
                                       double threshold)
            throws IOException {
        super(fileName);
        this.embedding = embedding;
        this.threshold = threshold;
    }

    public ArgEmbeddingAnchoredPathSet(String fileName) throws IOException {
        super(fileName);
    }

    public List<AnchoredPath> similarPaths(AnchoredPath p,
                                           Set<String> exclusionSet) {
        List<AnchoredPath> result = new ArrayList<AnchoredPath>();
        double[] pVec = argVector(p);

        for (AnchoredPath path : paths) {
            if (exclusionSet.contains(path.path)) {
                continue;
            }
            double[] pathVec = argVector(path);
            double score = IceUtils.innerProduct(pVec, pathVec)/2;
            if (score > threshold) {
                System.err.println(path.path + " : " + p + " = " + score);
                exclusionSet.add(path.path);
                result.add(path);
            }
        }
        return result;
    }

    private double[] argVector(AnchoredPath p) {
        double[] pVec = new double[DIM_SIZE * 2];
        if (embedding.containsKey(p.arg1)) {
            double[] vec = embedding.get(p.arg1);
            for (int i = 0; i < vec.length; i++) {
                pVec[i] = vec[i];
            }
        }
        if (embedding.containsKey(p.arg2)) {
            double[] vec = embedding.get(p.arg2);
            for (int i = 0; i < vec.length; i++) {
                pVec[i + DIM_SIZE] = vec[i];
            }
        }
        return pVec;
    }


}
