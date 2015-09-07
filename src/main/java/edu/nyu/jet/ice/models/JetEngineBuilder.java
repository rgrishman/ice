package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;

import java.util.*;
import java.io.*;

public class JetEngineBuilder {

    static Properties props;
    private static final boolean RECORD_NOUNS_AS_ENAMEX = true;

    /**
     * writes files to be read by Jet containing information on
     * entity sets and relations.
     */

    public static void load() {
        try {
	    props = new Properties();
	    props.load(new FileReader(FileNameSchema.getWD() + "parseprops"));
	    String dataPath = props.getProperty("Jet.dataPath");
	    if (dataPath.substring(0,1).equals("/")) {
		FileNameSchema.setDD(dataPath);
	    } else {
		FileNameSchema.setDD(FileNameSchema.getWD() + dataPath);
	    }
            if (!RECORD_NOUNS_AS_ENAMEX) {
                buildEDTtypeFile();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    public static void build() {
	load();
	try {
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
        String fileName = FileNameSchema.getDD() +
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
        String fileName = FileNameSchema.getDD() +
                props.getProperty("Onoma.fileName");
        buildOnomaFromNames(fileName, new ArrayList<String>(Ice.entitySets.keySet()));
    }

    public static String getOnomaFileName() {
	return FileNameSchema.getDD() + props.getProperty("Onoma.fileName");
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

    public static String getRelationPatternFileName() {
	return FileNameSchema.getDD() +
                props.getProperty("Ace.RelationModel.fileName");
    }

    public static void buildRelationPatternFile() throws IOException {
	buildRelationPatternFileFromNames(getRelationPatternFileName(), new ArrayList<String>(Ice.relations.keySet()));
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
        Set<String> printedPaths = new HashSet<String>();
        for (IceRelation rs : relations) {
            List<String> paths = rs.getPaths();
            String type = rs.getName();
            for (String path : paths) {
                String pattern = rs.arg1type + "--" + path +
                        "--" + rs.arg2type;
                if (printedPaths.contains(pattern)) {
                    continue;
                }
                pw.println(pattern + "\t" + type);
                printedPaths.add(pattern);
            }
        }
        pw.close();
	pw = new PrintWriter(new FileWriter(fileName + ".neg"));
	for (IceRelation rs : relations) {
	    if (rs.getNegPaths() != null && rs.getNegPaths().size() > 0) {
		List<String> paths = rs.getNegPaths();
		String type = rs.getName();
		for (String path : paths) {
		    String pattern = path.replaceAll(" \\-\\- ", "--");
		    pw.println(pattern + "\t" + type);
		}
	    }
        }
	pw.close();
    }

}
