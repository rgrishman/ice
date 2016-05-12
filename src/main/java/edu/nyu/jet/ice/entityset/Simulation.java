package edu.nyu.jet.ice.entityset;

import edu.nyu.jet.ice.utils.IceUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Simulation is used to run simulated experiments on entity set expansion. See documentation
 * for the main() method for usage.
 */
public class Simulation {

    public static Scorer scorer;

    public static void score(List<String> entities, List<String> goldEntities) {
        int correct = 0;
        for (String entity : entities) {
            if (goldEntities.contains(entity)) correct++;
        }
        double p = (double)correct/entities.size();
        double r = (double)correct/goldEntities.size();
        double f = 2*p*r/(p+r);
        System.out.println(String.format("Precision:%.4f Recall:%.4f F-score:%.4f",
                p, r, f));
    }


    public static void rocScore(List<String> entities, List<String> goldEntities, String fileName) throws IOException {
        int tp = 0;
        int tn = entities.size() - goldEntities.size();
        int fp = 0;
        int fn = goldEntities.size();
        PrintWriter w  = new PrintWriter(new FileWriter(fileName));
        for (int i = 0; i < entities.size(); i++) {
            String entity = entities.get(i);
            if (goldEntities.contains(entity)) {
                tp++;
                fn--;
            }
            else {
                fp++;
                tn--;
            }
            double tpr = (double)tp/(tp+fn); // true positive rate
            double fpr = (double)fp/(fp+tn); // false positive rate
            w.println(String.format("%.4f %.4f", tpr, fpr));
        }
        w.close();
    }

    public static void peekNScore(List<String> entities, List<String> goldEntities) throws IOException {
        int tp = 0;
        for (int i = 0; i < entities.size(); i++) {
            String entity = entities.get(i);
//            if (goldEntities.contains(entity)) {
//                tp++;
//            }
            tp += scorer.getWeight(entity);
        }
//        double tpr = (double)tp/goldEntities.size(); // true positive rate = recall
        double tpr = (double)tp/scorer.getTotal();
        System.out.println(String.format("%.4f @ %d", tpr, entities.size()));
    }

    /**
     * Simulation currently does not take arguments - the necessary files should be set in the source
     * code.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            int[] sizes = new int[]{20};
            List<String> seedStrings = new ArrayList<String>();
            // set weights for each entity; there are two possibilities:
            // 1. weight the entity with counts in the format of word\tcount
            // 2. use Scorer(file, init_count): just provide a entity list and all entities will have the
            //    same weight init_count
            scorer = new Scorer("drugs_gold.weighted.txt");
            List<String> gold = Arrays.asList(IceUtils.readLines("drugs_gold.txt")); // answer key
//            seedStrings.add("enforcement partners");
//            seedStrings.add("local law enforcement agencies");
            seedStrings.add("methamphetamine"); // seeds
            seedStrings.add("oxycodone");

            for (int size : sizes) {
                System.out.println("\n\n\nSize " + size);
                List<String> positives = new ArrayList<String>();
                List<String> negatives = new ArrayList<String>();
//                EntitySetExpander expander = new EmbeddingEntitySetExpander("cache/DEA/phrases.embedding.200",
//                        seedStrings);
                EntitySetExpander expander = new EntitySetExpander("cache/DEA/EntitySetIndex_nn",
                        seedStrings); // Set index here.
                expander.rank();
                System.out.println("Iteration 1");
                //List<String> proposed = extractTopN(expander.rankedEntities, size);
                List<String> proposed =
                        extractDistinctTopN(expander.rankedEntities, positives, negatives, size);
                peekNScore(proposed, gold);
                //List<String> allProposed = extractTopN(expander.rankedEntities, expander.rankedEntities.size());
                //rocScore(allProposed, gold, String.format("drugs_size%d_iteration%d.roc", size, 1));
                //score(proposed, gold);
                updateExpander(expander, positives, negatives, proposed, gold);
                for (int i = 1; i < 10; i++) {
                    System.out.println("Iteration " + (i + 1));
                    expander.rerank(expander.rankedEntities);
                    proposed = extractDistinctTopN(expander.rankedEntities, positives, negatives, size);
                    peekNScore(proposed, gold);
                    //allProposed = extractTopN(expander.rankedEntities, expander.rankedEntities.size());
                    //rocScore(allProposed, gold, String.format("drugs_size%d_iteration%d.roc", size, (i+1)));
                    updateExpander(expander, positives, negatives, proposed, gold);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> extractTopN(List<Entity> entities, int n) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; entities.size() > i && i < n; i++) {
            result.add(entities.get(i).getText().trim());
        }
        return result;
    }

    private static List<String> extractDistinctTopN(List<Entity> entities,
                                                    List<String> positives,
                                                    List<String> negatives,
                                                    int n) {
        List<String> result = new ArrayList<String>();
        int i = 0;
        int count = 0;
        while (entities.size() > i && count < n) {
            String entity = entities.get(i).getText().trim();
            if (positives.contains(entity) || negatives.contains(entity)) {
                i++;
                continue;
            }
            result.add(entity);
            i++;
            count++;
        }
        return result;
    }

    private static void updateExpander(EntitySetExpander expander,
                                       List<String> positives,
                                       List<String> negatives,
                                       List<String> proposed,
                                       List<String> gold) {
        for (String proposedStr : proposed) {
            if (gold.contains(proposedStr)) {
                if (!positives.contains(proposedStr)) {
                    positives.add(proposedStr);
                }
            }
            else {
                if (!negatives.contains(proposedStr)) {
                    negatives.add(proposedStr);
                }
            }
        }
        expander.updateSeeds(positives, negatives);
        expander.updateParameters();
    }

    public static class Scorer {
        private HashMap<String, Integer> map = new HashMap<String, Integer>();
        private int total = 0;

        public Scorer(String fileName) throws IOException {
            List<String> lines = Arrays.asList(IceUtils.readLines(fileName));
            for (String line : lines) {
                String[] parts = line.split("\\t");
                map.put(parts[0], Integer.valueOf(parts[1]));
                total += Integer.valueOf(parts[1]);
            }
        }

        public Scorer(String fileName, int init) throws IOException {
            List<String> lines = Arrays.asList(IceUtils.readLines(fileName));
            for (String line : lines) {
                String[] parts = line.split("\\t");
                map.put(parts[0], init);
                total += init;
            }
        }

        public int getWeight(String w) {
            if (map.containsKey(w)) {
                return map.get(w);
            }
            return 0;
        }

        public int getTotal() {
            return total;
        }
    }

}
