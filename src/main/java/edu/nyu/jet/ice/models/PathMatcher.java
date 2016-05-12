package edu.nyu.jet.ice.models;

import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PathMatcher is a Edit-Distance-based matcher that produces an alignment and an alignment score between
 * two MatcherPaths using the generalized Levenshtein algorithm. It can optionally use word embeddings to
 * compute the substitution cost, if embeddings is set.
 */
public class PathMatcher {

    private TObjectDoubleHashMap weights = new TObjectDoubleHashMap();
    private TObjectDoubleHashMap labelWeights = new TObjectDoubleHashMap();

    private static final double LABEL_MISMATCH_PENALTY = 2.5;
    private Map<String, double[]> embeddings = null;

    public PathMatcher() {
        weights.put("replace", 0.5);
        weights.put("insert", 0.25);
        weights.put("delete", 1.0);
        labelWeights.put("nsubj-1", 1.5);
        labelWeights.put("dobj-1", 1.5);
        labelWeights.put("nsubj", 1.0);
        labelWeights.put("dobj", 0.5);
        labelWeights.put("preps_of", 0.2);
        labelWeights.put("preps_with", 0.2);
    }

    public void setEmbeddings(Map<String, double[]> embeddings) {
        this.embeddings = embeddings;
    }

    public void updateCost(double replace, double insert, double delete) {
        weights.put("replace", replace);
        weights.put("insert", insert);
        weights.put("delete", delete);
    }

    public void loadWordEmbedding(String embeddingFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(embeddingFile));
        String line;
        embeddings = new HashMap<String, double[]>();
        int count = 1;
        System.err.println("Load embeddings...");
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(" ");
            double[] embedding = new double[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                embedding[i - 1] = Double.valueOf(parts[i]);
            }
            embeddings.put(parts[0], embedding);
            if (count % 10000 == 0) {
                System.err.print(count + "...");
            }
            count++;
        }
        System.err.println();
        br.close();
    }

    public double similarity(String word1, String word2) {
        if (embeddings == null) return word1.equals(word2) ? 1 : 0;
        double[] embedding1 = embeddings.get(word1);
        double[] embedding2 = embeddings.get(word2);
        if (embedding1 == null || embedding2 == null) return 0;
        return dotProduct(embedding1, embedding2) /
                (norm(embedding1) * norm(embedding2));
    }

    public double dotProduct(double[] v1, double[] v2) {
        double result = 0;
        for (int i = 0; i < v1.length && i < v2.length; i++) {
            result += v1[i] * v2[i];
        }
        return result;
    }

    public double norm(double[] v) {
        double result = 0;
        for (int i = 0; i < v.length; i++) {
            result += v[i] * v[i];
        }
        return Math.sqrt(result);
    }

    public double matchPaths(String path1, String path2) {
        MatcherPath matcherPath1 = new MatcherPath(path1);
        MatcherPath matcherPath2 = new MatcherPath(path2);

        return matchPaths(matcherPath1, matcherPath2);
    }

    public double matchPaths(MatcherPath matcherPath1, MatcherPath matcherPath2) {
        int len1 = matcherPath1.nodes.size();
        int len2 = matcherPath2.nodes.size();
        if (len1 == 1 && len2 == 1) {
            return matcherPath1.nodes.get(0).label.equals(matcherPath2.nodes.get(0).label)
                    && matcherPath1.arg1Type.equals(matcherPath2.arg1Type)
                    && matcherPath1.arg2Type.equals(matcherPath2.arg2Type) ?
                    0 : 1;
        }

        double[][] dp = new double[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        //iterate though, and check last char
        for (int i = 0; i < len1; i++) {
            MatcherNode c1 = matcherPath1.nodes.get(i);
            for (int j = 0; j < len2; j++) {
                MatcherNode c2 = matcherPath2.nodes.get(j);

                //if last two chars equal
                if (c1.equals(c2)) {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    double labelWeight = labelWeights.containsKey(c2.label) ?
                            labelWeights.get(c2.label) : 1;
                    double insertLabelWeight = labelWeights.containsKey(c1.label) ?
                            labelWeights.get(c1.label) : 1;
                    double replacePenalty = c1.label.equals(c2.label) ?
                            1 : LABEL_MISMATCH_PENALTY;
                    double replaceCost = 1 - similarity(c1.token, c2.token);
//                    if (c1.token.equals("distribute") || c2.token.equals("distribute")) {
//                        System.err.println("[LOG] " + c1.token + " " + c2.token + " " + replaceCost);
//                    }
                    double replace = dp[i][j] + weights.get("replace") * replacePenalty
                            * replaceCost
                            * labelWeight;
                    double insert = dp[i][j + 1] + weights.get("insert") * insertLabelWeight;
                    double delete = dp[i + 1][j] + weights.get("delete") * labelWeight;

                    double min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return matcherPath1.arg1Type.equals(matcherPath2.arg1Type) &&
            matcherPath1.arg2Type.equals(matcherPath2.arg2Type) ?
                    dp[len1][len2] : Math.max(matcherPath1.length(), matcherPath2.length());
    }

}

