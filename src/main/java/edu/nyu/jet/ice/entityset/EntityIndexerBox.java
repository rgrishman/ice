package edu.nyu.jet.ice.entityset;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.entityset.*;
import edu.nyu.jet.ice.uicomps.Ice;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 *  Button for initiating entity indexing and selecting cutoff.
 */

public class EntityIndexerBox  {

    public static JFrame currentFrame = Ice.mainFrame;

    public void buildIndex(double cutoff, String inType) {

        Corpus selectedCorpus = Ice.selectedCorpus;
        EntitySetIndexerThread indexer = new EntitySetIndexerThread(
                FileNameSchema.getTermsFileName(selectedCorpus.getName()),
                inType,
                String.valueOf(cutoff),
                "onomaprops",
                selectedCorpus.getDocListFileName(),
                selectedCorpus.getDirectory(),
                selectedCorpus.getFilter(),
                FileNameSchema.getEntitySetIndexFileName(selectedCorpus.getName(), inType)
        );

        indexer.start();
    }

    public Box makeSwingBox() {
        Box box = Box.createVerticalBox();
        box.setOpaque(false);
        TitledBorder border = new TitledBorder("Index Entities");
        box.setMinimumSize(new Dimension(480, 32));
        box.setBorder(border);
        JPanel indexBox = new JPanel();
        indexBox.setOpaque(false);
        indexBox.setLayout(new MigLayout());

        JLabel cutoffLabel = new JLabel("Cutoff");
        final JTextField cutoffField = new JTextField();
        cutoffField.setColumns(5);
        cutoffField.setText("3");
        indexBox.add(cutoffLabel);
        indexBox.add(cutoffField);

        JButton buildIndexButton = new JButton("Index");
        indexBox.add(buildIndexButton);

        box.add(indexBox);

        buildIndexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                double cutoff = 0.0;
                try {
                    cutoff = Double.valueOf(cutoffField.getText());
                    if (cutoff < 0.0) {
                        throw new Exception("Wrong cutoff value");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Cutoff is a number larger than 0.0",
                            "Cutoff value error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }

                int docCount = Ice.selectedCorpus.getNumberOfDocs();
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Building index...",
                        "Initializing Jet", 0, docCount + 5);
                EntitySetIndexer.setDefaultProgressMonitor(progressMonitor);

                File termFile = new File(FileNameSchema.getTermsFileName(Ice.selectedCorpusName));
                if (!termFile.exists()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entities file does not exist. Please run find entities first.",
                            "Find entities first",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                buildIndex(cutoff, "nn");
            }
        });
        return box;
    }

}

class EntitySetIndexerThread extends Thread {
        String[] args;

        EntitySetIndexerThread (String countFile, String type, String cutoff,
                String propsFile, String docList, String inputDir,
                String inputSuffix, String outputFile) {
            args = new String[8];
            args[0] = countFile;
            args[1] = type;
            args[2] = cutoff;
            args[3] = propsFile;
            args[4] = docList;
            args[5] = inputDir;
            args[6] = inputSuffix;
            args[7] = outputFile;
        }

        public void run() {
            try {
                Thread.sleep(1000);
                EntitySetIndexer.main(args);
            } catch (Exception e) {
                System.err.println ("Exception in EntitySetIndexer:\n");
                e.printStackTrace();
            }
        }
}

