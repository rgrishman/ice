package edu.nyu.jet.ice.entityset;

import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;
import org.la4j.vector.DenseVector;
import org.la4j.vector.sparse.CompressedVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Expand entity sets according to distributional similarity. Each noun group is represented by
 * its skip-gram embedding trained by word2vec.
 *
 * This class is not used by default by the ICE GUI, but can be activated easily.
 */
public class EmbeddingEntitySetExpander extends EntitySetExpander {

    public EmbeddingEntitySetExpander() {

    }

    public EmbeddingEntitySetExpander(String indexFileName, List<String> seeds) {
        used = new HashSet<String>();
        entityFeatureDict = new HashMap<String, Vector>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexFileName));
            line = br.readLine();
            int featureSize = Integer.valueOf(line.split(" ")[1]);
            centroid = new BasicVector(featureSize);
            negativeCentroid = new BasicVector(featureSize);

            while ((line = br.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line);
                String word = tok.nextToken().replaceAll("_", " ");
                Vector v = new BasicVector(featureSize); //BasicVector
                int i = 0;
                while (tok.hasMoreTokens()) {
                    v.set(i, Double.valueOf(tok.nextToken()));
                    i++;
                }
                entityFeatureDict.put(word, v);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (seeds != null) {
            for (String seed : seeds) {
                if (entityFeatureDict.containsKey(seed)) {
                    centroid = centroid.add(entityFeatureDict.get(seed));
                }
            }
        }
    }
}
