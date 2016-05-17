package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.uicomps.Ice;
import java.io.File;
import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;

/**
 * Created with IntelliJ IDEA.
 * User: joelsieh
 * Date: 9/9/14
 * Time: 10:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileNameSchema {
    private static String CACHE_ROOT = "cache";
    private static String entitySetIndexPrefix = "EntitySetIndex_";

    static {
        File cacheFile = new File(CACHE_ROOT);

        if(!cacheFile.exists()) {
            try {
                //Files.createDirectory(Paths.get(CACHE_ROOT));
                cacheFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getWD() {
	return Ice.workingDirectory;
    }

    public static String getCacheRoot() {
        return CACHE_ROOT;
    }

    public static String getCorpusNameFromDocList (String docListName) {
	return docListName.split(File.separator)[0].split("\\.")[0];
    }

    public static String getCorpusNameFromTermsFile (String termsFileName) {
	return termsFileName.split(File.separator)[1];
    }

    public static String getCorpusNameFromWordCountFileName (String wcfName) {
	return wcfName.split(File.separator)[1];
    }

    public static String getCorpusNameFromPatternFileName (String patternFileName) {
	return patternFileName.split(File.separator)[1];
    }

    public static String getPreprocessCacheDir(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "preprocess";
    }

    public static String getWordCountFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "counts";
    }

    public static String getDocListFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "docList";
    }

    public static String getPreprocessedDocListFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "docList.local";
    }

    public static String getTermsFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "terms";
    }

    public static String getRelationsFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "Relations";
    }

    public static String getRelationTypesFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "Relationtypes";
    }

    public static String getRelationReprFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "RelationRepr";
    }

    public static String getCorpusInfoDirectory(String corpusName) {
        return CACHE_ROOT + File.separatorChar + corpusName;
    }

    public static String getEntitySetIndexFileName(String corpusName, String inType) {
	return getEntitySetIndexFileName(corpusName, inType, 3.0);
    }
    public static String getEntitySetIndexFileName(String corpusName, String inType, Double cutoff) {
	String cutoffStr = String.valueOf(cutoff).replace('.', 'p');
        return CACHE_ROOT + File.separatorChar + corpusName + File.separator + entitySetIndexPrefix + inType + "_" + cutoffStr;
    }

public static String getPatternRatioFileName(String corpusName, String bgCorpusName) {
        return CACHE_ROOT + File.separatorChar + corpusName + File.separator + bgCorpusName + "-Pattern-Ratio";
    }

    public static String getSortedPatternRatioFileName(String corpusName, String bgCorpusName) {
        return getPatternRatioFileName(corpusName, bgCorpusName) + ".sorted";
    }
}
