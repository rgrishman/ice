package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.ice.events.IceTree;

import javax.swing.*;
import java.awt.*;

public class IceCellRenderer extends JLabel implements ListCellRenderer {;

    public IceCellRenderer() {
	setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list,
	    Object value,
	    int index,
	    boolean isSelected,
	    boolean cellHasFocus) {

	IceTree t = (IceTree) value;
	String repr = t.getRepr();
	IceTree.IceTreeChoice choice = t.getChoice();
	if (choice  == IceTree.IceTreeChoice.YES)
	    repr += " / YES";
	else if (choice == IceTree.IceTreeChoice.NO)
	    repr += " / NO";
	setText(repr);

	Color background;
	Color foreground;

	// check if this cell is selected
	if (isSelected) {
	    background = Color.BLUE;
	    foreground = Color.WHITE;
	} else {
	    background = Color.WHITE;
	    foreground = Color.BLACK;
	};

	setBackground(background);
	setForeground(foreground);

	// this.setToolTipText("hi" + t.getExample());
	return this;
    }
}


