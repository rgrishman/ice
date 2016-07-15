package edu.nyu.jet.ice.relation;

import edu.nyu.jet.aceJet.AceDocument;
import edu.nyu.jet.aceJet.AceEntityMention;
import edu.nyu.jet.aceJet.AceRelationMention;
import edu.nyu.jet.aceJet.EventSyntacticPattern;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import opennlp.model.Event;

import java.util.List;

/**
 * A Feature extractor using dependency path and entity types as features for supervised/simulated active learning
 * relation extraction.
 *
 * This class is not used by ICE GUI/CLI.
 *
 * @author yhe
 */
public class DepPathTypeFeatureExtractor implements RelationFeatureExtractor {
    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc) {
        String label = r == null || r.relation.type.toLowerCase().equals("null") ? "NONE" : r.relation.type;
        int h1 = m1.getJetHead().start();
        int h2 = m2.getJetHead().start();
        if (h1 >= h2) {
            int tmp = h1;
            h1 = h2;
            h2 = tmp;
        }
        String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, paths);
        path = path == null ? "EMPTY" : path.replaceAll("\\s+", "_");
        String type1 = m1.entity.type;
        String type2 = m2.entity.type;
        return new Event(label, new String[]{path, type1, type2});
    }
}
