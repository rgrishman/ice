package edu.nyu.jet.ice.uicomps;

import javax.swing.Box;

/**
 *  a filter for displaying or skipping items, controlled by a GUI
 */

public abstract class ListFilter {

	public abstract boolean filter(String item);

	public abstract Box makeBox();

}
