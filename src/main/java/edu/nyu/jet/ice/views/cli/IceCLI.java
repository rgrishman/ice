package edu.nyu.jet.ice.views.cli;

import edu.nyu.jet.JetTest;
import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.ice.entityset.EntitySetIndexer;
import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.models.RelationFinder;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProcessFarm;
import edu.nyu.jet.ice.views.swing.SwingEntitiesPanel;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.ho.yaml.YamlDecoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Command line interface for running Ice processing tasks.
 *
 * @author yhe
 * @version 1.0
 */

public class IceCLI {

    static String branch;

    public static void main(String[] args) {
        // create Options object
        Options options = new Options();
        Option inputDir = OptionBuilder.withLongOpt("inputDir").hasArg().withArgName("inputDirName")
                .withDescription("Location of new corpus").create("i");
        Option background = OptionBuilder.withLongOpt("background").hasArg().withArgName("backgroundCorpusName")
                .withDescription("Name of the background corpus").create("b");
        Option filter = OptionBuilder.withLongOpt("filter").hasArg().withArgName("filterFileExtension")
                .withDescription("File extension to process: sgm, txt, etc.").create("f");
        Option entityIndexCutoff = OptionBuilder.withLongOpt("entityIndexCutoff").hasArg().withArgName("cutoff")
                .withDescription("Cutoff of entity index: 1.0-25.0").create("e");
        Option numOfProcessesOpt = OptionBuilder.withLongOpt("processes").hasArg().withArgName("numOfProcesses")
                .withDescription("Num of parallel processes when adding and preprocessing corpus").create("p");
        Option targetDir = OptionBuilder.withLongOpt("targetDir").hasArg().withArgName("targetDirForCreateFrom")
                .withDescription("Directory for the new corpus, when combining old corpora").create("t");
        Option fromCorporaOpt = OptionBuilder.withLongOpt("fromCorpora").hasArg().withArgName("fromCorpora")
                .withDescription("Names for the corpora to be merged").create("s");
	Option branchOpt = OptionBuilder.withLongOpt("branch").hasArg().withArgName("branch")
                .withDescription("Yaml file for saving and restoring status").create("y");
	Option fromBranch = OptionBuilder.withLongOpt("fromBranch").hasArg().withArgName("fromBranch")
                .withDescription("Names for the corpora to be merged").create("g");
        options.addOption(inputDir);
        options.addOption(background);
        options.addOption(filter);
        options.addOption(entityIndexCutoff);
        options.addOption(numOfProcessesOpt);
        options.addOption(targetDir);
        options.addOption(fromCorporaOpt);
        options.addOption(branchOpt);
        options.addOption(fromBranch);

        CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] arguments = cmd.getArgs();
            if (arguments.length != 2) {
                System.err.println("Must provide exactly 2 arguments: ACTION CORPUS");
                printHelp(options);
                System.exit(-1);
            }
            String action  = arguments[0];
            String corpusName  = arguments[1];
	    branch = cmd.getOptionValue("branch");
	    if (branch == null) branch = "ice";
            //
            // ----- a d d   C o r p u s -----
            //
            if (action.equals("addCorpus")) {
                int numOfProcesses = 1;
                String inputDirName = cmd.getOptionValue("inputDir");
                if (inputDirName == null) {
                    System.err.println("--inputDir must be set for the addCorpus action.");
                    printHelp(options);
                    System.exit(-1);
                }
                String filterName = cmd.getOptionValue("filter");
                if (filterName == null) {
                    System.err.println("--filter must be set for the addCorpus action.");
                    printHelp(options);
                    System.exit(-1);
                }
                String numOfProcessesStr = cmd.getOptionValue("processes");
                if (numOfProcessesStr != null) {
                    try {
                        numOfProcesses = Integer.valueOf(numOfProcessesStr);
                        if (numOfProcesses < 1) {
                            throw new Exception();
                        }
                    }
                    catch (Exception e) {
                        System.err.println("--processes only accepts an integer (>=1) as parameter");
                        printHelp(options);
                        System.exit(-1);
                    }
                }
                File inputDirFile = new File(inputDirName);
                if (inputDirFile.exists() && inputDirFile.isDirectory()) {
                    init();
                    if (Ice.corpora.containsKey(corpusName)) {
                        System.err.println("Name of corpus already exists. Please choose another name.");
                        System.exit(-1);
                    }

                    String backgroundCorpusName = cmd.getOptionValue("background");
                    if (backgroundCorpusName != null) {
                        if (!Ice.corpora.containsKey(backgroundCorpusName)) {
                            System.err.println("Cannot find background corpus. Use one of the current corpora as background:");
                            printCorporaListExcept(corpusName);
                            System.exit(-1);
                        }
                    }

                    Corpus newCorpus = new Corpus(corpusName);
                    if (backgroundCorpusName != null) {
                        newCorpus.setBackgroundCorpus(backgroundCorpusName);
                    }
                    Ice.corpora.put(corpusName, newCorpus);
                    Ice.selectCorpus(corpusName);
                    Ice.selectedCorpus.setDirectory(inputDirName);
                    Ice.selectedCorpus.setFilter(filterName);
                    Ice.selectedCorpus.writeDocumentList();
                    if (Ice.selectedCorpus.docListFileName == null) {
                        System.err.println("Unable to find any file that satisfies the filter.");
                        System.exit(-1);
                    }
                    if (numOfProcesses == 1) {
                        preprocess(filterName, backgroundCorpusName);

                    }
                    else {
                        if (backgroundCorpusName == null) {
                            System.err.println("[WARNING]\tMultiprocess preprocessing will not handle background corpus.");
                        }
                        ProcessFarm processFarm = new ProcessFarm();
                        // split corpus
                        try {
                            String[] docList = IceUtils.readLines(Ice.selectedCorpus.docListFileName);
                            int splitCount = 1;
                            int start = 0;
                            int end   = 0;
                            int portion = docList.length / numOfProcesses;
                            if (portion > 0) {
                                for (int i = 0; i < numOfProcesses; i++) {
                                    end += portion;
                                    if (i == 0) {
                                        end += docList.length % numOfProcesses;
                                    }
                                    Corpus splitCorpus = new Corpus(splitCorpusName(corpusName, splitCount));
                                    splitCorpus.setDirectory(inputDirName);
                                    splitCorpus.setFilter(filterName);
                                    String docListFileName = FileNameSchema.getDocListFileName(
                                            splitCorpusName(corpusName, splitCount));
                                    IceUtils.writeLines(docListFileName, Arrays.copyOfRange(docList,
                                            start, end));
                                    splitCorpus.setDocListFileName(docListFileName);
                                    Ice.corpora.put(splitCorpusName(corpusName, splitCount), splitCorpus);
                                    processFarm.addTask(String.format("./icecli preprocess %s",
                                            splitCorpusName(corpusName, splitCount)));
                                    start = end;
                                    splitCount++;
                                }
                            }
                            saveStatus();
                        }
                        catch (Exception e) {
                            System.err.println("Error occured when preparing to submit processes.");
                            e.printStackTrace();
                        }
                        processFarm.submit();
                        boolean success = processFarm.waitFor();
                        if (!success) {
                            System.err.println("[WARNING] processFarm returned with error. Please check log.");
                        }
                        if (numOfProcesses > 1) {
                            System.err.println("Preprocessing finished. Merging splits...");
                            mergeSplit(corpusName, numOfProcesses);
                        }
                    }
                    System.err.println("Corpus added successfully.");
                }
                else {
                    System.err.println("--inputDir should specify a valid directory.");
                    printHelp(options);
                    System.exit(-1);
                }

            }
            else if (action.equals("preprocess")) {
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                preprocess(Ice.selectedCorpus.filter, Ice.selectedCorpus.backgroundCorpus);
            }
            else if (action.equals("mergeSplit")) {
                int numOfProcesses = 1;
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                String numOfProcessesStr = cmd.getOptionValue("processes");
                if (numOfProcessesStr != null) {
                    try {
                        numOfProcesses = Integer.valueOf(numOfProcessesStr);
                        if (numOfProcesses < 1) {
                            throw new Exception();
                        }
                    }
                    catch (Exception e) {
                        System.err.println("--processes only accepts an integer (>=1) as parameter");
                        printHelp(options);
                        System.exit(-1);
                    }
                }
                if (numOfProcesses == 1) {
                    System.err.println("No need to corpus that has 1 split.");
                    System.exit(0);
                }
                mergeSplit(corpusName, numOfProcesses);
            }
            //
            // ----- m e r g e   C o r p o r a   I n t o -----
            //
            //       combines several corpora into one
            //
            else if (action.equals("mergeCorporaInto")) {
                String corpusDir  = cmd.getOptionValue("targetDir");
                String filterName = cmd.getOptionValue("filter");
                String fromCorporaStr = cmd.getOptionValue("fromCorpora");
                if (corpusDir == null || filterName == null || fromCorporaStr == null) {
                    System.err.println("--targetDir --filter must be set for the mergeCorporaInto action.");
                    printHelp(options);
                    System.exit(-1);
                }
                String[] fromCorpora = fromCorporaStr.split(",");

                init();
                for (String fromCorpusName : fromCorpora) {
                    validateCorpus(fromCorpusName);
                }

                Ice.selectCorpus(corpusName);
                try {
                    // create cache directory for new corpus
                    String targetCacheDir = FileNameSchema.getPreprocessCacheDir(corpusName);
                    File targetCacheDirFile = new File(targetCacheDir);
                    targetCacheDirFile.mkdirs();
                    if (copyApfDTD(targetCacheDir)) return;

                    // create docList and preprocessMap files
                    String docListFileName = FileNameSchema.getDocListFileName(corpusName);
                    PrintWriter docListFileWriter = new PrintWriter(new FileWriter(docListFileName));
                    String preprocessCacheMapFileName = FileNameSchema.getPreprocessCacheMapFileName(corpusName);
                    PrintWriter preprocessCacheMapFileWriter = new PrintWriter(new FileWriter(preprocessCacheMapFileName));
		    int docCount = 0;
                    // create docList and preprocessMap for new corpus
                    for (String fromCorpusName : fromCorpora) {
                        String fromDir = Ice.corpora.get(fromCorpusName).directory;
                        Path fromDirPath = new File(fromDir).toPath();
                        Path corpusDirPath = new File(corpusDir).toPath();
                        Path relativePath = corpusDirPath.relativize(fromDirPath);
                        String fromDocListFileName = Ice.corpora.get(fromCorpusName).docListFileName;
                        BufferedReader br = new BufferedReader(new FileReader(fromDocListFileName));
                        String mapFileName = FileNameSchema.getPreprocessCacheMapFileName(fromCorpusName);
                        File mapFile = new File(mapFileName);
                        boolean mapFileExists = mapFile.exists();
                        BufferedReader mapReader = null;
                        if (mapFileExists)
                            mapReader = new BufferedReader(new FileReader (mapFile));
                        String line;
                        while ((line = br.readLine()) != null) {
                            Path docPath  = relativePath.resolve(line);
                            docListFileWriter.println(docPath);
                            String mapFrom =
                                cacheFileName(FileNameSchema.getPreprocessCacheDir(corpusName), corpusDir, docPath + "." + filterName);
                            String mapTo =
                                cacheFileName(FileNameSchema.getPreprocessCacheDir(fromCorpusName), fromDir, line + "." + filterName);
                            if (mapFileExists) {
                                String[] mapLine = mapReader.readLine().split(":");
                                mapTo = mapLine[1];
                            }
                            preprocessCacheMapFileWriter.println(mapFrom + ":" + mapTo);
			    docCount++;
                        }
                        br.close();
                        if (mapFileExists)
                            mapReader.close();
                        readSplitCounts(fromCorpusName);
                    }
                    // create new corpus
                    docListFileWriter.close();
                    preprocessCacheMapFileWriter.close();
                    Corpus newCorpus = new Corpus(corpusName);

                    Ice.corpora.put(corpusName, newCorpus);
                    Ice.selectCorpus(corpusName);
                    Ice.selectedCorpus.setDirectory(corpusDir);
                    Ice.selectedCorpus.setFilter(filterName);
                    Ice.selectedCorpus.setDocListFileName(docListFileName);
		    writeMergedCounts(docCount, corpusName);
                    saveStatus();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    printHelp(options);
                    System.exit(-1);
                }
            }
            //
            // ----- s e t   B a c k g r o u n d   F o r -----
            //
            else if (action.equals("setBackgroundFor")) {
                init();
                validateCorpus(corpusName);
                String backgroundCorpusName = cmd.getOptionValue("background");
                validateBackgroundCorpus(corpusName, backgroundCorpusName);
                Ice.selectCorpus(corpusName);
                Ice.selectedCorpus.backgroundCorpus = backgroundCorpusName;
                saveStatus();
                System.err.println("Background corpus set successfully.");
            }
            //
            // ----- f i n d   E n t i t i e s -----
            //
            else if (action.equals("findEntities")) {
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                validateCurrentBackground();
                SwingEntitiesPanel entitiesPanel = new SwingEntitiesPanel();
                entitiesPanel.findTerms();
                Ice.selectedCorpus.setTermFileName(FileNameSchema.getTermsFileName(Ice.selectedCorpus.getName()));
                saveStatus();
                System.err.println("Entities extracted successfully.");
            }
            else if (action.equals("indexEntities")) {
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                Corpus selectedCorpus = Ice.selectedCorpus;
                String termFileName = FileNameSchema.getTermsFileName(selectedCorpus.getName());
                File termFile = new File(termFileName);
                if (!termFile.exists() || ! termFile.isFile()) {
                    System.err.println("Entities file does not exist. Please use findEntities to generate entities file first.");
                    System.exit(-1);
                }
                String cutoff = "3.0";
                String userCutoff = cmd.getOptionValue("entityIndexCutoff");
                if (userCutoff != null) {
                    try {
                        double cutOffVal = Double.valueOf(userCutoff);
                        if (cutOffVal < 1.0 || cutOffVal > 25.0) {
                            System.err.println("Please specify an entityIndexCutoff value between 1.0 and 25.0.");
                            System.exit(-1);
                        }
                    }
                    catch (Exception e) {
                        System.err.println(userCutoff + " is not a valid value of entityIndexCutoff. Please use a number between 1.0 and 25.0.");
                        System.exit(-1);
                    }
                    cutoff = userCutoff;
                }
                else {
                    System.err.println("Using default cutoff: " + cutoff);
                }
                EntitySetIndexer.main(new String[]{FileNameSchema.getTermsFileName(selectedCorpus.getName()),
                        "nn",
                        String.valueOf(cutoff),
                        "onomaprops",
                        selectedCorpus.getDocListFileName(),
                        selectedCorpus.getDirectory(),
                        selectedCorpus.getFilter(),
                        FileNameSchema.getEntitySetIndexFileName(selectedCorpus.getName(), "nn")});
                System.err.println("Entities index successfully. Please use ICE GUI to build entity sets.");

            }
            else if (action.equals("findPhrases")) {
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                // validateCurrentBackground();
                RelationFinder finder = new RelationFinder(
                        Ice.selectedCorpus.docListFileName, Ice.selectedCorpus.directory,
                        Ice.selectedCorpus.filter, FileNameSchema.getRelationsFileName(Ice.selectedCorpusName),
                        FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName), null,
                        Ice.selectedCorpus.numberOfDocs,
                        null);
                finder.run();
                Ice.selectedCorpus.relationTypeFileName =
                        FileNameSchema.getRelationTypesFileName(Ice.selectedCorpus.name);
                Ice.selectedCorpus.relationInstanceFileName =
                        FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);
                if (Ice.selectedCorpus.backgroundCorpus != null) {
                    System.err.println("Generating path ratio file by comparing phrases in background corpus.");
                    Corpus.rankRelations(Ice.selectedCorpus.backgroundCorpus,
                            FileNameSchema.getPatternRatioFileName(Ice.selectedCorpusName,
                                    Ice.selectedCorpus.backgroundCorpus));
                }
                else {
                    System.err.println("Background corpus is not selected, so pattern ratio file is not generated. " +
                            "Use setBackgroundFor to set the background corpus.");
                }
                saveStatus();
                System.err.println("Phrases extracted successfully.");

            } else if (action.equals("import")) {
		String fromBranchName = cmd.getOptionValue("fromBranch");
		if (fromBranchName == null) {
       	            System.err.println("--fromBranch must be set for the import action.");
              	    System.exit(-1);
         	}
         	File fromBranchFile = new File(fromBranchName + ".yml");
                if (!fromBranchFile.exists() || fromBranchFile.isDirectory()) {
                    System.err.println("Cannot find exporting branch.");
                    System.exit(-1);
		}
            	if (Ice.corpora.containsKey(corpusName)) {
                    System.err.println("Corpus already exists.");
                    System.exit(-1);
	        }
	        init();
		try {
	            InputStream yamlInputStream = new FileInputStream(fromBranchFile);
	            YamlDecoder dec = new YamlDecoder(yamlInputStream);
	            Map foreignCorpora = new TreeMap((Map) dec.readObject());
	            dec.close();
	            if (foreignCorpora.get(corpusName) == null) {
		        System.err.println("Corpus not found on exporter.");
		        System.exit(-1);
	            }
	            Ice.corpora.put(corpusName, (Corpus) foreignCorpora.get(corpusName));
	        } catch (IOException e) {
	            System.err.println("Unable to read " + fromBranchFile + " from exporter.");
		    System.exit(-1);
		}
	        saveStatus();
	        System.err.println("Corpus imported successfully.");

            } else {
                System.err.println("Invalid action: " + action);
                printHelp(options);
                System.exit(-1);
            }
        }
        catch (MissingOptionException e) {
            System.err.println(e.getMessage());
            printHelp(options);
            System.exit(-1);
        }
        catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp(options);
            System.exit(-1);
        }
    }

    /**
     *  Place a copy of the APF DTD in the 
     *  cache directory to enable subsequent reading of the APF.
     */

    public static boolean copyApfDTD(String targetCacheDir) throws IOException {
        Properties props = new Properties();
        props.load(new FileReader("parseprops"));
        try {
            FileUtils.copyFile(new File(props.getProperty("Jet.dataPath") + File.separator + "apf.v5.1.1.dtd"),
                    new File(targetCacheDir + File.separator + "apf.v5.1.1.dtd"));
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * Merge preprocessed splits for a corpus by creating hard links for each
     * cache file
     * @param corpusName name of the corpus
     * @param numOfProcesses number of splits
     */
    public static void mergeSplit(String corpusName, int numOfProcesses) {
        String origCorpusName = corpusName;
        String origPreprocessDir = FileNameSchema.getPreprocessCacheDir(origCorpusName);
        File origPreprocessDirFile = new File(origPreprocessDir);
        origPreprocessDirFile.mkdirs();
        try {
            copyApfDTD(origPreprocessDir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String suffixName = Ice.selectedCorpus.filter;
        int docCount = 0;
        for (int i = 1; i < numOfProcesses + 1; i++) {
            try {
                String currentCorpusName = splitCorpusName(corpusName, i);
                Ice.selectCorpus(currentCorpusName);
                String currentPreprocessDir = FileNameSchema.getPreprocessCacheDir(currentCorpusName);

                String[] docList = IceUtils.readLines(Ice.selectedCorpus.docListFileName);
                for (String docName : docList) {
                    docName = docName + "." + Ice.selectedCorpus.filter;
                    copyCache(currentPreprocessDir, origPreprocessDir, 
                            Ice.selectedCorpus.directory, Ice.selectedCorpus.directory,
                            docName, docName);
                    docCount++;
                }
		readSplitCounts(currentCorpusName);
            }
            catch (Exception e) {
                System.err.println("Problem merging split " + i);
                e.printStackTrace();
            }
        }
	try {
	    writeMergedCounts(docCount, origCorpusName);
        } catch (Exception e) {
            System.err.println("Problem merging split ");
            e.printStackTrace();
        }
	saveStatus();
    }

    public static void copyCache(String sourceCacheDir,
                                 String targetCacheDir,
                                 String sourceSourceDir,
                                 String targetSourceDir,
                                 String sourceDocName,
                                 String targetDocName) throws IOException {
        String sourceFileName = IcePreprocessor.getAceFileName(sourceCacheDir,
                sourceSourceDir,
                sourceDocName
        );
        String targetFileName = IcePreprocessor.getAceFileName(targetCacheDir,
                targetSourceDir,
                targetDocName);
        copyFile(sourceFileName, targetFileName);


        sourceFileName = IcePreprocessor.getPosFileName(sourceCacheDir,
                sourceSourceDir,
                sourceDocName
        );
        targetFileName = IcePreprocessor.getPosFileName(targetCacheDir,
                targetSourceDir,
                targetDocName);
        copyFile(sourceFileName, targetFileName);

    }

    private static void copyFile(String sourceFileName, String targetFileName) throws IOException {
        File targetFile = (new File(targetFileName)); //.getCanonicalFile();
        Path target = targetFile.toPath();
        Path source = (new File(sourceFileName)).toPath(); //.getCanonicalFile().toPath();
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    // Tables for holding the combined word counts and relations from a set of splits.

    static Map<String, String> wordCount = new HashMap<String, String>();
    static Map<String, Integer> relationCount = new HashMap<String, Integer>();
    static Map<String, Integer> relationTypeCount = new HashMap<String, Integer>();
    static Map<String, String> relationRepr = new HashMap<String, String>();
    static List<String> events = new ArrayList<String>();

    /**
     *  Read the word count and relation files from one of the split directories.
     */

    public static void readSplitCounts (String corpusName) throws IOException {
        String wordCountFileName = FileNameSchema.getWordCountFileName(corpusName);
	System.out.println("Reading " + wordCountFileName);
        BufferedReader wordReader = new BufferedReader (new FileReader (wordCountFileName));
	// skip header on file
        wordReader.readLine();
        wordReader.readLine();
        wordReader.readLine();
        String line;
        while ((line = wordReader.readLine()) != null) {
	    String f[] = line.split("\t", 2);
            String word = f[0];
	    String counts = f[1];
            if (wordCount.get(word) == null) {
	        wordCount.put(word, counts);
	    } else {
	        wordCount.put(word, wordCount.get(word) + "\t" + counts);
	    }
        }
        wordReader.close();

	String relationFileName = FileNameSchema.getRelationsFileName(corpusName);
	System.out.println("Reading " + relationFileName);
        BufferedReader relationReader = new BufferedReader (new FileReader (relationFileName));
        while ((line = relationReader.readLine()) != null) {
	    String f[] = line.split("\t", 2);
	    int count = 0;
	    try {
            	count = Integer.valueOf(f[0]);
	    } catch (NumberFormatException e) {
		System.out.println ("Error in " + relationFileName);
	    }
	    String relation = f[1];
            if (relationCount.get(relation) == null) {
	        relationCount.put(relation, count);
	    } else {
	        relationCount.put(relation, relationCount.get(relation) + count);
	    }
        }
        relationReader.close();

	String relationTypesFileName = FileNameSchema.getRelationTypesFileName(corpusName);
	System.out.println("Reading " + relationTypesFileName);
        BufferedReader relationTypesReader = new BufferedReader (new FileReader (relationTypesFileName));
        while ((line = relationTypesReader.readLine()) != null) {
	    String f[] = line.split("\t", 2);
	    int count = 0;
	    try {
            	count = Integer.valueOf(f[0]);
	    } catch (NumberFormatException e) {
		System.out.println ("Error in " + relationTypesFileName);
	    }
	    String relation = f[1];
            if (relationTypeCount.get(relation) == null) {
	        relationTypeCount.put(relation, count);
	    } else {
	        relationTypeCount.put(relation, relationTypeCount.get(relation) + count);
	    }
        }
        relationTypesReader.close();

	String relationReprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	System.out.println("Reading " + relationReprFileName);
        BufferedReader relationReprReader = new BufferedReader (new FileReader (relationReprFileName));
        while ((line = relationReprReader.readLine()) != null) {
	    String f[] = line.split(":::", 2);
	    String relation = f[0];
	    String repr = f[1];
            String priorRepr = relationRepr.get(relation);
            if (priorRepr == null || repr.length() < priorRepr.length())
                // if a choice, take shorter sample sentence
                relationRepr.put(relation, repr);
        }
        relationReprReader.close();

        String depEventFileName = FileNameSchema.getDependencyEventFileName(corpusName);
        if ((new File(depEventFileName)).exists()) {
            System.out.println("Reading " + depEventFileName);
            BufferedReader eventReader = new BufferedReader (new FileReader (depEventFileName));
            while ((line = eventReader.readLine()) != null) {
                events.add(line);
            }
            eventReader.close();
        }
    }

    /**
     *  Write the result of combining the word counts and relation patterns from all
     *  the splits.
     */

    public static void writeMergedCounts (int docCount, String corpusName) throws IOException {
        Corpus mergedCorpus = Ice.corpora.get(corpusName);
        String wordCountFileName = FileNameSchema.getWordCountFileName(corpusName);
	System.out.println("Writing " + wordCountFileName);
        PrintWriter wordWriter = new PrintWriter (new FileWriter (wordCountFileName));
        wordWriter.println("# DOC_COUNT\n# TERM DOC_FREQS");
        wordWriter.println(docCount);
        for (String word : wordCount.keySet()) {
	    wordWriter.println(word + "\t" + wordCount.get(word));
        }
	wordWriter.close();
	mergedCorpus.wordCountFileName = wordCountFileName;

        String relationsFileName = FileNameSchema.getRelationsFileName(corpusName);
	System.out.println("Writing " + relationsFileName);
        PrintWriter relationsWriter = new PrintWriter (new FileWriter (relationsFileName));
        for (String relation : relationCount.keySet()) {
	    relationsWriter.println(relationCount.get(relation) + "\t" + relation);
        }
	relationsWriter.close();
	mergedCorpus.relationInstanceFileName = relationsFileName;

        String relationTypesFileName = FileNameSchema.getRelationTypesFileName(corpusName);
	System.out.println("Writing " + relationTypesFileName);
        PrintWriter relationTypesWriter = new PrintWriter (new FileWriter (relationTypesFileName));
        for (String relation : relationTypeCount.keySet()) {
	    relationTypesWriter.println(relationTypeCount.get(relation) + "\t" + relation);
        }
	relationTypesWriter.close();
	mergedCorpus.relationTypeFileName = relationTypesFileName;

        String relationReprFileName = FileNameSchema.getRelationReprFileName(corpusName);
	System.out.println("Writing " + relationReprFileName);
        PrintWriter relationReprWriter = new PrintWriter (new FileWriter (relationReprFileName));
        for (String relation : relationRepr.keySet()) {
	    relationReprWriter.println(relation + ":::" + relationRepr.get(relation));
        }
	relationReprWriter.close();
        
        String depEventFileName = FileNameSchema.getDependencyEventFileName(corpusName);
        if (!events.isEmpty()) {
            System.out.println("Writing " + depEventFileName);
            PrintWriter eventWriter = new PrintWriter (new FileWriter (depEventFileName));
            for (String event : events) {
                eventWriter.println(event);
            }
            eventWriter.close();
            EntitySetIndexer esi = new EntitySetIndexer();
            esi.computePMI(depEventFileName, FileNameSchema.getEntitySetIndexFileName(corpusName, "nn"));
        }
    }

    public static String splitCorpusName(String corpusName, int splitCount) {
        return "." + corpusName + "_split_" + splitCount;
    }

    public static void preprocess(String filterName, String backgroundCorpusName) {
        IcePreprocessor icePreprocessor = new IcePreprocessor(
                Ice.selectedCorpus.directory,
                Ice.iceProperties.getProperty("Ice.IcePreprocessor.parseprops"),
                Ice.selectedCorpus.docListFileName,
                filterName,
                FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName)
        );
        icePreprocessor.run();
        saveStatus();
        if (backgroundCorpusName == null) {
            System.err.println("[WARNING]\tBackground corpus is not set.");
        }
    }

    private static void validateCurrentBackground() {
        if (Ice.selectedCorpus.backgroundCorpus == null) {
            System.err.println("Background corpus is not set yet. Please use setBackground to pick a background corpus.");
            System.exit(-1);
        }
    }

    private static void validateCorpus(String corpusName) {
        if (!Ice.corpora.containsKey(corpusName)) {
            System.err.println(String.format("corpusName:%s does not exist. Please pick a corpus from the list below:",
                corpusName));
            printCorporaList();
            System.exit(-1);
        }
    }

    private static void validateBackgroundCorpus(String corpusName, String backgroundCorpusName) {
        if (backgroundCorpusName != null) {
            if (!Ice.corpora.containsKey(backgroundCorpusName)) {
                System.err.println("Cannot find background corpus. Use one of the current corpora as background:");
                printCorporaListExcept(corpusName);
                System.exit(-1);
            }
            else if (corpusName.equals(backgroundCorpusName)) {
                System.err.println("Foreground and background corpus should not be the same.");
                System.exit(-1);
            }
        }
        else {
            System.err.println("--background must be set for the selected action.");
            System.exit(-1);
        }
    }

    private static void saveStatus() {
        try {
            Nice.saveStatus(branch);
        }
        catch (Exception e) {
            System.err.println("Unable to save status. Please check if the ice config file is writable.");
            System.err.println(-1);
        }
    }

    private static void printCorporaList() {
        for (String key : Ice.corpora.keySet()) {
            System.err.println("\t" + key);
        }
    }

    private static void printCorporaListExcept(String corpusName) {
        for (String key : Ice.corpora.keySet()) {
            if (!key.equals(corpusName)) {
                System.err.println("\t" + key);
            }
        }
    }

    public static void init() {
        Nice.printCover();
        Properties iceProperties = Nice.loadIceProperties();
        Nice.initIce(branch);
        //Nice.loadPathMatcher(iceProperties);
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("IceCLI ACTION CORPUS [OPTIONS]\n" +
                        "ACTION=addCorpus|mergeCorporaInto|setBackgroundFor|findEntities|indexEntities|findPhrases",
                options);
    }

    static String cacheFileName(String cacheDir, String inputDir, String inputFile) {
        return cacheDir + File.separator +
            inputDir.replaceAll("/", "_") + "_" + inputFile.replaceAll("/", "_");
    }
}
