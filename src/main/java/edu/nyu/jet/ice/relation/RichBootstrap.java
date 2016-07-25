package edu.nyu.jet.ice.relation;// -*- tab-width: 4 -*-

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.aceJet.AnchoredPathSet;
import edu.nyu.jet.aceJet.SimAnchoredPathSet;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.*;
import java.io.*;
import javax.swing.*;

/**
 * a bootstrapping learner for relations, combining several
 * different se;ection scores.  Not currently inoked by Ice..
 */

public class RichBootstrap {
    public static boolean DEBUG     = true;

    public static boolean DIVERSIFY = true;

    public static PathMatcher pathMatcher = null;

    public static final int MIN_RELATION_COUNT = 1;

    public static final double MIN_BOOTSTRAP_SCORE = 0.05;

    public static final int    MAX_BOOTSTRAPPED_ITEM = 200;

    public static final double SCREEN_DIVERSITY_DISCOUNT = 0.7;

    public static final int    SCREEN_LINES = 20;

    public static final boolean USE_NEGATIVE = false; //TODO: configurable in props

    public List<IcePath> foundPatterns = new ArrayList<IcePath>();

    public Set<String> getSeedPaths() {
        return seedPaths;
    }

    public Set<String> getRejects() {return rejects; }

    private ProgressMonitorI progressMonitor = null;

    private String relationName = "";

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    Set<String> seedPaths = new HashSet<String>();
    Set<String> rejects = new HashSet<String>();
    AnchoredPathSet pathSet;
    String arg1Type = "";
    String arg2Type = "";

    public RichBootstrap() {

    }

    public RichBootstrap(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public String getArg2Type() {
        return arg2Type;
    }

    public String getArg1Type() {
        return arg1Type;
    }

    public static RichBootstrap makeBootstrap(String name, ProgressMonitorI progressMonitor, String relationName) {
        if (name.equals("ArgEmbeddingBootstrap")) {
            RichBootstrap instance =  new ArgEmbeddingBootstrap(progressMonitor);
            instance.relationName = relationName;
            return instance;
        }
        if (name.equals("LexicalSimilarityBootstrap")) {
            RichBootstrap instance =  new LexicalSimilarityBootstrap(progressMonitor);
            instance.relationName = relationName;
            return instance;
        }
        RichBootstrap instance = new RichBootstrap(progressMonitor);
        instance.relationName = relationName;
        return instance;
    }

    public List<IcePath> initialize(String seedPath, String patternFileName) {
        try {
            DepPathMap depPathMap = DepPathMap.getInstance();
            String[] splitPaths = seedPath.split(":::");
//            String firstSeedPath = null;
            List<String> allPaths = new ArrayList<String>();
            for (String p : splitPaths) {
//                if (firstSeedPath == null) {
//                    firstSeedPath = depPathMap.findPath(seedPath);
//                }
                List<String> currentPaths = depPathMap.findPath(p);
                if (currentPaths != null) {
                    for (String currentPath : currentPaths) {
                        String[] parts = currentPath.split("--");
                        //firstSeedPath = parts[1].trim();
                        allPaths.add(parts[1].trim());
                        arg1Type = parts[0].trim();
                        arg2Type = parts[2].trim();
                    }
                }
            }
            if (allPaths.size() == 0) {
                JOptionPane.showMessageDialog(Ice.mainFrame,
                        "Seed is invalid. Choose another seed or run [find common patterns].",
                        "Unable to proceed",
                        JOptionPane.WARNING_MESSAGE);
                return foundPatterns;
            }
            //seedPaths.add(args[0]);
            //seedPaths.add(firstSeedPath);
            seedPaths.addAll(allPaths);
            // Using SimAchoredPathSet
//            pathSet = new SimAnchoredPathSet(patternFileName, pathMatcher, 0.6);
            pathSet = new AnchoredPathSet(patternFileName);
            bootstrap(arg1Type, arg2Type);
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<IcePath>();
        }
        return foundPatterns;
    }

    public List<IcePath> iterate(List<IcePath> approvedPaths, List<IcePath> rejectedPaths) {
        addPathsToSeedSet(approvedPaths, seedPaths);
        addPathsToSeedSet(rejectedPaths, rejects);
        bootstrap(arg1Type, arg2Type);
        return foundPatterns;
    }

    public void addPathsToSeedSet(List<IcePath> approvedPaths, Set<String> pathSet) {
        for (IcePath approvedPath : approvedPaths) {
            pathSet.add(approvedPath.getPath());
        }
    }



    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    private void bootstrap(String arg1Type, String arg2Type) {
        // DEBUG = Show various distance scores (used to select instances) in tooltip
        if (Ice.iceProperties.getProperty("Ice.Bootstrapper.debug") != null) {
            DEBUG = Boolean.valueOf(Ice.iceProperties.getProperty("Ice.Bootstrapper.debug"));
        }
        // DIVERSIFY = Disallow similar paths in top 20
        if (Ice.iceProperties.getProperty("Ice.Bootstrapper.diversify") != null) {
            DIVERSIFY = Boolean.valueOf(Ice.iceProperties.getProperty("Ice.Bootstrapper.diversify"));
        }
        foundPatterns.clear();
        DepPathMap depPathMap = DepPathMap.getInstance();

        double minAllowedSimilarity =
                PathRelationExtractor.minThreshold * PathRelationExtractor.negDiscount * SCREEN_DIVERSITY_DISCOUNT;
        pathMatcher.updateCost(0.8, 0.3, 1.2);
        List<BootstrapAnchoredPath> seedPathInstances = new ArrayList<BootstrapAnchoredPath>();

        for (String sp : seedPaths) {
            List<AnchoredPath> posPaths = pathSet.getByPath(sp);
            if (posPaths != null) {
                for (AnchoredPath p : posPaths) {
                    seedPathInstances.add(new BootstrapAnchoredPath(p,
                            sp,
                            BootstrapAnchoredPathType.POSITIVE));
                }
            }
            // should we bootstrap negative paths?
            if (USE_NEGATIVE) {
                for (String rp : rejects) {
                    double sim = pathMatcher.matchPaths(arg1Type + "--" + sp + "--" + arg2Type,
                            arg1Type + "--" + rp + "--" + arg2Type) / sp.split(":").length;
                    if (sim < minAllowedSimilarity) {
                        System.err.println("Bootstrapping negative path:" + rp);
                        List<AnchoredPath> negPaths = pathSet.getByPath(rp);
                        for (AnchoredPath np : negPaths) {
                            seedPathInstances.add(new BootstrapAnchoredPath(np,
                                    rp,
                                    BootstrapAnchoredPathType.NEGATIVE));
                        }
                    }
                }
            }
        }
        if (seedPathInstances == null) {
            System.out.println("No examples of this path.");
            return;
        }
        System.out.println(seedPathInstances.size() + " examples of this path.");

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting argument pairs");
            progressMonitor.setProgress(1);
        }
        // collect set of argument pairs from these instances
//        Set<String> argPairs = new HashSet<String>();
//        for (AnchoredPath path : seedPathInstances) {
//            argPairs.add(path.arg1 + ":" + path.arg2);
//        }

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting new paths");
            progressMonitor.setProgress(2);
        }

        // now collect other paths connecting these argument pairs
        //   shared = set of arg pairs this pattern shares with seeds
        Map<String, Set<String>> shared = new HashMap<String, Set<String>>();
        Map<String, BootstrapAnchoredPathType> pathSourceMap =
                new HashMap<String, BootstrapAnchoredPathType>();
        for (BootstrapAnchoredPath seed : seedPathInstances) {
            for (AnchoredPath p : pathSet.getByArgs(seed.argPair())) {
                String pp = p.path;
                if (seedPaths.contains(pp)) continue;
                if (rejects.contains(pp)) continue;
                if (shared.get(pp) == null) {
                    shared.put(pp, new HashSet<String>());
                }
                shared.get(pp).add(seed.argPair());
                if (pathSourceMap.containsKey(pp) && pathSourceMap.get(pp) != seed.type) {
                    pathSourceMap.put(pp, BootstrapAnchoredPathType.BOTH);
                }
                else {
                    pathSourceMap.put(pp, seed.type);
                }
            }
        }

        if (progressMonitor != null) {
            progressMonitor.setNote("Computing scores");
            progressMonitor.setProgress(4);
        }

        // for each path which shares pairs with the seed, compute
        // -- sharedCount = number of distinct argument pairs it shares
        // -- totalCount = total number of distinct arg pairs it appears with
        Map<String, Integer> sharedCount = new HashMap<String, Integer>();

        List<IcePath> scoreList = new ArrayList<IcePath>();
        for (String p : shared.keySet()) {
            sharedCount.put(p, shared.get(p).size());
            if (sharedCount.get(p) < MIN_RELATION_COUNT) continue;
            Set<String> argPairsForP = new HashSet<String>();
            for (AnchoredPath ap : pathSet.getByPath(p)) {
                argPairsForP.add(ap.arg1 + ":" + ap.arg2);
            }
            //  arguments are similar to existing paths
            double argScore = 1 - (double)sharedCount.get(p)/argPairsForP.size();
            // how close the path is to positive seeds accoring to edit distance
            double posScore = minDistanceToSet(p, seedPaths);
            // how close the path is to negative seeds
            double negScore = 0.1; // minDistanceToSet(p, rejects);
            int    patternLength = 1 + p.split(":").length;
            // if the path is equally close to positive and negative paths
            double nearestNeighborConfusion     = 1 - Math.abs(posScore - negScore);

            double argConfusion    = Math.abs(argScore - posScore);

            double borderConfusion = 0;

            // In any case, candidate paths are ranked by the confusionScore they are assigned
            // confusion score can be an arbitrary combination of the scores above.
            double confusionScore  = Math.max(Math.max(nearestNeighborConfusion, borderConfusion), argConfusion);
            if (!USE_NEGATIVE) {
                confusionScore = 1/posScore;
            }

            String fullp = arg1Type + " -- " + p + " -- " + arg2Type;
            String pRepr = depPathMap.findRepr(fullp);
            if (pRepr == null) {
                continue;
            }
            String pExample = depPathMap.findExample(fullp);
            if (pExample == null) {
                continue;
            }
            String tooltip = IceUtils.splitIntoLine(depPathMap.findExample(fullp), 80);
            TObjectDoubleHashMap subScores = new TObjectDoubleHashMap();
            subScores.put("nearestNeighborConfusion", nearestNeighborConfusion);
            subScores.put("borderConfusion", borderConfusion);
            subScores.put("argConfusion", argConfusion);
            if (DEBUG) {
                tooltip += String.format("\nposScore:%.4f", posScore);
                tooltip += String.format("\nnegScore:%.4f", negScore);
                for (Object key : subScores.keys()) {
                    tooltip += "\n" + key + String.format(":%.4f", subScores.get((String)key));
                }
                tooltip += String.format("\nconfusionScore:%.4f", confusionScore);
            }
            tooltip = "<html>" + tooltip.replaceAll("\\n", "<\\br>");
            IcePath icePath = new IcePath(p, pRepr, tooltip, confusionScore);
            if (pRepr.equals(arg1Type + " " + arg2Type)) {
                continue;
            }
            scoreList.add(icePath);
        }

        Collections.sort(scoreList);
        List<IcePath> buffer = new ArrayList<IcePath>();
        Set<String>   existingReprs = new HashSet<String>();
        Set<String>   foundPatternStrings = new HashSet<String>();
        int count = 0;
        for (IcePath icePath : scoreList) {
            double simScore = minDistanceToSet(icePath.getPath(), foundPatternStrings);
            System.err.println("SimScore for " + icePath.toString() + " " + simScore);
            boolean isValid = count > SCREEN_LINES ||
                    !existingReprs.contains(icePath.getRepr()) &&
                            (!DIVERSIFY ||
                                    simScore > PathRelationExtractor.minThreshold * SCREEN_DIVERSITY_DISCOUNT);
            if (icePath.getScore() > MIN_BOOTSTRAP_SCORE
                    && count < MAX_BOOTSTRAPPED_ITEM) {
                if (isValid) {
                    foundPatterns.add(icePath);
                    foundPatternStrings.add(icePath.getPath());
                    existingReprs.add(icePath.getRepr());
                    count++;
                }
                else {
                    System.err.println("filtered out for diversity: " + icePath.toString());
                    buffer.add(icePath);
                }
            }
            if (count > SCREEN_LINES) {
                if (buffer.size() > 0) {
                    foundPatterns.addAll(buffer);
                    count += buffer.size();
                    buffer.clear();
                }
            }
        }
        if (progressMonitor != null) {
            progressMonitor.setProgress(5);
        }
        System.err.println("Bootstrapper.DIVERSIFY:" + DIVERSIFY);
    }

    public double minDistanceToSet(String path, Set<String> pathSet) {
        double result = 1;
        for (String pathInSet : pathSet) {
            double score =
                    pathMatcher.matchPaths("T1--" + path + "--T2", "T1--" + pathInSet + "--T2") /
                            (pathInSet.split(":").length + 1);
            if (score < result) {
                result = score;
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {

    }

    static boolean query(String p) {
        int result =
                JOptionPane.showConfirmDialog(null, p, " Keep?", JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }

    static int exitableQuery(String p) {
        Object[] options = {"Exit",
                "No",
                "Yes"};
        return JOptionPane.showOptionDialog(Ice.mainFrame,
                p,
                "Keep?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

    }

    public enum BootstrapAnchoredPathType {
        POSITIVE, NEGATIVE, BOTH
    }

    public class BootstrapAnchoredPath extends AnchoredPath {

        BootstrapAnchoredPathType type;

        String typedPath;

        public String argPair() {
            return String.format("%s:%s", arg1, arg2);
        }

        public BootstrapAnchoredPath(AnchoredPath path,
                                     String typedPath,
                                     BootstrapAnchoredPathType type) {
            super(path.arg1, path.path, path.arg2, path.source, -1, -1);
            this.type = type;
            this.typedPath = typedPath;
        }
    }

}
