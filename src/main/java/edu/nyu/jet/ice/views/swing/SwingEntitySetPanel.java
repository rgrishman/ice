package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.entityset.EmbeddingEntitySetExpander;
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
 * Panel that manages entity set.
 *
 * @author yhe
 */
public class SwingEntitySetPanel extends JPanel implements Refreshable {
    JList<IceEntitySet> entitySetList;
    JList<String> entriesList;
    DefaultListModel entityListModel;
    DefaultListModel entriesListModel;
    JTextField nameTextField;
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
	addEntitySetButton.setName("addEntitySetButton");
        leftPanel.add(addEntitySetButton);
        JButton suggestEntitySetButton = new JButton("Suggest");
        leftPanel.add(suggestEntitySetButton, "wrap");
        JButton deleteEntitySetButton = new JButton("Delete");
        leftPanel.add(deleteEntitySetButton);

        JLabel nameLabel = new JLabel("Type");
        rightPanel.add(nameLabel);
        nameTextField = new JTextField("");
        nameTextField.setMinimumSize(new Dimension(120, 20));
        nameTextField.setMaximumSize(new Dimension(120, 20));

        rightPanel.add(nameTextField, "span");
        JLabel itemsLabel = new JLabel("Items");
        rightPanel.add(itemsLabel, "span");

        entriesList   = new JList<String>();

        entriesList.setSize(205, 190);
        entriesList.setMinimumSize(new Dimension(205, 190));
        entriesList.setMaximumSize(new Dimension(205, 190));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(205, 190);
        entriesSetPane.setMinimumSize(new Dimension(205, 190));
        entriesSetPane.setMaximumSize(new Dimension(205, 190));

        rightPanel.add(entriesSetPane, "span");
        JButton addEntryButton = new JButton("Add");
	addEntryButton.setName("addEntityButton");
        rightPanel.add(addEntryButton);
        JButton expandEntitySetButton = new JButton("Expand");
        rightPanel.add(expandEntitySetButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton);
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
                if ((s == null) || (s.length() == 0)) 
			return;
		IceEntitySet currentEntitySet = entitySetList.getSelectedValue();
		if (currentEntitySet == null) {
			System.err.println("Error in addEntryButton: currentEntitySet == null");
			return;
		}
		entriesListModel.addElement(s);
		currentEntitySet.addNoun(s);
            }
        });

        suggestEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
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
                    seeds = suggestSeeds();
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
			entitySetName = entitySetName.toUpperCase();
                        IceEntitySet ies = new IceEntitySet(entitySetName);
                        entityListModel.addElement(ies);
                        entitySetList.setSelectedValue(ies, true);
                        entriesListModel.addElement(seeds.get(0));
                        entriesListModel.addElement(seeds.get(1));
			ies.addNoun(seeds.get(0));
			ies.addNoun(seeds.get(1));
			Ice.addEntitySet(ies);
                    }
                }
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
		String entry = (String) entriesListModel.get(idx);
		IceEntitySet currentEntitySet = entitySetList.getSelectedValue();
		if (currentEntitySet == null) {
			System.err.println("Error in deleteEntryButton: currentEntitySet == null");
			return;
		}
                entriesListModel.remove(idx);
		currentEntitySet.removeNoun(entry);
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
                    entriesListModel = null;
                    nameTextField.setText("");
		    Ice.removeEntitySet(e.getType());
                }
            }
        });

        addEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
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
                entityListModel.addElement(ies);
                entitySetList.setSelectedValue(ies, true);
		Ice.addEntitySet(ies);
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
                        seedStrings);
//                EntitySetExpander expander = new EmbeddingEntitySetExpander(
//                        FileNameSchema.getCorpusInfoDirectory(Ice.selectedCorpusName)
//                                + File.separator + "phrases.embedding.200",
//                        seedStrings);
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

    }

    /**
     *  Redraws the currently selected entitySet.
     */

    public void refresh() {
        iceStatusPanel.refresh();
        int idx = entitySetList.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        IceEntitySet entitySet = (IceEntitySet)entityListModel.getElementAt(idx);
        if (entitySet == null) {
            System.err.println("ICE internal error in SwingEntitySetPanel, entitySet is null");
            return;
        }
        // if (Ice.entitySets.containsKey(entitySet.getType())) {
        //     entitySet = Ice.entitySets.get(entitySet.getType());
        //     entityListModel.setElementAt(entitySet, idx);
        //     entitySetList.setSelectedIndex(idx);
        //  }
        nameTextField.setText(entitySet.getType());
        DefaultListModel newListModel = new DefaultListModel();
        for (String noun : entitySet.getNouns()) {
            newListModel.addElement(noun);
        }
        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    public java.util.List<String> suggestSeeds() {
        return  EntitySetExpander.recommendSeeds(FileNameSchema.getEntitySetIndexFileName(Ice.selectedCorpus.name, "nn"),
                Ice.selectedCorpus.termFileName, "nn");
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
