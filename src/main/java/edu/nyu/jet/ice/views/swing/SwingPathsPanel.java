package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.RelationFinder;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.RelationFilter;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by yhe on 10/13/14.
 */
public class SwingPathsPanel extends JPanel implements Refreshable {

    private SwingIceStatusPanel iceStatusPanel;
    public final JTextArea textArea = new JTextArea(16, 35);

    public SwingPathsPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        this.removeAll();
        JPanel patternBox = new JPanel(new MigLayout());
        patternBox.setOpaque(false);
        patternBox.setMinimumSize(new Dimension(480, 366));
        TitledBorder border = new TitledBorder("Phrases");
        patternBox.setBorder(border);
        this.add(patternBox);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setEditable(false);
        patternBox.add(scrollPane, "span");
        JButton allPatternsButton = new JButton("All phrases");
        JButton sententialPatternsButton = new JButton("Sentential phrases");

        patternBox.add(allPatternsButton);
        patternBox.add(sententialPatternsButton);

        //Box patternBox = Ice.selectedCorpus.swingPatternBox();
        //patternBox.setOpaque(false);
        //patternBox.setMinimumSize(new Dimension(480, 366));
        //this.add(patternBox);

        allPatternsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Extracting relation phrases",
                        "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
                checkForAndFindRelations(progressMonitor);
            }
        });

        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);
        fullRefresh();
    }

    public void refresh() {
        fullRefresh();
    }

    public void fullRefresh() {

        iceStatusPanel.refresh();
    }

    public void checkForAndFindRelations(ProgressMonitorI progressMonitor) {
        String relationInstanceFileName = FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);//name + "Relations";
        String relationTypeFileName = FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName);//name + "Relationtypes";
        File file = new File(relationTypeFileName);
        boolean shouldReuse = false;
        if (file.exists() &&
                !file.isDirectory()) {
            int n = JOptionPane.showConfirmDialog(
                    Ice.mainFrame,
                    "Extracted patterns already exist. Show existing patterns without recomputation?",
                    "Patterns exist",
                    JOptionPane.YES_NO_OPTION);
            if (n == 0) { // reuse existing paths
                shouldReuse = true;
//                Corpus.displayTerms(relationTypeFileName,
//                        40,
//                        relationTextArea,
//                        relationFilter);
//                return;
            }
        }
        if (!shouldReuse) {
            RelationFinder finder = new RelationFinder(
                    Ice.selectedCorpus.docListFileName, Ice.selectedCorpus.directory,
                    Ice.selectedCorpus.filter, FileNameSchema.getRelationsFileName(Ice.selectedCorpusName),
                    FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName), null,
                    Ice.selectedCorpus.numberOfDocs,
                    progressMonitor);
            finder.start();
            Ice.selectedCorpus.relationTypeFileName =
                    FileNameSchema.getRelationTypesFileName(Ice.selectedCorpus.name);
            Ice.selectedCorpus.relationInstanceFileName =
                    FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);
        }

        // rank paths
        Corpus.rankRelations(Ice.selectedCorpus.backgroundCorpus,
                FileNameSchema.getPatternRatioFileName(Ice.selectedCorpusName,
                        Ice.selectedCorpus.backgroundCorpus));
        // filter and show paths


        // findRelations(progressMonitor, "", relationTextArea);
    }

}
