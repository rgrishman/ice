package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.DepPathMap;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Panel that extracts dependency paths from in the current corpus
 *
 * @author yhe
 */

public class SwingPathsPanel extends JPanel implements Refreshable {

    private SwingIceStatusPanel iceStatusPanel;
    public final JTextArea textArea = new JTextArea(16, 35);

/**
 *  Create panel.
 */

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

        allPatternsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Extracting relation phrases",
                        "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
                checkForAndFindRelations(progressMonitor, false);
            }
        });

        sententialPatternsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Extracting relation phrases",
                        "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
                checkForAndFindRelations(progressMonitor, true);
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

    public void checkForAndFindRelations(ProgressMonitorI progressMonitor,
                                         boolean sententialOnly) {
        String relationInstanceFileName = 
        FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);//name + "Relations";
        String relationTypeFileName = FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName);//name + "Relationtypes";
        File file = new File(relationTypeFileName);
        boolean relationsFileExists = file.exists() && !file.isDirectory();
        boolean shouldReuse = false;
        if (preprocessedTextsAvailable(Ice.selectedCorpusName)) {
            if (relationsFileExists) {
                int n = JOptionPane.showConfirmDialog(
                        Ice.mainFrame,
                        "Extracted patterns already exist. Show existing patterns without recomputation?",
                        "Patterns exist",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) { // reuse existing paths
                    shouldReuse = true;
                }
            }
        } else {
            if (relationsFileExists) {
                shouldReuse = true;
             } else {
                 JOptionPane.showMessageDialog(Ice.mainFrame, "Source text not available, cannot rebuild pattern set");
             }
        }
        PathExtractionThread thread = new PathExtractionThread(shouldReuse,
                sententialOnly, textArea, progressMonitor);
        thread.start();
    }

    public static boolean preprocessedTextsAvailable (String corpusName) {
        return (new File(FileNameSchema.getPreprocessCacheDir(corpusName))).exists();
    }
}

class PathExtractionThread extends Thread {

    private boolean shouldReuse;
    private ProgressMonitorI progressMonitor;
    private JTextArea textArea;
    private boolean   sententialOnly;
    private static final int SIZE_LIMIT = 100;

    public PathExtractionThread(boolean shouldReuse,
                                boolean sententialOnly,
                                JTextArea textArea,
                                ProgressMonitorI progressMonitor) {
        this.shouldReuse = shouldReuse;
        this.sententialOnly = sententialOnly;
        this.textArea    = textArea;
        this.progressMonitor = progressMonitor;
    }

    @Override
    public void run() {
        if (!shouldReuse) {
            RelationFinder finder = new RelationFinder(
                    Ice.selectedCorpus.docListFileName, Ice.selectedCorpus.directory,
                    Ice.selectedCorpus.filter, FileNameSchema.getRelationsFileName(Ice.selectedCorpusName),
                    FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName), null,
                    Ice.selectedCorpus.numberOfDocs,
                    progressMonitor);
            finder.run();
            Ice.selectedCorpus.relationTypeFileName =
                    FileNameSchema.getRelationTypesFileName(Ice.selectedCorpus.name);
            Ice.selectedCorpus.relationInstanceFileName =
                    FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);
        }
        progressMonitor.setNote("Postprocessing...");
        // rank paths
        Corpus.rankRelations(Ice.selectedCorpus.backgroundCorpus,
                FileNameSchema.getPatternRatioFileName(Ice.selectedCorpusName,
                        Ice.selectedCorpus.backgroundCorpus));
        DepPathMap depPathMap = DepPathMap.getInstance();
        depPathMap.load();
        // filter and show paths
        try {
            BufferedReader reader = new BufferedReader(new FileReader(
                    FileNameSchema.getSortedPatternRatioFileName(Ice.selectedCorpusName,
                            Ice.selectedCorpus.backgroundCorpus)
            ));
            int k = 0;
            StringBuilder b = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (sententialOnly && !line.matches(".*nsubj-1:.*:dobj.*")) {
                    continue;
                }
                String[] parts = line.split("\\t");
                if (parts.length < 2) {
                    continue;
                }
                String repr = depPathMap.findRepr(parts[1]);
                if (repr == null) {
                    continue;
                }
                b.append(parts[0].trim()).append("\t")
                        .append(repr).append("\n");
                k++;
                if (k > SIZE_LIMIT) break;
            }
            if (progressMonitor == null || !progressMonitor.isCanceled()) {
                textArea.setText(b.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        progressMonitor.setProgress(progressMonitor.getMaximum());
    }
}
