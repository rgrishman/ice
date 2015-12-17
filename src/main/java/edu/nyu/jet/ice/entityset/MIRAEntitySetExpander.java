package edu.nyu.jet.ice.entityset;

import org.la4j.Vector;
import org.la4j.vector.sparse.CompressedVector;

import java.util.*;

/**
 * Entity set expansion using the MIRA-trained perceptron
 *
 * This is an alternative to the default distance based algorithm in EntitySetExpander. It has
 * similar performance as the default algorithm, so it is not activated by default in the current
 * system.
 *
 * Using the binary version of MIRA:
 * Koby Crammer and Yoram Singer. Ultraconservative Online Algorithms for Multiclass Problems
 * in JMLR 3 (2003): 965
 *
 * @author yhe
 * @version 1.0
 */
public class MIRAEntitySetExpander extends EntitySetExpander {

    private Vector parameters;
    public static final int EPOCHS = 50;

    public MIRAEntitySetExpander(String indexFileName, List<String> seeds) {
        super(indexFileName, seeds);
        parameters = new CompressedVector(this.getFeatureSize());
    }

    private double tao(double y, Vector wbar, Vector xbar) {
        double x = - y * wbar.innerProduct(xbar) / xbar.innerProduct(xbar);
        if (x < 0) return 0;
        if (x <= 1) return x;
        return 1;
    }


    @Override
    public void updateParameters() {
        List<String> positives = getPositives();
        List<String> negatives = getNegatives();
        List<Entity> examples = new ArrayList<Entity>(positives.size() + negatives.size());
        Map<String, Vector> featureDict = getEntityFeatureDict();
        Vector parameters = new CompressedVector(this.parameters.length());
        for (String positive : positives) {
            examples.add(new Entity(positive, "noun", 1));
            Vector p = featureDict.get(positive);
            p = p.divide(p.sum());
            parameters = parameters.add(p);
        }
        for (String negative : negatives) {
            examples.add(new Entity(negative, "noun", -1));
            Vector n = featureDict.get(negative);
            n = n.divide(n.sum());
            parameters = parameters.subtract(n);
        }
        List<Vector> parametersHistory  = new ArrayList<Vector>();

//        Random r = new Random();
//        for (int i = 0; i < parameters.length(); i++) {
//            double v = r.nextBoolean() ? r.nextDouble() : -r.nextDouble();
//            parameters.set(i, v);
//        }
        for (int i = 0; i < EPOCHS; i++) {
            Collections.shuffle(examples);
            for (int t = 0; t < examples.size(); t++) {
                Entity x    = examples.get(t);
                Vector xt   = featureDict.get(x.getText());
                double xtSum = xt.sum();
                xt = xt.divide(xtSum);
                double yhat = xt.innerProduct(parameters);
                if (yhat * examples.get(t).getScore() <= 0) {
                    // should update
                    Vector delta = xt.multiply(tao(x.getScore(), parameters, xt) * x.getScore());
                    //delta = delta.divide(i);
                    parameters = parameters.add(delta);
                }
            }
            Vector history = new CompressedVector(parameters.length());
            for (int j = 0; j < parameters.length(); j++) {
                if (parameters.get(j) != 0) {
                    history.set(j, parameters.get(j));
                }
            }
            parametersHistory.add(history);
        }
        this.parameters = new CompressedVector(this.parameters.length());
        for (int i = 0; i < parametersHistory.size(); i++) {
            this.parameters = this.parameters.add(parametersHistory.get(i));
        }
    }

    public void rerank(List<Entity> entities) {
        Map<String, Vector> entityFeatureDict = getEntityFeatureDict();

        if (progressMonitor != null) {
            progressMonitor.setNote("Calculating similarity...");
            progressMonitor.setProgress(0);
            progressMonitor.setMaximum(entityFeatureDict.size() + 5);
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int count = 0;
        boolean isCanceled = false;
        for (Entity e : entities) {
            Vector v = entityFeatureDict.get(e.getText());
//            double score = sim.measureSimilarity(centroid, v);
//            score -= GAMMA * sim.measureSimilarity(negativeCentroid, v);
            double vSum = v.sum();
            v = v.divide(vSum);
            double score = parameters.innerProduct(v);
            //Entity e = new Entity(k, "", -score);
            e.setScore(-score);
            count++;
            //System.out.println(count);
            if (progressMonitor != null) {
                if (progressMonitor.isCanceled()) {
                    isCanceled = true;
                    break;
                }
                progressMonitor.setProgress(count);
            }
        }
        if (progressMonitor != null) {
            progressMonitor.setNote("Sorting...");
        }
        Collections.sort(entities, new SimilarityComparator());
        if (progressMonitor != null && !isCanceled) {
            progressMonitor.setNote("Done.");
            progressMonitor.setProgress(progressMonitor.getMaximum());
        }
        if (!isCanceled) {
            rankedEntities = entities;
        }
    }


}
