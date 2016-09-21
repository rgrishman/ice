package edu.nyu.jet.ice.entityset;

import edu.nyu.jet.aceJet.Ace;
import edu.nyu.jet.aceJet.Gazetteer;
import edu.nyu.jet.Control;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.lex.Lexicon;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;

import java.io.*;
import java.util.*;

import gnu.trove.map.hash.TObjectIntHashMap;
import opennlp.model.DataIndexer;
import opennlp.model.Event;
import opennlp.model.FileEventStream;
import opennlp.model.OnePassDataIndexer;
import org.la4j.vector.sparse.CompressedVector;
import org.la4j.Vector;
import org.la4j.iterator.VectorIterator;

/**
 * Index entity feature vectors for set expansion.  Writes a file containing the terms in
 * the corpus and, for each term, its PMI with its dependency contexts.
 *
 * @author yhe
 * @version 1.0
 */

public class EntitySetIndexer {

    private static ProgressMonitorI defaultProgressMonitor = null;
    private static final String[] stopWordsArr = new String[]{
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    };
    private static final Set<String> stopWords = new HashSet<String>();
    {
        for (String word : stopWordsArr) {
            stopWords.add(word);
        }
    }
    private static Stemmer stemmer = Stemmer.getDefaultStemmer();

    public static void setDefaultProgressMonitor(ProgressMonitorI progressMonitor) {
        EntitySetIndexer.defaultProgressMonitor = progressMonitor;
    }

	private ProgressMonitorI progressMonitor;
	private Set<String> words = new HashSet<String>();

	public EntitySetIndexer() {}

	public ProgressMonitorI getProgressMonitor() {
		return progressMonitor;
	}

	public void setProgressMonitor(ProgressMonitorI progressMonitor) {
		this.progressMonitor = progressMonitor;
	}

	/**
	 *  Command-line callable method to index terms by their contextual features.
	 */

	public static void main(String[] args) {
		EntitySetIndexer indexer = new EntitySetIndexer();
		indexer.setProgressMonitor(defaultProgressMonitor);
		indexer.run(args[0], args[1], Double.valueOf(args[2]), args[3], args[4], args[5], args[6], args[7]);
    }

       /**
        *  Index terms by their contextual features.
        *
        *  @param termFile    file of terms, one per line
        *  @param type        type of terms to be indexed
        *  @param cutoff       minimum score of terms to be indexed
        *  @param propsFile   Jet properties file
        *  @param docList     list of document file names to be processed
        *  @param inputDir    input dirctory for documents
        *  @param inputSuffix file extension for document file names (* ==> no extension)
        *  @param outputFile  output file with indexed terms
        */

        public void run(String termFile, String type, double cutoff, String propsFile, String docList, String inputDir, String inputSuffix, String outputFile) {
            try {
                // read in set of terms
                //
                // note:  tokens in muilti-token terms are blank separated in term file, 
                // are processed as underscore-separated, and are written out blank separated
                //
                String line;
                BufferedReader r = new BufferedReader(new FileReader(termFile));
                while ((line = r.readLine()) != null) {
                    Entity entity = Entity.fromString(line);
                    if (entity == null) continue;
                    if (entity.getScore() > cutoff &&
                            entity.getType().equals(type) &&
                            entity.getText().trim().length() > 1) {
                        words.add(entity.getText().trim().replaceAll("\\s+", "_"));
                    }
                }
                // Processing documents
                if (progressMonitor != null) {
                    progressMonitor.setProgress(0);
                    progressMonitor.setNote("Initializing Jet");
                }
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JetTest.initializeFromConfig(propsFile);
                Lexicon.clear();
                for (String word : words) {
                    Lexicon.addEntry(Gazetteer.splitAtWS(word.replaceAll("_", " ")),
                            new FeatureSet(),
                            "term");
                }

                String docName;
                int docCount = 0;
                //DictionaryBasedNamedEntityPostprocessor nePostprocessor =
                //        new DictionaryBasedNamedEntityPostprocessor(propsFile);
                BufferedReader docListReader = new BufferedReader(new FileReader (docList));
                List<Event> allEvents = new ArrayList<Event>();
                boolean isCanceled = false;
                while ((docName = docListReader.readLine()) != null) {
                    docCount++;
                    String inputFile = docName;
                    if (!("*".equals(inputSuffix.trim()))) {
                        inputFile = inputFile + "." + inputSuffix;
                    }
                    System.out.println ("\nIndexing terms in document " + docCount + ": " + inputFile);
                    try {
                        ExternalDocument doc = new ExternalDocument ("sgml", inputDir, inputFile);
                        doc.setAllTags(true);
                        doc.open();
                        // process document
                        Ace.monocase = Ace.allLowerCase(doc);
                        Control.processDocument(doc, null, false, docCount);

                        IcePreprocessor.fetchAnnotations(
                                FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName),
                                inputDir,
                                inputFile);
                        IcePreprocessor.loadPOS(doc);
                        SyntacticRelationSet syntacticRelationSet = IcePreprocessor.loadSyntacticRelationSet();
                        List<Annotation> sentences = doc.annotationsOfType("sentence");
                        if (sentences == null) continue;
                        for (Annotation sentence : sentences) {
                            allEvents.addAll(processSentence(sentence, doc, syntacticRelationSet, words));
                        }
                    }
                    catch (Exception e) {
                        System.err.println("EntitySetIndexer: unable to read document " + inputFile);
                    }
                    if (progressMonitor != null) {
                        if (progressMonitor.isCanceled()) {
                            isCanceled = true;
                            break;
                        }
                        progressMonitor.setProgress(docCount);
                        progressMonitor.setNote(docCount + " files processed");
                    }
                }
                // Indexing
                if (!isCanceled) {
                    if (progressMonitor != null) {
                        progressMonitor.setNote("Start indexing features... Cannot cancel.");
                    }
                    String eventFileName = FileNameSchema.getDependencyEventFileName(Ice.selectedCorpusName);
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(eventFileName)));
                    for (Event event : allEvents) {
                        pw.println(FileEventStream.toLine(event).trim());
                    }
                    pw.close();
                    // write file for word2vecf training
                    pw = new PrintWriter(new BufferedWriter(new FileWriter(
                                    FileNameSchema.getCorpusInfoDirectory(Ice.selectedCorpusName)
                                    + File.separator
                                    + "deps.context"
                                    )));
                    for (Event event : allEvents) {
                        for (String context : event.getContext()) {
                            pw.println(event.getOutcome().replaceAll("\\s+", "_")
                                    + " " + context.replaceAll("\\s+", "_")
                                    );
                        }
                    }
                    pw.close();
                    computePMI (eventFileName, outputFile);
                }
            }
            catch (Exception e) {
                System.err.println("Jet.ExpandEntitySet.EntitySetIndexer countFile type cutoff " +
                        "propsFile docList inputDir inputSuffix outputFile");
                e.printStackTrace();
            }
        }

        /**
         *  Given a file containing all the dependency relations in the corpus
         *  between a term and its syntactic context, computes the PMI between
         *  a term and its context.
         *
         *  @param  eventFileName  a file containing all the dependency relations,
         *                         one per line
         *  @param  outputFile     file to contain term-context PMIs
         */

        public void computePMI (String eventFileName, String outputFile) {

            try {

                FileEventStream eventStream = new FileEventStream(eventFileName);
                DataIndexer indexer = new OnePassDataIndexer(eventStream, 0);

                if (progressMonitor != null) {
                    progressMonitor.setProgress(progressMonitor.getMaximum());
                    progressMonitor.setNote("Calculating features... Takes time...");
                }

                String[] contextLabels = indexer.getPredLabels();
                TObjectIntHashMap contextMap = new TObjectIntHashMap();

                for (int i = 0; i < contextLabels.length; i++) {
                    String contextLabel = contextLabels[i];
                    contextMap.put(contextLabel, i+1);          /* TObjectIntHashMap will return 0 for not found */
                }
                // build counter = table of terms and their contexts
                Map<String, CompressedVector> counter = new HashMap<String, CompressedVector>();
                eventStream = new FileEventStream(eventFileName);
                while (eventStream.hasNext()) {
                    Event e = eventStream.next();
                    String word = e.getOutcome();
                    if (!counter.containsKey(word)) {
                        counter.put(word, new CompressedVector(contextLabels.length));
                    }
                    org.la4j.Vector v = counter.get(word);
                    for (String context : e.getContext()) {
                        int contextIdx = contextMap.get(context)-1;
                        v.set(contextIdx, v.get(contextIdx) + 1);
                    }
                }
                updateContextWithPMI(counter);
                // Write to index
                if (progressMonitor != null) {
                    progressMonitor.setProgress(progressMonitor.getMaximum());
                    progressMonitor.setNote("Writing index...");
                }
                PrintWriter w = new PrintWriter(new FileWriter(outputFile));
                PrintWriter wInverse = new PrintWriter(new FileWriter(outputFile + ".info"));
                w.println(contextLabels.length + 1);
                for (String word : counter.keySet()) {
                    writeVector(word, counter.get(word), w);
                    writeVectorInfo(word, counter.get(word), contextLabels, wInverse);
                }
                w.close();
                wInverse.close();
                if (progressMonitor != null) {
                    progressMonitor.setProgress(progressMonitor.getMaximum());
                    progressMonitor.setNote("Start indexing features... done.");
                }
            }
            catch (Exception e) {
                System.err.println("Jet.ExpandEntitySet.EntitySetIndexer countFile type cutoff " +
                        "propsFile docList inputDir inputSuffix outputFile");
                e.printStackTrace();
            }
        }

    /**
     *  Compute the PMI (pointwise mutual information) between a term and its
     *  contexts.
     *
     *  @param  counter  context counts.  The i-th element of counter[term]
     *                   is the frequency of term appearing with the i-th context.
     *                   This method replaces the frequency with the PMI.
     */

    private static void updateContextWithPMI (Map<String, CompressedVector> counter) {
        System.out.println("Starting to update PMIs.");
        if (counter.size() == 0) return;
        //
        // compute N = total number of dependency relations in corpus
        //
        double N = 0;
        int featureLength    = 0;
        for (CompressedVector v : counter.values()) {
            VectorIterator it = v.nonZeroIterator();
            while (it.hasNext()) {
                double val = it.next();
                N += val;
            }
            if (featureLength == 0) featureLength = v.length();
        }
        //
        // compute priors[i] = probability of context i
        //
        double[] priors = new double[featureLength];
        for (CompressedVector v : counter.values()) {
            VectorIterator it = v.nonZeroIterator();
            while (it.hasNext()) {
                double val = it.next();
                int i = it.index();
                priors[i] += val / N;
            }
        }
        for (CompressedVector v : counter.values()) {
            double denominator = 0;
            VectorIterator it = v.nonZeroIterator();
            while (it.hasNext()) {
                double val = it.next();
                denominator += val;
            }
            it = v.nonZeroIterator();
            while (it.hasNext()) {
                double val = it.next();
                int i = it.index();
                double prior = priors[i];
                if (val != 0 && prior != 0) {
                    it.set(Math.log(val/denominator/prior));
                }
            }
        }
        System.out.println("Finished updating PMIs.");
    }

    private static void writeVector(String s, CompressedVector v, PrintWriter w) {
        w.print(s.replaceAll("_", " "));
        VectorIterator it = v.nonZeroIterator();
        while (it.hasNext()) {
            double val = it.next();
            int i = it.index();
            if (val > 0.1) {
                w.print("\t" + i + ":" + String.format("%.8f", val));
            }
        }
        w.println();
    }

    private static void writeVectorInfo(String s, CompressedVector v, String[] contextLabels, PrintWriter w) {
        w.print(s.replaceAll("_", " "));
        VectorIterator it = v.nonZeroIterator();
        while (it.hasNext()) {
            double val = it.next();
            int i = it.index();
            if (val > 0.1) {
                w.print("\t" + contextLabels[i] + ":" + String.format("%.8f", val));
            }
        }
        w.println();
    }

    /**
     *  Generates for a sentence a list of terms and their contexts.
     */

    private static List<Event> processSentence(Annotation sentence, Document doc, SyntacticRelationSet s, Set<String> wordSet) {
        List<Annotation> terms = doc.annotationsOfType("term", sentence.span());
        if (terms == null) return new ArrayList<Event>();
        List<Event> result = new ArrayList<Event>();
        for (int i = 0; i < terms.size(); i++) {
            String tokenString = doc.text(terms.get(i)).trim().replaceAll("\\s+", "_");
            Annotation termHead = IcePreprocessor.findTermHead(doc, terms.get(i), s);
            if (wordSet.contains(tokenString)) {
                List<String> contextList = new ArrayList<String>();
                SyntacticRelation syn = s.getRelationTo(termHead.start());
                if (syn != null) {
                    if (!stopWords.contains(syn.sourceWord.trim())) {
                        contextList.add(String.format("%s-1_%s", syn.type, stemmer.getStem(syn.sourceWord.trim(), "NULL")));
                    }
                }
                SyntacticRelationSet fromSet = s.getRelationsFrom(termHead.start());
                if (fromSet != null) {
                    for (int j = 0; j < fromSet.size(); j++) {
                        SyntacticRelation r = fromSet.get(j);
                        if (!stopWords.contains(r.targetWord.trim())) {
                            contextList.add(String.format("%s_%s", r.type, stemmer.getStem(r.targetWord.trim(), "NULL")));
                        }
                    }
                }
                Event event = new Event(tokenString,
                        contextList.toArray(new String[contextList.size()]));
				if (contextList.size() > 0) {
					result.add(event);
				}
            }
        }
        return result;
    }
}
