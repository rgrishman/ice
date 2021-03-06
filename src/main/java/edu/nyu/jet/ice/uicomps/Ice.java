package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.events.IceEvent;
import edu.nyu.jet.ice.events.DepTreeMap;
import edu.nyu.jet.concepts.ConceptHierarchy;

import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;

/**
 *  Top-level objects for ICE
 */

public class Ice {

    static final Logger logger = LoggerFactory.getLogger(Ice.class);

    public static SortedMap<String, Corpus> corpora = new TreeMap<String, Corpus> ();
    public static SortedMap<String, IceEntitySet> entitySets = new TreeMap<String, IceEntitySet>();
    public static SortedMap<String, IceRelation> relations = new TreeMap<String, IceRelation>();
    public static SortedMap<String, IceEvent> events = new TreeMap<String, IceEvent>();
    public static Corpus selectedCorpus = null;
    public static String selectedCorpusName = null;

    public static Properties iceProperties = new Properties();

    public static JFrame mainFrame;

    public static void selectCorpus (String corpus) {
        selectedCorpusName = corpus;
        selectedCorpus = corpora.get(selectedCorpusName);
        DepPathMap depPathMap = DepPathMap.getInstance();
        depPathMap.loadPaths(false);
        DepTreeMap depTreeMap = DepTreeMap.getInstance();
        depTreeMap.loadTrees(false);
    }

    public static ConceptHierarchy ontology = null;;

    public static void addEntitySet (IceEntitySet entitySet) {
	entitySets.put(entitySet.getType(), entitySet);
    }

    public static IceEntitySet getEntitySet (String type) {
	return entitySets.get(type);
    }

    public static void removeEntitySet (String type) {
	entitySets.remove(type);
    }

    public static void addRelation (IceRelation relation) {
	relations.put(relation.getName(), relation);
    }

    public static IceRelation getRelation (String type) {
	return relations.get(type);
    }

    public static void removeRelation (String type) {
        if (relations.get(type) == null)
            logger.warn("Relation to be deleted does not exist.");
        else relations.remove(type);
    }

    public static void addEvent (IceEvent event) {
	events.put(event.getName(), event);
    }

    public static IceEvent getEvent (String type) {
	return events.get(type);
    }
    
    public static void removeEvent (String type) {
	events.remove(type);
    }
    }

