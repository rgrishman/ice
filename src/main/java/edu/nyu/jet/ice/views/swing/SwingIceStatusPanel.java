package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.JetEngineBuilder;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.ice.utils.IceUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * Panel that tracks the status of the current corpus in Ice
 *
 * @author yhe
 */
public class SwingIceStatusPanel extends JPanel {
    final JLabel corpusLabel = new JLabel("Corpus ");
    final JLabel corpusNameLabel = new JLabel("N/A ");
    final JLabel corpusEntitiesLabel = new JLabel("Need entities");
    final JLabel corpusRelationsLabel = new JLabel("Need paths");
    final JLabel corpusIndexedLabel = new JLabel("Need indexing");
    final JLabel backgroundLabel = new JLabel("Background ");
    final JLabel backgroundNameLabel = new JLabel("N/A ");
    final JButton refreshButton      = new JButton("Refresh");
    final JButton saveProgressButton = new JButton("Persist");
    final JButton exportToJetButton  = new JButton("Export");

    public SwingIceStatusPanel() {
        super();
        this.setMinimumSize(new Dimension(130, 366));
        this.setMaximumSize(new Dimension(130, 366));
        Border lowerEtechedBorder = BorderFactory.createEtchedBorder();
        Border titleBorder        = BorderFactory.createTitledBorder(lowerEtechedBorder, "Status");
        this.setBorder(titleBorder);
        this.setOpaque(false);
        this.setLayout(new MigLayout());
        bold(corpusLabel);
        this.add(corpusLabel, "wrap");
        this.add(corpusNameLabel, "wrap");
        this.add(corpusEntitiesLabel, "wrap");
        this.add(corpusRelationsLabel, "wrap");
        this.add(corpusIndexedLabel, "wrap");
        bold(backgroundLabel);
        this.add(backgroundLabel, "wrap");
        this.add(backgroundNameLabel, "wrap");
        this.add(refreshButton, "wrap");
        this.add(saveProgressButton, "wrap");
        this.add(exportToJetButton);

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                refresh();
            }
        });

        saveProgressButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Nice.saveStatus();
                    if (Nice.instance != null &&
                            (Nice.instance.entitiesPanel == null || Nice.instance.pathsPanel == null)) {
                        Nice.instance.reassemble();
                    }
                } catch (IOException e) {
                    System.out.println("Error writing ice.yml.");
                }
                refresh();
            }
        });

        exportToJetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JetEngineBuilder.build();
            }
        });
    }

    public static void bold(JLabel label) {
        Font font = label.getFont();
        Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
        label.setFont(boldFont);
    }

    public void refresh() {
        if (Ice.selectedCorpus != null) {
            corpusNameLabel.setText(Ice.selectedCorpusName);
            if (Ice.selectedCorpus.wordCountFileName != null) {
                corpusEntitiesLabel.setText("Words counted");
                corpusEntitiesLabel.setForeground(Color.GREEN);
            }
            else {
                corpusEntitiesLabel.setText("Count words");
                corpusEntitiesLabel.setForeground(Color.RED);
            }
            File relationFile = new File(FileNameSchema.getRelationsFileName(Ice.selectedCorpusName));
            if (Ice.selectedCorpus.relationInstanceFileName != null) {
                corpusRelationsLabel.setText("Phrases found");
                corpusRelationsLabel.setForeground(Color.GREEN);
            }
            else {
                corpusRelationsLabel.setText("Find phrases");
                corpusRelationsLabel.setForeground(Color.RED);
            }
            if (Ice.selectedCorpus.backgroundCorpus != null) {
                backgroundNameLabel.setText(Ice.selectedCorpus.backgroundCorpus);
                backgroundNameLabel.setForeground(Color.BLACK);
            }
            else {
                backgroundNameLabel.setText("Select background");
                backgroundNameLabel.setForeground(Color.RED);
            }
            String indexFileName = FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpusName, "nn");
            File indexFile = new File(indexFileName);
            if (indexFile.exists()) {
                corpusIndexedLabel.setText("Indexed");
                corpusIndexedLabel.setForeground(Color.GREEN);
            }
            else {
                corpusIndexedLabel.setText("Need indexing");
                corpusIndexedLabel.setForeground(Color.RED);
            }
        }
        else {
            corpusNameLabel.setText("Select Corpus");
            corpusNameLabel.setForeground(Color.RED);
            corpusEntitiesLabel.setText("Count words");
            corpusEntitiesLabel.setForeground(Color.RED);
            corpusRelationsLabel.setText("Find phrases");
            corpusRelationsLabel.setForeground(Color.RED);
            backgroundNameLabel.setText("Select background");
            backgroundNameLabel.setForeground(Color.RED);
            corpusIndexedLabel.setText("Need indexing");
            corpusIndexedLabel.setForeground(Color.RED);
        }
    }
}
