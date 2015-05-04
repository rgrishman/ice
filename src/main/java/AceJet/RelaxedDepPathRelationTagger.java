package AceJet;

import Jet.IceModels.DepPathRegularizer;
import Jet.IceUtils.IceUtils;
import Jet.JetTest;
import Jet.Parser.SyntacticRelationSet;
import Jet.Refres.Resolve;
import Jet.RelationAL.PathRelationExtractor;
import Jet.Tipster.Document;
import Jet.Tipster.ExternalDocument;
import Jet.Zoner.SentenceSet;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  a relaxed relation tagger based on dependency paths and argument types,
 *  as produced by Jet ICE.
 */

public class RelaxedDepPathRelationTagger {

    final static Logger logger = LoggerFactory.getLogger(RelaxedDepPathRelationTagger.class);

    static Document doc;
    static AceDocument aceDoc;
    static String currentDoc;
    static PathRelationExtractor pathRelationExtractor;
    static boolean searchMode = false;
    private static DepPathRegularizer pathRegularizer = new DepPathRegularizer();


    // model:  a map from AnchoredPath strings to relation types
    static Map<String, String> model = null;

    /**
     * relation 'decoder':  identifies the relations in document 'doc'
     * (from file name 'currentDoc') and adds them
     * as AceRelations to AceDocument 'aceDoc'.
     */

    public static void findRelations(String currentDoc, Document d, AceDocument ad) {
        doc = d;
        RelationTagger.doc = d;
        doc.relations.addInverses();
        aceDoc = ad;
        RelationTagger.docName = currentDoc;
        RelationTagger.sentences = new SentenceSet(doc);
        RelationTagger.relationList = new ArrayList<AceRelation>();
        RelationTagger.findEntityMentions(aceDoc);
        // collect all pairs of nearby mentions
        List<AceEntityMention[]> pairs = RelationTagger.findMentionPairs();
        // iterate over pairs of adjacent mentions, using model to determine which are ACE relations
        for (AceEntityMention[] pair : pairs)
            predictRelation(pair[0], pair[1], doc.relations);
        // combine relation mentions into relations
        RelationTagger.relationCoref(aceDoc);
        RelationTagger.removeRedundantMentions(aceDoc);
    }

    /**
     * load the model used by the relation tagger.  Each line consists of an
     * AnchoredPath [a lexicalized dependency path with information on the
     * endpoint types], a tab, and a relation type.
     */

    static void loadModel(String modelFile, String embeddingFile) throws IOException {
        pathRelationExtractor = new PathRelationExtractor();
        pathRelationExtractor.loadRules(modelFile);
        pathRelationExtractor.loadNeg(modelFile + ".neg");
//        pathRelationExtractor.loadEmbeddings(embeddingFile);
    }


    /**
     * use dependency paths to determine whether the pair of mentions bears some
     * ACE relation;  if so, add the relation to relationList.
     */

    private static void predictRelation(AceEntityMention m1, AceEntityMention m2,
                                        SyntacticRelationSet relations) {
        // compute path
        int h1 = m1.getJetHead().start();
        int h2 = m2.getJetHead().start();
        String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, relations);
        if (path == null) return;
        path = AceJet.AnchoredPath.reduceConjunction(path);
        if (path == null) return;
        path = AceJet.AnchoredPath.lemmatizePath(path);
        // simplify path to improve recall
        path = path.replace("would:vch:", "");
        path = path.replace("be:vch:", "");
        path = path.replace("were:vch:", "");
        // build pattern = path + arg types
        String pattern = m1.entity.type + "--" + path + "--" + m2.entity.type;
        // look up path in model
        Event event = new Event("UNK", new String[]{
                pathRegularizer.regularize(path), m1.entity.type, m2.entity.type});
        String outcome = pathRelationExtractor.predict(event);
        if (outcome == null) return;
        // if (!RelationTagger.blockingTest(m1, m2)) return;
        // if (!RelationTagger.blockingTest(m2, m1)) return;
        String[] typeSubtype = outcome.split(":", 2);
        String type = typeSubtype[0];
        String subtype;
        if (typeSubtype.length == 1) {
            subtype = "";
        } else {
            subtype = typeSubtype[1];
        }
        if (subtype.endsWith("-1")) {
            subtype = subtype.replace("-1", "");
            AceRelationMention mention = new AceRelationMention("", m2, m1, doc);
            AceRelation relation = new AceRelation("", type, subtype, "", m2.entity, m1.entity);
            relation.addMention(mention);
            RelationTagger.relationList.add(relation);
        } else {
            AceRelationMention mention = new AceRelationMention("", m1, m2, doc);
            System.out.println("Found " + outcome + " relation " + mention.text);  //<<<
            AceRelation relation = new AceRelation("", type, subtype, "", m1.entity, m2.entity);
            relation.addMention(mention);
            RelationTagger.relationList.add(relation);
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 6) {
            System.err.println(RelaxedDepPathRelationTagger.class.getName() +
                    "propsFile rulesFile embeddingsFile txtFileList outputFileList answerFileList");
            System.exit(-1);
        }

        String propsFile   = args[0];
        String rulesFile   = args[1];
        String embeddingsFile = args[2];
        String txtFileList = args[3];
        String outputFileList = args[4];
        String answerFileList = args[5];

        String[] txtFiles = IceUtils.readLines(txtFileList);
        String[] outputFiles = IceUtils.readLines(outputFileList);
        String[] answerFiles = IceUtils.readLines(answerFileList);

        if (txtFiles.length != outputFiles.length ||
                txtFiles.length != answerFiles.length) {
            System.err.println("Length of txt, output, and answer files should equal");
            System.exit(-1);
        }

        JetTest.initializeFromConfig(propsFile);
        loadModel(rulesFile, embeddingsFile);
        if (searchMode) {
            double bestScore = 0.0;
            String bestParameterString = "NONE";
            for (int replace = 1; replace < 11; replace++) {
                for (int insert = 1; insert < 11; insert++) {
                    for (int delete = 1; delete < 11; delete++) {
                        pathRelationExtractor.updateCost(replace / 5.0, insert / 5.0, delete / 5.0);
                        String parameterString = String.format("replace:%.2f\tinsert:%.2f\tdelete:%.2f",
                                replace / 5.0, insert / 5.0, delete / 5.0);
                        clearRelationScorer();
                        for (int i = 0; i < txtFiles.length; i++) {
                            Resolve.ACE = true;
                            String fileName = txtFiles[i];
                            ExternalDocument doc = new ExternalDocument("sgml", fileName);
                            doc.open();
                            AceDocument aceDocument = Ace.processDocument(doc, "doc", fileName, ".");
                            findRelations(fileName, doc, aceDocument);
                            PrintWriter pw = new PrintWriter(new FileWriter(outputFiles[i]));
                            aceDocument.write(pw, doc);
                            pw.close();
                            RelationScorer.scoreDocument(fileName, outputFiles[i], answerFiles[i]);
                        }
                        RelationScorer.reportScores();
                        double score = fscore();
                        if (bestScore < score) {
                            bestScore = score;
                            bestParameterString = parameterString;
                        }
                        System.err.println("[TUNING]\t" + parameterString + "\t" + score);
                    }
                }
            }
            System.err.println("[BEST]\t" + bestParameterString + "\t" + bestScore);
        }
        else {
////            // pathRelationExtractor.updateCost(0.8, 0.4, 1.2);
            pathRelationExtractor.updateCost(100, 100, 100);
//            pathRelationExtractor.updateCost(0.8, 0.3, 1.2);
//            pathRelationExtractor.setNegDiscount(1);
            pathRelationExtractor.setNegDiscount(100);

            clearRelationScorer();
            TypedRelationExtractorScorer sellScorer = new TypedRelationExtractorScorer("RESIDENT-OF");
            for (int i = 0; i < txtFiles.length; i++) {
                Resolve.ACE = true;
                String fileName = txtFiles[i];
                ExternalDocument doc = new ExternalDocument("sgml", fileName);
                doc.open();
                AceDocument aceDocument = Ace.processDocument(doc, "doc", fileName, ".");
                findRelations(fileName, doc, aceDocument);
                PrintWriter pw = new PrintWriter(new FileWriter(outputFiles[i]));
                aceDocument.write(pw, doc);
                pw.close();
                sellScorer.scoreDocument(fileName, outputFiles[i], answerFiles[i]);
            }
            sellScorer.reportScores();
            //RelationScorer.reportScores();
        }
    }

    public static void clearRelationScorer() {
        RelationScorer.correctEntityMentions = 0;
        RelationScorer.missingEntityMentions = 0;
        RelationScorer.spuriousEntityMentions = 0;
        RelationScorer.correctRelationMentions = 0;
        RelationScorer.missingRelationMentions = 0;
        RelationScorer.spuriousRelationMentions = 0;
        RelationScorer.relationMentionTypeErrors = 0;
    }

    static double precision() {
        int responseCount = RelationScorer.correctRelationMentions +
                RelationScorer.relationMentionTypeErrors +
                RelationScorer.spuriousRelationMentions;
        int keyCount = RelationScorer.correctRelationMentions +
                RelationScorer.relationMentionTypeErrors +
                RelationScorer.missingRelationMentions;
        return (double)RelationScorer.correctRelationMentions/responseCount;
    }

    static double recall() {
        int responseCount = RelationScorer.correctRelationMentions +
                RelationScorer.relationMentionTypeErrors +
                RelationScorer.spuriousRelationMentions;
        int keyCount = RelationScorer.correctRelationMentions +
                RelationScorer.relationMentionTypeErrors +
                RelationScorer.missingRelationMentions;
        return (double)RelationScorer.correctRelationMentions/keyCount;
    }

    static double fscore() {
        double precision = precision();
        double recall    = recall();
        return 2*precision*recall/(precision + recall);
    }


}
