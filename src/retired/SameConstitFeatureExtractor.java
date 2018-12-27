package edu.nyu.jet.ice.relation;

import edu.nyu.jet.aceJet.AceDocument;
import edu.nyu.jet.aceJet.AceEntityMention;
import edu.nyu.jet.aceJet.AceRelationMention;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import opennlp.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A Feature extractor that checks if both arguments belong to the same syntactic constituent for supervised/simulated
 * active learning relation extraction.
 *
 * This class is not used by ICE GUI/CLI.
 *
 * @author yhe
 */
public class SameConstitFeatureExtractor implements RelationFeatureExtractor {
    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc) {
        String label = r == null ? "NONE" : r.relation.type;
        int h1Start = m1.getJetHead().start();
        int h2End = m2.getJetHead().end();
        if (h1Start >= h2End) {
            h1Start = m2.getJetHead().start();
            h2End   = m1.getJetHead().end();
        }
        List<String> sameConstits = extractSameConstits(h1Start, h2End, doc);
        return new Event(label,
                sameConstits.toArray(new String[sameConstits.size()]));
    }

    public static List<String> extractSameConstits(int start, int end, Document doc) {
        List<String> result = new ArrayList<String>();
        List<Annotation> constits = doc.annotationsOfType("constit");
        if (constits != null) {
            for (Annotation constit : constits) {
                if (constit.start() < start && constit.end() > end) {
                    String cat  = ((String)constit.get("cat")).toUpperCase();
                    if (cat.equals("NP")) {
                        String feat = "SameConstit=" + cat.toUpperCase();
                        if (!result.contains(feat)) {
                            result.add(feat);
                        }
                    }
                }
            }
        }
//        if (result.size() == 0) {
//            result.add("SameConstit=NONE");
//        }
        return result;
    }
}
