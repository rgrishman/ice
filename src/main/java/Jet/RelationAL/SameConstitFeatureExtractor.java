package Jet.RelationAL;

import AceJet.AceDocument;
import AceJet.AceEntityMention;
import AceJet.AceRelationMention;
import AceJet.EventSyntacticPattern;
import Jet.Parser.SyntacticRelationSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import opennlp.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yhe on 9/1/14.
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
