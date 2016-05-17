package edu.nyu.jet.ice.models;

import AceJet.AnchoredPath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;

import java.io.*;
import java.util.*;

/**
 * DepPathMap maintains a map between depPaths and linearizations. It originally computes linearizations
 * for each path and saves it. Linearization is currently handled by the DepPath class, so DepPathMap only
 * handles serialization/deserialization of the map.
 */
public class DepPathMap {

    private String corpusName;

    private static DepPathMap instance = null;

    private HashMap<String, String> pathReprMap = new HashMap<String, String>();
    private HashMap<String, List<String>> reprPathMap = new HashMap<String, List<String>>();
    private HashMap<String, String> pathExampleMap = new HashMap<String, String>();
    private DepPathMap(String corpusName) {
	this.corpusName = corpusName;
    }
    private String previousFileName = null;

    public void setCorpusName (String corpusName) {
	this.corpusName = corpusName;
    }

    public String getCorpusName () {
	return this.corpusName;
    }

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
    public static DepPathMap getInstance(String corpusName) {
        if (instance == null || !instance.corpusName.equals(corpusName)) {
            instance = new DepPathMap(corpusName);
        }
        return instance;
    }

    public String findRepr(String path) {
        return pathReprMap.get(path);
    }

    public List<String> findPath(String repr) {
        return reprPathMap.get(normalizeRepr(repr));
    }

    public String findExample(String path) {
        return pathExampleMap.get(path);
    }

    public void clear() {
        pathReprMap.clear();
        reprPathMap.clear();
        pathExampleMap.clear();
    }

    public void unpersist() {
        String fileName = FileNameSchema.getRelationReprFileName(this.corpusName);
        try {
            File f = new File(fileName);
            f.delete();
            previousFileName = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void persist() {
        String fileName = FileNameSchema.getRelationReprFileName(this.corpusName);
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

    public boolean forceLoad() {
        String fileName = FileNameSchema.getRelationReprFileName(this.corpusName);
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
//                if (repr.equals("PERSON sell DRUGS")) {
//                    System.err.println(path + " <> " + repr);
//                }
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

    public boolean load() {
        String fileName = FileNameSchema.getRelationReprFileName(this.corpusName);
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
//                if (repr.equals("PERSON sell DRUGS")) {
//                    System.err.println(path + " <> " + repr);
//                }
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

//    private String lemmatize(String path) {
//        String[] parts = path.split(" -- ");
//        if (parts.length != 3) {
//            return null;
//        }
//        return parts[0] + " -- " + AnchoredPath.lemmatizePath(parts[1]) + " -- " + parts[2];
//    }


    /**
     Normalize the internal copy of DepPath representation: all lowercase, single space, trimmed.
     Used by find and add.
     */
    public static String normalizeRepr(String repr) {
        return repr.toLowerCase().replaceAll("\\s+", " ").trim();
    }



}
