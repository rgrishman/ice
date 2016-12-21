package edu.nyu.jet.aceJet;

import edu.nyu.jet.Control;
import edu.nyu.jet.ice.models.DepPaths;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.parser.DepParser;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;
import edu.nyu.jet.tipster.Span;
import gnu.trove.set.hash.TIntHashSet;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dependency tree based event tagger
 *
 * @author yhe
 * @version 1.0
 */
public class EventTrees {
    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            System.err.println(RelaxedDepPathRelationTagger.class.getName() +
                    "propsFile rulesFile embeddingsFile docFileList outputFileList answerFileList");
            System.exit(-1);
        }

        String propsFile   = args[0];
        String rulesFile   = args[1];
        String embeddingsFile = args[2];
        String docFileList = args[3];
        String outputFileList = args[4];
        String answerFileList = args[5];

        String[] docFiles = IceUtils.readLines(docFileList);
//        String[] outputFiles = IceUtils.readLines(outputFileList);
//        String[] answerFiles = IceUtils.readLines(answerFileList);

//        if (docFiles.length != outputFiles.length ||
//                docFiles.length != answerFiles.length) {
//            System.err.println("Length of txt, output, and answer files should equal");
//            System.exit(-1);
//        }

        JetTest.initializeFromConfig(propsFile);
        int docCount = 0;
        String preprocessDir = "preprocess";
        File f = new File(preprocessDir);
        f.mkdirs();
        for (String apfDoc : docFiles) {
            docCount++;
            Set<String> keyTriggers = new HashSet<String>();
            Set<String> keyArguments = new HashSet<String>();
            Set<String> keyRoles = new HashSet<String>();
            String textFileName   = "training" + File.separator + apfDoc + ".sgm";
            String apfFileName = "training" + File.separator + apfDoc + ".apf.xml";
            String parsesFileName   = preprocessDir + File.separator + apfDoc + ".event_parses";

            File parsesFile = new File(parsesFileName.substring(0, parsesFileName.lastIndexOf(File.separator)));
            parsesFile.mkdirs();
            String mentionsFileName = preprocessDir + File.separator + apfDoc + ".event_mentions";
            AceDocument aceDoc = new AceDocument(textFileName, apfFileName);
            ExternalDocument doc = new ExternalDocument("sgml", textFileName);
            doc.setAllTags(true);
            doc.open();
            doc.stretchAll();
            Ace.monocase = Ace.allLowerCase(doc);
            Control.processDocument(doc, null, docCount == -1, docCount);
            // IcePreprocessor.tagAdditionalMentions(doc, aceDoc);
            List<Annotation> sentences = doc.annotationsOfType("sentence");
            List<Annotation> names = doc.annotationsOfType("ENAMEX");
            if (names == null) {
                continue;
            }
            PrintWriter parsesWriter = new PrintWriter(new BufferedWriter(new FileWriter(parsesFileName)));
            PrintWriter mentionsWriter = new PrintWriter(new BufferedWriter(new FileWriter(mentionsFileName)));

            int sid = 1;
            if (sentences != null) {
                for (Annotation sentence : sentences) {
                    SyntacticRelationSet relations = new SyntacticRelationSet();
                    List<Annotation> tokens  = doc.annotationsOfType("token", sentence.span());
                    List<Annotation> posTags = doc.annotationsOfType("tagger", sentence.span());
                    if (tokens == null || posTags == null ||
                            tokens.size() == 0 || posTags.size() == 0 ||
                            posTags.size() != tokens.size()) {
                        continue;
                    }
                    // collect local mentions
                    System.err.println("SENTENCE:" + doc.text(sentence));
                    DepParser.parseSentence(doc, sentence.span(), relations);
                    // ****** must be updated to reflect new representation ***********
                    // IcePreprocessor.appendSyntacticRelationSet(relations, parsesWriter, sid);

                    List<Annotation> localNames = new ArrayList<Annotation>();
                    List<Span> localHeadSpans   = new ArrayList<Span>();
                    for (Annotation name : names) {
                        if (DepPaths.annotationInSentence(name, sentence)) {
                            Annotation termHead = IcePreprocessor.findTermHead(doc, name, relations);
                            if (termHead != null) {
                                localNames.add(name);
                                localHeadSpans.add(termHead.span());
                            }
                        }
                    }
                    if (localNames.size() > DepPaths.MAX_MENTIONS_IN_SENTENCE) {
                        System.err.println("Too many mentions in one sentence. Skipped.");
                        continue;
                    }
                    for (int i = 0; i < localNames.size(); i++) {
                        if (localNames.get(i).get("TYPE") == null) {
                            continue;
                        }
                        int start = localNames.get(i).start();
                        int end   = localNames.get(i).end();
                        String surface = doc.text(localNames.get(i)).replaceAll("\\s+", " ").trim();
                        String value   = localNames.get(i).get("val") ==
                                null ? surface : ((String)localNames.get(i).get("val")).replaceAll("\\s+", " ").trim();
                        mentionsWriter.println(String.format("%d\t%d\t%d\t%s\t%s", sid, start, end, surface, value));
                    }

                    // collect subtrees governing mentions
                    // 1 mention


                    // 2 mentions

                    // 3 mentions
                    sid++;
                }
            }
            parsesWriter.close();
            mentionsWriter.close();
        }
    }

    /**
     * Extract and save dependency trees between arguments of events
     * @param doc
     * @param relations relations here should not include inverses
     */
    static void collectTrees (Document doc,
                              SyntacticRelationSet relations) {

        List<Annotation> jetSentences = doc.annotationsOfType("sentence");
        if (jetSentences == null) return;
        List<Annotation> names = doc.annotationsOfType("ENAMEX");
        if (names == null) {
            return;
        }
        int sentCount = 0;
        for (Annotation sentence : jetSentences) {
            sentCount++;

            List<Annotation> localNames = new ArrayList<Annotation>();
            List<Span> localHeadSpans   = new ArrayList<Span>();
            for (Annotation name : names) {
                if (DepPaths.annotationInSentence(name, sentence)) {
                    localNames.add(name);
                    localHeadSpans.add(
                            IcePreprocessor.findTermHead(doc, name, relations).span());
                }
            }
            if (localNames.size() > DepPaths.MAX_MENTIONS_IN_SENTENCE) {
                System.err.println("Too many mentions in one sentence. Skipped.");
                continue;
            }
            for (int i = 0; i < localNames.size(); i++) {
                for (int j = 0; j < localNames.size(); j++) {
                    if (i == j) continue;
                    int h1 = localHeadSpans.get(i).start();
                    int h2 = localHeadSpans.get(j).start();
                    // - mention1 precedes mention2
                    if (h1 >= h2) continue;
                    // - in same sentence
//                    if (!sentences.inSameSentence(h1, h2)) continue;
                    // find and record dep path from head of m1 to head of m2
                    // DepPath path = buildSyntacticPathOnSpans(h1, h2, relations, localHeadSpans);
                    // recordPath(doc, sentence, relations, localNames.get(i), path, localNames.get(j));
                }
            }
        }
    }

    public List<Integer> findAncestors(SyntacticRelationSet relationSet, int posn) {
        List<Integer> result = new ArrayList<Integer>();
        int currentPosn = posn;
        SyntacticRelation r;
        while ((r = relationSet.getRelationTo(currentPosn)) != null) {
            result.add(r.sourcePosn);
            currentPosn = r.sourcePosn;
        }
        return result;
    }

    public TIntHashSet findCommonRoot(SyntacticRelationSet relations,
                                        TIntHashSet argumentPosns) {
        TIntHashSet result = null;
        for (int argPosn : argumentPosns.toArray()) {
            TIntHashSet localRoots = new TIntHashSet();
            SyntacticRelation r;
            int currentPosn = argPosn;
            while ((r = relations.getRelationTo(currentPosn)) != null) {
                localRoots.add(r.sourcePosn);
                currentPosn = r.sourcePosn;
            }
            if (result == null) {
                result = localRoots;
            }
            else {
                int[] resultArray = result.toArray();
                TIntHashSet newResult = new TIntHashSet();
                for (int i = 0; i < resultArray.length; i++) {
                    if (result.contains(resultArray[i])) {
                        newResult.add(resultArray[i]);
                    }
                }
                result = newResult;
            }
        }
        return result;
    }
}
