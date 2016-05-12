package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.relation.Bootstrap;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * provides the GUI for building a new relation
 */

public class RelationBuilder {

    // properties
    String name;
    String seed;
    public String pathListFileName;

    // property methods
    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String s) {
        seed = s;
    }

    public RelationBuilder() {
        this.name = "?";
        this.seed = "?";
    }

    JTextField nameField;
    JTextField seedField;
    JTextArea textArea;

    /**
     * box for relation set:
     * once seed is filled in (with dependency path), invoke Jet.RelationAL.Bootstrap
     * to build a set of similar patterns to form relation
     */

    Box relationSetBox() {

        Box box = Box.createVerticalBox();
        TitledBorder border = new TitledBorder("Build Relation");
        border.setTitleColor(Color.RED);
        box.setBorder(border);

        Box box_1 = Box.createHorizontalBox();
        JLabel nameLabel = new JLabel("Name");
        box_1.add(nameLabel);
        nameField = new JTextField(name);
        box_1.add(nameField);
        box.add(box_1);

        Box box_2 = Box.createHorizontalBox();
        JLabel seedLabel = new JLabel("Seed");
        box_2.add(seedLabel);
        seedField = new JTextField(seed);
        box_2.add(seedField);
        JButton runButton = new JButton("Run");
        box_2.add(runButton);
        box.add(box_2);

//        Box box_3 = Box.createHorizontalBox();
//        JTextField candidate = new JTextField(15);
//        box_3.add(candidate);
//        JButton accept = new JButton("accept");
//        box_3.add(accept);
//        JButton reject = new JButton("reject");
//        box_3.add(reject);
//        box.add(box_3);

        textArea = new JTextArea(5, 30);
        JScrollPane scrollPane = new JScrollPane(textArea);
        box.add(scrollPane);

        JButton saveButton = new JButton("Save Relation");
        box.add(saveButton);

        // listeners -----------
        runButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {

                String tempName = nameField.getText().trim();
                if (Ice.relations.containsKey(tempName)) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            tempName + " relation already exists. Choose another name.",
                            "Unable to proceed",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String tempSeed = seedField.getText();
                buildRelation(Ice.selectedCorpusName, tempName, tempSeed);
            }
        });


        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                String text = textArea.getText().trim();
                if (text.length() == 0 || nameField.getText().trim().length() == 0) return;
                String[] pathReprs = text.split("\n");

                try {
                    saveRelation(nameField.getText().trim(), Arrays.asList(pathReprs));
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            e.getMessage(),
                            "Unable to proceed",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        return box;
    }


    public void saveRelation(String name, List<String> pathReprs) throws IllegalArgumentException {
        IceRelation r = createIceRelation(pathReprs);
        Ice.relations.put(name, r);
    }

    public IceRelation createIceRelation(List<String> pathReprs) throws IllegalArgumentException{
        DepPathMap depPathMap = DepPathMap.getInstance();
        List<String> pathList = new ArrayList<String>();
        String arg1type = null;
        String arg2type = null;
        for (String pathRepr : pathReprs) {
            List<String> paths = depPathMap.findPath(pathRepr);
            for (String path : paths) {
                String[] parts = path.split(" -- ");
                path = parts[1];
                if (path != null) {
                    pathList.add(path);
                    parts = pathRepr.toUpperCase().split(" ");
                    if (arg1type == null) {
                        arg1type = parts[0];
                    }
                    if (arg2type == null) {
                        arg2type = parts[parts.length - 1];
                    }
                } else {
                    throw new IllegalArgumentException("Pattern " + pathRepr + " is invalid. Please modify.");
                }

            }
        }

        IceRelation r = new IceRelation();
        r.setName(name);
//                String[] paths = pathList.toArray(new String[pathList.size()]);
//                for (String path : paths) {
//                    pathList.add(path.trim());
//                }
        r.setPaths(pathList);
        r.setArg1type(arg1type);
        r.setArg2type(arg2type);
        return r;
    }


    public void buildRelation(String corpusName, String inName, String inSeed) {

        name = inName;

        pathListFileName = FileNameSchema.getRelationPathListFileName(corpusName);
        Bootstrap bootstrap = new Bootstrap(new SwingProgressMonitor(
                Ice.mainFrame, "Initializing Relation Builder",
                "Collecting seeds...",
                0,
                5
        ));
        RelationBuilderFrame frame = new RelationBuilderFrame("Bootstrap relations",
                RelationBuilder.this,
                bootstrap,
                null);

        frame.setSize(400, 525);
        frame.setAlwaysOnTop(true);
//                frame.setVisible(true);
//                frame.listPane.revalidate();
//                frame.listPane.repaint();
//                frame.rankedList.revalidate();
//                frame.rankedList.repaint();
//                System.out.println("plfn = " + pathListFileName);
        seed = inSeed;
//                System.out.println("plfn = " + pathListFileName);
        RelationBuilderThread builder = new RelationBuilderThread(
                seed,
                FileNameSchema.getRelationsFileName(corpusName),
                pathListFileName,
                RelationBuilder.this,
                bootstrap,
                frame,
                null);
        builder.start();
    }
}

