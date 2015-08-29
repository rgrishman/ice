package edu.nyu.jet.ice.utils;

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

    private static String workingDirectory = "";
    private static String dataDirectory = "";
    private static String entitySetIndexPrefix = "EntitySetIndex_";
    private static String infoSuffix = ".info";

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
	String wd = workingDirectory;
	if (wd.length() > 0) { wd += "/"; }
	return wd;
    }

    public static String getWDnoSep() {
	return workingDirectory;
    }

    public static void setWD(String wd) {
	workingDirectory = wd;
	CACHE_ROOT = wd + File.separator + "cache";
    }

    public static void setDD (String dd) {
	dataDirectory = dd;
    }

    public static String getDD() {
	String dd = dataDirectory;
	if (dd.length() > 0) {
	    dd += File.separator;
	}
	return dd;
    }

    public static String getCacheRoot() {
        return CACHE_ROOT;
    }

    public static String getPreprocessCacheDir(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "preprocess";
    }

    public static String getSourceCacheDir(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "sources";
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

    public static String getRelationPathListFileName(String corpusName) {
        return CACHE_ROOT + File.separator + corpusName + File.separator + "RelationPathList";
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

    public static boolean isEntitySetIndexFileName (String fileName) {
	return (fileName.startsWith(entitySetIndexPrefix) && !fileName.endsWith(infoSuffix));
    }

   public static String getEntitySetIndexFileNameAlone (String inType, double cutoff) {
	String cutoffStr = String.valueOf(cutoff).replace('.', 'p');
        return entitySetIndexPrefix + inType + "_" + cutoffStr;
    }

    public static String getEntitySetIndexType (String indexFileName) {
	String type = "";
	String[] indexInfoStr = indexFileName.split("_");
	if (indexInfoStr.length > 2) {
	    type = indexInfoStr[1];
	}
	return type;
    }

    public static double getEntitySetIndexCutoff (String indexFileName) {
	double cutoff = 0.0;
	String[] indexInfoStr = indexFileName.split("_");
	if (indexInfoStr.length > 2) {
	    cutoff = Double.parseDouble(indexInfoStr[2].replace('p', '.'));
	}
	return cutoff;
    }

    public static String getPatternRatioFileName(String corpusName, String bgCorpusName) {
        return CACHE_ROOT + File.separatorChar + corpusName + File.separator + bgCorpusName + "-Pattern-Ratio";
    }

    public static String getTermsRatioFileName(String corpusName, String bgCorpusName) {
        return CACHE_ROOT + File.separatorChar + corpusName + File.separator + bgCorpusName + "-Ratio";
    }

    public static String getSortedPatternRatioFileName(String corpusName, String bgCorpusName) {
        return getPatternRatioFileName(corpusName, bgCorpusName) + ".sorted";
    }
}
