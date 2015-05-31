package edu.nyu.jet.ice.models;

import AceJet.AnchoredPath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;

import java.io.*;
import java.util.*;

/**
 * Created by yhe on 3/6/14.
 */
public class DepPathMap {
    private static DepPathMap instance = null;

    private TreeMap<String, String> pathReprMap = new TreeMap<String, String>();
    private TreeMap<String, List<String>> reprPathMap = new TreeMap<String, List<String>>();
    private TreeMap<String, String> pathExampleMap = new TreeMap<String, String>();
    private DepPathMap() { }
    private String previousFileName = null;

    public enum AddStatus {
        SUCCESSFUL,
        EXIST,
        ILLEGAL
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
    public static DepPathMap getInstance() {
        if (instance == null) {
            instance = new DepPathMap();
        }
        return instance;
    }

    public String findRepr(String path) {
        return pathReprMap.get(lemmatize(path));
    }

    public List<String> findPath(String repr) {
        return reprPathMap.get(normalizeRepr(repr));
    }

    public String findExample(String path) {
        return pathExampleMap.get(lemmatize(path));
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
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        if (previousFileName != null && previousFileName.equals(fileName)) return true; // use old data
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

    public Set<String> reprs() {
        return reprPathMap.keySet();
    }

//    public AddStatus addPath(String typedPath, String example) {
//        // Assuming path has form "TYPE -- path -- TYPE"
////        String[] parts = typedPath.split(" -- ");
////        if (parts.length != 3) {
////            return AddStatus.ILLEGAL;
////        }
////        String path = parts[0] + ":" + AnchoredPath.lemmatizePath(parts[1]) + ":" + parts[2];
//        //String repr = linearize(path.replaceAll(" -- ", ":"));
//        String path = lemmatize(typedPath);
//        if (path ==  null) {
//            return AddStatus.ILLEGAL;
//        }
//        String repr = linearize(path.replaceAll(" -- ", ":"));
//        if (repr.equals("")) return AddStatus.ILLEGAL;
//        String normalizedRepr = normalizeRepr(repr);
//        // if (reprPathMap.containsKey(normalizedRepr)) return AddStatus.EXIST;
//        if (!reprPathMap.containsKey(normalizedRepr)) {
//            reprPathMap.put(normalizedRepr, new ArrayList<String>());
//        }
//        pathReprMap.put(path, repr);
//        reprPathMap.get(normalizedRepr).add(path);
//        pathExampleMap.put(path, example);
//        return AddStatus.SUCCESSFUL;
//    }

    private String lemmatize(String path) {
        String[] parts = path.split(" -- ");
        if (parts.length != 3) {
            return null;
        }
        return parts[0] + " -- " + AnchoredPath.lemmatizePath(parts[1]) + " -- " + parts[2];
    }


    /**
     Normalize the internal copy of DepPath representation: all lowercase, single space, trimmed.
     Used by find and add.
     */
    public static String normalizeRepr(String repr) {
        return repr.toLowerCase().replaceAll("\\s+", " ").trim();
    }

//    public String linearize (String path) {
//        String[] pathArray = path.split(":");
//        LinkedList ll = new LinkedList(Arrays.asList(pathArray));
//        return linearize (ll);
//    }

    /**
     *  generate an English expression corresponding to the path 'path'
     *  in a dependency tree.
     */

//    String linearize (LinkedList<String> path) {
//        // if path has been reduced to a single string, we are done
//        if (path.size() == 1)
//            return path.get(0);
//	if (path.size() == 2) {
//            System.err.println ("*** path of length 2: " +
//		path.get(0) + ":" + path.get(1));
//            return "";
//	}
//        // if first dep relation in path is an inverse relation
//        // collapse it into a string, with dependent to left or
//        // right of head, depending on relation
//        if (isInverse(path.get(1))) {
//            String dep  = path.removeFirst();
//            String role = abs(path.removeFirst());
//            String head = path.removeFirst();
//            String result;
//            if (leftChild(role)) {
//                result = dep + lexicalContent(role, dep, head) + head;
//            } else {
//                result = head + lexicalContent(role, head, dep) + dep;
//            }
//            path.addFirst(result);
//            return linearize (path);
//        }
//        // if last dep relation in path is not an inverse relation
//        // collapse it into a string
//        if (!isInverse(path.get(path.size()-2))) {
//            String dep  = path.removeLast();
//            String role = path.removeLast();
//            String head = path.removeLast();
//            String result;
//            if (leftChild(role)) {
//                result = dep + lexicalContent(role, dep, head) + head;
//            } else {
//                result = head + lexicalContent(role, head, dep) + dep;
//            }
//            path.addLast(result);
//            return linearize (path);
//        }
//        // stuck ... some non-inverse relation precedes an
//        // inverse relatiom
//        System.err.print ("*** unexected path: " + path.get(0));
//	    for (int i = 1; i < path.size(); i++)
//		System.err.print (":" + path.get(i));
//	    System.err.println ();
//        return "";
//    }

    static boolean isInverse (String x) {
        return x.endsWith("-1");
    }

    /**
     *  if passed the name of an inverse relation, return the
     *  corresponding non-inverse relation, otherwise return the
     *  original relation name.
     */

    String abs(String x) {
        if (x.endsWith("-1"))
            return x.substring(0, x.length()-2);
        else
            return x;
    }

    /**
     *  return true if child should precede parent if connected
     *  by dep relation 'role'
     */

    boolean leftChild (String role) {
        return leftRelations.contains(role);
    }

    /**
     *   some reduced dep relations, such as 'prep_of', have
     *  lexical content;  return that content
     */

    static String lexicalContent (String role, String left, String right) {
        if (role.startsWith("prep_"))
            return " " + role.substring(5) + " ";
        if (role.equals("appos"))
            return " , ";
        if (role.startsWith("poss"))
            return " 's ";
        if (role.equals("conj") &&
                !(left.equals("and") || left.endsWith(" and") || left.equals("or") || left.endsWith(" or")) &&
                !(right.equals("and") || right.startsWith("and ") || right.equals("or") || right.endsWith(" or"))) {
            return " and ";
        }
        return " ";
    }

    public static void main (String[] args) {
//        DepPathMap depPathMap = new DepPathMap();
//        System.out.println(depPathMap.linearize("PERSON:prep_of:ORGANIZATION"));
//        System.out.println(depPathMap.linearize("GPE:amod-1:protesters:nsubj-1:demonstrate:prep_outside:FACILITY"));
//        System.out.println(depPathMap.linearize("PERSON:nsubj-1:eats:dobj:FOOD"));
//        System.out.println(depPathMap.linearize("GPE:amod-1:hit:prep_in-1:site:appos:LOCATION"));
//        System.out.println(depPathMap.linearize("GPE:amod-1:head:infmod:visit:dobj:GPE"));
    }

//    private String stemIfNecessary(String s) {
//        if (allUpper(s)) return s;
//        if (s.trim().contains(" ")) return s;
//        return stemmer.getStem(s, "NULL");
//    }

    private boolean allUpper(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isUpperCase(c)) return false;
        }
        return true;
    }

}
