package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Created by yhe on 10/13/14.
 */
public class SwingEntitiesPanel extends JPanel implements Refreshable {
    public SwingEntitiesPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        refresh();
//        Box termBox = Ice.selectedCorpus.swingTermBox();
//        Box indexBox = Ice.selectedCorpus.entitySetBuilder.makeSwingBox();
//        this.add(termBox, "cell 0 0");
//        SwingIceStatusPanel statusPanel = new SwingIceStatusPanel();
//        this.add(statusPanel, "cell 1 0 1 2");
//        this.add(indexBox, "cell 0 1");
    }

    public void refresh() {
        this.removeAll();
        Box termBox = Ice.selectedCorpus.swingTermBox();
        Box indexBox = Ice.selectedCorpus.entitySetBuilder.makeSwingBox();
        this.add(termBox, "cell 0 0");
        SwingIceStatusPanel statusPanel = new SwingIceStatusPanel();
        this.add(statusPanel, "cell 1 0 1 2");
        this.add(indexBox, "cell 0 1");
        statusPanel.refresh();
    }
}
