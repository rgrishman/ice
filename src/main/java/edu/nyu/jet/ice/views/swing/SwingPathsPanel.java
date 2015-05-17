package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Created by yhe on 10/13/14.
 */
public class SwingPathsPanel extends JPanel implements Refreshable {

    private SwingIceStatusPanel iceStatusPanel;

    public SwingPathsPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        fullRefresh();
    }

    public void refresh() {
        fullRefresh();
//        this.removeAll();
//        Box patternBox = Ice.selectedCorpus.swingPatternBox();
//        patternBox.setOpaque(false);
//        patternBox.setMinimumSize(new Dimension(480, 366));
//        this.add(patternBox);
//        SwingIceStatusPanel iceStatusPanel = new SwingIceStatusPanel();
//        this.add(iceStatusPanel);
//        iceStatusPanel.refresh();
    }

    public void fullRefresh() {
        this.removeAll();
        Box patternBox = Ice.selectedCorpus.swingPatternBox();
        patternBox.setOpaque(false);
        patternBox.setMinimumSize(new Dimension(480, 366));
        this.add(patternBox);
        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);
        iceStatusPanel.refresh();
    }

}
