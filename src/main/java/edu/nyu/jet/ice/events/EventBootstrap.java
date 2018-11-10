package edu.nyu.jet.ice.events;// -*- tab-width: 4 -*-

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.aceJet.AnchoredTreeSet;
import edu.nyu.jet.aceJet.SimAnchoredTreeSet;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.events.IceTree;
import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;


import java.util.*;
import java.io.*;
import javax.swing.*;

/**
 * a simple bootstrapping learner for relations.  Given a set of seed
 * patterns, it ranks each candidate pattern (lexical dependency path) P either
 * by its word-embedding-based distance from the seeds (if word embeddings have been
 * loaded) or by the number of argument pairs shared by the seed and P.
 */

public class EventBootstrap {

    public static boolean DEBUG     = true;

    Logger logger = LoggerFactory.getLogger(EventBootstrap.class);

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

    public static final int    MAX_BOOTSTRAPPED_ITEMS = 100;

    /**
     *  A ranked list of lexicalized dependency paths which are
     *  candidates for the relation being built.
     */

    public List<IceTree> foundPatterns = new ArrayList<IceTree>();

    private ProgressMonitorI progressMonitor = null;

    public String eventName = "";

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     *  Trees accepted by user.
     *  Initialized by 'initialize', augmented by 'iterate', used by 'bootstrap'.
     */

    public Set<IceTree> seedTrees = new HashSet<IceTree>();

    /**
     *  Trees accepted by user (in the form of String representation of dependency path).
     */

    public Set<IceTree> getSeedTrees() {
        return seedTrees;
    }

    /**
     *  Trees explicitly rejected by user 
     *  (in the form of String representation of dependency paths).
     */

    public Set<IceTree> rejects = new HashSet<IceTree>();

    /**
     *  Trees explicitly rejected by user 
     *  (in the form of String representation of dependency paths).
     */

    public Set<IceTree> getRejects() {
        return rejects; }

    /**
     *  Set of all paths in corpus.
     */

    SimAnchoredTreeSet treeSet;

    String argTypes;

    public EventBootstrap() {
    }

    public EventBootstrap(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    /**
     *  Create a Bootstrap object.
     *
     *  @param  name          the class of bootstrpping strategy to be used
     *  @param  progressMonitor
     *  @param  eventName  the name of the event to be modeled
     *
     */

    public static EventBootstrap makeBootstrap(String name, ProgressMonitorI progressMonitor, String eventName) {
        EventBootstrap instance = new EventBootstrap(progressMonitor);
        instance.eventName = eventName;
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

    public List<IceTree> initialize(String bigSeedString, String patternFileName) {
	LoggerFactory.setLevel("edu.nyu.jet.ice", "TRACE");

        try {
	    System.out.println("patternFileName == " + patternFileName);
            DepTreeMap depTreeMap = DepTreeMap.getInstance();
            String[] splitSeedString = bigSeedString.split(":::");
            List<IceTree> allTrees = new ArrayList<IceTree>();
            // iterate over all seed English expressions,
            // finding for each expression the dependency subtrees (if any)
            // and putting these subtrees together in seedTrees
	    for (String p : splitSeedString) {
		if (DEBUG)
		    System.out.println("Processing seed " + p);
		List<IceTree> currentTrees = depTreeMap.findTree(p);
		if (DEBUG)
		    System.out.println
			("Found " + ((currentTrees == null) ? "no" : currentTrees.size()) + " trees");  
		if (currentTrees != null) {
		    for (IceTree it : currentTrees)
			allTrees.add(it.lemmatize());
		}
	    }
	    if (allTrees.size() == 0) {
		JOptionPane.showMessageDialog(Ice.mainFrame,
			"Seed is invalid or not in corpus. Choose another seed or run [find common patterns].",
			"Unable to proceed",
			JOptionPane.WARNING_MESSAGE);
		return foundPatterns;
	    }
	    seedTrees.addAll(allTrees);

	    String argTypes = entityTypes(allTrees.get(0));

	    treeSet = new SimAnchoredTreeSet(patternFileName);

	    bootstrap(argTypes);
	}
	catch (IOException e) {
	    e.printStackTrace();
	    return new ArrayList<IceTree>();
	}
	return foundPatterns;
    }

    String entityTypes (IceTree it) {
	String result = "";
	for (String s : it.entityType)
	    if (s != null)
		result += " " + s;
	return result;
    }

    double threshold = 0.2;

    /**
     *  Perform the next iteration of bootstrapping to build a relation model
     *  (in response to the Iterate button of the relation panel):  adds
     *  <code>approvedTrees</CODE> to the set of positive seeds, adds
     *  <CODE>rejectedTrees</CODE> to the set of negative seeds, and then
     *  selects the next set of path candidates.
     */

    public List<IceTree> iterate(List<IceTree> approvedTrees, List<IceTree> rejectedTrees) {
        addTrees(approvedTrees, seedTrees);
        addTrees(rejectedTrees, rejects);
        bootstrap(argTypes);
        return foundPatterns;
    }

    public void addTrees(List<IceTree> approvedTrees, Set<IceTree> treeSet) {
        for (IceTree approvedTree : approvedTrees) {
            treeSet.add(approvedTree); // dropped getTree
        }
    }

    public void setProgressMonitor(ProgressMonitorI progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    Map<String, Integer> sharedCount = new HashMap<String, Integer>();
    Map<String, Integer> totalCount = new HashMap<String, Integer>();

    /**
     *  Starting from a set of seed trees, generate a ranked list of candidate trees
     *  to be displayed and (if approved by the user) added to the tree set
     *  for this relation.  This list is returned in <CODE>foundPatterns</CODE>
     *  (a list of IceTree).
     */

    public void bootstrap(String argTypes) { 
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
        DepTreeMap depTreeMap = DepTreeMap.getInstance();

        if (progressMonitor != null) {
            progressMonitor.setNote("Computing scores");
            progressMonitor.setProgress(4);
        }

        List<IceTree> scoreList = WordEmbedding.isLoaded() ? scoreUsingWordEmbeddings() : scoreUsingSharedArguments(argTypes);
        if (scoreList == null) return;
        //
        // sort the patterns by score
        //
        Collections.sort(scoreList);
        //
        // filter the pattern set:
        //   drop trees which have the same linearization as 
	//   a seed or a higher-ranked path
        //
        Set<String>   existingReprs = new HashSet<String>();
	for (IceTree s : seedTrees)
	    existingReprs.add(depTreeMap.findRepr(s));
        Set<String>   foundPatternStrings = new HashSet<String>();
        int count = 0;
        for (IceTree iceTree : scoreList) {
	    String p = iceTree.core();
            if (DEBUG) {
                System.err.print("Score for " + iceTree.toString() + " " + iceTree.getScore());
                System.err.println(" (shared = " + sharedCount.get(p) + " total = " + totalCount.get(p) + ")");
            }
	    if (existingReprs.contains(iceTree.getRepr())) continue;
            if (iceTree.getScore() < MIN_BOOTSTRAP_SCORE) continue; 
	    foundPatterns.add(iceTree);
	    foundPatternStrings.add(p);
	    existingReprs.add(iceTree.getRepr());
	    count++;
	    if (count > MAX_BOOTSTRAPPED_ITEMS) break;
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
        // if (RelationOracle.exists()) {
	// RelationOracle.label(foundPatterns);
        // }
    }

    /**
     *  Construct a list of candidate relation patterns, with scores
     *  based on number of shared arguments.
     */

    List<IceTree> scoreUsingSharedArguments (String argTypes) {

	// given seedTrees, with no arguments, collect seed tree instances
	// with the same trigger and argument types

        // List<BootstrapAnchoredTree> seedTreeInstances = new ArrayList<BootstrapAnchoredTree>();
        List<IceTree> seedTreeInstances = new ArrayList<IceTree>();

	logger.trace ("Building seedTreeInstances");
        for (IceTree sp : seedTrees) {
	    logger.trace ("    For seed tree = {}", sp);
            List<IceTree> posTrees = treeSet.getByPath(sp.getTrigger());
            if (posTrees != null) {
                for (IceTree p : posTrees) {
		    if (entityTypes(p).equals(argTypes)) {
			    logger.trace ("        Adding seed instance tree = {}", p);
			    seedTreeInstances.add(p);
		    }
		    /*
			    new BootstrapAnchoredTree
			    (p,
			     sp,
			     BootstrapAnchoredPathType.POSITIVE));
			     */
                }
            }
        }
        if (seedTreeInstances == null) {
            System.out.println("No examples of this path.");
            return null;
        }
        System.out.println(seedTreeInstances.size() + " examples of this path.");

        if (progressMonitor != null) {
            progressMonitor.setNote("Collecting argument pairs");
            progressMonitor.setProgress(1);
        }
        // collect other paths connecting the same argument pairs connected by seeds
        //   shared = set of arg pairs this pattern shares with seeds
        Map<String, Set<String>> shared = new HashMap<String, Set<String>>();
        Map<String, BootstrapAnchoredPathType> pathSourceMap =
                new HashMap<String, BootstrapAnchoredPathType>();
        // for (BootstrapAnchoredTree seed : seedTreeInstances) {
	logger.trace("collecting shared arguments");
        for (IceTree seed : seedTreeInstances) {
	    logger.trace("    For seed instance = {}", seed);
            for (IceTree p : treeSet.getByArgs(seed.argPair())) {
		logger.trace("        For event sharing arguments = {}", p);
		p = IceTree.clearArgValues(p);
                String pp = p.core();
                if (seedTreeInstances.contains(p)) continue;
                if (rejects.contains(p)) continue;
                if (shared.get(pp) == null) {
                    shared.put(pp, new HashSet<String>());
                }
                shared.get(pp).add(seed.argPair());
		logger.trace("        recording shared {} with args {}", pp, seed.argPair());
                if (pathSourceMap.containsKey(pp) && pathSourceMap.get(pp) != seed.type) {
                    pathSourceMap.put(pp, BootstrapAnchoredPathType.BOTH);
                }
                else {
                    pathSourceMap.put(pp, seed.type);
                }
            }
        }
        List<IceTree> scoreList = new ArrayList<IceTree>();
        DepTreeMap depTreeMap = DepTreeMap.getInstance();

        // for each path which shares pairs with the seed, compute
        // -- sharedCount = number of distinct argument pairs it shares
        // -- totalCount = total number of argument pairs for this path (not currently used)
        // -- score
	logger.trace ("counting shared args");
        for (String p : shared.keySet()) {
	    logger.trace ("    For p = {}", p);
	    IceTree itx = IceTreeFactory.getIceTree(p);
	    // itx = IceTree.clearArgValues(itx);
            sharedCount.put(p, shared.get(p).size());
            if (sharedCount.get(p) < MIN_RELATION_COUNT) continue;
            Set<String> argPairsForP = new HashSet<String>();
            for (IceTree ap : treeSet.getByPath(itx.getTrigger())) {
                argPairsForP.add(ap.getArgValue("nsubj") + ":" + ap.getArgValue("dobj"));
            }
            totalCount.put(p, argPairsForP.size());
            double score = (double)sharedCount.get(p) / totalCount.get(p) * Math.log(sharedCount.get(p) + 1);
            // double score = (double)sharedCount.get(p);
	    itx.setScore(score);

	    String repr = itx.getRepr();
	    if (repr == null) {
		logger.error ("Null repr for {}", itx.core());
		continue;
	    }
	    String example = itx.getToolTip();
	    if (example == null) {
		logger.warn ("Null tooltip for {}", itx);
	    } else {
		String toolTip = IceUtils.splitIntoLine(example, 80);
		toolTip = "<html>" + toolTip.replaceAll("\\n", "<\\br>");
		itx.setToolTip(toolTip);
	    }
            scoreList.add(itx);
        }
        return scoreList;
    }
 
    /**
     *  Construct a list of candidate relation patterns with scores based on
     *  similarities of embeddings.
     */

    List<IceTree> scoreUsingWordEmbeddings () {
	PathMatcher matcher = new PathMatcher();
	String fileName = FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName);
	IceTreeSet eventtypes = new IceTreeSet(fileName);
        List<IceTree> scoreList = new ArrayList<IceTree>();
	for (IceTree s : seedTrees) {
	    logger.info ("Expanding seed {}", s.core());
	    for (IceTree a : eventtypes.list) {
		String repr = a.getRepr();
		if (repr == null) {
		    logger.error ("Null repr for {}", a.core());
		    continue;
		}
		String example = a.getToolTip();
		if (example == null) {
		    logger.warn ("Null tooltip for {}", a);
		} else {
		    String toolTip = IceUtils.splitIntoLine(example, 80);
		    toolTip = "<html>" + toolTip.replaceAll("\\n", "<\\br>");
		    a.setToolTip(toolTip);
		}
		double score = WordEmbedding.treeSimilarity(a, s);
		a.setScore(score);
		scoreList.add(a);
	    }
	}
	return scoreList;
    }

    public enum BootstrapAnchoredPathType {
        POSITIVE, NEGATIVE, BOTH
    }

    public class BootstrapAnchoredTree extends IceTree {

        BootstrapAnchoredPathType type;

        IceTree typedPath;

        public String argPair() {
            return String.format("%s:%s", getArgValue("nsubj"), getArgValue("dobj"));
        }

        public BootstrapAnchoredTree(IceTree path,
                                     IceTree typedPath,
                                     BootstrapAnchoredPathType type) {
            // super(path.arg1, path.path, path.arg2, path.source, -1, -1); 
            this.type = type;
            this.typedPath = typedPath;
        }
    }

    public class IceTreeSet {

	List<IceTree> list = new ArrayList<IceTree>();
	int count;

	IceTreeSet (String fileName) {
	    try {
		BufferedReader reader = new BufferedReader (new FileReader (fileName));
		String line;
		while ((line = reader.readLine()) != null) {
		    int j = line.indexOf("\t");
		    if (j >= 0) {
			count = Integer.parseInt(line.substring(0, j));
			line = line.substring(j + 1);
		    }
		    IceTree iceTree = IceTreeFactory.getIceTree(line);
		    iceTree.count = count;
		    list.add(iceTree);
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (NumberFormatException e) {
		e.printStackTrace();
	    }
	}
    }
}
