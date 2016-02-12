package edu.nyu.jet.ice.models;

import AceJet.AnchoredPath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;

import java.io.*;
import java.util.*;

/**
 * DepPathMap maintains maps between depPaths, linearizations, and example sentences. 
 * It originally computed linearizations and saved them for each path. Linearization 
 * is currently handled by the DepPath class, so DepPathMap only
 * handles serialization/deserialization of the map.
 */

public class DepPathMap {
    private static DepPathMap instance = null;

    private HashMap<String, String> pathReprMap = new HashMap<String, String>();
    private HashMap<String, List<String>> reprPathMap = new HashMap<String, List<String>>();
    private HashMap<String, String> pathExampleMap = new HashMap<String, String>();
    private DepPathMap() { }
    private String previousFileName = null;

    private Set<String> leftRelations = new HashSet<String>();
    {
        leftRelations.add("nsubj");
        leftRelations.add("amod");
        leftRelations.add("advmod");
        leftRelations.add("auxpass");
        leftRelations.add("cop");
        leftRelations.add("det");
        leftRelations.add("expl");
        leftRelations.add("neg");
        leftRelations.add("nn");
        leftRelations.add("num");
        leftRelations.add("number");
        leftRelations.add("poss");
        leftRelations.add("preconj");
        leftRelations.add("quantmod");
    }

    /**
    DepPathMap is a singleton which is shared across the program.
     */
    public static DepPathMap getInstance() {
        if (instance == null) {
            instance = new DepPathMap();
        }
        return instance;
    }

    /**
     *  Given a dependency path, returns its linearization as
     *  an English phrase, or <CODE>null</CODE> if this path does
     *  not occur in the current corpus.
     */

    public String findRepr(String path) {
        return pathReprMap.get(path);
    }

    /**
     *  Given a linearized English phrase derived from a
     *  dependency path in the selected corpus, returns the original path.
     */

    public List<String> findPath(String repr) {
        return reprPathMap.get(normalizeRepr(repr));
    }

    /**
     *  Given a dependency path in the current corpus, returns an
     *  example sentence containing tha path.

    public String findExample(String path) {
        return pathExampleMap.get(path);
    }

    public void clear() {
        pathReprMap.clear();
        reprPathMap.clear();
        pathExampleMap.clear();
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
     *  Saves the mapping between paths, linearizations, and examples for the
     *  currently selected corpus to a file.
     */

    public void persist() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            for (String path : pathReprMap.keySet()) {
                if (pathExampleMap.containsKey(path)) {
                    pw.println(String.format("%s:::%s:::%s", path,
                            pathReprMap.get(path),
                            pathExampleMap.get(path)));
                }
            }
            pw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Loads the mappings between paths, linearizations, and examples for the
     *  currently selected corpus.
     */

    public boolean forceLoad() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        // if (previousFileName != null && previousFileName.equals(fileName)) return true; // use old data
        pathExampleMap.clear();
        pathReprMap.clear();
        reprPathMap.clear();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(":::");
                if (parts.length != 3) continue;
                String path = parts[0];
                String repr = parts[1];
                String example = parts[2];
                pathReprMap.put(path, repr);
                String normalizedRepr = normalizeRepr(repr);
                if (!reprPathMap.containsKey(normalizedRepr)) {
                    reprPathMap.put(normalizedRepr, new ArrayList());
                }
                reprPathMap.get(normalizedRepr).add(path);
                pathExampleMap.put(path, example);
            }
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
     *  Loads the mappings between paths, linearizations, and examples for the
     *  currently selected corpus from a file.  Skips the load if the mappings
     *  have been previously loaded from the same file.
     */

    public boolean load() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        if (previousFileName != null && previousFileName.equals(fileName) && pathExampleMap.size() > 0) return true; // use old data
        pathExampleMap.clear();
        pathReprMap.clear();
        reprPathMap.clear();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(":::");
                if (parts.length != 3) continue;
                String path = parts[0];
                String repr = parts[1];
                String example = parts[2];
                pathReprMap.put(path, repr);
                String normalizedRepr = normalizeRepr(repr);
                if (!reprPathMap.containsKey(normalizedRepr)) {
                    reprPathMap.put(normalizedRepr, new ArrayList());
                }
                reprPathMap.get(normalizedRepr).add(path);
                pathExampleMap.put(path, example);
            }
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
     Normalize the internal copy of DepPath representation: all lowercase, single space, trimmed.
     Used by find and add.
     */

    public static String normalizeRepr(String repr) {
        return repr.toLowerCase().replaceAll("\\s+", " ").trim();
    }

}
