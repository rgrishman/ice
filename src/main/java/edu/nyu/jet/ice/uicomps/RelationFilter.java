package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import edu.nyu.jet.ice.models.Corpus;

import javax.swing.*;
import java.awt.event.*;

/**
 *  a filter for selecting only sentential patterns (those with
 *  an nsubj and dobj) or passing all patterns
 */

public class RelationFilter extends ListFilter {

	public boolean onlySententialPatterns;
	public JCheckBox sententialPatternCheckBox;
    JTextArea area = null;


	/**
	 *  return true if 'term' is a selected part of speech
	 */
	public boolean filter (String term) {
		if (onlySententialPatterns)
			return term.matches(".*nsubj-1:.*:dobj.*");
		else
			return true;
	}

    public void setArea(JTextArea area) {
        this.area = area;
    }

    /**
	 * draw a Box with the check box for selecting sentential patterns
	 */

        public Box makeBox () {
                Box box = Box.createHorizontalBox();
		sententialPatternCheckBox = new JCheckBox("show only sentential patterns");
		box.add(sententialPatternCheckBox);

		// listener -----

		sententialPatternCheckBox.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
			onlySententialPatterns = sententialPatternCheckBox.isSelected();
                try {
                    Corpus.displayTerms(Ice.selectedCorpus.getRelationTypeFileName(),
                            40,
                            area,
                            Corpus.relationFilter);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
		    }
		});

                return box;
        }
}
