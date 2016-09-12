package edu.nyu.jet.ice.views.cli;

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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;


/**
 * Command line interface for running Ice processing tasks.
 *
 * Java 6 compatibility
 *
 * @author yhe
 * @version 1.0
 */
public class IceCLI6 {

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
        options.addOption(inputDir);
        options.addOption(background);
        options.addOption(filter);
        options.addOption(entityIndexCutoff);
        options.addOption(numOfProcessesOpt);

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
            else if (action.equals("addCorpusFrom")) {
                int numOfProcesses = 1;
                init();
                validateCorpus(corpusName);
                Ice.selectCorpus(corpusName);
                // TODO: merge corpus
            }
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
            }
            else {
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
        String suffixName = Ice.selectedCorpus.filter;

        for (int i = 1; i < numOfProcesses + 1; i++) {
            try {
                String currentCorpusName = splitCorpusName(corpusName, i);
                Ice.selectCorpus(currentCorpusName);
                String currentPreprocessDir = FileNameSchema.getPreprocessCacheDir(currentCorpusName);

                String[] docList = IceUtils.readLines(Ice.selectedCorpus.docListFileName);
                for (String docName : docList) {
                    docName = docName + "." + Ice.selectedCorpus.filter;
                    String sourceFileName = IcePreprocessor.getAceFileName(currentPreprocessDir,
                            Ice.selectedCorpus.directory,
                            docName
                    );
                    String targetFileName = IcePreprocessor.getAceFileName(origPreprocessDir,
                            Ice.selectedCorpus.directory,
                            docName);
                    File target = new File(targetFileName);
                    File source = new File(sourceFileName);
                    FileUtils.copyFile(source, target);
                    sourceFileName = IcePreprocessor.getPosFileName(currentPreprocessDir,
                            Ice.selectedCorpus.directory,
                            docName
                    );
                    targetFileName = IcePreprocessor.getPosFileName(origPreprocessDir,
                            Ice.selectedCorpus.directory,
                            docName);
                    copyFile(sourceFileName, targetFileName);
                }
            }
            catch (Exception e) {
                System.err.println("Problem merging split " + i);
                e.printStackTrace();
            }
        }
    }

    public static void copyFile(String sourceFileName, String targetFileName) throws IOException {
        File target;
        File source;
        target = new File(targetFileName);
        source = new File(sourceFileName);
        FileUtils.copyFile(source, target);
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
            System.err.println("corpusName does not exist. Please pick a corpus from the list below:");
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
            Nice.saveStatus();
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
        Nice.initIce();
        //Nice.loadPathMatcher(iceProperties);
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("IceCLI ACTION CORPUS [OPTIONS]\n" +
                        "ACTION=addCorpus|setBackgroundFor|findEntities|indexEntities|findPhrases",
                options);
    }

}
