package Jet.Dictionary;

import Jet.Lex.Lexicon;
import Jet.Lisp.FeatureSet;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yhe
 * Date: 9/19/13
 * Time: 9:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class NamedEntityDictionary {
    private TObjectIntHashMap map = new TObjectIntHashMap();
    private TObjectIntHashMap tagsMap = new TObjectIntHashMap();
    private TObjectIntHashMap nameMap = new TObjectIntHashMap();
    private Map<String, String> userMap = new HashMap<String, String>();
    private static final int PERSON_MASK = 1;
    private static final int ORGANIZATION_MASK = 2;
    private static final int GPE_MASK = 4;
    private static final int OTHER_MASK = 8;

    public boolean isPerson(String s) {
        return (PERSON_MASK & map.get(s)) != 0;
    }

    public boolean isOrganization(String s) {
        return (ORGANIZATION_MASK & map.get(s)) != 0;
    }

    public boolean isGPE(String s) {
        return (GPE_MASK & map.get(s)) != 0;
    }

    public boolean isOther(String s) {
        return (OTHER_MASK & map.get(s)) != 0;
    }

    public boolean isName(String s) {
        return nameMap.get(s) != 0;
    }

    public void loadLexicon() {
        for (Object ne : map.keys()) {
            int types = map.get(ne);
            String multi = ((String) ne).trim().split(" ").length > 1 ? "t" : "f";

            if ((PERSON_MASK & types) != 0) {
                Lexicon.addEntry(((String) ne).trim().split(" "),
                        new FeatureSet("multi", multi),
                        "wikiNEPerson");
            }
            if ((ORGANIZATION_MASK & types) != 0) {
                Lexicon.addEntry(((String) ne).trim().split(" "),
                        new FeatureSet("multi", multi),
                        "wikiNEOrganization");
            }
            if ((GPE_MASK & types) != 0) {
                Lexicon.addEntry(((String) ne).trim().split(" "),
                        new FeatureSet("multi", multi),
                        "wikiNEGPE");
            }
            if ((OTHER_MASK & types) != 0) {
                Lexicon.addEntry(((String) ne).trim().split(" "),
                        new FeatureSet("multi", multi),
                        "wikiNEOther");
            }
        }
        for (Object name : nameMap.keys()) {
            Lexicon.addEntry(((String) name).trim().split(" "),
                    new FeatureSet(),
                    "wikiPersonName");
        }
    }

    private void processEntry(String line, TObjectIntHashMap map) {
        try {
//            if (line.startsWith("Planned")) {
//                System.err.println("Debug");
//            }
            String[] parts = line.split("\t");
            String candidate = parts[0];
            String tag = parts[1];
            String[] candidates = parseCandidate(candidate);
            //if (!tag.equals("GPE")) {
            //}
            for (String c : candidates) {
//                System.err.println(c.trim());
                int val = map.get(c.trim());
                map.put(c.trim().replaceAll("-", " - "), val | tagsMap.get(tag));
            }
            if (tag.equals("Person")) {
                String[] names = candidates[0].split(" ");
                for (String name : names) {
                    nameMap.put(name.trim().replaceAll("-", " - "), 1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] parseCandidate(String candidate) throws UnsupportedEncodingException {
        String result = URLDecoder.decode(candidate, "UTF-8").replace("_", " ");
        result = result.split("\\(")[0];
        String[] candidates = result.split(",");
        return new String[]{candidates[0]};
    }

    public NamedEntityDictionary(String fileName) {
        tagsMap.clear();
        tagsMap.put("UNK", 0);
        tagsMap.put("Person", PERSON_MASK);
        tagsMap.put("Organization", ORGANIZATION_MASK);
        tagsMap.put("GPE", GPE_MASK);
        tagsMap.put("OTHER", OTHER_MASK);
        String line = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                processEntry(line, map);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserType(String s) {
        return userMap.get(s);
    }

    public NamedEntityDictionary(String fileName, List<String> userFileNames) {
        this(fileName);
        String line;
        try {
            for (String userFileName : userFileNames) {
                BufferedReader br = new BufferedReader(new FileReader(userFileName));
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    String[] parts = line.split("\t");
                    userMap.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.err.print("Loading dictionary... ");
        NamedEntityDictionary dict = new NamedEntityDictionary(args[0]);
//        try {
//            dict.parseCandidate("Planned_Parenthood\tOrganization");
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
        System.err.println("Loaded.");
        System.err.println(dict.map.get("Planned Parenthood"));
    }
}
