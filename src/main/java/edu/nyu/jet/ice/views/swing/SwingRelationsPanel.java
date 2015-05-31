package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.RelationBuilderFrame;
import edu.nyu.jet.ice.uicomps.RelationBuilderThread;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;


/**
 * Created by yhe on 4/14/14.
 */
public class SwingRelationsPanel extends JPanel implements Refreshable {
    JList relationList;
    JList entriesList;
    DefaultListModel relationListModel;
    DefaultListModel entriesListModel;
    JTextField nameTextField;
    JTextField arg1TextField;
    JTextField arg2TextField;
    IceRelation currentRelation;
    SwingIceStatusPanel iceStatusPanel;
    public List<String> negPaths = new ArrayList<String>();
    private static final int DELAY_MS = 250;
    static final Set<String> ACE_TYPES = new HashSet<String>();
    {
        ACE_TYPES.add("GPE");
        ACE_TYPES.add("email");
        ACE_TYPES.add("url");
        ACE_TYPES.add("PERSON");
        ACE_TYPES.add("ORGANIZATION");
        ACE_TYPES.add("LOCATION");
        ACE_TYPES.add("VEHICLE");
        ACE_TYPES.add("WEAPON");
        ACE_TYPES.add("FACILITY");
    }
    public SwingRelationsPanel() {
        super();
        //JPanel entitySetPanel  = new JPanel(new MigLayout());
        //entitySetPanel.setSize(800, 520);
        this.setLayout(new MigLayout());
        this.setOpaque(false);

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(210, 366));
        leftPanel.setMaximumSize(new Dimension(210, 366));
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border leftTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Relations");
        leftPanel.setBorder(leftTitleBorder);
        JLabel entitySetLabel = new JLabel("Relations");
        //leftPanel.add(entitySetLabel, "wrap");

        this.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setOpaque(false);
        rightPanel.setMinimumSize(new Dimension(266, 366));
        rightPanel.setMaximumSize(new Dimension(266, 366));
        Border rightTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Members");
        rightPanel.setBorder(rightTitleBorder);
        this.add(rightPanel);
        relationListModel = new DefaultListModel();
        for (String k : Ice.relations.keySet()) {
            relationListModel.addElement(Ice.relations.get(k));
        }
        relationList = new JList();
        JScrollPane entitySetPane = new JScrollPane(relationList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(170, 228);
        relationList.setSize(170, 228);
        relationList.setMinimumSize(new Dimension(170, 228));
        entitySetPane.setPreferredSize(new Dimension(170, 228));
        leftPanel.add(entitySetPane, "span");
        relationList.setModel(relationListModel);

        JButton addRelationButton = new JButton("Add");
        leftPanel.add(addRelationButton);
        JButton suggestRelationButton = new JButton("Suggest");
        leftPanel.add(suggestRelationButton, "wrap");
        JButton deleteRelationButton = new JButton("Delete");
        leftPanel.add(deleteRelationButton);
        JButton saveRelationButton = new JButton("Save");
        leftPanel.add(saveRelationButton);

        JLabel nameLabel = new JLabel("Type");
        rightPanel.add(nameLabel);

        nameTextField = new JTextField("");
        nameTextField.setMinimumSize(new Dimension(120, 20));
        nameTextField.setMaximumSize(new Dimension(120, 20));
        rightPanel.add(nameTextField, "span");

        JLabel arg1Label = new JLabel("Arg1");
        rightPanel.add(arg1Label);

        arg1TextField = new JTextField("");
        arg1TextField.setMinimumSize(new Dimension(120, 20));
        arg1TextField.setMaximumSize(new Dimension(120, 20));

        rightPanel.add(arg1TextField, "span");

        JLabel arg2Label = new JLabel("Arg2");
        rightPanel.add(arg2Label);

        arg2TextField = new JTextField("");
        arg2TextField.setMinimumSize(new Dimension(120, 20));
        arg2TextField.setMaximumSize(new Dimension(120, 20));

        rightPanel.add(arg2TextField, "span");
        JLabel itemsLabel = new JLabel("Items");
        rightPanel.add(itemsLabel, "span");

        entriesList   = new JList();

        entriesList.setSize(235, 130);
        entriesList.setMinimumSize(new Dimension(235, 130));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(235, 130);
        entriesSetPane.setMinimumSize(new Dimension(235, 130));
        rightPanel.add(entriesSetPane, "span");
        JButton addEntryButton = new JButton("Add");
        rightPanel.add(addEntryButton);
        JButton expandEntriesButton = new JButton("Expand");
        rightPanel.add(expandEntriesButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton);
        JButton saveEntriesButton = new JButton("Save");
        rightPanel.add(saveEntriesButton);
        entriesSetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);
        //this.add(entitySetPanel);

        // listeners

        relationList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                refresh();
//                int idx = relationList.getSelectedIndex();
//                if (idx < 0) return;
//                IceRelation iceRelation = (IceRelation) relationListModel.getElementAt(idx);
//                currentRelation = iceRelation;
//                nameTextField.setText(iceRelation.getName());
//                arg1TextField.setText(iceRelation.getArg1type());
//                arg2TextField.setText(iceRelation.getArg2type());
//                DefaultListModel newListModel = new DefaultListModel();
//                for (String path : iceRelation.getPaths()) {
//                    newListModel.addElement(path);
//                }
//                entriesListModel = newListModel;
//                entriesList.setModel(entriesListModel);
            }
        });

        addRelationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String relationName = JOptionPane.showInputDialog("Name of the relation");
                if (relationName == null) {
                    return;
                }
                relationName = relationName.toUpperCase();
                for (Object existingRelation : relationListModel.toArray()) {
                    if (existingRelation.toString().equals(relationName)) {
                        JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                                "The name is already in use by another relation.",
                                "Relation Name Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                String relationInstance = JOptionPane.showInputDialog(
                        "Please provide an example of the relation");
                if (relationInstance == null) {
                    return;
                }
                DepPathMap depPathMap = DepPathMap.getInstance();
                List<String> paths = depPathMap.findPath(relationInstance);
                if (paths == null) {
                    JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                            "The provided path is invalid.",
                            "Dependency Path Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                installSeedRelation(relationName, paths);
                // System.err.println(path);
            }
        });

        suggestRelationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.Timer timer = switchToBusyCursor(SwingRelationsPanel.this);
                String relationInstance = null;
                try {
                    relationInstance = suggestRelation();
                }
                finally {
                    switchToNormalCursor(SwingRelationsPanel.this, timer);
                }
                if (relationInstance != null) {
                    int n = JOptionPane.showConfirmDialog(
                            SwingRelationsPanel.this,
                            String.format("Do you want to start with path [%s]?", relationInstance),
                            "Path Suggestion",
                            JOptionPane.YES_NO_OPTION);
                    if (n == 0) {
                        String relationName = JOptionPane.showInputDialog("Name of the relation");
                        if (relationName == null) {
                            return;
                        }
                        relationName = relationName.toUpperCase();
                        for (Object existingRelation : relationListModel.toArray()) {
                            if (existingRelation.toString().equals(relationName)) {
                                JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                                        "The name is already used by another relation.",
                                        "Relation Name Error",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        DepPathMap depPathMap = DepPathMap.getInstance();
                        List<String> paths = depPathMap.findPath(relationInstance);
                        if (paths == null) {
                            JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                                    "The provided path is invalid.",
                                    "Dependency Path Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        installSeedRelation(relationName, paths);
                    }
                }
            }
        });

        expandEntriesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < entriesListModel.size(); i++) {
                    Object entry = entriesListModel.getElementAt(i);
                    if (i > 0) {
                        b.append(":::");
                    }
                    b.append(entry);
                }
                buildRelation(currentRelation.getName(), b.toString());
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                String relationInstance = JOptionPane.showInputDialog(
                        "Please provide an example of the relation");
                DepPathMap depPathMap = DepPathMap.getInstance();
                if (relationInstance == null) {
                    return;
                }
                List<String> paths = depPathMap.findPath(relationInstance);
                if (paths == null) {
                    JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                            "The provided path is invalid.",
                            "Dependency Path Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                for (Object entry : entriesListModel.toArray()) {
                    if (entry.toString().equals(relationInstance)) {
                        JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                                "The provided path exists in the path set.",
                                "Dependency Path Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                entriesListModel.addElement(relationInstance);
                // currentRelation.getPaths().add(path);
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                // String repr = (String)entriesListModel.elementAt(idx);
                // DepPathMap depPathMap = DepPathMap.getInstance();
                // String path = depPathMap.findPath(repr);
                // currentRelation.getPaths().remove(path);
                entriesListModel.remove(idx);
            }
        });

        saveEntriesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (currentRelation == null ||
                        nameTextField.getText().trim().length() == 0 ||
                        entriesListModel == null ||
                        entriesListModel.size() == 0) {
                    return;
                }
                currentRelation.setName(nameTextField.getText().trim());
                currentRelation.setArg1type(arg1TextField.getText());
                currentRelation.setArg2type(arg2TextField.getText());
                java.util.List<String> relationPaths = new ArrayList<String>();
                for (Object e : entriesListModel.toArray()) {
                    DepPathMap depPathMap = DepPathMap.getInstance();
                    List<String> paths = depPathMap.findPath(e.toString());
                    if (paths != null) {
                        for (String path : paths) {
                            String[] parts = path.split(" -- ");
                            if (parts.length == 3) {
                                relationPaths.add(parts[1]);
                            }
                        }
                    }
                }
                currentRelation.setPaths(relationPaths);
                currentRelation.setNegPaths(negPaths);
                relationList.revalidate();
                relationList.repaint();
            }
        });

        deleteRelationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (relationListModel == null) return;
                int idx = relationList.getSelectedIndex();
                if (idx < 0) return;
                IceRelation e = (IceRelation) relationListModel.get(idx);
                int n = JOptionPane.showConfirmDialog(
                        SwingRelationsPanel.this,
                        String.format("Are you sure you want to remove the relation [%s]?", e.getName()),
                        "Confirm relation deletion",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    relationListModel.remove(idx);
                    entriesListModel.clear();
                    entriesListModel = null;
                    nameTextField.setText("");
                    arg1TextField.setText("");
                    arg2TextField.setText("");
                    currentRelation = null;
                }
            }
        });

        saveRelationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Ice.relations.clear();
                for (Object e : relationListModel.toArray()) {
                    IceRelation es = (IceRelation) e;
                    Ice.relations.put(es.getName(), es);
                }
                JOptionPane.showMessageDialog(SwingRelationsPanel.this,
                        "User relations in ICE updated. You will still need to save/export to Jet\n" +
                                "if you want to save relations to hard disk.");
            }
        });

    }

    private void installSeedRelation(String relationName, List<String> paths) {
        String arg1 = "";
        String arg2 = "";
        List<String> seedPaths = new ArrayList<String>();
        for (String path : paths) {
            String[] parts = path.split("--");
            arg1 = parts[0].trim();
            arg2 = parts[2].trim();
            seedPaths.add(parts[1].trim());
        }

        IceRelation iceRelation = new IceRelation(relationName);
        iceRelation.setArg1type(arg1);
        iceRelation.setArg2type(arg2);

        iceRelation.setPaths(seedPaths);
        relationListModel.addElement(iceRelation);
        relationList.setSelectedValue(iceRelation, true);
        currentRelation = iceRelation;
    }

    public void buildRelation(String inName, String inSeed) {

        String name = inName;

        String pathListFileName = "";  // not used anyway
        Bootstrap bootstrap = new Bootstrap(new SwingProgressMonitor(
                Ice.mainFrame, "Initializing Relation Builder",
                "Collecting seeds...",
                0,
                5
        ));
        RelationBuilderFrame frame = new RelationBuilderFrame("Bootstrap relations",
                null,
                bootstrap,
                SwingRelationsPanel.this);

        frame.setSize(400, 580);
        frame.setAlwaysOnTop(true);
//                frame.setVisible(true);
//                frame.listPane.revalidate();
//                frame.listPane.repaint();
//                frame.rankedList.revalidate();
//                frame.rankedList.repaint();
//                System.out.println("plfn = " + pathListFileName);
        String seed = inSeed;
//                System.out.println("plfn = " + pathListFileName);
        RelationBuilderThread builder = new RelationBuilderThread(
                seed,
                Ice.selectedCorpus.relationInstanceFileName,
                pathListFileName,
                null,
                bootstrap,
                frame,
                SwingRelationsPanel.this);
        builder.start();
    }

    public void refresh() {
        iceStatusPanel.refresh();
        int idx = relationList.getSelectedIndex();
        if (idx < 0) return;
        // System.err.println(idx);
        IceRelation relation = (IceRelation)relationListModel.getElementAt(idx);
        // System.err.println(relation.getType());
        if (Ice.relations.containsKey(relation.getName())) {
            relation = Ice.relations.get(relation.getName());
            relationListModel.setElementAt(relation, idx);
            relationList.setSelectedIndex(idx);
        }
        currentRelation = relation;
        nameTextField.setText(relation.getName());
        arg1TextField.setText(relation.getArg1type());
        arg2TextField.setText(relation.getArg2type());
        DefaultListModel newListModel = new DefaultListModel();
        DepPathMap depPathMap = DepPathMap.getInstance();
        depPathMap.load();
        for (String path : relation.getPaths()) {
            String fullPath = relation.getArg1type() +
                    " -- " + path + " -- " +
                    relation.getArg2type();
            String repr = depPathMap.findRepr(fullPath);
            if (repr != null && ! newListModel.contains(repr)) {

                newListModel.addElement(repr);
            }
            else {
                System.err.println("Repr not found for path:" + fullPath);
            }
        }
        //System.err.println(newListModel.size());

        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    public void updateEntriesListModel(java.util.List<String> paths) {
        DefaultListModel newListModel = new DefaultListModel();
        for (String path : paths) {
            newListModel.addElement(path);
        }
        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    public String suggestRelation() {
        String result = "";
        try {
            String[] lines =
                    IceUtils.readLines(FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName));
            DepPathMap depPathMap = DepPathMap.getInstance();
            for (String line : lines) {
                String[] parts = line.split("\t");
                String path = parts[1];
                if (!path.matches(".*nsubj-1:.*:dobj.*")) {
                    continue;
                }
                parts = path.split("--");
                if (parts.length == 3 && (
                        !ACE_TYPES.contains(parts[0].trim()) ||
                        !ACE_TYPES.contains(parts[2].trim()))) {
                    if (depPathMap.findRepr(path) != null) {
                        return depPathMap.findRepr(path);
                    }
                }
            }
            for (String line : lines) {
                String[] parts = line.split("\t");
                String path = parts[1];
                if (!path.matches(".*nsubj-1:.*:dobj.*")) {
                    continue;
                }
                if (depPathMap.findRepr(path) != null) {
                    return depPathMap.findRepr(path);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static java.util.Timer switchToBusyCursor(final javax.swing.JPanel frame) {
        java.util.TimerTask timerTask = new java.util.TimerTask() {

            public void run() {
                startWaitCursor(frame);
            }

        };
        final java.util.Timer timer = new java.util.Timer();
        timer.schedule(timerTask, DELAY_MS);
        return timer;
    }

    public static void switchToNormalCursor(final javax.swing.JPanel frame, final java.util.Timer timer) {
        timer.cancel();
        stopWaitCursor(frame);
    }

    private static void startWaitCursor(javax.swing.JPanel frame) {
        frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
    }

    private static void stopWaitCursor(javax.swing.JPanel frame) {
        frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

}
