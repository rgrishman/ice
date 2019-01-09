package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.ice.events.IceTree;
import edu.nyu.jet.ice.models.IcePath;

import javax.swing.*;
import java.awt.*;

/**
 *  This cell renderer is intended to support displays of Lists
 *  where the list elements are IcePaths or IceTrees.  What is
 *  displayed is the "repr" (English phrase) in either case and
 *  the choice made by the user regarding that phrase.
 */

public class IceCellRenderer extends JLabel implements ListCellRenderer {

    public IceCellRenderer() {
	setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list,
	    Object value,
	    int index,
	    boolean isSelected,
	    boolean cellHasFocus) {

    if (value == null) {
        System.out.println ("CellRenderer got null");
    } else if (value instanceof IceTree) {
        IceTree t = (IceTree) value;
        String repr = t.getRepr();
        if (repr == null) System.out.println ("CellRenderer getting trees with null repr");
        IceTree.IceTreeChoice choice = t.getChoice();
        if (choice  == IceTree.IceTreeChoice.YES)
            repr += " / YES";
        else if (choice == IceTree.IceTreeChoice.NO)
            repr += " / NO";
        setText(repr);
    } else if (value instanceof IcePath) {
        IcePath t = (IcePath) value;
        String repr = t.getRepr();
        if (repr == null) System.out.println ("CellRenderer getting paths with null repr");
        IcePath.IcePathChoice choice = t.getChoice();
        if (choice  == IcePath.IcePathChoice.YES)
            repr += " / YES";
        else if (choice == IcePath.IcePathChoice.NO)
            repr += " / NO";
        setText(repr);
    } else System.out.println ("Cell renderer got " + value);

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


