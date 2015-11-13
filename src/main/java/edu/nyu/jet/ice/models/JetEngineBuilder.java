package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.uicomps.Ice;

import java.util.*;
import java.io.*;

public class
JetEngineBuilder {

    static Properties props;
    static String dataDirectory;
    private static final boolean RECORD_NOUNS_AS_ENAMEX = true;

    /**
     * writes files to be read by Jet containing information on
     * entity sets and relations.
     */

    public static void build() {
        try {
            String jetHome = System.getProperty("jetHome");
            if (jetHome == null)
                jetHome = "";
            else
                jetHome += "/";
            props = new Properties();
            props.load(new FileReader("parseprops"));
            dataDirectory = jetHome + props.getProperty("Jet.dataPath") + "/";

            if (!RECORD_NOUNS_AS_ENAMEX) {
                buildEDTtypeFile();
            }
            buildOnoma();
            buildRelationPatternFile();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * writes auxEDTtypeFile containing common nouns in new
     * entity types.
     */

    public static void buildEDTtypeFile() throws IOException {
        String fileName = dataDirectory +
                props.getProperty("Ace.EDTtype.auxFileName");
        PrintWriter pw = new PrintWriter(new FileWriter(fileName));
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

    public static void buildOnoma() throws IOException {
        String fileName = dataDirectory +
                props.getProperty("Onoma.fileName");
        buildOnomaFromNames(fileName, new ArrayList<String>(Ice.entitySets.keySet()));
    }

    public static void buildOnomaFromNames(String fileName, List<String> entitySetNames) throws IOException {
        List<IceEntitySet> entitySets = new ArrayList<IceEntitySet>();
        for (String type : entitySetNames) {
            IceEntitySet es = Ice.entitySets.get(type);
            if (null != es) {
                entitySets.add(es);
            }
        }
        buildOnoma(fileName, entitySets);
    }

    public static void buildOnoma(String fileName, List<IceEntitySet> entitySets) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(fileName));
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

    public static void buildRelationPatternFile() throws IOException {
        String fileName = dataDirectory +
                props.getProperty("Ace.RelationModel.fileName");
        buildRelationPatternFileFromNames(fileName, new ArrayList<String>(Ice.relations.keySet()));
    }

    public static void buildRelationPatternFileFromNames(String fileName, List<String> relationNames) throws IOException {
        List<IceRelation> relations = new ArrayList<IceRelation>();
        for (String type : relationNames) {
            IceRelation rs = Ice.relations.get(type);
            if (null != rs) {
                relations.add(rs);
            }
        }
        buildRelationPatternFile(fileName, relations);
    }

    public static void buildRelationPatternFile(String fileName, List<IceRelation> relations) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(fileName));
        for (IceRelation rs : relations) {
            List<String> paths = rs.getPaths();
            String type = rs.getName();
            for (String path : paths) {
                String pattern = rs.arg1type + "--" + path +
                        "--" + rs.arg2type;
                pw.println(pattern + "\t" + type);
            }
        }
        pw.close();
        pw = new PrintWriter(new FileWriter(fileName + ".neg"));
        for (IceRelation rs : relations) {
            List<String> paths = rs.getNegPaths();
            if (rs != null) {
                String type = rs.getName();
                for (String path : paths) {
                    String pattern = path.replaceAll(" -- ", "--");
                    pw.println(pattern + "\t" + type);
                }
            }
        }
        pw.close();
    }

}
