package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Created by yhe on 10/13/14.
 */
public class SwingEntitiesPanel extends JPanel implements Refreshable {
    private SwingIceStatusPanel statusPanel = new SwingIceStatusPanel();

    public SwingEntitiesPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        this.removeAll();
        JPanel termBox = new JPanel();
        termBox.setOpaque(true);
        termBox.setMinimumSize(new Dimension(480, 270));
        Box indexBox = Ice.selectedCorpus.entitySetBuilder.makeSwingBox();
        this.add(termBox, "cell 0 0");
        this.add(statusPanel, "cell 1 0 1 2");
        this.add(indexBox, "cell 0 1");
        refresh();
    }

    public void refresh() {

        statusPanel.refresh();
    }
}
