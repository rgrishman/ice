package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.entityset.EntitySetExpander;
import edu.nyu.jet.ice.entityset.EntitySetRankThread;
import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.uicomps.EntitySetRankerFrame;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
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
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Timer;


/**
 * Created by yhe on 4/14/14.
 */
public class SwingEntitySetPanel extends JPanel implements Refreshable {
    JList entitySetList;
    JList entriesList;
    DefaultListModel entityListModel;
    DefaultListModel entriesListModel;
    JTextField nameTextField;
    IceEntitySet currentEntitySet;
    private static final int DELAY_MS = 250;
    private final SwingIceStatusPanel iceStatusPanel;

    public SwingEntitySetPanel() {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout());
        //JPanel entitySetPanel  = new JPanel(new MigLayout());
        //entitySetPanel.setOpaque(false);
        //entitySetPanel.setSize(480, 400);
        //entitySetPanel.setMaximumSize(new Dimension(480, 400));

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setOpaque(false);
        leftPanel.setSize(210, 366);
        leftPanel.setMinimumSize(new Dimension(210, 366));
        leftPanel.setMaximumSize(new Dimension(210, 366));


        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border leftTitledBorder = BorderFactory.createTitledBorder(lowerEtched, "Entity sets");
        leftPanel.setBorder(leftTitledBorder);
        JLabel entitySetLabel = new JLabel("Entity sets");
        //leftPanel.add(entitySetLabel, "span");

        this.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setSize(266, 366);
        rightPanel.setMinimumSize(new Dimension(266, 366));
        Border rightTitledBorder = BorderFactory.createTitledBorder(lowerEtched, "Members");
        rightPanel.setBorder(rightTitledBorder);
        rightPanel.setOpaque(false);
        this.add(rightPanel);
        entityListModel = new DefaultListModel();
        for (String k : Ice.entitySets.keySet()) {
            entityListModel.addElement(Ice.entitySets.get(k));
        }
        entitySetList   = new JList();
        JScrollPane entitySetPane = new JScrollPane(entitySetList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(170, 228);
        entitySetList.setSize(170, 228);
        entitySetList.setMinimumSize(new Dimension(170, 228));
        entitySetPane.setPreferredSize(new Dimension(170, 228));
        leftPanel.add(entitySetPane, "span");
        entitySetList.setModel(entityListModel);

        JButton addEntitySetButton = new JButton("Add");
        leftPanel.add(addEntitySetButton);
        JButton suggestEntitySetButton = new JButton("Suggest");
        leftPanel.add(suggestEntitySetButton, "wrap");
        JButton deleteEntitySetButton = new JButton("Delete");
        leftPanel.add(deleteEntitySetButton);
        JButton saveEntitySetsButton = new JButton("Save");
        leftPanel.add(saveEntitySetsButton);

        JLabel nameLabel = new JLabel("Type");
        rightPanel.add(nameLabel);
        nameTextField = new JTextField("");
        nameTextField.setMinimumSize(new Dimension(120, 20));
        nameTextField.setMaximumSize(new Dimension(120, 20));

        rightPanel.add(nameTextField, "span");
        JLabel itemsLabel = new JLabel("Items");
        rightPanel.add(itemsLabel, "span");

        entriesList   = new JList();

        entriesList.setSize(205, 190);
        entriesList.setMinimumSize(new Dimension(205, 190));
        entriesList.setMaximumSize(new Dimension(205, 190));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(205, 190);
        entriesSetPane.setMinimumSize(new Dimension(205, 190));
        entriesSetPane.setMaximumSize(new Dimension(205, 190));

        rightPanel.add(entriesSetPane, "span");
        JButton addEntryButton = new JButton("Add");
        rightPanel.add(addEntryButton);
        JButton expandEntitySetButton = new JButton("Expand");
        rightPanel.add(expandEntitySetButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton);
        JButton saveEntitySetButton = new JButton("Save");
        rightPanel.add(saveEntitySetButton);
        entriesSetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);

        // listeners

        entitySetList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                refresh();
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                String s = (String)JOptionPane.showInputDialog(
                        SwingEntitySetPanel.this,
                        "Entry",
                        "Add new entry to entity set",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");

                if ((s != null) && (s.length() > 0)) {
                    entriesListModel.addElement(s);
                }
            }
        });

        suggestEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                refresh();
                Timer timer = switchToBusyCursor(SwingEntitySetPanel.this);
                String seedString = null;
                List<String> seeds = null;
                if (Ice.selectedCorpus.termFileName == null ||
                        FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpusName, "nn") == null ||
                        !(new File(FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpusName, "nn"))).exists()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entity index file does not exist. Please run indexing first.",
                            "Index entities first",
                            JOptionPane.ERROR_MESSAGE);
                    switchToNormalCursor(SwingEntitySetPanel.this, timer);
                    return;
                }
                try {
                    Set<String> excludedEntities = Ice.getExclusionEntities();
                    seeds = suggestSeeds(excludedEntities);
                    seedString = "[" + seeds.get(0) + "] and [" + seeds.get(1) + "]";
                }
                finally {
                    switchToNormalCursor(SwingEntitySetPanel.this, timer);
                }
                if (seedString != null && seeds != null) {
                    int n = JOptionPane.showConfirmDialog(
                            SwingEntitySetPanel.this,
                            String.format("Do you want to start with seeds %s?", seedString),
                            "Seed Suggestion",
                            JOptionPane.YES_NO_OPTION);
                    if (n == 0) {
                        String entitySetName = JOptionPane.showInputDialog("Input name of the entity type");
                        if (entitySetName == null) {
                            return;
                        }
                        IceEntitySet ies = new IceEntitySet(entitySetName);
                        nameTextField.setText(ies.getType());
                        entityListModel.addElement(ies);
                        entitySetList.setSelectedValue(ies, true);
                        entriesListModel.addElement(seeds.get(0));
                        entriesListModel.addElement(seeds.get(1));
                        //currentEntitySet = new IceEntitySet();
                    }
                }
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                entriesListModel.remove(idx);
            }
        });

        saveEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entitySetList.getSelectedValue() != null) {
                    currentEntitySet = (IceEntitySet)entitySetList.getSelectedValue();
                }
                if (currentEntitySet == null ||
                        nameTextField.getText().trim().length() == 0 ||
                        entriesListModel == null ||
                        entriesListModel.size() == 0) {
                    return;
                }
                currentEntitySet.setType(nameTextField.getText().trim());
                java.util.List<String> nouns = new ArrayList<String>();
                for (Object e : entriesListModel.toArray()) {
                    nouns.add((String)e);
                }
                currentEntitySet.setNouns(nouns);
                entitySetList.revalidate();
                entitySetList.repaint();
            }
        });

        deleteEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entityListModel == null) return;
                int idx = entitySetList.getSelectedIndex();
                if (idx < 0) return;
                IceEntitySet e = (IceEntitySet)entityListModel.get(idx);
                int n = JOptionPane.showConfirmDialog(
                        SwingEntitySetPanel.this,
                        String.format("Are you sure you want to remove the entity set [%s]?", e.getType()),
                        "Confirm entity set deletion",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    entityListModel.remove(idx);
                    entriesListModel.clear();
                    //entriesListModel = null;
                    nameTextField.setText("");
                    currentEntitySet = null;
                }
            }
        });

        saveEntitySetsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Ice.entitySets.clear();
                for (Object e : entityListModel.toArray()) {
                    IceEntitySet es = (IceEntitySet)e;
                    Ice.entitySets.put(es.getType(), es);
                }
                JOptionPane.showMessageDialog(SwingEntitySetPanel.this,
                        "Entity sets in ICE updated. You will still need to save/export to Jet\n" +
                                "if you want to save entity sets to hard disk.");
            }
        });

        addEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                refresh();
                String entitySetName = JOptionPane.showInputDialog("Input name of the entity type");
                if (entitySetName == null) {
                    return;
                }
                entitySetName = entitySetName.toUpperCase();
                for (Object entitySet : entityListModel.toArray()) {
                    if (((IceEntitySet)entitySet).getType().equals(entitySetName)) {
                        JOptionPane.showMessageDialog(SwingEntitySetPanel.this,
                                "The provided name of entity set already exists.",
                                "Entity Set Name Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                IceEntitySet ies = new IceEntitySet(entitySetName);
                nameTextField.setText(ies.getType());
                entityListModel.addElement(ies);
                entitySetList.setSelectedValue(ies, true);
            }
        });

        expandEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String entitySetIndexFileName = FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpus.getName(),
                        "nn");
                File entitySetIndexFile = new File(entitySetIndexFileName);
                if (!entitySetIndexFile.exists() || entitySetIndexFile.isDirectory()) {
                    JOptionPane.showMessageDialog(Ice.mainFrame,
                            "Entity set index file not found. Please build index first.",
                            "Entity set index not found",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                java.util.List<String> seedStrings = new ArrayList<String>();
                for (Object seed : entriesListModel.toArray()) {
                    seedStrings.add(seed.toString());
                }
                EntitySetExpander expander = new EntitySetExpander(entitySetIndexFileName,
                        seedStrings, Ice.getExclusionEntities());
                EntitySetRankerFrame entitySetWindow = new EntitySetRankerFrame("Expand entity set",
                        nameTextField.getText(),
                        expander);
                entitySetWindow.setSize(400, 580);
                expander.setProgressMonitor(new SwingProgressMonitor(
                        entitySetWindow,
                        "Expanding entity set",
                        "Ranking Entities...",
                        0,
                        expander.getEntityFeatureDictSize()
                ));
                entitySetWindow.setLocationRelativeTo(null);
                entitySetWindow.setCallingPanel(SwingEntitySetPanel.this);
                EntitySetRankThread rankerThread = new EntitySetRankThread(expander, entitySetWindow, true);
                rankerThread.start();

            }
        });

        // add to UI
        //this.add(entitySetPanel);
        entriesListModel = new DefaultListModel();
        entriesList.setModel(entriesListModel);
    }

    public void refresh() {
        iceStatusPanel.refresh();
        int idx = entitySetList.getSelectedIndex();
        if (idx < 0 || Ice.entitySets.size() == 0) {
            return;
        }
        // System.err.println(idx);
        IceEntitySet entitySet = (IceEntitySet)entityListModel.getElementAt(idx);
        // System.err.println(entitySet.getType());
        if (Ice.entitySets.containsKey(entitySet.getType())) {
            entitySet = Ice.entitySets.get(entitySet.getType());
            entityListModel.setElementAt(entitySet, idx);
            entitySetList.setSelectedIndex(idx);
        }
        currentEntitySet = entitySet;
        nameTextField.setText(entitySet.getType());
        DefaultListModel newListModel = new DefaultListModel();
        for (String noun : entitySet.getNouns()) {
            newListModel.addElement(noun);
        }
        //System.err.println(newListModel.size());

        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    public java.util.List<String> suggestSeeds() {
        return  EntitySetExpander.recommendSeeds(FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpus.name, "nn"),
                Ice.selectedCorpus.termFileName, "nn", new HashSet());
    }

    public java.util.List<String> suggestSeeds(Set<String> excludedEntities) {
        return  EntitySetExpander.recommendSeeds(FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpus.name, "nn"),
                Ice.selectedCorpus.termFileName, "nn", excludedEntities);
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
