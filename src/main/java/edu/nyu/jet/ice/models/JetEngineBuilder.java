package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.uicomps.Ice;

import java.util.*;
import java.io.*;

/**
 *  writes files to be read by Jet containing information on
 *  entity sets and relations.
 */

public class JetEngineBuilder {

    static final boolean RECORD_NOUNS_AS_ENAMEX = true;

    private static Writer commonNounWriter = null;
    private static Writer properNounWriter = null;
    private static Writer relationPatternWriter = null;
    private static Writer negatedPatternWriter = null;

    public static void setCommonNounWriter (Writer w) {
        commonNounWriter = w;}
    public static void setProperNounWriter (Writer w) {
        properNounWriter = w;}
    public static void setRelationPatternWriter (Writer w) {
        relationPatternWriter = w;}
    public static void setNegatedPatternWriter (Writer w) {
        negatedPatternWriter = w;}

    /**
     * writes files to be read by Jet containing information on
     * entity sets and relations.
     */

    public static void build() {
        try {
            String jetHome = System.getProperty("jetHome");
            if (jetHome == null) {
		System.err.println("JET_HOME not set, cannot export.");
		return;
	    }
            String dataDirectory = jetHome + "/data/";
            if (commonNounWriter == null) {
                String commonNounFileName = dataDirectory + "iceEDTtype.dict";
                commonNounWriter = new FileWriter(commonNounFileName);
            }
            if (properNounWriter == null) {
                String properNounFileName = dataDirectory + "iceOnoma.dict";
                properNounWriter = new FileWriter(properNounFileName);
            }
            if (relationPatternWriter == null) {
                String relationPatternFileName = dataDirectory +"ldpRelationModel";
                relationPatternWriter = new FileWriter(relationPatternFileName);
            }
            if (negatedPatternWriter == null) {
                String negatedPatternFileName = dataDirectory + "ldpNegRelations";
                negatedPatternWriter = new FileWriter(negatedPatternFileName);
            }
            if (!RECORD_NOUNS_AS_ENAMEX) {
                buildEDTtypeFile(commonNounWriter);
            }
            buildOnoma(properNounWriter);
            buildRelationPatternFile(relationPatternWriter, negatedPatternWriter);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * writes auxEDTtypeFile containing common nouns in new
     * entity types.
     */

    public static void buildEDTtypeFile(Writer w) throws IOException {
        PrintWriter pw = new PrintWriter(w);
        for (String type : Ice.entitySets.keySet()) {
            IceEntitySet es = Ice.entitySets.get(type);
            List<String> nouns = es.getNouns();
            for (String noun : nouns) {
                pw.println(noun + " | " + type + ":" + type + " 1");
            }
        }
        pw.close();
    }

    /**
     * writes onoma (name dictionary) containing names in new
     * entity types.
     */

    public static void buildOnoma (Writer w) throws IOException {
        buildOnomaFromNames(w, new ArrayList<String>(Ice.entitySets.keySet()));
    }

    public static void buildOnomaFromNames(Writer w, List<String> entitySetNames) throws IOException {
        List<IceEntitySet> entitySets = new ArrayList<IceEntitySet>();
        for (String type : entitySetNames) {
            IceEntitySet es = Ice.entitySets.get(type);
            if (null != es) {
                entitySets.add(es);
            }
        }
        buildOnoma(w, entitySets);
    }

    public static void buildOnoma(Writer w, List<IceEntitySet> entitySets) throws IOException {
        PrintWriter pw = new PrintWriter(w);
        for (IceEntitySet entitySet : entitySets) {
            List<String> names = entitySet.getNames();
            String type = entitySet.getType();
            for (String name : names) {
                pw.println(name + "\t" + type);
            }
            if (RECORD_NOUNS_AS_ENAMEX) {
                List<String> nouns = entitySet.getNouns();
                for (String noun : nouns) {
                    pw.println(noun + "\t" + type);
                }
            }
        }
        pw.close();
    }

    /**
     * writes file with patterns for new relations.
     */

    public static void buildRelationPatternFile(Writer w, Writer negw) throws IOException {
        buildRelationPatternFileFromNames(w, negw, new ArrayList<String>(Ice.relations.keySet()));
    }

    public static void buildRelationPatternFileFromNames(Writer w, Writer negw, List<String> relationNames) throws IOException {
        List<IceRelation> relations = new ArrayList<IceRelation>();
        for (String type : relationNames) {
            IceRelation rs = Ice.relations.get(type);
            if (null != rs) {
                relations.add(rs);
            }
        }
        buildRelationPatternFile(w, negw, relations);
    }

    public static void buildRelationPatternFile(Writer w, Writer negw, List<IceRelation> relations) throws IOException {
        PrintWriter pw = new PrintWriter(w);
        for (IceRelation rs : relations) {
            List<String> paths = rs.getCleanPaths();
            String type = rs.getName();
            for (String path : paths) {
                boolean inv = rs.invertedPath(path);
                String pattern = path.replaceAll(" -- ", "--");
                pw.println(pattern + " = " + type + (inv ? "-1" : ""));
            }
        }
        pw.close();
        pw = new PrintWriter(negw);
        for (IceRelation rs : relations) {
            List<String> paths = rs.getNegPaths();
            if (paths != null) {
                String type = rs.getName();
                for (String path : paths) {
                    boolean inv = rs.invertedPath(path);
                    String pattern = path.replaceAll(" -- ", "--");
                    pw.println(pattern + " = " + type + (inv ? "-1" : ""));
                }
            }
        }
        pw.close();
    }

}

