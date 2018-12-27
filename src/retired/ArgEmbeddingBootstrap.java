package edu.nyu.jet.ice.relation;
import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.aceJet.AnchoredPathSet;
import edu.nyu.jet.aceJet.ArgEmbeddingAnchoredPathSet;
import edu.nyu.jet.aceJet.SimAnchoredPathSet;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * An experimental boostrapper, using phrase embeddings of relation arguments.
 *
 * It is not currently used by default by the current ICE GUI/CLI, but can be turned on in config files
 *
 * @author yhe
 */

public class ArgEmbeddingBootstrap extends RichBootstrap {

    @Override
    public Set<String> getSeedPaths() {
        return seedPaths;
    }

    @Override
    public Set<String> getRejects() {return rejects; }

    private ProgressMonitorI progressMonitor = null;

    private Map<String, double[]> normalizedPhraseEmbeddings = new HashMap<String, double[]>();

    public ArgEmbeddingBootstrap() {
        super();
    }

    public ArgEmbeddingBootstrap(ProgressMonitorI progressMonitor) {
        super(progressMonitor);
    }

    @Override
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
//            loadEmbeddings(FileNameSchema.getCorpusInfoDirectory(Ice.selectedCorpusName)
//                    + File.separator +"deps.context.complete.vec.dim200");
            pathSet = new ArgEmbeddingAnchoredPathSet(patternFileName, normalizedPhraseEmbeddings, 0.8);
//            pathSet = new AnchoredPathSet(patternFileName);
            bootstrap(arg1Type, arg2Type);
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<IcePath>();
        }
        return foundPatterns;
    }

    @Override
    public List<IcePath> iterate(List<IcePath> approvedPaths, List<IcePath> rejectedPaths) {
        addPathsToSeedSet(approvedPaths, seedPaths);
        addPathsToSeedSet(rejectedPaths, rejects);
        bootstrap(arg1Type, arg2Type);
        return foundPatterns;
    }

    private void loadEmbeddings(String fileName) {
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            line = br.readLine();
            int featureSize = Integer.valueOf(line.split(" ")[1]);

            while ((line = br.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line);
                String word = tok.nextToken().replaceAll("_", " ");
                double[] v = new double[featureSize];
                int i = 0;
                while (tok.hasMoreTokens()) {
                    v[i] = Double.valueOf(tok.nextToken());
                    i++;
                }
                IceUtils.l2norm(v);
                normalizedPhraseEmbeddings.put(word, v);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void bootstrap(String arg1Type, String arg2Type) {
        if (Ice.iceProperties.getProperty("Ice.Bootstrapper.debug") != null) {
            DEBUG = Boolean.valueOf(Ice.iceProperties.getProperty("Ice.Bootstrapper.debug"));
        }
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
            for (AnchoredPath p : posPaths) {
                seedPathInstances.add(new BootstrapAnchoredPath(p,
                        sp,
                        BootstrapAnchoredPathType.POSITIVE));
            }
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
        if (seedPathInstances == null || seedPathInstances.size() == 0) {
            System.out.println("No examples of this path.");
            return;
        }
        System.out.println(seedPathInstances.size() + " examples of this path.");

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting argument pairs");
            progressMonitor.setProgress(1);
        }

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting new paths");
            progressMonitor.setProgress(2);
        }

        // now collect other paths connecting these argument pairs
        //   shared = set of arg pairs this pattern shares with seeds
        Map<String, Set<String>> shared = new HashMap<String, Set<String>>();
        Map<String, BootstrapAnchoredPathType> pathSourceMap =
                new HashMap<String, BootstrapAnchoredPathType>();
        Set<String> exclusionSet = new TreeSet<String>();
        exclusionSet.addAll(seedPaths);
        exclusionSet.addAll(rejects);
        for (BootstrapAnchoredPath seed : seedPathInstances) {
            for (AnchoredPath p : pathSet.getByArgs(seed.argPair())) {
                String pp = p.path;
                if (seedPaths.contains(pp)) continue;
                if (rejects.contains(pp)) continue;
                exclusionSet.add(pp);
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
            List<AnchoredPath> simPaths = ((ArgEmbeddingAnchoredPathSet)pathSet).similarPaths(seed, exclusionSet);
            for (AnchoredPath p : simPaths) {
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
        // Map<String, Integer> totalCount = new HashMap<String, Integer>();
        // Map<String, Integer> score = new HashMap<String, Integer>();
        List<IcePath> scoreList = new ArrayList<IcePath>();
        for (String p : shared.keySet()) {
            sharedCount.put(p, shared.get(p).size());
            if (sharedCount.get(p) < MIN_RELATION_COUNT) continue;
            Set<String> argPairsForP = new HashSet<String>();
            for (AnchoredPath ap : pathSet.getByPath(p)) {
                argPairsForP.add(ap.arg1 + ":" + ap.arg2);
            }
            // totalCount.put(p, argPairsForP.size());
//            score.put(p, 100 * sharedCount.get(p) / argPairsForP.size()
//                    / (1 + p.split(":").length));
            // score.put(p, (100 * sharedCount.get(p) - 90) / totalCount.get(p));
            double argScore = 1 - (double)sharedCount.get(p)/argPairsForP.size();
            double posScore = minDistanceToSet(p, seedPaths);
            double negScore = minDistanceToSet(p, rejects);
            int    patternLength = 1 + p.split(":").length;
            double nearestNeighborConfusion     = 1 - Math.abs(posScore - negScore);
            // double borderConfusion = 1 - Math.abs(posScore - PathRelationExtractor.minThreshold);
            // double borderConfusion = posScore - PathRelationExtractor.minThreshold;
            double argConfusion    = Math.abs(argScore - posScore);
            //if (borderConfusion > 0) {
            //    borderConfusion = 1 - posScore + PathRelationExtractor.minThreshold / (1 - PathRelationExtractor.minThreshold);
            //}
            //else if (borderConfusion < 0) {
            //    borderConfusion = posScore / PathRelationExtractor.minThreshold;
            //}
            double borderConfusion = 0;
            double confusionScore  = Math.max(Math.max(nearestNeighborConfusion, borderConfusion), argConfusion);
//            System.err.println("[Bootstrap score]\t" + p + "\t" + (int)Math.round(confusionScore * 100) +
//                    "\tnnConfusion:" + nearestNeighborConfusion +
//                    "\tborderConfusion:" + borderConfusion +
//                    "\targConfusion:" + argConfusion);
            // score.put(p, (int) Math.round(confusionScore * 100));
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
            progressMonitor.setProgress(progressMonitor.getMaximum());
        }

    }

    public static void main(String[] args) throws IOException {

    }

}
