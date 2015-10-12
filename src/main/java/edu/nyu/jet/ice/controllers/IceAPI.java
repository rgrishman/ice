package edu.nyu.jet.ice.controllers;

import edu.nyu.jet.ice.entityset.Entity;
import edu.nyu.jet.ice.entityset.EntitySetExpander;
import edu.nyu.jet.ice.entityset.EntitySetIndexer;
import edu.nyu.jet.ice.models.*;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.IceInfoStatus;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.DummyMonitor;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.terminology.Term;
import edu.nyu.jet.ice.terminology.TermRanker;
import org.ho.yaml.YamlDecoder;
import org.ho.yaml.YamlEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by jsieh on 6/12/14.
 * Extensively adapted and expanded by grvsmth, 2014-2015.
 */
public class IceAPI {

    protected static Logger log = LoggerFactory.getLogger(IceAPI.class);
    private static String configFile;
    private String workingDirectory;

    public IceAPI() {
    }

    // Need to initialize some of the same members as Ice.main() does.
    public void init(String wd, String configFileName) {
	File kashrutFile = new File("/misc/proteus108/angus/ice/test/cache/");
	log.info("init(" + wd + ", " + configFileName + ")");
	this.workingDirectory = wd;
	log.info("Setting working directory to " + this.workingDirectory);
	configFile = this.workingDirectory + File.separator + configFileName;
	log.info("configFile = " + configFile);


	if (null == System.getProperty("jetHome")) {
	    System.setProperty("jetHome", System.getenv().get("JET_HOME"));
	}

	File cacheRootFile = new File(FileNameSchema.getCacheRoot());
	if (!cacheRootFile.exists()) {
	    cacheRootFile.mkdir();
	} else if (!cacheRootFile.isDirectory()) {
	    log.error("Cache root file is not a directory!");
	    System.exit(0);
	}
	log.info(Integer.toString(kashrutFile.listFiles().length) + " directories in cache" );

        Ice.loadConfig(configFile);
	log.info("Ice.loadConfig() complete");
	log.info(Integer.toString(kashrutFile.listFiles().length) + " directories in cache" );
	if (null != workingDirectory && workingDirectory.length() > 0) {
	    setWorkingDirectory(workingDirectory);
	}
	log.info("setWorkingDirectory(" + workingDirectory + ") complete");
	log.info(Integer.toString(kashrutFile.listFiles().length) + " directories in cache" );
        if (!Ice.corpora.isEmpty()) {
	    log.debug ("Selecting first key: " + Ice.corpora.firstKey());
            selectCorpus(Ice.corpora.firstKey());
        }
    }

    public void clear() {
	FileNameSchema.setWD("");
	configFile = null;
	Ice.selectedCorpus = null;
	Ice.selectedCorpusName = null;
	Ice.corpora = null;
	Ice.entitySets = null;
	Ice.relations = null;
    }


    public String wd() { 
	String wd = FileNameSchema.getWD();
	if (null != this.workingDirectory && this.workingDirectory.length() > 0) {
	    wd = this.workingDirectory + File.separator;
	}
	return wd;
    }

    public Corpus getCorpus (String corpusName) {
	return Ice.corpora.get(corpusName);
    }

    public SortedMap<String, Corpus> getCorpora() {
	return Ice.corpora;
    }

    public Corpus selectCorpus (String corpusName) {
	log.info("selectCorpus(" + corpusName + ")");
	if (null == corpusName || corpusName.equals("null")) {
	    log.info("null selection");
	    Ice.selectedCorpus = null;
	    Ice.selectedCorpusName = null;
	    return null;
	} else if (Ice.corpora.containsKey(corpusName)) {
	    Corpus corpus = Ice.corpora.get(corpusName);
	    log.debug("selectCorpus " + corpusName);
	    Ice.selectedCorpus = corpus;
	    Ice.selectedCorpusName = corpusName;
	    return Ice.selectedCorpus;
	} else {
	    log.error("Corpus " + corpusName + " not found!");
	    return null;
	}
    }

    public String getSelectedCorpus() {
	return Ice.selectedCorpusName;
    }

    public void setWorkingDirectory(String wd) {
	this.workingDirectory = wd;
	FileNameSchema.setWD(wd);
    }

    public String getWorkingDirectory() {
	return this.workingDirectory;
    }

    public Corpus setBackgroundCorpus (String selected, String corpusName) {
        Corpus corpus = Ice.corpora.get(corpusName);
	log.debug("setBackgroundCorpus for " + selected + " to " + corpusName);
        if (null == corpus) {
	    addCorpus(corpusName, "?");
        }
	Corpus fgCorpus = Ice.corpora.get(selected);
	if (null == fgCorpus) {
	    log.error("Can't find foreground corpus " + fgCorpus);
	} else {
	    fgCorpus.setBackgroundCorpus(corpusName);
	}
        return corpus;
    }

    public String getBackgroundCorpus(String corpusName) {
	String bgCorpus = "";
	if (null != corpusName) {
	    try {
		bgCorpus = Ice.corpora.get(corpusName).getBackgroundCorpus();
	    } catch (Exception e) {
		log.error("Can't get background corpus for " + corpusName + ": " + e.getMessage());
	    }
	}
	return bgCorpus;
    }

    public int getNumberOfCorpusDocs(String corpusName) {
        return Ice.corpora.get(corpusName).numberOfDocs;
    }

    public SortedMap<String, IceRelation> getRelations() {
	// ABG
	return Ice.relations;
    }

    public void addCorpus(String corpusName, String corpusDir) {
        // Set the new corpus to select all files in its directory structure by default.
	addCorpus(corpusName, corpusDir, "*");
    }

    public void addCorpus(String corpusName, String corpusDir, String filter) {
        Corpus newCorpus = new Corpus(corpusName);
	log.debug("addCorpus " + corpusName + " with directory " + corpusDir + " and filter " + filter);
	newCorpus.setDirectory(corpusDir);

        newCorpus.setFilter(filter);
        newCorpus.writeDocumentList();
        Ice.corpora.put(corpusName, newCorpus);
    }

    public Corpus removeCorpus(String corpusName) {
        return Ice.corpora.remove(corpusName);
    }

    public boolean renameCorpus(String originalName, String newName) {
        if (originalName.equals(newName) || Ice.corpora.containsKey(newName)) {
            return false;
        }
        Corpus corpus = Ice.corpora.remove(originalName);
        if (null != corpus) {
            File originalCorpusDir = new File(FileNameSchema.getCorpusInfoDirectory(originalName));
	    String newCorpusInfoDirName = FileNameSchema.getCorpusInfoDirectory(newName);
            File newCorpusDir = new File(newCorpusInfoDirName);
            originalCorpusDir.renameTo(newCorpusDir);

            corpus.setName(newName);
            corpus.setWordCountFileName(FileNameSchema.getWordCountFileName(newName));
	    corpus.setTermFileName(FileNameSchema.getTermsFileName(newName));
	    if (corpus.getDocListFileName().equals(FileNameSchema.getDocListFileName(originalName))) {
		corpus.setDocListFileName(FileNameSchema.getDocListFileName(newName));
	    } else if (corpus.getDocListFileName().equals(FileNameSchema.getPreprocessedDocListFileName(originalName))) {
		corpus.setDocListFileName(FileNameSchema.getPreprocessedDocListFileName(newName));
	    } else {
		log.error("docListFileName schema unrecognized.  Unable to change to reflect new name");
	    }
            Ice.corpora.put(newName, corpus);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Just in case you want to set a file name suffix filter on your corpus dir.
     *
     * @param corpusName The name of the corpus
     * @param filter The suffix you want to match files against.  Matches files that end with ".&lt;filter&gt;"
     */
    public void setFilter(String corpusName, String filter) {
	log.debug("Setting filter " + filter + " for corpus " + corpusName);
        getCorpus(corpusName).setFilter(filter);
    }

    public List<String> getCorpusNames() {
	ArrayList<String> corpusNames = new ArrayList(Ice.corpora.keySet());
        return corpusNames;
    }


    public boolean loadState(String fileName) {

	// ABG 150118 change to return success boolean so that we can
	// create the yaml file if it doesn't exist.
	log.info("Retrieving config from " + fileName);
	boolean success = false;
        try {
            File yamlFile = new File(fileName);
            InputStream yamlInputStream = new FileInputStream(yamlFile);
            YamlDecoder dec = new YamlDecoder(yamlInputStream);
	    setWorkingDirectory ( (String) dec.readObject());
            Ice.corpora = new TreeMap((Map) dec.readObject());
            Ice.entitySets = new TreeMap((Map) dec.readObject());
            Ice.relations = new TreeMap((Map) dec.readObject());
            dec.close();
	    yamlInputStream.close();
	    for(Corpus corpus : Ice.corpora.values()) {
		log.info("Found corpus " + corpus.getName());
		corpus.setWordCountFileName(FileNameSchema.getWordCountFileName(corpus.getName()));
	    }
	    success = true;
        } catch (IOException e) {
            log.error("Did not load " + fileName + ": " + e.getMessage());
        } finally {
	    return success;
	}

    }

    public void saveState(String fileName) {
	log.info("Saving to " + fileName);
        try {
            File yamlFile = new File(fileName);
            OutputStream yamlOutputStream = new FileOutputStream(yamlFile);
            YamlEncoder enc = new YamlEncoder(yamlOutputStream);
	    enc.writeObject(workingDirectory);
            enc.writeObject(Ice.corpora);
            enc.writeObject(Ice.entitySets);
            enc.writeObject(Ice.relations);
            enc.close();
	    yamlOutputStream.close();
        } catch (IOException e) {
            log.error("Error writing " + fileName + ".");
        }
    }

    public void loadStateFromDefault() {
        if (!loadState("ice.yaml")) {
            Ice.corpora = new TreeMap();
            Ice.entitySets = new TreeMap();
            Ice.relations = new TreeMap();
	}
    }
    public void saveStateToDefault() {
	saveState(configFile);
    }

    public void countWords(String corpusName, ProgressMonitorI monitor) {
	log.debug("Counting words for " + corpusName);
        getCorpus(corpusName).countWords(monitor);
	saveStateToDefault();
    }

    public List<Term> findTerms(String corpusName, String backgroundCorpus) {
	String bgCorpusFile = Ice.corpora.get(backgroundCorpus).getWordCountFileName();
	String analysisCorpusFile = getCorpus(corpusName).getWordCountFileName();
	String termsFile = FileNameSchema.getTermsFileName(corpusName);
	log.debug ("Finding terms for corpus file " + analysisCorpusFile + " with background corpus file " + bgCorpusFile + " and termsFile " + termsFile);
	List<Term> toReturn = null;
	File fgCountFile = new File(analysisCorpusFile);
	if (analysisCorpusFile == null || !fgCountFile.exists()) {
	    log.error("No word count file for corpus " + corpusName + ": " + analysisCorpusFile);
	    return toReturn;
	}
	File bgCountFile = new File(bgCorpusFile);
	if (!bgCountFile.exists()) {
	    log.error("No word count file for background corpus " + backgroundCorpus + ": " + bgCorpusFile);
	    return toReturn;
	}

	try {
	    toReturn = TermRanker.rankTerms(analysisCorpusFile, bgCorpusFile, termsFile);
	} catch (IOException e) {
	    log.error("Can't rank terms in " + corpusName + ": " + e.getMessage());
	    //	e.printStackTrace();
	}
	return toReturn;
    }

    public void findPatterns(String corpusName, ProgressMonitorI monitor) {
	log.debug("Finding patterns for " + corpusName);
        getCorpus(corpusName).findRelations(monitor, corpusName, null);
	saveStateToDefault();
    }

    public List<String> getPatterns(String corpusName, String filter) {
	log.debug("Getting relations for " + corpusName);
        return getCorpus(corpusName).checkRelations(filter);
    }

    public List<String> rankRelations(String corpusName, String bgCorpusName) {
	log.debug("Ranking relations for " + corpusName);
        return getCorpus(corpusName).rank();
    }

    public Map<String, IceEntitySet> getEntitySets() {
	return Ice.entitySets;
    }

    public void addEntitySet(String name, IceEntitySet entitySet) {
	Ice.entitySets.put(name, entitySet);
    }

    public void removeEntitySet(String name) {
	Ice.entitySets.remove(name);
    }

    public void setEntitySets(TreeMap<String, IceEntitySet> entitySets) {
	//	SortedMap<String,IceEntitySet> es = new TreeMap<String, IceEntitySet>();
	// es.putAll(entitySets);
	Ice.entitySets = entitySets;
    }

    public void indexCorpusForEntities(String corpusName, String indexName, String type, double cutoff, ProgressMonitorI monitor) {

	EntitySetIndexer indexer = new EntitySetIndexer();
	indexer.setProgressMonitor(monitor);

	indexer.run(corpusName,
		    type,
		    cutoff,
		    wd() + "parseprops");
	saveStateToDefault();
    }

    public void preprocess (String corpusName, String filter, ProgressMonitorI monitor) {
	Corpus corpus = Ice.corpora.get(corpusName);
	try {
	    if (corpus.docListFileName == null ||
		!(new File(corpus.docListFileName)).exists()) {
		log.error("Cannot find relevant documents for " + corpusName + ". Please reset directory or filter.");
		log.error("docListFileName = " + corpus.getDocListFileName());
		return;
	    }
	    String[] docArr = IceUtils.readLines(corpus.docListFileName);
	    if (docArr.length == 0) {
		log.error("Found 0 relevant documents for " + corpusName + " with filter " + filter + ". Please reset directory or filter.");
		log.error("docListFileName = " + corpus.getDocListFileName());
		return;
	    }
	} catch (IOException e) {
	    log.error("Cannot read docList file:", e);
	    return;
	}
	IcePreprocessor icePreprocessor = new IcePreprocessor(corpusName);
	icePreprocessor.setProgressMonitor(monitor);
	try {
	    Thread.sleep(500);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	monitor.setNote("Queued");
	icePreprocessor.processFiles();
	saveStateToDefault();
    }

    public HashMap<String, Index> findIndices(String corpusName) {
	HashMap<String, Index> indices = new HashMap<String, Index>();
	Path corpusDir = Paths.get(FileNameSchema.getCorpusInfoDirectory(corpusName));
	String fileName = "";
	Index indexInfo;
	String[] indexInfoStr;
	try {
	    DirectoryStream<Path> stream = Files.newDirectoryStream(corpusDir);
	    for (Path file: stream) {
		    fileName = file.getFileName().toString();
		    if (FileNameSchema.isEntitySetIndexFileName(fileName)) {
			indexInfo = new Index();
			indexInfo.setName(fileName);
			indexInfo.setType(FileNameSchema.getEntitySetIndexType(fileName));
			indexInfo.setCutoff(FileNameSchema.getEntitySetIndexCutoff(fileName));
			indexInfo.setStatus(IceInfoStatus.PROCESSED);
			indices.put(fileName, indexInfo);
		    }
		}
		stream.close();
	    } catch (IOException e) {
	    log.error("Exception reading corpus directory:", e);
	}
	return indices;
    }

    public List<String> expandEntitySet(String corpusName, String indexFileName, List<String> seedStrings, ProgressMonitorI monitor) {

	String indexFile = FileNameSchema.getCorpusInfoDirectory(corpusName) + File.separator + indexFileName;

	EntitySetExpander expander = new EntitySetExpander(indexFile, seedStrings);

        expander.setProgressMonitor(monitor);
	expander.rank();
	List<Entity> rankedEntities = expander.getRankedEntities();
	ArrayList<String> entityStrings = new ArrayList<String>(rankedEntities.size());
	for (Entity entity : rankedEntities) {
	    entityStrings.add(entity.getText());
	}
	return entityStrings;
    }

    public List<String> suggestEntitySeeds(String corpusName, String indexFileName) {
	
	// ABG 20150326 use FileNameSchema to get the index file name	
	// String cutoffStr = String.valueOf(cutoff).replace('.', 'p');
	//	return EntitySetExpander.recommendSeeds(FileNameSchema.getCorpusInfoDirectory(corpus) + File.separator + "esi-"+ entitySetName + "-" + type + "-" + cutoffStr,
	//			FileNameSchema.getTermsFileName(corpus), type);

	String type = FileNameSchema.getEntitySetIndexType(indexFileName);
	String indexFile = FileNameSchema.getCorpusInfoDirectory(corpusName) + File.separator + indexFileName;
	return EntitySetExpander.recommendSeeds(indexFile, FileNameSchema.getTermsFileName(corpusName), type);
    }

//    public List<Map<String, String>> rankEntitySetIndex(String corpus, String seed) {
//        List<Entity> entities = selectCorpus(corpus).entitySetBuilder.rankEntities(seed);
//
//        ArrayList<Map<String, String>> returnList = new ArrayList<>();
//
//        // These are each of subtype RankChoiceEntity....
//        for (Entity e : entities) {
//            HashMap<String, String> map = new HashMap<>();
//            map.put("text", e.getText());
//            map.put("type", e.getType());
//            map.put("score", Double.toString(e.getScore()));
//
//            if (e instanceof RankChoiceEntity) {
//                RankChoiceEntity rce = (RankChoiceEntity)e;
//                map.put("decision", rce.getDecision().toString());
//            }
//
//            returnList.add(map);
//        }
//        return returnList;
//    }

    /**
     * Saves a list of ranked entities for a corpus.
     * @param corpus The corpus these entities came from / are for.
     * @param entitySetName The name to save the entity set under.
     * @param entityType The type of the entity set, "nn" indicates nouns
     * @param entities The list of entity strings to save.
     */
    public void saveRankedEntitySet(String corpusName, String entitySetName, String entityType, List<String> entities) {
	log.debug("Saving ranked entity set for " + corpusName);
        IceEntitySet ies = getCorpus(corpusName).entitySetBuilder.saveRankedEntities(entitySetName, entityType, entities);
    }

    public IceRelation getRelation(String relationName) {
	return Ice.relations.get(relationName);
    }

    public void addRelation (String corpusName, String relationName, String relationInstance) {
	addToRelation(corpusName, relationName, relationInstance);
    }


    public void addToRelation (String corpusName, String relationName, String relationInstance) {
	IceRelation relation;
	if (!Ice.relations.containsKey(relationName)) {
	    relation = new IceRelation(relationName);
	    Ice.relations.put(relationName, relation);
	    log.info("Added relation " + relationName + " with instance " + relationInstance);
	} else {
	    relation = Ice.relations.get(relationName);
	}
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	DepPathMap depPathMap = DepPathMap.getInstance(reprFileName);
	List<String> paths = depPathMap.findPath(relationInstance);
	String arg1 = "";
	String arg2 = "";
	List<String> seedPaths = new ArrayList<String>();
	if (null != paths) {
	    for (String path: paths) {
		String[] parts = path.split("--");
		arg1 = parts[0].trim();
		arg2 = parts[2].trim();
		seedPaths.add(parts[1].trim());
	    }
	    relation.setArg1type(arg1);
	    relation.setArg2type(arg2);
	    relation.setPaths(seedPaths);
	    String seed = String.join(":::", paths);
	    log.info("Getting IcePaths for seed " + seed);
	    List<IcePath> icePaths = buildPathsFromStrings(corpusName, paths);
	    log.info("Retrieved " + Integer.toString(icePaths.size()) + " IcePaths.");
	    relation.setIcePaths(icePaths);
	} else {
	    log.error("No paths found for instance " + relationInstance);
	}
	saveStateToDefault();
    }

    public void addPaths (String relationName, List<IcePath> icePathsToAdd) {
	if (Ice.relations.containsKey(relationName)) {
	    IceRelation rel = Ice.relations.get(relationName);
	    List<String> paths = rel.getPaths();
	    List<IcePath> icePaths = rel.getIcePaths();
	    for (IcePath icePath: icePathsToAdd) {
		paths.add(icePath.getPath());
	    }
	    icePaths.addAll(icePathsToAdd);

	    rel.setPaths(paths);
	    rel.setIcePaths(icePaths);
	} else {
	    log.error("Unable to add paths: relation " + relationName + " not found in Ice.relations");
	}
    }

    public List<IcePath> buildRelation(String corpusName, String seed, ProgressMonitorI monitor) {
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
        String patternFileName = FileNameSchema.getRelationsFileName(corpusName);
        Bootstrap bootstrap = new Bootstrap(monitor);
	List<IcePath> toReturn = bootstrap.initialize(seed, reprFileName, patternFileName);
	
	buildPaths(corpusName, toReturn);
        return toReturn;
    }

    public List<IcePath> buildRelationMulti (String corpusName, List<IcePath> approvedPaths) {
	DummyMonitor monitor = new DummyMonitor();
        Bootstrap bootstrap = new Bootstrap(monitor);
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	String patternFileName = FileNameSchema.getRelationsFileName(corpusName);
	List<String> paths = new ArrayList<String>();
	for (IcePath icePath: approvedPaths) {
	    paths.add(icePath.getRepr());
	}
	String[] splitPaths = paths.toArray(new String[paths.size()]);
	List<IcePath> outputPaths = bootstrap.initMulti(splitPaths, reprFileName, patternFileName);
	// buildPaths(corpusName, outputPaths);
        return outputPaths;

    }

    public List<IcePath> buildRelation(String corpusName, String seed, List<IcePath> approvedPaths, List<IcePath> rejectedPaths) {
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
        String patternFileName = FileNameSchema.getRelationsFileName(corpusName);
	DummyMonitor monitor = new DummyMonitor();
        Bootstrap bootstrap = new Bootstrap(monitor);
        List<IcePath> toReturn = bootstrap.initialize(seed, reprFileName, patternFileName);
        if ((null == approvedPaths || approvedPaths.isEmpty()) &&
                (null == rejectedPaths || rejectedPaths.isEmpty())) {
            return toReturn;
        }
	toReturn = bootstrap.iterate(approvedPaths, rejectedPaths);
	//	buildPaths(corpusName, toReturn);
        return toReturn;
    }

    public IceRelation createRelation(String corpusName, List<String> pathReprs) {
        return getCorpus(corpusName).relationBuilder.createIceRelation(pathReprs);
    }

    public boolean renameRelation(String originalName, String newName) {
	if (originalName.equals(newName)) {
	    return false;
	}
	IceRelation relation = Ice.relations.remove(originalName);
	if (null != relation) {
	    relation.setName(newName);
	    Ice.relations.put(newName, relation);
	    return true;
	} else {
	    return false;
	}
    }

    public void removeRelation(String relationName) {
	Ice.relations.remove(relationName);
    }

	//
	// Methods to retrieve stored relations/entity sets from the cache
	//

    public Map<String, List<IcePath>> getCorpusPaths(String corpusName) {
	Map<String, List<IcePath>> corpusPaths = new HashMap<String, List<IcePath>>();

	// ABG 20150521 return paths for all relations based on this corpus
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	DepPathMap depPathMap = DepPathMap.getInstance(reprFileName);
	if (!depPathMap.load()) {
	    depPathMap.forceLoad();
	    depPathMap.persist();
	}
	for (String relName : Ice.relations.keySet()) {
	    List<IcePath> crPaths = new ArrayList<IcePath>();
	    IceRelation rel = Ice.relations.get(relName);
	    for (String pathStr: rel.getPaths() ) {
		String fullPath = rel.getArg1type() +
                    " -- " + pathStr + " -- " +
                    rel.getArg2type();
		String repr = depPathMap.findRepr(fullPath);
		String example = depPathMap.findExample(fullPath);
		IcePath path = new IcePath(fullPath, repr, example, 0);
		crPaths.add(path);
	    }
	    corpusPaths.put(relName, crPaths);
	}
	return corpusPaths;
    }

    public void buildPaths (String corpusName, List<IcePath> paths) {
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	DepPathMap depPathMap = DepPathMap.getInstance(reprFileName);
	for (IcePath path: paths) {
	    String pathString = path.getPath();
	    log.info("Building path for " + pathString);
	    path.setRepr(depPathMap.findRepr(pathString));
	    path.setExample(depPathMap.findExample(pathString));
	}

    }

    public List<IcePath> buildPathsFromStrings (String corpusName, List<String> paths) {
	List<IcePath> icePaths = new ArrayList<IcePath>();
	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	DepPathMap depPathMap = DepPathMap.getInstance(reprFileName);
	for (String pathString: paths) {
	    String repr = depPathMap.findRepr(pathString);
	    String example = depPathMap.findExample(pathString);
	    IcePath icePath = new IcePath(pathString, repr, example, 0.0);
	    icePaths.add(icePath);
	}
	return icePaths;
    }

    public List<IcePath> getCorpusRelPaths (String corpusName, String relationName) {
	// ABG 20150521
	List<IcePath> crPaths = new ArrayList<IcePath>();

	IceRelation relation = null;
	for (String relName: Ice.relations.keySet()) {
	    if (relName.equals(relationName)) {
		relation = Ice.relations.get(relName);
	    }
	}
	if (null == relation) {
	    log.error("Relation " + relationName + " not found!");
	    return crPaths; 
	}

	String reprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	DepPathMap depPathMap = DepPathMap.getInstance(reprFileName);
	for (String pathStr: relation.getPaths() ) {
	    String fullPath = relation.getArg1type() +
		" -- " + pathStr + " -- " +
		relation.getArg2type();
	    String repr = depPathMap.findRepr(fullPath);
	    if (repr == null) {
		log.error("Repr not found for path:" + fullPath);
	    }
	    String example = depPathMap.findExample(fullPath);
	    IcePath path = new IcePath(fullPath, repr, example, 0);
	    crPaths.add(path);
	    
	}
	return crPaths;

    }


}
