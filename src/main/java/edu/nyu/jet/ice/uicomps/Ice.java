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

/**
 *  Top-level objects for ICE
 */

public class Ice {

    public static SortedMap<String, Corpus> corpora = new TreeMap<String, Corpus> ();
    public static SortedMap<String, IceEntitySet> entitySets = new TreeMap<String, IceEntitySet>();
    public static SortedMap<String, IceRelation> relations = new TreeMap<String, IceRelation>();
    public static Corpus selectedCorpus = null;
    public static String selectedCorpusName = null;

    public static Properties iceProperties = new Properties();

    public static JFrame mainFrame;

    public static void selectCorpus (String corpus) {
        selectedCorpusName = corpus;
        selectedCorpus = corpora.get(selectedCorpusName);
    }

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
	relations.remove(type);
    }
}
