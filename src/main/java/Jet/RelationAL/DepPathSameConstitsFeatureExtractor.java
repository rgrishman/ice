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
public class DepPathSameConstitsFeatureExtractor implements RelationFeatureExtractor {
    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc) {
        String label = r == null ? "NONE" : r.relation.type;
        int h1 = m1.getJetHead().start();
        int h2 = m2.getJetHead().start();
        int h1Start = m1.getJetHead().start();
        int h2End = m2.getJetHead().end();
        if (h1 >= h2) {
            int tmp = h1;
            h1 = h2;
            h2 = tmp;
            h1Start = m2.getJetHead().start();
            h2End   = m1.getJetHead().end();
        }
        String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, paths);
        path = path == null ? "EMPTY" : path.replaceAll("\\s+", "_");
        String type1 = m1.entity.type;
        String type2 = m2.entity.type;
        String concatTypes = type1 + ":::" + type2;
        String concatAll = type1 + ":::" + path + ":::" + type2;
        List<String> feats = SameConstitFeatureExtractor.extractSameConstits(h1Start, h2End, doc);
        int pathLength = path.split(":").length;
        //System.err.println("Path length = " + pathLength);
        List<String> updatedFeats = new ArrayList<String>();
        for (String feat : feats) {
            updatedFeats.add("CONJ_SAME_LENGTH=" + feat + ":::" + pathLength);
        }
        updatedFeats.add("PATH_LENGTH=" + pathLength);
        updatedFeats.add("PATH_WITH_TYPE=" + concatAll);
        return new Event(label, updatedFeats.toArray(new String[updatedFeats.size()]));
    }
}
