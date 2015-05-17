package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import javax.swing.*;
import java.awt.event.*;

/**
 *  part-of-speech filter for high-frequency words;
 *  distinguishes nouns, names, verbs, and other
 */

public class TermFilter extends ListFilter {

	boolean showNouns;
	boolean showNames;
	boolean showVerbs;
	boolean showOther;

	/**
	 *  return true if 'term' is a selected part of speech
	 */

	public boolean filter (String term) {
        return (showNouns && termIsType(term, "nn")) ||
                (showNames && termIsType(term, "nnp")) ||
                (showVerbs && termIsType(term, "vb")) ||
                (showOther && termIsType(term, "o"));
	}

    private boolean termIsType(String term, String type) {
        String[] parts = term.split("/");
        if (parts.length < 2) return false;
        return parts[1].equals(type);
    }
	
	/**
	 * draw a Box including check boxes for the different parts of speech
	 */

        public Box makeBox () {
                Box box = Box.createHorizontalBox();
                box.add(new JLabel("show"));
                JCheckBox nounButton = new JCheckBox("nouns");
		nounButton.setSelected(showNouns);
                box.add(nounButton);
                JCheckBox nameButton = new JCheckBox("names");
		nameButton.setSelected(showNames);
                box.add(nameButton);
                JCheckBox verbButton = new JCheckBox("verbs");
		verbButton.setSelected(showVerbs);
                box.add(verbButton);
                JCheckBox otherButton = new JCheckBox("other");
		otherButton.setSelected(showOther);
                box.add(otherButton);

		// -------- listeners
                nounButton.addItemListener (new ItemListener() {
                        public void itemStateChanged (ItemEvent ev) {
                                showNouns = ev.getStateChange() == ItemEvent.SELECTED;
                        }
                });
                nameButton.addItemListener (new ItemListener() {
                        public void itemStateChanged (ItemEvent ev) {
                                showNames = ev.getStateChange() == ItemEvent.SELECTED;
                        }
                });
                verbButton.addItemListener (new ItemListener() {
                        public void itemStateChanged (ItemEvent ev) {
                                showVerbs = ev.getStateChange() == ItemEvent.SELECTED;
                        }
                });
                otherButton.addItemListener (new ItemListener() {
                        public void itemStateChanged (ItemEvent ev) {
                                showOther = ev.getStateChange() == ItemEvent.SELECTED;
                        }
                });

                return box;
        }

}
