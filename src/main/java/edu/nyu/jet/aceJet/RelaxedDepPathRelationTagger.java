package edu.nyu.jet.aceJet;

import edu.nyu.jet.JetTest;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;
import edu.nyu.jet.zoner.SentenceSet;
import edu.nyu.jet.ice.models.DepPathRegularizer;
import edu.nyu.jet.ice.relation.PathRelationExtractor;
import edu.nyu.jet.ice.utils.IceUtils;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 *  a relaxed relation tagger based on dependency paths and argument types,
 *  as produced by Jet ICE.  The tagger can perform 3 functions:
 *  <br>
 *     Find the beat soft-match parameters
 *  <br>
 *     Identify the relatiom instances in a corpu using exact match.
 *  <br>
 *     Identify the relation instqnces in a corpus using soft match.
 */


public class RelaxedDepPathRelationTagger {

    final static Logger logger = LoggerFactory.getLogger(RelaxedDepPathRelationTagger.class);

    static Document doc;
    static AceDocument aceDoc;
    static String currentDoc;
    static PathRelationExtractor pathRelationExtractor;
    static boolean searchMode = false;;
    static boolean softMatch = true;
    private static DepPathRegularizer pathRegularizer = new DepPathRegularizer();


    // model:  a map from AnchoredPath strings to relation types
    static Map<String, String> model = null;

    /**
     * relation 'decoder':  identifies the relations in document 'doc'
     * (from file name 'currentDoc') and adds them
     * as AceRelations to AceDocument 'aceDoc'.
     */

    public static void findRelations(String currentDoc, Document d, AceDocument ad, AceDocument keyDoc) {
        doc = d;
        RelationTagger.doc = d;
        doc.relations.addInverses();
        aceDoc = ad;
        RelationTagger.docName = currentDoc;
        RelationTagger.sentences = new SentenceSet(doc);
        RelationTagger.relationList = new ArrayList<AceRelation>();
        ad.entities = keyDoc.entities;
        RelationTagger.findEntityMentions(keyDoc);
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
        path = AnchoredPath.reduceConjunction(path);
        if (path == null) return;
        path = AnchoredPath.lemmatizePath(path);
        // simplify path to improve recall
        path = path.replace("would:vch:", "");
        path = path.replace("be:vch:", "");
        path = path.replace("were:vch:", "");
        // build pattern = path + arg types
        String pattern = m1.entity.type + "--" + path + "--" + m2.entity.type;
        // look up path in model
        String outcome;
        if (softMatch) {
            Event event = new Event("UNK", new String[]{
                pathRegularizer.regularize(path), m1.entity.type, m2.entity.type});
            outcome = pathRelationExtractor.predict(event);
        } else {
            outcome = predict(pattern);
        }
        if (outcome == null) return;
        logger.info("Matched pattern {}, predicting {}", pattern, outcome);
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
            System.out.println("Found " + outcome + " relation " + mention.text);  //<<<
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

        if (args.length != 9) {
            System.err.println(RelaxedDepPathRelationTagger.class.getName() +
                    " propsFile txtFileList keyFileList outputFileList");
            System.exit(-1);
        }
        String function = args[0];
        String propsFile = args[1];
        String docListFile = args[2];
        String sourceDir = args[3];
        String sourceExt = args[4];
        String responseDir = args[5];
        String responseExt = args[6];
        String keyDir = args[7];
        String keyExt = args[8];

        JetTest.initializeFromConfig(propsFile);
        BufferedReader reader = new BufferedReader (new FileReader (docListFile));
        String line;
        List<String> docList = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            docList.add(line);
        }
        tagCollection (function, docList, docListFile,
                         sourceDir, sourceExt,
                         responseDir, responseExt,
                         keyDir, keyExt);
    }

    /**
     * Tag a set of text files with relations based on patterns created by ICE.
     * Reads an APF file with entities (keyFile) and writes an APF file
     * cotaining entties and relations (response files).
     *
     * @param function  either 'exact' (exact match), 'soft' (soft matcch), or 'train'
     *                  (find optimal soft-match weights)
     * @param propsfile Jet properties file for preprocessing text
     * @param docList   list of document names
     * @param docListFile file containing docList, one entry per line
     * @param sourceDir directory containing source files
     * @param sourceExt file extention for source files
     * @param responseDir directory containing source files
     * @param responseExt file extention for response files
     * @param keyDir directory containing key files
     * @param keyExt file extention for key files
     */

    public static void tagCollection (String function, List<String> docList, String docListFile,
                         String sourceDir, String sourceExt,
                         String responseDir, String responseExt,
                         String keyDir, String keyExt) throws IOException {

        if (function.equals("exact")) {
            softMatch = false;
        } else if (function.equals("soft")) {
            softMatch = true;
        } else if (function.equals("train")) {
            searchMode = true;
        } else {
            System.err.println ("Invalid function (require exact, soft, or train)");
            System.exit(-1);
        }
        
        // loadModel(JetTest.getConfig("Jet.dataPath")
        // + File.separator
        // + JetTest.getConfig("Ace.RelationModel.fileName"), "null");

        String jetHome = System.getProperty("jetHome");
        loadModel(jetHome + "/data/ldpRelationModel");
        // loadModel("rules");

        if (searchMode) {
            double bestScore = 0.0;
            String bestParameterString = "NONE";
            int limit = 7;
            for (int replace = 1; replace < limit; replace++) {
                for (int insert = 1; insert < limit; insert++) {
                    for (int delete = 1; delete < limit; delete++) {
                        pathRelationExtractor.updateCost(replace / 5.0, insert / 5.0, delete / 5.0);
                        String parameterString = String.format("replace:%.2f\tinsert:%.2f\tdelete:%.2f",
                                replace / 5.0, insert / 5.0, delete / 5.0);
                        int docCount = 0;
                        for (String docName : docList) {
                            docCount++;
                            System.out.println ("\nTagging document " + docCount + ": " + docName);
                            String sourceFile = sourceDir + "/" + docName + "." + sourceExt;
                            String responseFile = responseDir + "/" + docName + "." + responseExt;
                            String keyFile = keyDir + "/" + docName + "." + keyExt;
                            Resolve.ACE = true;
                            ExternalDocument doc = new ExternalDocument("sgml", sourceFile);
                            doc.setAllTags(true);
                            doc.open();
                            AceDocument aceDocument = Ace.processDocument(doc, "doc", sourceFile, ".");
                            AceDocument keyDoc = new AceDocument(sourceFile, keyFile);
                            findRelations(sourceFile, doc, aceDocument, keyDoc);
                            PrintWriter pw = new PrintWriter(new FileWriter(responseFile));
                            aceDocument.write(pw, doc);
                            pw.close();
                        }
                        double score = edu.nyu.jet.aceJet.RelationScorer.scoreCollection(docListFile,
                           sourceDir, sourceExt, responseDir, responseExt, keyDir, keyExt);
                        if (bestScore < score) {
                            bestScore = score;
                            bestParameterString = parameterString;
                        }
                        System.err.println("[TUNING]\t" + parameterString + "\t" + score);
                    }
                }
            }
            System.err.println("[BEST]\t" + bestParameterString + "\t" + bestScore);
        } else {
//            pathRelationExtractor.updateCost(100, 100, 100);
             pathRelationExtractor.updateCost(0.8, 0.3, 1.2);
            // pathRelationExtractor.setNegDiscount(1);
             for (String docName : docList) {
                Resolve.ACE = true;
                String sourceFile = sourceDir + "/" + docName + "." + sourceExt;
                String responseFile = responseDir + "/" + docName + "." + responseExt;
                String keyFile = keyDir + "/" + docName + "." + keyExt;
                ExternalDocument doc = new ExternalDocument("sgml", sourceFile);
                doc.setAllTags(true);
                doc.open();
                AceDocument aceDocument = Ace.processDocument(doc, "doc", sourceFile, "docs");
                AceDocument keyDoc = new AceDocument(sourceFile, keyFile);
                findRelations(sourceFile, doc, aceDocument, keyDoc);
                PrintWriter pw = new PrintWriter(new FileWriter(responseFile));
                aceDocument.write(pw, doc);
                pw.close();
                // sellScorer.scoreDocument(fileName, outputFiles[i], responceFile);
            }

        }
    }

    static void loadModel (String fileName) throws IOException {
        loadModelForExactMatch (fileName);
        //pathRelationExtractor.loadNeg(modelFile + ".neg");
        PathRelationExtractor.loadModelForSoftMatch (fileName);
    }

    static void loadModelForExactMatch (String fileName) throws IOException {
        pathRelationExtractor = new PathRelationExtractor();
        model = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader (new FileReader (fileName));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("=");
            model.put(fields[0].trim(), fields[1].trim());
        }
        System.out.println("Loaded " + model.size() + " rules");
    }

    static String predict (String candidate) {
        System.out.println("candidate = " + candidate);
        return model.get(candidate);
    }
}
