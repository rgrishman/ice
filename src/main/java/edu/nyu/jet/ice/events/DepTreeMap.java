package edu.nyu.jet.ice.events;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * DepTreeMap maintains maps between depTrees, linearizations, and example sentences. 
 * It originally computed linearizations and saved them for each tree. Linearization 
 * is currently handled by the DepPath class, so DepPathMap only
 * handles serialization/deserialization of the map.
 */

public class DepTreeMap {

    static final Logger logger = LoggerFactory.getLogger(DepTreeMap.class);

    private static DepTreeMap instance = null;

    private HashMap<IceTree, String> treeReprMap = new HashMap<IceTree, String>();
    private HashMap<String, List<IceTree>> reprTreeMap = new HashMap<String, List<IceTree>>();
    private HashMap<IceTree, String> treeExampleMap = new HashMap<IceTree, String>();
    private DepTreeMap() { }
    private String previousFileName = null;

    /**
    DepTreeMap is a singleton which is shared across the program.
     */
    public static DepTreeMap getInstance() {
        if (instance == null) {
            instance = new DepTreeMap();
        }
        return instance;
    }

    /**
     *  Returns the set of dependency trees for the corpus.
     */

    public Set<IceTree> getTreeSet () {
        return treeReprMap.keySet();
    }

    /**
     *  Given a dependency tree, returns its linearization as
     *  an English phrase, or <CODE>null</CODE> if this tree does
     *  not occur in the current corpus.
     */

    public String findRepr(IceTree tree) {
        return treeReprMap.get(tree);
    }

    /**
     *  Given a linearized English phrase derived from a
     *  dependency tree in the selected corpus, returns the original tree.
     */

    public List<IceTree> findTree(String repr) {
        String norm = normalizeRepr(repr);
        List<IceTree> trees = reprTreeMap.get(norm);
	return trees;
    }

    /**
     *  Given a dependency tree in the current corpus, returns an
     *  example sentence containing tha tree.
     */

    public String findExample(String tree) {
        return treeExampleMap.get(tree);
    }

    public void clear() {
        treeReprMap.clear();
        reprTreeMap.clear();
        treeExampleMap.clear();
	logger.info ("Clearing reprs and tooltips");
    }

    public void unpersist() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        try {
            File f = new File(fileName);
            f.delete();
            previousFileName = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Saves the mapping between trees, linearizations, and examples for the
     *  currently selected corpus to a file.
     */

    public void persist() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            for (IceTree tree : treeReprMap.keySet()) {
                if (treeExampleMap.containsKey(tree)) {
                    pw.println(String.format("%s:::%s:::%s", tree,
                            treeReprMap.get(tree),
                            treeExampleMap.get(tree)));
                }
            }
            pw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setMaps (IceTree tree, String repr, String example) {
	treeReprMap.put(tree, repr);
	String normalizedRepr = normalizeRepr(repr);
	if (!reprTreeMap.containsKey(normalizedRepr)) {
	    reprTreeMap.put(normalizedRepr, new ArrayList());
	}
	reprTreeMap.get(normalizedRepr).add(tree);
	treeExampleMap.put(tree, example);
	tree.setExample(example);
    }

    public boolean load () {
	return load (false);
    }

    public boolean forceLoad () {
	return load (true);
    }

    /**
     *  Loads the mappings between trees, linearizations, and examples for the
     *  currently selected corpus from a file.  Skips the load if the mappings
     *  have been previously loaded from the same file.
     *
     *  @param  always  if true, the load is performed uunconditionally;  if false,
     *                  the load is skipped if the file being requested is the same as
     *                  the most recent previous request
     *
     *  @return  true if the load was successful
     */

    public boolean load (boolean always) {
        String fileName = FileNameSchema.getEventReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        if (previousFileName != null && previousFileName.equals(fileName) && treeExampleMap.size() > 0) return true; 
        logger.info ("Loading reprs and tooltips from file {}", fileName);
        treeExampleMap.clear();
        treeReprMap.clear();
        reprTreeMap.clear();
        int count = 0;
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(":::");
                if (parts.length != 3) {
                    logger.warn ("Invalid input to DepTreeMap: {}", line);
                    continue;
                }
                String tree = parts[0];
                IceTree it = IceTreeFactory.getIceTree(tree);
                String repr = parts[1];
                String example = parts[2];
                treeReprMap.put(it, repr);
                String normalizedRepr = normalizeRepr(repr);
                if (!reprTreeMap.containsKey(normalizedRepr)) {
                    reprTreeMap.put(normalizedRepr, new ArrayList());
                }
                reprTreeMap.get(normalizedRepr).add(it);
                treeExampleMap.put(it, example);
                it.setExample(example);
                count++;
                if (count % 100000 == 0) {
                    System.out.print(count + "...");
                }
            }
            System.out.println();
            r.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        previousFileName = fileName;
        return true;
    }

    /**
     Normalize the internal copy of DepTree representation: all lowercase, single space, trimmed.
     Used by find and add.
     */

    public static String normalizeRepr(String repr) {
        repr = PhraseLemmatizer.lemmatize(repr);
        String[] tokens = repr.split("\\s+");
        StringBuffer sb = new StringBuffer();
        for (String token : tokens) {
            if (entityTypesList.contains(token.toUpperCase())) {
                sb.append(token.toUpperCase());
            } else {
                sb.append(token.toLowerCase());
            }
            sb.append(" ");
        }
        // return repr.toLowerCase().replaceAll("\\s+", " ").trim();
        return sb.toString().trim();
    }

    static public List<String> entityTypesList = 
        Arrays.asList(new String[]{"PERSON", "GPE", "ORGANIZATION", "WEA", "VEH"});

    /** 
     *  Find the English phrase most similar to 'repr' (as measured by edit
     *  distance) which is the representation of a dependency tree.
     */

    public String findClosestTree (String repr) {
        String norm = normalizeRepr(repr);
        int distance = 1000;
        String closest = null;
        for (String p : reprTreeMap.keySet()) {
            int d = minDistance (norm, normalizeRepr(p));
            if (d < distance) {
                distance = d;
                closest = p;
            }
        }
        return closest;
    }

    /**
     *  maximum length of arguments to minDistance
     */
    static final int MAX_MINDISTANCE = 100;

    private static int[][] dp = new int[MAX_MINDISTANCE][MAX_MINDISTANCE];

    /**
     * Computes the minimum edit distance between strings 'word1' and 'word2'.
     */

    public static int minDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();
        if (len1 >= MAX_MINDISTANCE || len2 >= MAX_MINDISTANCE) 
            return 1001;

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        //iterate through, and check last char
	for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                //if last two chars equal
                if (c1 == c2) {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }
        return dp[len1][len2];
    }
}
