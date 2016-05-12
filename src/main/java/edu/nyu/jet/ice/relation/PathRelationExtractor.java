package edu.nyu.jet.ice.relation;

import edu.nyu.jet.ice.models.MatcherPath;
import edu.nyu.jet.ice.models.PathMatcher;
import opennlp.model.Event;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tag relations in a document by using positive and negative dependency path rules created with ICE
 *
 * @author yhe
 */
public class PathRelationExtractor {

    public static double minThreshold = 0.5;

    public void setNegDiscount(double nDiscount) {
        negDiscount = nDiscount;
    }

    public static double negDiscount = 0.8;

    private PathMatcher pathMatcher = new PathMatcher();

    private List<MatcherPath> ruleTable = new ArrayList<MatcherPath>();

    private List<MatcherPath> negTable  = new ArrayList<MatcherPath>();

    public void updateCost(double replace, double insert, double delete) {
        pathMatcher.updateCost(replace, insert, delete);
    }

    public void loadRules(String rulesFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(rulesFile));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            MatcherPath path = new MatcherPath(parts[0]);
            if (parts[0].contains("EMPTY")) {
                continue;
            }
            if (!path.isEmpty()) {
                path.setRelationType(parts[1]);
            }
            ruleTable.add(path);
        }
    }

    public void loadNeg(String negRulesFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(negRulesFile));
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\t");
            MatcherPath path = new MatcherPath(parts[0]);
            if (parts[0].contains("EMPTY")) {
                continue;
            }
            if (!path.isEmpty()) {
                path.setRelationType(parts[1]);
            }
            negTable.add(path);
        }
    }

    public void loadEmbeddings(String embeddingFile) throws IOException {
        pathMatcher.loadWordEmbedding(embeddingFile);
    }

    /**
     * Predict the relation type of an Event. The context[] array of the Event
     * should have the format [dependency path, arg1 type, arg2 type]
     * @param e An OpenNLP context[]:label pair
     * @return
     */
    public String predict(Event e) {
        String[] context = e.getContext();
        String depPath  = context[0];
        String arg1Type = context[1];
        String arg2Type = context[2];
        String fullDepPath = arg1Type + "--" + depPath + "--" + arg2Type;
        MatcherPath matcherPath = new MatcherPath(fullDepPath);
        double minScore = 1;
        double minNegScore = 1;
        MatcherPath minRule = null;
        for (MatcherPath rule : ruleTable) {
            double score = pathMatcher.matchPaths(matcherPath, rule)/
                    rule.length();
            if (arg1Type.equals("PERSON") && arg2Type.equals("DRUGS")) {
//                System.err.println("\tScore:"+ score);
//                System.err.println("\tRule:" + rule);
//                System.err.println("\tCurrent:" + matcherPath);
                //System.err.println("Gold:" + e.getOutcome()
                //        + "\tPredicted:" + rule.getRelationType());
            }
            if (score < minScore) {
                minScore = score;
                minRule = rule;
            }
        }
        MatcherPath minNegRule = null;
        if (minScore < minThreshold) {

            for (MatcherPath rule : negTable) {
                if (!rule.getRelationType().equals(minRule.getRelationType())) {
                    continue;
                }
                double score = pathMatcher.matchPaths(matcherPath, rule) / rule.length();
                if (score < minNegScore) {
                    minNegScore = score;
                    minNegRule  = rule;
                }
            }
        }
        else {
            return null;
        }


        if (minScore < minThreshold && minScore < minNegScore* negDiscount) {
            System.err.println("Score:"+ minScore);
            System.err.println("Rule:" + minRule);
            System.err.println("Current:" + matcherPath);
            System.err.println("Gold:" + e.getOutcome()
                    + "\tPredicted:" + minRule.getRelationType());

            return minRule.getRelationType();
        }
        if (minScore > minNegScore* negDiscount) {
            System.err.println("[REJ] Score:"+ minScore);
            System.err.println("[REJ] Neg Score:"+ minNegScore* negDiscount);
            System.err.println("[REJ] Rule:" + minRule);
            System.err.println("[REJ] Neg Rule:" + minNegRule);
            System.err.println("[REJ] Current:" + matcherPath);
            System.err.println("[REJ] Gold:" + e.getOutcome()
                    + "\tPredicted:" + minRule.getRelationType());
        }
        return null;
    }
}
