package edu.nyu.jet.ice.relation;

import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by yhe on 5/25/15.
 */
public class EmbeddingBootstrapper {
    Map<String, Vector> pathFeatureDict;

    public EmbeddingBootstrapper() {

    }

    public EmbeddingBootstrapper(String indexFileName) {
        pathFeatureDict = new HashMap<String, Vector>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexFileName));
            line = br.readLine();
            int featureSize = Integer.valueOf(line.split(" ")[1]);

            while ((line = br.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line);
                String word = tok.nextToken();
                Vector v = new BasicVector(featureSize); //BasicVector
                int i = 0;
                while (tok.hasMoreTokens()) {
                    v.set(i, Double.valueOf(tok.nextToken()));
                    i++;
                }
                pathFeatureDict.put(word, v);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void findPaths(String path) {
        final double THRESHOLD = 0.7;
        Vector v = pathFeatureDict.get(path);
        Similarity sim = new Similarity();
        for (String p : pathFeatureDict.keySet()) {
            double score = sim.measureSimilarity(v, pathFeatureDict.get(p));
            if (score > THRESHOLD) {
                System.err.println(score + "\t" + p);
            }
        }
    }

    class Similarity {
        public double measureSimilarity(Vector v1, Vector v2) {
            return v1.innerProduct(v2)/Math.sqrt(v1.innerProduct(v1) * v2.innerProduct(v2));
        }
    }

    public static void main(String[] args) {
        EmbeddingBootstrapper bootstrapper = new EmbeddingBootstrapper(args[0]);
        bootstrapper.findPaths("nsubj-1:sell:dobj");
    }
}
