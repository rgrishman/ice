// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

package edu.nyu.jet.ice.models;

import java.util.*;
import java.io.*;
import javax.swing.ProgressMonitor;

import edu.nyu.jet.aceJet.EDTtype;
import edu.nyu.jet.Control;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.pat.Pat;
import edu.nyu.jet.aceJet.Ace;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.lex.*;

/**
 *  process a set of documents through Jet and then accumulate word counts,
 *  stemmed and tagged for part of speech (common noun / name / verb / other)
 */

public class Words {

    //static Stemmer stemmer=new Stemmer().getDefaultStemmer();
    static Stemmer stemmer;

    static Map<String, Integer> wordCounts = new TreeMap<String, Integer>();

    public static ProgressMonitor progressMonitor = null;

    /**
     *  counts the number of instances of each word in a set of files.  Invoked by <br>
     *  Words  propsFile docList inputDir inputSuffix outputFile
     *
     *    propsFile     Jet properties file
     *    docList       file containing list of documents to be processed, 1 per line
     *    inputDir      directory containing files to be processed
     *    inputSuffix   file extension to be added to document name to obtain name of input file
     *    outputFile    file to contain word counts
     */

    public static void main (String[] args) throws IOException {

	if (args.length != 5) {
	    System.err.println ("Words requires 5 arguments:");
	    System.err.println ("  propsFile docList inputDir inputSuffix outputFile");
	    System.exit (1);
	}
	String propsFile = args[0];
	String docList = args[1];
	String inputDir = args[2];
	String inputSuffix = args[3];
	String outputFile = args[4];
		
        stemmer = Stemmer.getDefaultStemmer();
	// initialize Jet
		
	System.out.println("Starting Jet Word Counter ...");
	JetTest.initializeFromConfig(propsFile);
	// load ACE type dictionary
	EDTtype.readTypeDict();
	// turn off traces
	Pat.trace = false;
	Resolve.trace = false;
	// ACE mode (provides additional antecedents ...)
	Resolve.ACE = true;
		
	String docName;
	int docCount = 0;
	//        DictionaryBasedNamedEntityPostprocessor nePostprocessor =
	//                new DictionaryBasedNamedEntityPostprocessor(propsFile);
	BufferedReader docListReader = new BufferedReader(new FileReader (docList));
        boolean isCanceled = false;
	while ((docName = docListReader.readLine()) != null) {
            if (progressMonitor != null && progressMonitor.isCanceled()) {
                isCanceled = true;
                System.err.println("Word counter canceled.");
                break;
            }
	    docCount++;
	    String inputFile = docName + "." + inputSuffix;
	    System.out.println ("\nProcessing document " + docCount + ": " + inputFile);
	    ExternalDocument doc = new ExternalDocument ("sgml", inputDir, inputFile);
	    doc.setAllTags(true);
	    doc.open();
	    // process document
	    Ace.monocase = Ace.allLowerCase(doc);
	    Control.processDocument(doc, null, false, docCount);
	    //            nePostprocessor.tagDocument(doc);
            countWordsInDoc (doc);
            if (progressMonitor != null) {
		progressMonitor.setProgress(docCount);
		progressMonitor.setNote(docCount + " files processed");
	    }
	}
	// write counts
        if (!isCanceled) {
	    PrintWriter writer = new PrintWriter (new FileWriter (outputFile));
	    for (String r : wordCounts.keySet()) {
		writer.printf ("%8d\t%s\n", wordCounts.get(r), r);
	    }
	    writer.close();
        }
    }

    /** Partition POS tag into four types*/

    static String normalizePos(String pos){
	if (pos.toLowerCase().startsWith("nnp"))
	    return "nnp";
	if (pos.toLowerCase().startsWith("nn"))
	    return "nn";
	if (pos.toLowerCase().startsWith("vb"))
	    return "vb";
	return "o";
    }
	
    static String normalizeWord(String word,String pos){
	String np=word.replaceAll("[ \t]+", "_");
	String ret = "";
	String[] tmp;
	tmp = np.split("_ \t");
		
	if(tmp.length==0)
	    return "";
		
	if(tmp.length == 1)
	    ret = stemmer.getStem(tmp[0].toLowerCase(), pos.toLowerCase());
	else {
	    for(int i=0; i<tmp.length-1; i++) {
		ret += stemmer.getStem(tmp[i].toLowerCase(), pos.toLowerCase()) + "_";
	    }
	    ret += stemmer.getStem(tmp[tmp.length-1].toLowerCase(), pos.toLowerCase());
	}
	//System.out.println("[stemmer] " + ret);
	return ret;
		
    }

    /**
     *  increments entries in the table 'wordCounts' based on the number
     *  of word / name types present in document 'doc'.  Names identified
     *  by the name tagger are treated as units;  other tokens are counted
     *  individually.
     */

    static void countWordsInDoc (Document doc) {
	Vector<Annotation> sentences = doc.annotationsOfType("sentence");
	if (sentences == null) return;
	for (Annotation sentence : sentences) {
	    int posn = sentence.start();
	    int limit = sentence.end();
	    while (posn < limit) {
		Vector<Annotation> enamexAnns = doc.annotationsAt(posn, "ENAMEX");
		String edtType;
		String r;
		if (enamexAnns != null) {
		    Annotation enamexAnn = enamexAnns.get(0);
		    Annotation token = doc.tokenAt(posn);
		    edtType = (String)enamexAnn.get("TYPE");
		    String name = doc.normalizedText(token);
		    String pos = "nnp";
		    posn = token.end();
		    int nameEnd = enamexAnn.end();
		    while (posn < nameEnd) {
			token = doc.tokenAt(posn);
			name = name + "_" + doc.normalizedText(token);
			posn = token.end();
		    }
		    r = name + "/" + normalizePos(pos);
		} else {
		    Vector<Annotation> taggerAnns = doc.annotationsAt(posn, "tagger");
		    if (taggerAnns == null) return;
		    Annotation taggerAnn = taggerAnns.get(0);
		    edtType = EDTtype.getTypeSubtype(doc, taggerAnn, taggerAnn);
		    String pos = (String) taggerAnn.get("cat");
		    String word = doc.normalizedText(taggerAnn);
		    String stem = stemmer.getStem(word, pos);
		    posn = taggerAnn.end();
		    if (word.matches("\\d+")) continue;
		    r = stem + "/" + normalizePos(pos);
		} 
		if (!edtType.startsWith("OTHER")) {
		    r = r + "/" + edtType;
		}
		Integer count = wordCounts.get(r);
		if (count == null) count = 0;
		wordCounts.put(r, count + 1);
	    }
	}
    }
	
}
