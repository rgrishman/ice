package edu.nyu.jet.ice.models;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.50
//Copyright:    Copyright (c) 2011
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

import AceJet.*;
import Jet.Control;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import Jet.JetTest;
import Jet.Parser.DepParser;
import Jet.Parser.SyntacticRelationSet;
import Jet.Pat.Pat;
import Jet.Refres.Resolve;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;

import java.io.*;
import java.util.*;

/**
 *  collect a list of all dependency paths connecting two named entity mentions
 */

public class DetailedDepPaths {
    static PrintWriter writer;
	static PrintWriter sententialWriter;

    static String inputDir;
    static String inputFile;

    private static final int MAX_MENTIONS_IN_SENTENCE = 50;

    public static ProgressMonitorI progressMonitor = null;

    /**
     *  counts the number of instances of each dependency triple in a set
     *  of files.  Invoked by <br>
     *  DepCounter  propsFile docList inputDir inputSuffix outputFile
     *
     *    propsFile     Jet properties file
     *    docList       file containing list of documents to be processed, 1 per line
     *    inputDir      directory containing files to be processed
     *    inputSuffix   file extension to be added to document name to obtain name of input file
     *    outputFile    file to contain counts of dependency relations
     *    typeOutputFile    file to contain counts of dependency relations
     */

    public static void main (String[] args) throws IOException {


		if (args.length != 6) {
			System.err.println ("DepCounter requires 6 arguments:");
			System.err.println ("  propsFile docList inputDir inputSuffix outputFile outputSententialFile");
			System.exit (1);
		}
		String propsFile = args[0];
		String docList = args[1];
		inputDir = args[2];
		String inputSuffix = args[3];
		String outputFile = args[4];
		String outputSententialFile = args[5];

		// initialize Jet

		System.out.println("Starting Jet DepCounter ...");
		JetTest.initializeFromConfig(propsFile);
		// load ACE type dictionary
		EDTtype.readTypeDict();
		// turn off traces
		Pat.trace = false;
		Resolve.trace = false;
		// ACE mode (provides additional antecedents ...)
		Resolve.ACE = true;
        Properties props = new Properties();
        props.load(new FileReader(propsFile));

		String docName;
		int docCount = 0;

		BufferedReader docListReader = new BufferedReader(new FileReader (docList));
        boolean isCanceled = false;
		writer = new PrintWriter (new FileWriter (outputFile));
		sententialWriter = new PrintWriter(new FileWriter(outputSententialFile));

		while ((docName = docListReader.readLine()) != null) {
			docCount++;
            if ("*".equals(inputSuffix.trim())) {
                inputFile = docName;
            } else {
    			inputFile = docName + "." + inputSuffix;
            }
			System.out.println ("\nProcessing document " + docCount + ": " + inputFile);
			ExternalDocument doc = new ExternalDocument ("sgml", inputDir, inputFile);
			doc.setAllTags(true);
			doc.open();
			// process document
			Ace.monocase = Ace.allLowerCase(doc);
			// --------------- code from Ace.java
			doc.stretchAll();
			// process document
			Ace.monocase = Ace.allLowerCase(doc);

			Control.processDocument(doc, null, docCount == -1, docCount);

            Ace.tagReciprocalRelations(doc);
			String docId = Ace.getDocId(doc);
			if (docId == null)
				docId = docName;
			// create empty Ace document
			String sourceType = "text";
			AceDocument aceDoc =
				new AceDocument(inputFile, sourceType, docId, doc.text());
			// build entities
			Ace.buildAceEntities (doc, docId, aceDoc);
			// ---------------
			// invoke parser on 'doc' and accumulate counts
			SyntacticRelationSet relations = null;
			try {
				relations = DepParser.parseDocument(doc);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if (relations == null) {
				continue;
			}
			relations.addInverses();
			collectPaths (doc, aceDoc, relations);
		}
		docListReader.close();
        writer.close();
		sententialWriter.close();
	}

    static void collectPaths (Document doc, AceDocument aceDoc, SyntacticRelationSet relations) {
        List<Annotation> jetSentences = doc.annotationsOfType("sentence");
        if (jetSentences == null) return;
        Set<AceEntityMention> allMentions = new HashSet<AceEntityMention>();
        for (AceEntity entity : aceDoc.entities) {
            for (AceEntityMention mention : entity.mentions) {
                if (mention.type.startsWith("PRO")) continue;
                allMentions.add(mention);
            }
        }

        int sentCount = 0;
        for (Annotation sentence : jetSentences) {
            sentCount++;
            List<AceEntityMention> localMentions = new ArrayList<AceEntityMention>();
            for (AceEntityMention mention : allMentions) {
                if (mentionInSentence(mention, sentence)) {
                    localMentions.add(mention);
                }
            }
//            System.err.println("Total mentions in sentence:" + localMentions.size());
            if (localMentions.size() > MAX_MENTIONS_IN_SENTENCE) {
                System.err.println("Too many mentions in one sentence. Skipped.");
                continue;
            }

            for (AceEntityMention mention1 : localMentions) {
                for (AceEntityMention mention2 : localMentions) {
                    if (mention1.entity == mention2.entity) continue;
//                    if (mention1.type.startsWith("PRO")) continue;
//                    if (mention2.type.startsWith("PRO")) continue;
					String entity1Name = getLongestName(mention1.entity, doc);
					String entity2Name = getLongestName(mention2.entity, doc);
					if (entity1Name.length() == 0 || entity2Name.length() == 0) {
						continue;
					}
                    int h1 = mention1.getJetHead().start();
                    int h2 = mention2.getJetHead().start();
                    // - mention1 precedes mention2
                    if (h1 >= h2) continue;
                    // - in same sentence
//                    if (!sentences.inSameSentence(h1, h2)) continue;
                    // find and record dep path from head of m1 to head of m2
                    String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, relations, localMentions);
                    recordPath(doc, sentence, aceDoc, relations, mention1, path, mention2, entity1Name, entity2Name);
                }
            }
        }
    }

	private static String getLongestName(AceEntity entity, Document doc) {
		String result = "";
		for (AceEntityMention name : entity.mentions) {
			int start = name.jetExtent.start();
			if (doc.annotationsAt(start, "ENAMEX") == null) {
				continue;
			}
			String currentName = doc.text(name.jetExtent).replaceAll("\\s+", " ").trim();
			if (currentName.length() > result.length()) {
				result = currentName;
			}
		}
		return result;
	}

    private static boolean mentionInSentence(AceEntityMention mention, Annotation sentence) {
        return mention.jetExtent.start() >= sentence.start() &&
                mention.jetExtent.end() <= sentence.end();
    }


	static void recordPath (Document doc, Annotation sentence, AceDocument aceDoc, SyntacticRelationSet relations,
		                AceEntityMention mention1, String path, AceEntityMention mention2,
						String name1, String name2) {
//		if (path == null) return;
//		int pathLength = 0;
//		for (int i=0; i < path.length(); i++)
//			if (path.charAt(i) == ':')
//				pathLength++;
//		if (pathLength > 5) return;
//		String m1 = mention1.getHeadText();
//		String m2 = mention2.getHeadText();
//
//		// String source = inputDir + "/" + inputFile + " | " + start + " | " + end;
//		String source = DepPaths.pathText(doc, sentence, mention1, mention2);
//		String sourceDoc = aceDoc.sourceFile;
//		DepPathMap pathMap = DepPathMap.getInstance();
//		String vp = pathMap.linearize(mention1.getType() + ":" + path + ":" + mention2.getType());
//		String output = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
//				sourceDoc, m1, name1, m2, name2, path, vp, source);
//		if (m1.trim().length() <= 1 || m2.trim().length() <= 1) {
//			return;
//		}
//		writer.println(output);
//		if (path.matches(".*?subj-1:.*:?obj.*")) {
//			sententialWriter.println(output);
//		}
	}

}
