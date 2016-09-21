package edu.nyu.jet.ice.entityset;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.la4j.Vector;
import org.la4j.Vectors;
import org.la4j.vector.sparse.CompressedVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Performs entity set expansion based on distributional similarity.
 */
public class EntitySetExpander {
    // centroid = positive seeds + gamma * negative seeds
    public static final double GAMMA = 0.5;
    // size of recommended postitive or negative seeds;
    // not related to functionalities in current (N)ice GUI　
    public static final int    RECOMMENDATION_SIZE = 10;
    // Number of entities to select seed from　
    public static final int    SUGGEST_SEED_SAMPLE_SIZE = 20;

    public EntitySetExpander() {

    }

    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        EntitySetExpander.progressMonitor = progressMonitor;
    }

    protected static ProgressMonitorI progressMonitor;

    protected Map<String, Vector> entityFeatureDict;
    protected Vector centroid;
    protected Vector negativeCentroid;
    protected List<String> positives;
    protected List<String> negatives;
    protected Similarity sim = new Similarity();
    public List<Entity> rankedEntities = null;
    public Set<String> used = null;

    /**
     * Based on an entity index and a terms file, recommend seeds for entity set construction.
     * Uses agglomerative clustering to build some clusteers of terms and returns
     * the highest-raanking cluster.  Terms which are already part of an entity set
     * are not included, nor are terms which have been previously suggested.
     *
     * @param indexFileName
     * @param termFileName
     * @param type
     * @return   a list of suggested seeds
     */
    public static List<String> recommendSeeds(String indexFileName, String termFileName, String type) {
        Map<String, Vector> entityFeatureDict = new HashMap<String, Vector>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexFileName));
            int featureSize = Integer.valueOf(br.readLine());
            while ((line = br.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line, "\t");
                String word = tok.nextToken();
                Vector v    = new CompressedVector(featureSize);
                while (tok.hasMoreTokens()) {
                    String[] parts = tok.nextToken().split(":");
                    v.set(Integer.valueOf(parts[0]), Double.valueOf(parts[1]));
                }
                entityFeatureDict.put(word, v);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(termFileName));
            double totalWeight = 0.0;
            while ((line = br.readLine()) != null &&
                    dataPoints.size() < SUGGEST_SEED_SAMPLE_SIZE) {
                line = line.trim();
                String[] parts = line.split("\t");
                String[] subParts  = parts[1].trim().split("/");
                String text = subParts[0];
                if (text.endsWith("tion") ||
                        text.endsWith("ment") ||
                        text.contains("www.") ||
                        text.contains("@") ||
                        text.contains("http") ||
                        text.length() == 0 ||
                        Character.isUpperCase(text.charAt(0))) {
                    continue;
                }
                if (Ice.selectedCorpus.entitiesSuggested.contains(text))
                    continue;
                for (IceEntitySet eset : Ice.entitySets.values()) {
                    if(eset.getNames().contains(text))
                        continue;
                    if(eset.getNouns().contains(text))
                        continue;
                }
                String entityType = subParts[1];
                if (!entityType.equals(type)) {
                    continue;
                }
                if (entityFeatureDict.containsKey(text)) {
                    Vector v = entityFeatureDict.get(text);
                    double weight = Double.valueOf(parts[0]);
                    DataPoint d = new DataPoint(text, v, weight);
                    totalWeight += weight;
                    dataPoints.add(d);
                }
            }
            for (DataPoint d : dataPoints) {
                d.score /= totalWeight;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<DataPointCluster> dataPointClusters = new ArrayList<DataPointCluster>();
        for (DataPoint p : dataPoints) {
            dataPointClusters.add(new DataPointCluster(p));
        }

        while (!shouldStop(dataPointClusters)) {
            agglomerativeCluster(dataPointClusters);
        }

        Collections.sort(dataPointClusters);
        DataPointCluster chosen = dataPointClusters.get(0);
        for (DataPointCluster c : dataPointClusters) {
            if (c.size() > 2) {
                chosen = c;
                break;
            }
        }

        List<String> suggestion = extractSeeds(chosen);
        for (String term : suggestion)
            Ice.selectedCorpus.entitiesSuggested.add(term);
        return suggestion;
    }

    private static List<String> extractSeeds(DataPointCluster c) {
        List<DataPoint> dataPoints = c.getDataPoints();
        class DataPointsCentroidComparator implements Comparator<DataPoint> {
            double[] centroid;
            //EuclideanDistance d = new EuclideanDistance();
            DataPointsCentroidComparator(double[] centroid) {
                this.centroid = centroid;
            }

            public int compare(DataPoint dataPoint, DataPoint dataPoint2) {
                //double result = d.compute(centroid, dataPoint.getPoint()) -
                //        d.compute(centroid, dataPoint2.getPoint());
                double result = Gravitation.invCosine(centroid, dataPoint.getPoint()) -
                        Gravitation.invCosine(centroid, dataPoint2.getPoint());
                if (result < 0) return -1;
                if (result > 0) return  1;
                return 0;
            }
        }
        DataPointsCentroidComparator comparator =
                new DataPointsCentroidComparator(c.getCentroid());
        Collections.sort(dataPoints, comparator);
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < dataPoints.size() && i < 2; i++) {
            result.add(dataPoints.get(i).text);
        }
        return result;
    }

    private static void agglomerativeCluster(List<DataPointCluster> clusters) {
        int minI = 0;
        int minJ = 1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                double distance = clusters.get(i).distance(clusters.get(j));
                if (distance < minDistance) {
                    minI = i;
                    minJ = j;
                    minDistance = distance;
                }
            }
        }
        DataPointCluster clusterI = clusters.get(minI);
        DataPointCluster clusterJ = clusters.get(minJ);
        DataPointCluster mergedCluster = clusterI.merge(clusterJ);
        clusters.remove(clusterI);
        clusters.remove(clusterJ);
        clusters.add(mergedCluster);
    }


    private static boolean shouldStop(List<DataPointCluster> clusters) {
        if (clusters.size() < 3) return true;
        Collections.sort(clusters);
        if (clusters.get(0).size() > 4) {
            return true;
        }
        return false;
    }

    public EntitySetExpander(String indexFileName, List<String> seeds) {
        used = new HashSet<String>();
        entityFeatureDict = new HashMap<String, Vector>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexFileName));
            int featureSize = Integer.valueOf(br.readLine());
            centroid = new CompressedVector(featureSize);
            negativeCentroid = new CompressedVector(featureSize);
            while ((line = br.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line, "\t");
                String word = tok.nextToken();
                Vector v = new CompressedVector(featureSize);
                while (tok.hasMoreTokens()) {
                    String[] parts = tok.nextToken().split(":");
                    v.set(Integer.valueOf(parts[0]), Double.valueOf(parts[1]));
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

    public Map<String, Vector> getEntityFeatureDict() {
        return entityFeatureDict;
    }

    protected int getFeatureSize() {
        return centroid.length();
    }

    public int getEntityFeatureDictSize() {
        return entityFeatureDict.keySet().size();
    }

    /**
     * Not related to current functionality of current (N)ice GUI
     */
    public void recommend() {
        PriorityQueue<Entity> positiveQueue = new PriorityQueue<Entity>(RECOMMENDATION_SIZE,
                new SimilarityComparator());
        PriorityQueue<Entity> negativeQueue = new PriorityQueue<Entity>(RECOMMENDATION_SIZE,
                new SimilarityComparator());

        if (progressMonitor != null) {
            progressMonitor.setNote("Calculating similarity...");
            progressMonitor.setProgress(0);
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int count = 0;
        for (String k : entityFeatureDict.keySet()) {
            Vector v = entityFeatureDict.get(k);
            double score = sim.measureSimilarity(centroid, v);
            Entity pos = new Entity(k, "", score);
            Entity neg = new Entity(k, "", -score);
            addToPriorityQueue(pos, positiveQueue);
            addToPriorityQueue(neg, negativeQueue);
            count++;
            //System.out.println(count);
            if (progressMonitor != null) {
                progressMonitor.setProgress(count);
            }
        }
        positives = new ArrayList<String>();
        for (Entity e : positiveQueue) {
            positives.add(e.getText());
        }
        negatives = new ArrayList<String>();
        for (Entity e : negativeQueue) {
            negatives.add(e.getText());
        }
    }

    /**
     * Rank entities based on closeness to type centroid
     * @return
     */
    public boolean rank() {
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
        List<Entity> entities = new ArrayList<Entity>();
        for (String k : entityFeatureDict.keySet()) {
            Vector v = entityFeatureDict.get(k);
            double score = sim.measureSimilarity(centroid, v);
            Entity e = new RankChoiceEntity(k, "", -score);
            entities.add(e);
            count++;
            //System.out.println(count);
            if (progressMonitor != null) {
                if (progressMonitor.isCanceled()) {
                    return false;
                }
                progressMonitor.setProgress(count);
            }
        }
        if (progressMonitor != null) {
            progressMonitor.setNote("Sorting... Unable to cancel at this stage.");
        }
        Collections.sort(entities, new SimilarityComparator());
        if (progressMonitor != null) {
            progressMonitor.setNote("Done.");
            progressMonitor.setProgress(progressMonitor.getMaximum());
        }
        rankedEntities = entities;
        return true;
    }

    /**
     * Update centroids based on user assigned class labels. Then rerank entities based on
     * the new centroids.
     *
     * @param entities
     */
    public void rerank(List<Entity> entities) {
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
            double score = sim.measureSimilarity(centroid, v);
            score -= GAMMA * sim.measureSimilarity(negativeCentroid, v);
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

    protected void addToPriorityQueue(Entity e, PriorityQueue<Entity> q) {
        if (q.size() < RECOMMENDATION_SIZE) {
            q.add(e);
        }
        else {
            double score = q.peek().getScore();
            if (score < e.getScore()) {
                q.poll();
                q.add(e);
            }
        }
    }

    public void updateSeeds(List<String> positives, List<String> negatives) {
        this.positives = positives;
        this.negatives = negatives;
    }

    public void updateParameters() {
        centroid = new CompressedVector(centroid.length());
        negativeCentroid = new CompressedVector(centroid.length());
        for (String positive : positives) {
            if (entityFeatureDict.containsKey(positive)) {
                centroid = centroid.add(entityFeatureDict.get(positive));
            }
        }
        for (String negative : negatives) {
            if (entityFeatureDict.containsKey(negative)) {
                negativeCentroid = negativeCentroid.add(entityFeatureDict.get(negative));
            }
        }
    }

    public List<String> getPositives() {
        return positives;
    }

    public List<String> getNegatives() {
        return negatives;
    }

    public static void main(String[] args) {
        System.out.println("Index file:");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            String indexFile = br.readLine();
            System.out.println("Input seeds separated by comma:");
            String[] seedsArr = br.readLine().split(",");
            List<String> seeds = Arrays.asList(seedsArr);
            EntitySetExpander expander = new EntitySetExpander(indexFile, seeds);
            int maxIteration = 5;
            for (int i = 0; i < maxIteration; i++) {
                expander.recommend();
                System.out.println("Positives:");
                for (String p : expander.getPositives()) {
                    System.out.println(p);
                }
                System.out.println("Negatives:");
                for (String n : expander.getNegatives()) {
                    System.out.println(n);
                }
                System.out.println("Input positive seeds separated by comma:");
                seedsArr = br.readLine().split(",");
                List<String> positiveSeeds = Arrays.asList(seedsArr);
                System.out.println("Input negative seeds separated by comma:");
                seedsArr = br.readLine().split(",");
                List<String> negativeSeeds = Arrays.asList(seedsArr);
                expander.updateSeeds(positiveSeeds, negativeSeeds);
                expander.updateParameters();
            }
            expander.recommend();
            System.out.println("Positives:");
            for (String p : expander.getPositives()) {
                System.out.println(p);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/*
    Compute cosine similarity
     */
class Similarity {
    public double measureSimilarity(Vector v1, Vector v2) {
        return v1.innerProduct(v2)/Math.sqrt(v1.innerProduct(v1) * v2.innerProduct(v2));
    }

}

class SquaredEuclideanSimilarity extends Similarity {
    @Override
    public double measureSimilarity(Vector v1, Vector v2) {
        Vector nv1 = v1.divide(v1.sum());
        Vector nv2 = v2.divide(v2.sum());
        Vector sub = nv1.subtract(nv2);
        return sub.innerProduct(sub);
    }
}

class SimilarityComparator implements Comparator<Entity> {
    public int compare(Entity entity1, Entity entity2) {
        if (entity1.getScore() > entity2.getScore()) return 1;
        if (entity1.getScore() < entity2.getScore()) return -1;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}

class Gravitation {
    public static final EuclideanDistance d = new EuclideanDistance();

    public static double invCosine(double[] a, double[] b) {
        double normA = 0;
        for (double aI : a) {
            normA += aI * aI;
        }
        double normB = 0;
        for (double bI : b) {
            normB += bI * bI;
        }
        double innerProduct = 0.00000001;
        for (int i = 0; i < a.length; i++) {
            innerProduct += a[i] * b[i];
        }
        return (Math.sqrt(normA) * Math.sqrt(normB)) / innerProduct;
    }

    public static double compute(DataPointCluster p1, DataPointCluster p2) {
        //double r = d.compute(p1.getCentroid(), p2.getCentroid());
        double r = invCosine(p1.getCentroid(), p2.getCentroid());
        // return Math.log(p1.getScore()) * Math.log(p2.getScore()) / (r*r);
        return p1.getMax() * p2.getMax() / (r*r);
    }
}

class DataPointCluster implements Comparable<DataPointCluster> {
    private List<DataPoint> dataPoints = new ArrayList<DataPoint>();
    private double[] centroid;
    private double score;
    private double max;
    //private static final EuclideanDistance d = new EuclideanDistance();

    DataPointCluster(DataPoint p) {
        dataPoints.clear();
        dataPoints.add(p);
        centroid = p.point;
        score = p.score;
        max = p.score;
    }

    DataPointCluster(List<DataPoint> dataPoints, double[] centroid, double score) {
        this.dataPoints = dataPoints;
        this.centroid = centroid;
        this.score = score;
        double max = 0;
        for (DataPoint dataPoint : dataPoints) {
            if (dataPoint.score > max) {
                max = dataPoint.score;
            }
        }
        this.max = max;
    }

    double distance(DataPointCluster c) {
        //return d.compute(c.centroid, this.centroid);
        return 1/Gravitation.compute(c, this);
    }

    int size() {
        return dataPoints.size();
    }

    DataPointCluster merge(DataPointCluster c) {
        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        dataPoints.addAll(c.dataPoints);
        dataPoints.addAll(this.dataPoints);
        double score = this.score + c.score;
        double[] centroid = new double[this.centroid.length];
        for (DataPoint p : dataPoints) {
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] += p.getPoint()[i];
            }
        }
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= dataPoints.size();
        }
        return new DataPointCluster(dataPoints, centroid, score);
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public double[] getCentroid() {
        return centroid;
    }

    public int compareTo(DataPointCluster dataPointCluster) {
        if (this.getScore() > dataPointCluster.getScore()) return -1;
        if (this.getScore() < dataPointCluster.getScore()) return 1;
        return 0;
    }

    public double getScore() {
        return score;
    }

    public double getMax() {
        return max;
    }
}

class DataPoint implements Clusterable {
    double[] point;
    String   text;
    double   score;

    public DataPoint(String text, Vector v, double score) {
        point = new double[v.length()];
        this.text = text;
        this.score = score;
        double norm = v.fold(Vectors.mkEuclideanNormAccumulator());
        if (norm == 0.0) {
            norm = 1;
        }
        for (int i = 0; i < point.length; i++) {
            point[i] = v.get(i) / norm;
        }
    }

    public double[] getPoint() {
        return point;
    }
}
