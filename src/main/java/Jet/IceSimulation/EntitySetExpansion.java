package Jet.IceSimulation;

import Jet.ExpandEntitySet.Entity;
import Jet.ExpandEntitySet.EntitySetExpander;
import Jet.IceUtils.IceUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yhe on 5/29/14.
 */
public class EntitySetExpansion {

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
            if (goldEntities.contains(entity)) {
                tp++;
            }
        }
        double tpr = (double)tp/goldEntities.size(); // true positive rate = recall
        System.out.println(String.format("%.4f @ %d", tpr, entities.size()));
    }

    public static void main(String[] args) {
        try {
            int[] sizes = new int[]{20};
            List<String> seedStrings = new ArrayList<String>();
            List<String> gold = Arrays.asList(IceUtils.readLines("agents_gold.txt"));
            seedStrings.add("special agents");
            seedStrings.add("law enforcement officers");

            for (int size : sizes) {
                System.out.println("\n\n\nSize " + size);
                List<String> positives = new ArrayList<String>();
                List<String> negatives = new ArrayList<String>();
                EntitySetExpander expander = new EntitySetExpander("kdddeaEntitySetIndex_nn",
                        seedStrings);
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

}
