package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.RelationBuilder;
import edu.nyu.jet.ice.uicomps.ListFilter;
import edu.nyu.jet.ice.uicomps.TermFilter;
import edu.nyu.jet.ice.uicomps.RelationFilter;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.Ratio;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.terminology.TermCounter;
import edu.nyu.jet.ice.terminology.TermRanker;

//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.awt.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.List;

/**
 *  A corpus (set of documents) for ICE.
 *
 *  A corpus is specified by the root directory of a directory tree and
 *  a file extension filter:  it consists of all non-directory files under the
 *  root with a matching extension.  Exception:  if the filter is "*", all
 *  files are included, regardless of extension.
 */

public class Corpus {

    // properties
    public String name;
    public String directory;
    public String filter;
    public int numberOfDocs;
    public String docListFileName;
    public Map<String, String> preprocessCacheMap;
    // for term finder
    public String wordCountFileName;
    public String backgroundCorpus;
    public String termFileName;
    // for relation finder
    public String relationTypeFileName;
    public String relationInstanceFileName;
    public RelationBuilder relationBuilder;

    public Set<String> entitiesSuggested = new HashSet<String>();
    public Set<String> relationsSuggested = new HashSet<String>();

    public TermFilter termFilter = new TermFilter();
    static public RelationFilter relationFilter = new RelationFilter();

    // property methods
    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String s) {
        directory = s;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String s) {
        String corpusInfoDirectory = FileNameSchema.getCorpusInfoDirectory(name);
        //Files.createDirectory(Paths.get(corpusInfoDirectory));
        File dir = new File(corpusInfoDirectory);
        dir.mkdirs();
        filter = s;
    }

    public int getNumberOfDocs() {
        return numberOfDocs;
    }

    public void setNumberOfDocs(int n) {
        numberOfDocs = n;
    }

    public String getDocListFileName() {
        return docListFileName;
    }

    public void setDocListFileName(String s) {
        docListFileName = s;
    }

    public String getWordCountFileName() {
        return wordCountFileName;
    }

    public void setWordCountFileName(String s) {
        wordCountFileName = s;
    }

    public String getBackgroundCorpus() {
        return backgroundCorpus;
    }

    public void setBackgroundCorpus(String s) {
        backgroundCorpus = s;
    }

    public String getTermFileName() {
        return termFileName;
    }

    public void setTermFileName(String s) {
        termFileName = s;
    }

    public String getRelationTypeFileName() {
        return relationTypeFileName;
    }

    ProgressMonitorI wordProgressMonitor;

    public Corpus(String name) {
        this.name = name;
        directory = "?";
        filter = "?";
        relationBuilder = new RelationBuilder();

        try {
            String corpusInfoDirectory = FileNameSchema.getCorpusInfoDirectory(name);
            //Files.createDirectory(Paths.get(corpusInfoDirectory));
            File dir = new File(corpusInfoDirectory);
            dir.mkdirs();
        }
        catch(Exception e) {
            System.err.println("Unable to create info directory: " + e.getMessage());
        }
    }

    public Corpus() {
        this("?");
    }

    JTextArea textArea;
    JTextArea relationTextArea;

    public void countWords(ProgressMonitorI monitor) {
        wordCountFileName = FileNameSchema.getWordCountFileName(name);
        //WordCounter counter = new WordCounter(
        //        docListFileName, directory, filter, wordCountFileName);

        //Words.progressMonitor = wordProgressMonitor;
        //counter.start();
        String[] docFileNames = null;
        try {
            docFileNames = IceUtils.readLines(FileNameSchema.getDocListFileName(name));
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }
        TermCounter counter = TermCounter.prepareRun("onomaprops",
                Arrays.asList(docFileNames),
                directory,
                filter,
                wordCountFileName,
                monitor);
        counter.start();
    }

    public void findTerms(Corpus bgCorpus) {

        termFileName = FileNameSchema.getTermsFileName(name);
//                run("Terms", "utils/findTerms " + wordCountFileName + " " +
//                        Ice.corpora.get(backgroundCorpus).wordCountFileName + " " +
//                        termFileName);
        String ratioFileName = Ice.selectedCorpusName + "-" + backgroundCorpus + "-" + "Ratio";
        try {
            //Ratio.main(new String[] {wordCountFileName, Ice.corpora.get(backgroundCorpus).wordCountFileName, ratioFileName});
            //IceUtils.numsort(ratioFileName, termFileName);
            TermRanker.rankTerms(wordCountFileName, bgCorpus.wordCountFileName, termFileName);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }
    }

    public void checkForAndFindRelations(ProgressMonitorI progressMonitor, RelationFilter relationFilter) {
        relationInstanceFileName = FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);//name + "Relations";
        relationTypeFileName = FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName);//name + "Relationtypes";
        File file = new File(relationTypeFileName);
        if (file.exists() &&
                !file.isDirectory()) {
            int n = JOptionPane.showConfirmDialog(
                    Ice.mainFrame,
                    "Extracted patterns already exist. Show existing patterns without recomputation?",
                    "Patterns exist",
                    JOptionPane.YES_NO_OPTION);
            if (n == 0) {
                Corpus.displayTerms(relationTypeFileName,
                        40,
                        relationTextArea,
                        relationFilter);
                return;
            }
        }
        relationFilter.sententialPatternCheckBox.setSelected(false);
        relationFilter.onlySententialPatterns = false;
        findRelations(progressMonitor, "", relationTextArea);
    }

    public void findRelations(ProgressMonitorI progressMonitor, String docListPrefix, JTextArea publishToTextArea) {
        relationInstanceFileName = FileNameSchema.getRelationsFileName(name);
        relationTypeFileName = FileNameSchema.getRelationTypesFileName(name);
        docListFileName = FileNameSchema.getDocListFileName(name);

        RelationFinder finder = new RelationFinder(
                docListFileName, directory, filter, relationInstanceFileName,
                relationTypeFileName, publishToTextArea, numberOfDocs,
                progressMonitor);
        finder.start();
    }

    public void rankRelations() {
        String ratioFileName = FileNameSchema.getPatternRatioFileName(name, backgroundCorpus);
        rankRelations(backgroundCorpus, ratioFileName);
    }

    public static void rankRelations(String bgCorpus, String fileName) {
        try {
            String sortedRatioFileName = fileName + ".sorted";
            Ratio.main(new String[]{
                    Ice.selectedCorpus.relationTypeFileName,
                    Ice.corpora.get(bgCorpus).relationTypeFileName,
                    fileName
            });
            IceUtils.numsort(fileName, sortedRatioFileName);

            // return getTerms(sortedRatioFileName, 40, relationFilter);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(Ice.mainFrame,
                    "Error ranking patterns. Please make sure both fore and background patterns are counted.",
                    "Error ranking patterns",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.err);
        }
        //return new ArrayList<String>();
    }

    static void sort(String infile, String outfile) throws IOException{
        //run("Sort", "./numsort " + infile + " " + outfile);
        IceUtils.numsort(infile, outfile);
    }

    /**
     *  returns an array containing the paths (relative to the root of
     *  the directory tree for this corpus) of all the documents in the corpus.
     */

    String[] buildDocumentList() {
        if (directory.equals("?") || filter.equals("?")) return null;
        //File dir = new File(directory);
        //String[] docs = dir.list(new Jet.IceModels.CorpusFileFilter(filter));
        String[] docs = findAllFiles(directory, filter).toArray(new String[0]);
        numberOfDocs = docs.length;
        System.out.println("Directory has " + numberOfDocs + " files.");
        return docs;
    }

    public static List<String> findAllFiles(String sDir, String filter) {
        return findAllFiles(sDir, sDir, filter);
    }

    /**
     *  Determines the files in a corpus.  Returns a list of files beneath <CODE>sDir</CODE>
     *  whose file name has extension <CODE>filter</CODE>.  If <CODE>filter</CODE> is "*",
     *  all files are included regardless of file extension.
     */

    private static List<String> findAllFiles(String sDir, String topDir, String filter) {
        List<String> allFiles = new ArrayList<String>();
        File[] faFiles = new File(sDir).listFiles();
        String topPath = new File(topDir).getAbsolutePath();
        for (File file : faFiles) {
            // Added basic support for * wildcard to select all files.
            if (file.isFile() &&
                    ("*".equals(filter.trim()) ||
                        file.getName().endsWith(String.format(".%s", filter))
                    )
               ) {
                allFiles.add(file.getAbsolutePath().substring(topPath.length() + 1));
            }
            if (file.isDirectory()) {
                allFiles.addAll(findAllFiles(file.getAbsolutePath(), topDir, filter));
            }
        }
        return allFiles;
    }

    /**
     *  Writes to the docListFileName (as specified by the FileNameSchema) the
     *  names of all files in the corpus, excludng the file extension.
     */

    public void writeDocumentList() {
        String[] docs = buildDocumentList();
        if (docs == null) return;
        try {
            docListFileName = FileNameSchema.getDocListFileName(name);
            File docListFile = new File(docListFileName);
            PrintWriter writer = new PrintWriter(new FileWriter(docListFile));
            for (String doc : docs) {

                if ("*".equals(filter.trim())) {
                    writer.println(doc);
                } else {
                    String nameWithoutSuffix =
                            doc.substring(0, doc.length() - filter.length() - 1);
                    writer.println(nameWithoutSuffix);
                }
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Error writing document List");
            e.printStackTrace();
        }
    }

    public List<String> getRelations(int limit) {
        relationTypeFileName = FileNameSchema.getRelationTypesFileName(name);
        return getTerms(relationTypeFileName, limit, Corpus.relationFilter);
    }

    public static List<String> getTerms(String termFile, int limit, ListFilter tf) {
        List<String> topTerms = new ArrayList<String>();
        int k = 0;
        DepPathMap depPathMap = DepPathMap.getInstance();
        boolean displayRelation = false;
        if (tf != null && tf instanceof RelationFilter) {
            // depPathMap.clear();
            displayRelation = true;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(termFile));
            // System.out.println("TERMFILE: " + termFile);
//            File exampleFile = new File(termFile + ".source.dict");
//            BufferedReader exampleReader = null;
//            if (exampleFile.exists() && !exampleFile.isDirectory()) {
//                exampleReader = new BufferedReader(new FileReader(exampleFile));
//            }
            boolean shouldReload = !depPathMap.load();
            if (shouldReload) {
                depPathMap.clear();
            }
            while (true) {
                String term = reader.readLine();
//                String example = "";
//                if (exampleReader != null) {
//                    example = exampleReader.readLine();
//                }
                if (term == null) break;
                if (tf != null && !tf.filter(term)) continue;
                if (displayRelation) {
                    String[] parts = term.split("\t");
//                    if (example.indexOf("|||") > -1) {
//                        example = example.split("\\|\\|\\|")[1].trim();
//                    }
//                    if (shouldReload) {
//                        if (parts.length == 2 &&
//                                depPathMap.addPath(parts[1], example) == DepPathMap.AddStatus.SUCCESSFUL) {
//                            topTerms.add(parts[0] + "\t" + depPathMap.findRepr(parts[1]));
//                        }
//                    }
//                    else {
                        String repr = depPathMap.findRepr(parts[1]);
                        if (repr != null) {
                            topTerms.add(parts[0] + "\t" + repr);
                        }
//                    }
                }
                else {
                    topTerms.add(term);
                    k++;
                }
                if (k >= limit) break;
            }
			reader.close();
            if (displayRelation && shouldReload) {
                depPathMap.persist();
                System.err.println("DepPathMap saved.");
            }
//            if (null != exampleReader) {
//    			exampleReader.close();
//            }
        } catch (IOException e) {
            System.out.println("IOException");
        }
        return topTerms;
    }

    /**
     * display the first (up to) 'limit' lines of file 'termFile' in
     * TextArea 'textArea'.
     */

    public static void displayTerms(String termFile, int limit, JTextArea textArea, ListFilter tf) {
        if (null == textArea) {
            System.err.println("displayTerms() returning, no text area to publish to.");
            return;
        }
        List<String> topTerms = getTerms(termFile, limit, tf);
        textArea.setText("");
        for (String term : topTerms)
            textArea.append(term + "\n");
    }
}

class CorpusFileFilter implements FilenameFilter {

    String suffix;

    CorpusFileFilter(String suffix) {
        this.suffix = suffix;
    }

    public boolean accept(File dir, String name) {
        return name.endsWith("." + suffix);
    }
}

/**
 * count all words in corpus.
 */

class WordCounter extends Thread {

    String[] args;

    WordCounter(String docListFileName, String directory, String filter,
                String wordCountFileName) {
        args = new String[5];
        args[0] = "onomaprops";
        args[1] = docListFileName;
        args[2] = directory;
        args[3] = filter;
        args[4] = wordCountFileName;
    }

    public void run() {
        try {
            Words.main(args);
        } catch (IOException e) {
            System.out.println("IOException in Words " + e);
        }
    }
}


