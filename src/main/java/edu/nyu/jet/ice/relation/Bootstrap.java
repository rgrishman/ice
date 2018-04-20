package edu.nyu.jet.ice.relation;// -*- tab-width: 4 -*-

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.aceJet.AnchoredPathSet;
import edu.nyu.jet.aceJet.SimAnchoredPathSet;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.*;
import java.io.*;
import javax.swing.*;

/**
 * a simple bootstrapping learner for relations.  Given a set of seed
 * patterns, it ranks each candidate pattern (lexical dependency path) P by
 * the number of argument pairs shared by the seed and P.
 */

public class Bootstrap {
    public static boolean DEBUG     = true;

    public static PathMatcher pathMatcher = null;

    /**
     *  Minimum number of argument pairs which must be shared by
     *  seeds and candidate pattern.
     */

    public static final int MIN_RELATION_COUNT = 1;

    public static final double MIN_BOOTSTRAP_SCORE = 0.05;

    /**
     *  Maximum number of candidates to be displayed in one
     *  bootstrapping iteration.
     */

    public static final int    MAX_BOOTSTRAPPED_ITEMS = 200;

    /**
     *  A ranked list of lexicalized dependency paths which are
     *  candidates for the relation being built.
     */

    public List<IcePath> foundPatterns = new ArrayList<IcePath>();

    private ProgressMonitorI progressMonitor = null;

    public String relationName = "";

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    /**
     *  Paths accepted by user (in the form of String representation of dependency path).
     *  Initialized by 'initialize', augmented by 'iterate', used by 'bootstrap'.
     */

    public Set<String> seedPaths = new HashSet<String>();

    /**
     *  Paths accepted by user (in the form of String representation of dependency path).
     */

    public Set<String> getSeedPaths() {
        return seedPaths;
    }

    /**
     *  Paths explicitly rejected by user 
     *  (in the form of String representation of dependency paths).
     */

    public Set<String> rejects = new HashSet<String>();

    /**
     *  Paths explicitly rejected by user 
     *  (in the form of String representation of dependency paths).
     */

    public Set<String> getRejects() {
        return rejects; }

    /**
     *  Set of all paths in corpus.
     */

    SimAnchoredPathSet pathSet;

    String arg1Type = "";
    String arg2Type = "";

    public Bootstrap() {
    }

    public Bootstrap(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public String getArg2Type() {
        return arg2Type;
    }

    public String getArg1Type() {
        return arg1Type;
    }

    /**
     *  Create a Bootstrap object.
     *
     *  @param  name          the class of bootstrpping strategy to be used
     *  @param  progressMonitor
     *  @param  relationName  the name of the relation to be modeled (ignored)
     *
     */

    public static Bootstrap makeBootstrap(String name, ProgressMonitorI progressMonitor, String relationName) {
        Bootstrap instance = new Bootstrap(progressMonitor);
        instance.relationName = relationName;
        return instance;
    }

    /**
     *  Initialize the process of bootstrapping to build a relation model,
     *  creating an initial set of candidate paths.  These candidates are
     *  returned throguh the variable <CODE>foundPatterns</CODE>.
     *  Invoked by pressing the "expand" button on the relation panel.
     *
     *  @param  bigSeedString    one or more English expressions, separated by
     *                           ':::', the seed for the bootstrapping
     *  @param  patternFileName  name of file containing all dependency
     *                           path instances extracted from the corpus
     */

    public List<IcePath> initialize(String bigSeedString, String patternFileName) {
        try {
            DepPathMap depPathMap = DepPathMap.getInstance();
            String[] splitSeedString = bigSeedString.split(":::");
            List<String> allPaths = new ArrayList<String>();
            // iterate over all seed English expressions,
            // finding for each expression the dependency paths (if any)
            // and putting these paths together in seedPaths
            for (String p : splitSeedString) {
System.out.println("Processing seed " + p); // <<<<<<<<<<<<
                // >>>>>>>>>>>>>> convert directly to AnchoredPath
                List<String> currentPaths = depPathMap.findPath(p);
System.out.println("Found " + ((currentPaths == null) ? "no" : currentPaths.size()) + " paths");  // <<<<<<<<<
                if (currentPaths != null) {
                    for (String currentPath : currentPaths) {
                        String[] parts = currentPath.split("--");
                        allPaths.add(currentPath);
                        arg1Type = parts[0].trim();
                        arg2Type = parts[2].trim();
                    }
                }
            }
            if (allPaths.size() == 0) {
                JOptionPane.showMessageDialog(Ice.mainFrame,
                        "Seed is invalid or not in corpus. Choose another seed or run [find common patterns].",
                        "Unable to proceed",
                        JOptionPane.WARNING_MESSAGE);
                return foundPatterns;
            }
            seedPaths.addAll(allPaths);

            pathSet = new SimAnchoredPathSet(patternFileName, new PathMatcher(), threshold);
            bootstrap(arg1Type, arg2Type);
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<IcePath>();
        }
        return foundPatterns;
    }

    double threshold = 0.2;

    /**
     *  Perform the next iteration of bootstrapping to build a relation model
     *  (in response to the Iterate button of the relation panel):  adds
     *  <code>approvedPaths</CODE> to the set of positive seeds, adds
     *  <CODE>rejectedPaths</CODE> to the set of negative seeds, and then
     *  selects the next set of path candidates.
     */

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

    Map<String, Integer> sharedCount = new HashMap<String, Integer>();
    Map<String, Integer> totalCount = new HashMap<String, Integer>();

    /**
     *  Starting from a set of seed paths, generate a ranked list of candidate paths
     *  to be displayed and (if approved by the user) added to the path set
     *  for this relation.  This list is returned in <CODE>foundPatterns</CODE>
     *  (a list of IcePaths).
     */

    public void bootstrap(String arg1Type, String arg2Type) { // <<<<<
        // DEBUG = report counts and score
        if (Ice.iceProperties.getProperty("Ice.Bootstrapper.debug") != null) {
            DEBUG = Boolean.valueOf(Ice.iceProperties.getProperty("Ice.Bootstrapper.debug"));
        }
	if (Ice.iceProperties.getProperty("Ice.Bootstrapper.threshold") != null) {
	    try {
		threshold = Double.valueOf(Ice.iceProperties.getProperty("Ice.Bootstrapper.threshold"));
	    } catch (NumberFormatException e) {
		threshold = 0.2;
	    }
	    System.out.println ("Bootstrap threshold = " + threshold);
	}
        foundPatterns.clear();
        DepPathMap depPathMap = DepPathMap.getInstance();

        if (progressMonitor != null) {
            progressMonitor.setNote("Computing scores");
            progressMonitor.setProgress(4);
        }

        List<IcePath> scoreList = WordEmbedding.isLoaded() ? scoreUsingWordEmbeddings() : scoreUsingSharedArguments();
        if (scoreList == null) return;
        //
        // sort the patterns by score
        //
        Collections.sort(scoreList);
        //
        // filter the pattern set:
        //   drop paths which have the same linearization as 
	//   a seed or a higher-ranked path
        //
        Set<String>   existingReprs = new HashSet<String>();
	for (String s : seedPaths)
	    existingReprs.add(depPathMap.findRepr(s));
        Set<String>   foundPatternStrings = new HashSet<String>();
        int count = 0;
        for (IcePath icePath : scoreList) {
            String p = icePath.getPath();
            if (DEBUG) {
                System.err.print("Score for " + icePath.toString() + " " + icePath.getScore());
                System.err.println(" (shared = " + sharedCount.get(p) + " total = " + totalCount.get(p) + ")");
            }
            boolean isValid = !existingReprs.contains(icePath.getRepr());
            if (icePath.getScore() > MIN_BOOTSTRAP_SCORE && count < MAX_BOOTSTRAPPED_ITEMS  && isValid) {
                foundPatterns.add(icePath);
                foundPatternStrings.add(p);
                existingReprs.add(icePath.getRepr());
                count++;
            }
        }
        if (foundPatterns.isEmpty()) {
            JOptionPane.showMessageDialog(Ice.mainFrame, "Cannot suggest any [more] patterns.");
            return;
        }
        if (progressMonitor != null) {
            progressMonitor.setProgress(5);
        }
                
        //
        //  if a RelationOracle is present, use it to classify examples
        //
        if (RelationOracle.exists()) {
            RelationOracle.label(foundPatterns);
        }
    }

    /**
     *  Construct a list of candidate relation patterns, with scores
     *  based on number of shared arguments.
     */

    List<IcePath> scoreUsingSharedArguments () {

        List<BootstrapAnchoredPath> seedPathInstances = new ArrayList<BootstrapAnchoredPath>();

        for (String sp : seedPaths) {
            List<AnchoredPath> posPaths = pathSet.getByPath(new AnchoredPath(sp).path);
            if (posPaths != null) {
                for (AnchoredPath p : posPaths) {
                    seedPathInstances.add(new BootstrapAnchoredPath(p,
                            sp,
                            BootstrapAnchoredPathType.POSITIVE));
                }
            }
        }
        if (seedPathInstances == null) {
            System.out.println("No examples of this path.");
            return null;
        }
        System.out.println(seedPathInstances.size() + " examples of this path.");

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting argument pairs");
            progressMonitor.setProgress(1);
        }
        // collect other paths connecting the same argument pairs connected by seeds
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
        List<IcePath> scoreList = new ArrayList<IcePath>();
        DepPathMap depPathMap = DepPathMap.getInstance();

        // for each path which shares pairs with the seed, compute
        // -- sharedCount = number of distinct argument pairs it shares
        // -- totalCount = total number of argument pairs for this path (not currently used)
        // -- score
        for (String p : shared.keySet()) {
            sharedCount.put(p, shared.get(p).size());
            if (sharedCount.get(p) < MIN_RELATION_COUNT) continue;
            Set<String> argPairsForP = new HashSet<String>();
            for (AnchoredPath ap : pathSet.getByPath(p)) {
                argPairsForP.add(ap.arg1 + ":" + ap.arg2);
            }
            totalCount.put(p, argPairsForP.size());
            // double score = (double)sharedCount.get(p) / totalCount.get(p) * Math.log(sharedCount.get(p) + 1);
            double score = (double)sharedCount.get(p);

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
            tooltip = "<html>" + tooltip.replaceAll("\\n", "<\\br>");
            IcePath icePath = new IcePath(fullp, pRepr, tooltip, score);
            if (pRepr.equals(arg1Type + " " + arg2Type)) {
                continue;
            }
            scoreList.add(icePath);
        }
        return scoreList;
    }
 
    /**
     *  Construct a list of candidate relation patterns with scores based on
     *  similarities of embeddings.
     */

    List<IcePath> scoreUsingWordEmbeddings () {
	PathMatcher matcher = new PathMatcher();
        List<IcePath> scoreList = new ArrayList<IcePath>();
        DepPathMap depPathMap = DepPathMap.getInstance();
	for (String s : seedPaths) {
	    if (DEBUG) System.out.println("Expanding seed " + s);
	    for (AnchoredPath a : pathSet.similarPaths(s)) {
            String fullp = arg1Type + " -- " + a.path + " -- " + arg2Type;
		String pRepr = depPathMap.findRepr(fullp);
		if (pRepr == null) {
		    continue;
		}
		String pExample = depPathMap.findExample(fullp);
		if (pExample == null) {
		    continue;
		}
		String tooltip = IceUtils.splitIntoLine(depPathMap.findExample(fullp), 80);
		tooltip = "<html>" + tooltip.replaceAll("\\n", "<\\br>");

		// if (! arg1Type.equals(a.arg1)) continue;
		// if (! arg2Type.equals(a.arg2)) continue;
		//double score = matcher.matchPaths("UNK -- " + a.path + " -- UNK",
			//"UNK -- " + s + " -- UNK") / (s.split(":").length + 1);
		String[] x = s.split("--");
		double score = WordEmbedding.pathSimilarity(a.path, x[1].trim());

		IcePath icePath = new IcePath(fullp, pRepr, tooltip, score);
		if (pRepr.equals(arg1Type + " " + arg2Type)) {
		    continue;
		}
		scoreList.add(icePath);
	    }
	}
	return scoreList;
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
