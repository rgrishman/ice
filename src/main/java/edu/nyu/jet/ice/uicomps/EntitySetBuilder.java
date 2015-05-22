package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
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
 * this is a placeholder for the code to build an entity set.
 */

public class EntitySetBuilder {

    public static String type;

    public static ButtonGroup categoryGroup;

    private final Set<String> allowedEntity = new HashSet<String>();
    {
        allowedEntity.add("nn");
        allowedEntity.add("nnp");
    }
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

    public List<String> suggestSeeds() {
        return  EntitySetExpander.recommendSeeds(FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpus.name, type),
                Ice.selectedCorpus.termFileName, type);
    }

    private List<String> splitSeedString(String seedString) {
        List<String> seedStrings = new ArrayList<String>();
        for (String s : seedString.split(",")) {
            if (s.trim().length() > 1) {
                seedStrings.add(s.trim());
            }
        }
        return seedStrings;
    }

    public List<Entity> rankEntities(String seedString) {

        Corpus selectedCorpus = Ice.selectedCorpus;

        List<String> seedStrings = splitSeedString(seedString);

        EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + type,
                seedStrings);

        expander.rank();
        return expander.rankedEntities;
    }

    public IceEntitySet saveRankedEntities(String entitySetName, String entityType, List<String> elements) {

        String type = entitySetName;

        IceEntitySet es = new IceEntitySet(type);
        Ice.entitySets.put(type, es);
        if (entityType.equals("nn"))
            es.setNouns(elements);
        else
            es.setNames(elements);

        return es;
    }


    public Box makeSwingBox() {
        Box box = Box.createVerticalBox();
        box.setOpaque(false);
        TitledBorder border = new TitledBorder("Index Entities");
        box.setMinimumSize(new Dimension(480, 32));
        // border.setTitleColor(Color.RED);
        box.setBorder(border);
        //box.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel indexBox = new JPanel();//Box.createVerticalBox();
        indexBox.setOpaque(false);
        indexBox.setLayout(new MigLayout());
        Box nameBox = Box.createHorizontalBox();
        JLabel nameLabel = new JLabel("Name");
        final JTextField nameField = new JTextField();
        nameBox.add(nameLabel);
        nameBox.add(nameField);
        //indexBox.add(nameBox);
        Box catBox = Box.createHorizontalBox();
        JLabel catLabel = new JLabel("Category");
        final JTextField catTextField = new JTextField();
        catBox.add(catLabel);
        //catBox.add(catTextField);

        //Box radioBox = Box.createHorizontalBox();
        JRadioButton nounButton = new JRadioButton("nouns");
        nounButton.setActionCommand("nn");
        JRadioButton nameButton = new JRadioButton("names");
        nameButton.setActionCommand("nnp");

        categoryGroup = new ButtonGroup();
        categoryGroup.add(nounButton);
        categoryGroup.add(nameButton);
        //radioBox.add(nounButton);
        //radioBox.add(nameButton);
        // nounButton.setSelected(true);
        catBox.add(nounButton);
        // catBox.add(nameButton);


        indexBox.add(catBox);

        //JPanel cutoffBox = new JPanel();
        //cutoffBox.setLayout(new MigLayout());
        JLabel cutoffLabel = new JLabel("Cutoff");
        final JTextField cutoffField = new JTextField();
        cutoffField.setColumns(5);
        cutoffField.setText("3");
        //cutoffBox.add(cutoffLabel);
        //cutoffBox.add(cutoffField);
        //indexBox.add(cutoffBox);
        indexBox.add(cutoffLabel);
        indexBox.add(cutoffField);

        JButton buildIndexButton = new JButton("Index");
        indexBox.add(buildIndexButton);

        JButton eraseUserEntityButton = new JButton("Erase User Entity Sets");
        // indexBox.add(eraseUserEntityButton);


        Box runBox = Box.createVerticalBox();
        // runBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        runBox.add(nameBox);
        JLabel seedsLabel = new JLabel("Seeds (separated by comma)");
        // seedsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Box seedBox = Box.createHorizontalBox();
        final JTextField seedsArea = new JTextField("");
        JButton suggestSeedsButton = new JButton("Suggest seeds");
        seedBox.add(seedsArea);
        seedBox.add(suggestSeedsButton);
        //seedsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton runButton = new JButton("Run");
        JButton rankButton = new JButton("Rank");
        runBox.add(seedsLabel);
        runBox.add(seedBox);
        runBox.add(rankButton);
        //runBox.add(runButton);

        box.add(indexBox);
        // box.add(runBox);

        // listeners ...
        CategoryRadioGroupActionListener categoryListener = new CategoryRadioGroupActionListener();
        nounButton.addActionListener(categoryListener);
        nameButton.addActionListener(categoryListener);

        buildIndexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                double cutoff = 0.0;
                //type   = "";
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


                Corpus selectedCorpus = Ice.selectedCorpus;
                int docCount = selectedCorpus.getNumberOfDocs();
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Building index...",
                        "Initializing Jet", 0, docCount + 5);
                EntitySetIndexer.setDefaultProgressMonitor(progressMonitor);


                try {
                    //type = catTextField.getText();
                    if (!allowedEntity.contains(type.trim().toLowerCase())) {
                        throw new Exception("Only nn and nnp are allowed for set expansion.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Please select nouns or names for set expansion.",
                            "Unsupported entity type",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }
                File termFile = new File(FileNameSchema.getTermsFileName(Ice.selectedCorpusName));
                if (!termFile.exists()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entities file does not exist. Please run find entities first.",
                            "Find entities first",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                buildIndex(cutoff, type);
            }
        });

        eraseUserEntityButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int reply = JOptionPane.showConfirmDialog(Ice.mainFrame,
                        "Are you sure to erase all entity sets created by the user? They cannot be recovered.",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.NO_OPTION) {
                    return;
                }
                Properties props = new Properties();
                try {
                    props.load(new FileReader("parseprops"));
                    String current = props.getProperty("Jet.dataPath") + "/" + props.getProperty("Ace.EDTtype.fileName");
                    String backup = current + ".backup";
                    IceUtils.copyFile(new File(backup), new File(current));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Corpus selectedCorpus = Ice.selectedCorpus;

                java.util.List<String> seedStrings = splitSeedString(seedsArea.getText());
                for (String s : seedsArea.getText().split(",")) {
                    if (s.trim().length() > 1) {
                        seedStrings.add(s.trim());
                    }
                }
                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + catTextField.getText(),
                        seedStrings);
                expander.setProgressMonitor(new SwingProgressMonitor(
                        Ice.mainFrame,
                        "Expanding entity set",
                        "Processing features...",
                        0,
                        expander.getEntityFeatureDictSize()
                ));
                EntitySetBuilderFrame entitySetWindow = new EntitySetBuilderFrame("Entity set expansion",
                        expander);
                currentFrame = entitySetWindow;
                EntitySetRecommendThread recommendThread = new EntitySetRecommendThread(expander, entitySetWindow, true);
                recommendThread.start();
            }
        });

        rankButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Corpus selectedCorpus = Ice.selectedCorpus;
                try {
                    //type = catTextField.getText();
                    if (!allowedEntity.contains(type.trim().toLowerCase())) {
                        throw new Exception("Only nn and nnp are allowed for set expansion.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Please select nouns or names for set expansion.",
                            "Unsupported entity type",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }
                String name = nameField.getText();
                try {
                    //type = catTextField.getText();
                    if (name == null || name.trim().length() == 0 || Ice.relations.containsKey(name)) {
                        throw new Exception("Name not allowed.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Name of entity set is empty or conflicts with existing name. Please try another name.",
                            "Unsupported entity name",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }

                java.util.List<String> seedStrings = new ArrayList<String>();
                for (String s : seedsArea.getText().split(",")) {
                    if (s.trim().length() > 1) {
                        seedStrings.add(s.trim());
                    }
                }
                //File entitySetIndexFile = new File(selectedCorpus.name + "EntitySetIndex_" + type);
                String entitySetIndexFileName = FileNameSchema.getEntitySetIndexFileName(selectedCorpus.getName(), type);
                File entitySetIndexFile = new File(entitySetIndexFileName);
                if (!entitySetIndexFile.exists() || entitySetIndexFile.isDirectory()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entity set index file not found. Please build index first.",
                            "Entity set index not found",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
//                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + type,
//                        seedStrings);
//                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + type,
//                        seedStrings);
                EntitySetExpander expander = new EntitySetExpander(entitySetIndexFileName,
                        seedStrings);
                expander.setProgressMonitor(new SwingProgressMonitor(
                        Ice.mainFrame,
                        "Expanding entity set",
                        "Ranking Entities...",
                        0,
                        expander.getEntityFeatureDictSize()
                ));
                EntitySetRankerFrame entitySetWindow = new EntitySetRankerFrame("Expand entity set", name, expander);
                currentFrame = entitySetWindow;
                EntitySetRankThread rankerThread = new EntitySetRankThread(expander, entitySetWindow, true);
                rankerThread.start();
            }
        });


        suggestSeedsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (type == null || Ice.selectedCorpus.termFileName == null) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Cannot suggest seeds. Please extract term and set type first.",
                            "Error suggesting seeds",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                List<String> recommended = suggestSeeds();
                String result = "";
                for (String s : recommended) {
                    if (!result.equals("")) {
                        result = result + ", ";
                    }
                    result = result + s;
                }
                seedsArea.setText(result);
            }
        });

        nounButton.setSelected(true);
        type = "nn";

        return box;
    }


    public Box makeBox() {

        Box box = Box.createVerticalBox();
        TitledBorder border = new TitledBorder("Build Entity Set");
        border.setTitleColor(Color.RED);
        box.setBorder(border);
        //box.setAlignmentX(Component.LEFT_ALIGNMENT);
        Box indexBox = Box.createVerticalBox();
        Box nameBox = Box.createHorizontalBox();
        JLabel nameLabel = new JLabel("Name");
        final JTextField nameField = new JTextField();
        nameBox.add(nameLabel);
        nameBox.add(nameField);
        //indexBox.add(nameBox);
        Box catBox = Box.createHorizontalBox();
        JLabel catLabel = new JLabel("Category");
        final JTextField catTextField = new JTextField();
        catBox.add(catLabel);
        //catBox.add(catTextField);

        //Box radioBox = Box.createHorizontalBox();
        JRadioButton nounButton = new JRadioButton("nouns");
        nounButton.setActionCommand("nn");
        JRadioButton nameButton = new JRadioButton("names");
        nameButton.setActionCommand("nnp");

        categoryGroup = new ButtonGroup();
        categoryGroup.add(nounButton);
        categoryGroup.add(nameButton);
        //radioBox.add(nounButton);
        //radioBox.add(nameButton);
       // nounButton.setSelected(true);
        catBox.add(nounButton);
        catBox.add(nameButton);

        indexBox.add(catBox);

        Box cutoffBox = Box.createVerticalBox();
        JLabel cutoffLabel = new JLabel("Cutoff");
        final JTextField cutoffField = new JTextField();
        cutoffBox.add(cutoffLabel);
        cutoffBox.add(cutoffField);
        indexBox.add(cutoffBox);

        JButton buildIndexButton = new JButton("Build Index");
        indexBox.add(buildIndexButton);

        JButton eraseUserEntityButton = new JButton("Erase User Entity Sets");
        // indexBox.add(eraseUserEntityButton);


        Box runBox = Box.createVerticalBox();
        // runBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        runBox.add(nameBox);
        JLabel seedsLabel = new JLabel("Seeds (separated by comma)");
        // seedsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Box seedBox = Box.createHorizontalBox();
        final JTextField seedsArea = new JTextField("");
        JButton suggestSeedsButton = new JButton("Suggest seeds");
        seedBox.add(seedsArea);
        seedBox.add(suggestSeedsButton);
        //seedsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton runButton = new JButton("Run");
        JButton rankButton = new JButton("Rank");
        runBox.add(seedsLabel);
        runBox.add(seedBox);
        runBox.add(rankButton);
        //runBox.add(runButton);

        box.add(indexBox);
        box.add(runBox);

        // listeners ...
        CategoryRadioGroupActionListener categoryListener = new CategoryRadioGroupActionListener();
        nounButton.addActionListener(categoryListener);
        nameButton.addActionListener(categoryListener);

        buildIndexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                double cutoff = 0.0;
                //type   = "";
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


                Corpus selectedCorpus = Ice.selectedCorpus;
                int docCount = selectedCorpus.getNumberOfDocs();
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(Ice.mainFrame, "Building index...",
                        "Initializing Jet", 0, docCount + 5);
                EntitySetIndexer.setDefaultProgressMonitor(progressMonitor);


                try {
                    //type = catTextField.getText();
                    if (!allowedEntity.contains(type.trim().toLowerCase())) {
                        throw new Exception("Only nn and nnp are allowed for set expansion.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Please select nouns or names for set expansion.",
                            "Unsupported entity type",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }

                buildIndex(cutoff, type);
            }
        });

        eraseUserEntityButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int reply = JOptionPane.showConfirmDialog(Ice.mainFrame,
                        "Are you sure to erase all entity sets created by the user? They cannot be recovered.",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.NO_OPTION) {
                    return;
                }
                Properties props = new Properties();
                try {
                    props.load(new FileReader("parseprops"));
                    String current = props.getProperty("Jet.dataPath") + "/" + props.getProperty("Ace.EDTtype.fileName");
                    String backup = current + ".backup";
                    IceUtils.copyFile(new File(backup), new File(current));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Corpus selectedCorpus = Ice.selectedCorpus;

                java.util.List<String> seedStrings = splitSeedString(seedsArea.getText());
                for (String s : seedsArea.getText().split(",")) {
                    if (s.trim().length() > 1) {
                        seedStrings.add(s.trim());
                    }
                }
                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + catTextField.getText(),
                        seedStrings);
                expander.setProgressMonitor(new SwingProgressMonitor(
						Ice.mainFrame,
						"Expanding entity set",
						"Processing features...",
						0,
						expander.getEntityFeatureDictSize()
				));
                EntitySetBuilderFrame entitySetWindow = new EntitySetBuilderFrame("Entity set expansion",
                        expander);
                currentFrame = entitySetWindow;
                EntitySetRecommendThread recommendThread = new EntitySetRecommendThread(expander, entitySetWindow, true);
                recommendThread.start();
            }
        });

        rankButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Corpus selectedCorpus = Ice.selectedCorpus;
                try {
                    //type = catTextField.getText();
                    if (!allowedEntity.contains(type.trim().toLowerCase())) {
                        throw new Exception("Only nn and nnp are allowed for set expansion.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Please select nouns or names for set expansion.",
                            "Unsupported entity type",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }
                String name = nameField.getText();
                try {
                    //type = catTextField.getText();
                    if (name == null || name.trim().length() == 0 || Ice.relations.containsKey(name)) {
                        throw new Exception("Name not allowed.");
                    }
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Name of entity set is empty or conflicts with existing name. Please try another name.",
                            "Unsupported entity name",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }

                java.util.List<String> seedStrings = new ArrayList<String>();
                for (String s : seedsArea.getText().split(",")) {
                    if (s.trim().length() > 1) {
                        seedStrings.add(s.trim());
                    }
                }
                //File entitySetIndexFile = new File(selectedCorpus.name + "EntitySetIndex_" + type);
                String entitySetIndexFileName = FileNameSchema.getEntitySetIndexFileName(selectedCorpus.getName(), type);
                File entitySetIndexFile = new File(entitySetIndexFileName);
                if (!entitySetIndexFile.exists() || entitySetIndexFile.isDirectory()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entity set index file not found. Please build index first.",
                            "Entity set index not found",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
//                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + type,
//                        seedStrings);
//                EntitySetExpander expander = new EntitySetExpander(selectedCorpus.name + "EntitySetIndex_" + type,
//                        seedStrings);
                EntitySetExpander expander = new EntitySetExpander(entitySetIndexFileName,
                        seedStrings);
                expander.setProgressMonitor(new SwingProgressMonitor(
                        Ice.mainFrame,
                        "Expanding entity set",
                        "Ranking Entities...",
                        0,
                        expander.getEntityFeatureDictSize()
                ));
                EntitySetRankerFrame entitySetWindow = new EntitySetRankerFrame("Expand entity set", name, expander);
                currentFrame = entitySetWindow;
                EntitySetRankThread rankerThread = new EntitySetRankThread(expander, entitySetWindow, true);
                rankerThread.start();
            }
        });


        suggestSeedsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (type == null || Ice.selectedCorpus.termFileName == null) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Cannot suggest seeds. Please extract term and set type first.",
                            "Error suggesting seeds",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                List<String> recommended = suggestSeeds();
                String result = "";
                for (String s : recommended) {
                    if (!result.equals("")) {
                        result = result + ", ";
                    }
                    result = result + s;
                }
                seedsArea.setText(result);
            }
        });


        return box;
    }

}

class CategoryRadioGroupActionListener implements ActionListener {

//    private ButtonGroup buttonGroup;
//
//    CategoryRadioGroupActionListener(ButtonGroup buttonGroup) {
//        this.buttonGroup = buttonGroup;
//    }

    public void actionPerformed(ActionEvent actionEvent) {
        EntitySetBuilder.type = actionEvent.getActionCommand();
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
        /**
         * String countFile   = args[0];
         String type        = args[1];
         double cutoff      = Double.valueOf(args[2]);
         String propsFile   = args[3];
         String docList     = args[4];
         String inputDir    = args[5];
         String inputSuffix = args[6];
         String outputFile  = args[7];
         */
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
