package Jet.IceUI;

import Jet.ExpandEntitySet.EntitySetExpander;
import Jet.ExpandEntitySet.EntitySetRecommendThread;
import Jet.IceUtils.ProgressMonitorI;
import Jet.IceUtils.SwingProgressMonitor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

/**
 * Created by yhe on 2/16/14.
 */
public class EntitySetBuilderFrame extends JFrame {
    private EntitySetExpander expander;
    private int count = 0;
    public JList positiveList;
    public DefaultListModel positiveListModel = new DefaultListModel();
    public JList negativeList;
    public DefaultListModel negativeListModel = new DefaultListModel();
    public JLabel iterationLabel;

    public EntitySetBuilderFrame(String title, final EntitySetExpander expander) {
        super(title);
        this.expander = expander;
        JPanel entitySetPanel  = new JPanel(new MigLayout());
        entitySetPanel.setSize(800, 600);

        iterationLabel = new JLabel("Iteration 1");
        JLabel blankLabel12 = new JLabel("    "); // 1st row, 2nd col
        JLabel blankLabel13 = new JLabel("    ");
        JLabel positiveSeedsLabel = new JLabel("Positive seeds");
        JLabel blankLabel22 = new JLabel("    ");
        JLabel negativeSeedsLabel = new JLabel("Negative seeds");
        entitySetPanel.add(iterationLabel);
        entitySetPanel.add(blankLabel12);
        entitySetPanel.add(blankLabel13, "wrap");
        entitySetPanel.add(positiveSeedsLabel);
        entitySetPanel.add(blankLabel22);
        entitySetPanel.add(negativeSeedsLabel, "wrap");

        JScrollPane positivePane = new JScrollPane();
        positiveList       = new JList(positiveListModel);
        JScrollPane negativePane = new JScrollPane();
        negativeList       = new JList(negativeListModel);

        positivePane.setSize(200, 400);
        positiveList.setSize(200, 400);
        positiveList.setMinimumSize(new Dimension(200, 400));
        positivePane.setPreferredSize(new Dimension(200, 400));
        positivePane.add(positiveList);
        entitySetPanel.add(positivePane);

        JPanel movementButtonsPanel = new JPanel(new MigLayout());
        JButton moveToPositiveButton = new JButton("<< Negative to Positive");
        moveToPositiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                moveElement(negativeListModel, positiveListModel, negativeList.getSelectedIndex());
            }
        });
        JButton moveToNegativeButton = new JButton("Positive to Negative >>");
        moveToNegativeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                moveElement(positiveListModel, negativeListModel, positiveList.getSelectedIndex());
            }
        });

        movementButtonsPanel.add(moveToNegativeButton, "wrap");
        movementButtonsPanel.add(moveToPositiveButton);

        entitySetPanel.add(movementButtonsPanel);

        negativePane.setSize(200, 400);
        negativeList.setSize(200, 400);
        negativeList.setMinimumSize(new Dimension(200, 400));
        negativePane.setPreferredSize(new Dimension(200, 400));
        negativePane.add(negativeList);
        entitySetPanel.add(negativePane, "wrap");

        JPanel actionButtonsPanel = new JPanel(new MigLayout());
        JButton iterateButton = new JButton("Iterate");
        iterateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<String> positiveSeeds = new ArrayList<String>();
                java.util.List<String> negativeSeeds = new ArrayList<String>();
                for (Object o : positiveListModel.toArray()) {
                    positiveSeeds.add((String) o);
                }
                for (Object o : negativeListModel.toArray()) {
                    negativeSeeds.add((String) o);
                }
                expander.updateSeeds(positiveSeeds, negativeSeeds);
                expander.updateParameters();
                int size = expander.getEntityFeatureDictSize();
                ProgressMonitorI progressMonitor = new SwingProgressMonitor(
                        EntitySetBuilder.currentFrame,
                        "Progressing",
                        "Calculating recommended seeds",
                        0,
                        size
                );
                expander.setProgressMonitor(progressMonitor);
                EntitySetRecommendThread recommender = new EntitySetRecommendThread(expander,
                        EntitySetBuilderFrame.this);
                recommender.start();
                //SwingUtilities.invokeLater(recommender);
                //expander.recommend();
                //updateLists(expander.getPositives(), expander.getNegatives());
                //count++;
                //iterationLabel.setText("Iteration " + count);
            }
        });
        JButton exitButton   = new JButton("Exit");

        // handle the click of [Exit]
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EntitySetBuilder.currentFrame = Ice.mainFrame;
				expander.setProgressMonitor(null);

                EntitySetBuilderFrame.this.dispose();
            }
        });

        // handle the click of [x]
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                EntitySetBuilder.currentFrame = Ice.mainFrame;
				expander.setProgressMonitor(null);
            }
        });
        actionButtonsPanel.add(iterateButton);
        actionButtonsPanel.add(exitButton);
        entitySetPanel.add(actionButtonsPanel);

        this.add(entitySetPanel);
    }

    public void updateLists(java.util.List<String> positiveList, java.util.List<String> negativeList) {
        positiveListModel.clear();
        negativeListModel.clear();
        for (String s : positiveList) {
            positiveListModel.addElement(s);
        }
        for (String s : negativeList) {
            negativeListModel.addElement(s);
        }
        iterationLabel.setText("Iteration " + ++count);
    }

    private void moveElement(DefaultListModel fromListModel, DefaultListModel toListModel, int fromIdx) {
        if (fromIdx != -1) {
            Object elem = fromListModel.elementAt(fromIdx);
            fromListModel.remove(fromIdx);
            toListModel.addElement(elem);
        }

    }
}
