package edu.nyu.jet.ice.relation;

import AceJet.AceDocument;
import AceJet.AceEntityMention;
import AceJet.AceRelationMention;
import Jet.Parser.SyntacticRelationSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import opennlp.model.Event;

import java.util.List;

/**
 * Interface for a feature extractor for relation classifiers.
 *
 * Used for supervised/simulated active learning; not used by ICE GUI/CLI.
 */
public interface RelationFeatureExtractor {
    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc);
}
