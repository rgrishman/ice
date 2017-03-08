package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.entityset.Entity;
import edu.nyu.jet.ice.entityset.EntitySetExpander;
import edu.nyu.jet.ice.entityset.EntitySetRerankThread;
import edu.nyu.jet.ice.entityset.RankChoiceEntity;
import edu.nyu.jet.ice.views.swing.SwingEntitySetPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Frame for capturing user feedback regarding membership of entities
 * in an entity set.
 *
 * @author yhe
 * @version 1.0
 */

public class EntitySetRankerFrame extends JFrame {
    private EntitySetExpander expander;
    public JScrollPane listPane;
    public JList rankedList;
    public DefaultListModel rankedListModel = new DefaultListModel();
    public JRadioButton yesButton;
    public JRadioButton noButton;
    public JRadioButton undecidedButton;
    public String name;
    private SwingEntitySetPanel callingPanel = null;

    public void setCallingPanel(SwingEntitySetPanel callingPanel) {
        this.callingPanel = callingPanel;
    }

    public EntitySetRankerFrame(String title, String name, final EntitySetExpander expander) {
        super(title);
        this.expander = expander;
        this.name = name;
        JPanel entitySetPanel = new JPanel(new MigLayout());
        entitySetPanel.setSize(400, 525);

        JLabel rankedListLabel = new JLabel("Ranked entities");
        entitySetPanel.add(rankedListLabel, "wrap");

//        JScrollPane positivePane = new JScrollPane();
//        positiveList       = new JList(positiveListModel);
//        JScrollPane negativePane = new JScrollPane();
//        negativeList       = new JList(negativeListModel);
//
//        positivePane.setSize(200, 400);
//        positiveList.setSize(200, 400);
//        positiveList.setMinimumSize(new Dimension(200, 400));
//        positivePane.setPreferredSize(new Dimension(200, 400));
//        positivePane.add(positiveList);
//        entitySetPanel.add(positivePane);

        rankedList = new JList(rankedListModel) {
            @Override
            public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
                return -1;
            }
        };
        listPane = new JScrollPane(rankedList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listPane.setSize(new Dimension(350, 360));
        listPane.setPreferredSize(new Dimension(350, 360));
        listPane.setMinimumSize(new Dimension(350, 360));
        listPane.setMaximumSize(new Dimension(350, 360));
        JPanel decisionPanel = new JPanel(new MigLayout());
        TitledBorder border = new TitledBorder("Decision");
        decisionPanel.setBorder(border);
        decisionPanel.setSize(new Dimension(350, 100));
        decisionPanel.setPreferredSize(new Dimension(350, 100));
        decisionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        yesButton = new JRadioButton("Yes");
        yesButton.setActionCommand("YES");
        noButton = new JRadioButton("No");
        noButton.setActionCommand("NO");
        undecidedButton = new JRadioButton("Undecided");
        undecidedButton.setActionCommand("UNDECIDED");
        ButtonGroup group = new ButtonGroup();
        group.add(yesButton);
        group.add(noButton);
        group.add(undecidedButton);
        decisionPanel.add(yesButton);
        decisionPanel.add(noButton);
        decisionPanel.add(undecidedButton);
        ActionListener decisionActionListener = new DecisionActionListener(this);
        yesButton.addActionListener(decisionActionListener);
        noButton.addActionListener(decisionActionListener);
        undecidedButton.addActionListener(decisionActionListener);
//        rankedList.setSize(new Dimension(350, 400));
//        rankedList.setPreferredSize(new Dimension(350, 400));
        //listPane.add(rankedList);
        entitySetPanel.add(listPane, "wrap");
        entitySetPanel.add(decisionPanel, "wrap");

        JPanel actionButtonsPanel = new JPanel(new MigLayout());
        JButton rerankButton = new JButton("Rerank");
        JButton saveButton = new JButton("Save");
        JButton exitButton = new JButton("Exit");
        actionButtonsPanel.add(rerankButton);
        actionButtonsPanel.add(saveButton);
        actionButtonsPanel.add(exitButton);
        entitySetPanel.add(actionButtonsPanel);
        this.add(entitySetPanel);

        // listeners...
        rankedList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int idx = rankedList.getSelectedIndex();
                if (idx < 0) return;
                RankChoiceEntity e = (RankChoiceEntity) rankedListModel.getElementAt(idx);
                if (e.getDecision() == RankChoiceEntity.EntityDecision.YES) {
                    yesButton.setSelected(true);
                }
                if (e.getDecision() == RankChoiceEntity.EntityDecision.NO) {
                    noButton.setSelected(true);
                }
                if (e.getDecision() == RankChoiceEntity.EntityDecision.UNDECIDED) {
                    undecidedButton.setSelected(true);
                }
            }
        });

        rerankButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<String> positiveSeeds = new ArrayList<String>();
                java.util.List<String> negativeSeeds = new ArrayList<String>();
                java.util.List<Entity> entities = new ArrayList<Entity>();
                for (Object o : rankedListModel.toArray()) {
                    RankChoiceEntity e = (RankChoiceEntity) o;
                    entities.add(e);
                    if (e.getDecision() == RankChoiceEntity.EntityDecision.YES) {
                        positiveSeeds.add(e.getText());
                    }
                    if (e.getDecision() == RankChoiceEntity.EntityDecision.NO) {
                        negativeSeeds.add(e.getText());
                    }
                }
                expander.updateSeeds(positiveSeeds, negativeSeeds);
                expander.updateParameters();
                int size = expander.getEntityFeatureDictSize();
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(
                        EntitySetRankerFrame.this,
                        "Progressing",
                        "Calculating recommended seeds",
                        0,
                        size
                );
                expander.setProgressMonitor(progressMonitor);
                EntitySetRerankThread rerankThread = new EntitySetRerankThread(expander,
                        EntitySetRankerFrame.this, entities);
                rerankThread.start();
            }
        });

        //
        //  save button
        //
        //  build a new IceEntitySet consisting of all terms labeled YES in frame
        //

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String type = EntitySetRankerFrame.this.name;
		IceEntitySet ies = Ice.getEntitySet(type);
		if (ies == null) {
			System.err.println("Error in save button: unknown entity set " + type);
			return;
			}
                for (Object o : rankedListModel.toArray()) {
                    RankChoiceEntity e = (RankChoiceEntity) o;
                    if (e.getDecision() == RankChoiceEntity.EntityDecision.YES) {
                        ies.addNoun(e.getText().trim());
                    }
                }
                if (callingPanel != null) {
                    callingPanel.refresh();
                }
                EntitySetRankerFrame.this.dispose();
            }
        });

        // handle the click of [Exit]
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                // EntitySetBuilder.currentFrame = Ice.mainFrame;
                expander.setProgressMonitor(null);

                EntitySetRankerFrame.this.dispose();
            }
        });

        // handle the click of [x]
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // EntitySetBuilder.currentFrame = Ice.mainFrame;
                expander.setProgressMonitor(null);

            }
        });

        // Key bindings
        rankedList.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {

            }

            public void keyPressed(KeyEvent keyEvent) {

            }

            public void keyReleased(KeyEvent keyEvent) {

            }
        });
        rankedList.setFocusTraversalKeysEnabled(false);
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("Y"), "YES");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("y"), "YES");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("N"), "NO");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("n"), "NO");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("U"), "UNDECIDED");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("u"), "UNDECIDED");
        rankedList.getActionMap().put("YES", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                yesButton.doClick();
                rankedList.ensureIndexIsVisible(rankedList.getSelectedIndex());

            }
        });
        rankedList.getActionMap().put("NO", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                noButton.doClick();
                rankedList.ensureIndexIsVisible(rankedList.getSelectedIndex());
            }
        });
        rankedList.getActionMap().put("UNDECIDED", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                undecidedButton.doClick();
                rankedList.ensureIndexIsVisible(rankedList.getSelectedIndex());

            }
        });
    }

    private void saveEntitySetToAuxFile(String typeName) {
        try {
            Properties props = new Properties();
            props.load(new FileReader("parseprops"));
            String fileName = props.getProperty("Jet.dataPath")
                    + "/"
                    + props.getProperty("Ace.EDTtype.auxFileName");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));

            for (Object o : rankedListModel.toArray()) {
                RankChoiceEntity e = (RankChoiceEntity) o;
                if (e.getDecision() == RankChoiceEntity.EntityDecision.YES) {
                    pw.println(e.getText().trim() + " | " + typeName + ":" + typeName + " 1");
                }
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateList() {
        //rankedListModel.clear();
        DefaultListModel newListModel = new DefaultListModel();
        int count = 0;
        for (Entity s : expander.rankedEntities) {
//            if (count > 10) break;
            newListModel.addElement(s);
            count++;
        }
        rankedListModel = newListModel;
        rankedList.setModel(rankedListModel);
//        yesButton.setSelected(false);
//        noButton.setSelected(false);
//        undecidedButton.setSelected(false);
    }

//    public void testRankedModel() {
//        rankedListModel.clear();
//        rankedListModel.addElement("test");
//    }

}

class DecisionActionListener implements ActionListener {
    EntitySetRankerFrame frame;

    DecisionActionListener(EntitySetRankerFrame frame) {
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        int idx = frame.rankedList.getSelectedIndex();
        if (idx < 0) return;
        RankChoiceEntity e = (RankChoiceEntity) frame.rankedListModel.getElementAt(idx);
        e.setDecision(RankChoiceEntity.EntityDecision.valueOf(actionEvent.getActionCommand()));
        frame.rankedList.revalidate();
        frame.rankedList.repaint();
    }
}
