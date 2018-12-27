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
 * A Feature extractor using token sequence as features for supervised/simulated active learning
 * relation extraction.
 *
 * This class is not used by ICE GUI/CLI.
 *
 * @author yhe
 */
public class TokenFeatureExtractor implements RelationFeatureExtractor {
    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc) {
        String label = r == null ? "NONE" : r.relation.type;
        int h1Start = m1.getJetHead().end();
        int h2End = m2.getJetHead().start();
        if (h1Start >= h2End) {
            h1Start = m2.getJetHead().end();
            h2End   = m1.getJetHead().start();
        }
        List<String> sameTokens = extractSameTokens(h1Start, h2End, doc);
        return new Event(label,
                sameTokens.toArray(new String[sameTokens.size()]));
    }

    public static List<String> extractSameTokens(int start, int end, Document doc) {
        List<String> result = new ArrayList<String>();
        List<Annotation> tokens = doc.annotationsOfType("token");
        if (tokens != null) {
            StringBuilder b = new StringBuilder();
            for (Annotation token : tokens) {
                if (token.start() > start && token.end() < end) {
                    String word  = doc.text(token).toLowerCase().trim().replaceAll("\\s+", "_");
                    b.append(word + ":");
                }
            }
            if (b.length() > 0) {
                String feat = "Tokens=" + b.toString().substring(0, b.length() - 1);
                if (!result.contains(feat)) {
                    result.add(feat);
                }
            }
        }
        if (result.size() == 0) {
            result.add("Tokens=NONE");
        }
        return result;
    }
}
