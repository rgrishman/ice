package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.RelationBuilderFrame;
import edu.nyu.jet.ice.uicomps.RelationBuilderThread;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
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
 * Panel to bootstrap relation extractor
 *
 * @author yhe
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
    // public List<String> negPaths = new ArrayList<String>();
    public Map<String, List<String>> negPaths = new TreeMap<String, List<String>>();
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

        negPaths.clear();
        for (String relationName : Ice.relations.keySet()) {
            IceRelation r = Ice.relations.get(relationName);
            if (r == null) {
                continue;
            }
            List<String> paths = new ArrayList<String>();
            if (r.getNegPaths() != null) {
                paths.addAll(r.getNegPaths());
            }
            negPaths.put(relationName, paths);
        }

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
        addRelationButton.setName("addRelationButton");
        leftPanel.add(addRelationButton);
        JButton suggestRelationButton = new JButton("Suggest");
        leftPanel.add(suggestRelationButton, "wrap");
        JButton deleteRelationButton = new JButton("Delete");
        leftPanel.add(deleteRelationButton);

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
        addEntryButton.setName("addRelationMemberButton");
        rightPanel.add(addEntryButton);
        JButton expandEntriesButton = new JButton("Expand");
        rightPanel.add(expandEntriesButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton);
        entriesSetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);
        //this.add(entitySetPanel);

        // listeners

        relationList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                refresh();
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
                List<String> paths = findPathOrClosest (relationInstance);
                if (paths != null) {
                    installSeedRelation(relationName, relationInstance, paths);
                }
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
                    if (n == JOptionPane.YES_OPTION) {
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
                                    "The provided path does not appear in the corpus.",
                                    "Dependency Path Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        installSeedRelation(relationName, relationInstance, paths);
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
                if (relationInstance == null) {
                    return;
                }
                List<String> paths = findPathOrClosest (relationInstance);
                if (paths == null) {
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
                entriesListModel.addElement(closestPhrase);
		currentRelation.addRepr(closestPhrase);
                currentRelation.addPaths(paths);
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                String repr = (String) entriesListModel.elementAt(idx);
                entriesListModel.remove(idx);
		currentRelation.removeRepr(repr);
		currentRelation.updatePaths();
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

    }

    public String closestPhrase;

    /**
     *  Finds the LDPs which corresponds to the English phrase 'relationInstance'. If
     *  'relationInstance' is not derived from any LDP in the corpus, search for the
     *  most similar phrase (in edit distance) which is derived from an LDP in the
     *  corpus, ask the user if they want to use this phrase instead, and return
     *  the LDPs associated with this phrase.
     *  <br>
     *  As a side effect, sets 'closestPhrase' to the corresponding English phrase.
     */

    public List<String> findPathOrClosest (String relationInstance) {
        DepPathMap depPathMap = DepPathMap.getInstance();
        depPathMap.load(false);
        List<String> paths = depPathMap.findPath(relationInstance);
        if (paths != null) {
            closestPhrase = relationInstance;
            return paths;
        }
        closestPhrase = depPathMap.findClosestPath(relationInstance);
        if (closestPhrase == null) {
            System.err.println("*** findPathOrClosest failed.");
            return null;
        }
        int n = JOptionPane.showConfirmDialog(
                SwingRelationsPanel.this,
                String.format("No example in the corpus for [%s]\nDo you want to use [%s]?", 
                    relationInstance, closestPhrase),
                "Path Suggestion",
                JOptionPane.YES_NO_OPTION);
        if (n == JOptionPane.NO_OPTION) 
            return null;
        paths = depPathMap.findPath(closestPhrase);
        if (paths == null) {
            System.err.println("*** findPathOrClosest failed.");
            return null;
        }
        return paths;
    }

    /**
     *  Creates a new IceRelation.  This method is invoked in response to
     *  the AddRelation and Suggest buttons.
     *
     *  @param  relationName        the name of the relation
     *  @param  relationInstance    an ERnglish phrase representing the relatio
     *  @param  paths               a list of dependency paths for the relation
     */

    private void installSeedRelation(String relationName, String relationInstance, List<String> paths) {
        String path = paths.get(0);
        String[] parts = path.split("--");
        String arg1 = parts[0].trim();
        if (arg1.endsWith("(1)") || arg1.endsWith("(2)"))
            arg1 = arg1.substring(0, arg1.length() - 3);
        String arg2 = parts[2].trim();
        if (arg2.endsWith("(1)") || arg2.endsWith("(2)"))
            arg2 = arg2.substring(0, arg2.length() - 3);

        IceRelation iceRelation = new IceRelation(relationName);
	Ice.addRelation(iceRelation);
	iceRelation.addRepr(relationInstance);
        iceRelation.setArg1type(arg1);
        iceRelation.setArg2type(arg2);

        iceRelation.setPaths(paths);
        relationListModel.addElement(iceRelation);
        relationList.setSelectedValue(iceRelation, true);
        // selecting new element forces refresh
        currentRelation = iceRelation;
    }

    /**
     *  Use bootstrapping to build a model for a relation.  This
     *  method is invoked by pressing the 'EXPAND' button on the
     *  relation panel.
     *
     *  @param  inName  the name of the relation
     *  @param  inSeed  the seed for bootstrapping this relation,
     *                  consisting of one or more Englist expressions
     *                  separated by ':::'
     */

    public void buildRelation(String inName, String inSeed) {

        String name = inName;

        String pathListFileName = "";  // not used anyway
        ProgressMonitorI  progressMonitorI = new SwingProgressMonitor(
                Ice.mainFrame, "Initializing Relation Builder",
                "Collecting seeds...",
                0,
                5
        );
        String bootstrapperName =
                Ice.iceProperties.getProperty("Ice.Bootstrapper.name") != null ?
                        Ice.iceProperties.getProperty("Ice.Bootstrapper.name") : "Bootstrap";
        Bootstrap bootstrap = Bootstrap.makeBootstrap(bootstrapperName, progressMonitorI, name);
        RelationBuilderFrame frame = new RelationBuilderFrame("Bootstrap relations",
                null,
                bootstrap,
                SwingRelationsPanel.this);

        frame.setSize(400, 580);
        frame.setAlwaysOnTop(true);
        String seed = inSeed;
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

    /**
     *  Refreshes the display of the currently selected relation.
     */

    public void refresh() {
        iceStatusPanel.refresh();
        int idx = relationList.getSelectedIndex();
        if (idx < 0) return;
        IceRelation relation = (IceRelation)relationListModel.getElementAt(idx);
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
        depPathMap.load(false);
        for (String path : relation.getPaths()) {
            String repr = depPathMap.findRepr(path);
            if (repr != null && ! newListModel.contains(repr)) {
                newListModel.addElement(repr);
            }
            else {
                System.err.println("Repr not found for path:" + path);
            }
        }

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

    /**
     *  Suggests a path to use as a seed for building a new relation.  It selects
     *  a path which is a sentential (subject-verb-object) structure, has not
     *  been suggested before (for this corpus), and is not a member of an
     *  existing relation.
     */

    public String suggestRelation() {
        String result = "";
        try {
            String[] lines =
                    IceUtils.readLines(FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName));
            DepPathMap depPathMap = DepPathMap.getInstance();
            depPathMap.load(false);
      loop: for (String line : lines) {
                String[] parts = line.split("\t");
                String path = parts[1];
                if (!path.matches(".*nsubj-1:.*:dobj.*")) 
                    continue;
                String phrase = depPathMap.findRepr(path);
                if (phrase == null)
                    continue;
                // skip phrase if prevoously suggested
                if (Ice.selectedCorpus.relationsSuggested.contains(phrase))
                    continue;
                // skip phrase if it is part of an existing relation
                for (IceRelation relation : Ice.relations.values()) {
                    for (String relPath : relation.getPaths()) {
                        String repr = depPathMap.findRepr(relPath);
                        if (phrase.equals(repr)) {
                            continue loop;
                        }
                    }
                }
                Ice.selectedCorpus.relationsSuggested.add(phrase);
                return phrase;
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
