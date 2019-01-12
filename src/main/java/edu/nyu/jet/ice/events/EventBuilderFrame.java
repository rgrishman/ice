package edu.nyu.jet.ice.events;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
// import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.events.EventBootstrap;
import edu.nyu.jet.ice.events.SwingEventsPanel;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.IceCellRenderer;;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 *  A frame which enables user review of paths generated by bootstrapping as part
 *  of the process of defining a relation.  The paths are displayed in a
 *  scrollable list;  individual entries may be assigned the label "Yes" or "No" by
 *  key bindings or buttons.
 *
 *  @author yhe
 *  @version 1.0
 */

public class EventBuilderFrame extends JFrame {
    private EventBootstrap bootstrap;
    public JScrollPane listPane;
    public JList rankedList;
    public DefaultListModel rankedListModel = new DefaultListModel();
    public JRadioButton yesButton;
    public JRadioButton noButton;
    public JRadioButton undecidedButton;
    // public RelationBuilder relationBuilder;
    public SwingEventsPanel swingEventsPanel;

    public EventBuilderFrame(String title, /* final RelationBuilder relationBuilder,*/  final EventBootstrap bootstrap,
                                final SwingEventsPanel swingEventsPanel) {
        super(title);
        this.bootstrap = bootstrap;
        // this.relationBuilder = relationBuilder;
        this.swingEventsPanel = swingEventsPanel;
        JPanel entitySetPanel = new JPanel(new MigLayout());
        entitySetPanel.setSize(400, 700);

        JLabel rankedListLabel = new JLabel("Bootstrapped patterns");
        entitySetPanel.add(rankedListLabel, "wrap");

        rankedList = new JList(rankedListModel){
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
        ActionListener decisionActionListener = new EventBootstrappingActionListener(this);
        yesButton.addActionListener(decisionActionListener);
        noButton.addActionListener(decisionActionListener);
        undecidedButton.addActionListener(decisionActionListener);

        entitySetPanel.add(listPane, "wrap");
        entitySetPanel.add(decisionPanel, "wrap");

        JPanel actionButtonsPanel = new JPanel(new MigLayout());
        JButton iterateButton = new JButton("Iterate");
        JButton saveButton = new JButton("Save");
        JButton exitButton = new JButton("Exit");
        actionButtonsPanel.add(iterateButton);
        actionButtonsPanel.add(saveButton);
        actionButtonsPanel.add(exitButton);
        entitySetPanel.add(actionButtonsPanel);
        this.add(entitySetPanel);

        // listeners...
        rankedList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int idx = rankedList.getSelectedIndex();
                if (idx < 0) return;
                IceTree e = (IceTree) rankedListModel.getElementAt(idx);
                if (e.getChoice() == IceTree.IceTreeChoice.YES) {
                    yesButton.setSelected(true);
                }
                if (e.getChoice() == IceTree.IceTreeChoice.NO) {
                    noButton.setSelected(true);
                }
                if (e.getChoice() == IceTree.IceTreeChoice.UNDECIDED) {
                    undecidedButton.setSelected(true);
                }
            }
        });

        //
        //  when Iterate button is pressed, collect pattern which have been
        //  labeled YES or NO and start new thread for next bootstrapping iteration
        //
        iterateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<IceTree> approvedPaths = new ArrayList<IceTree>();
                java.util.List<IceTree> rejectedPaths = new ArrayList<IceTree>();
                for (Object o : rankedListModel.toArray()) {
                    IceTree e = (IceTree) o;
                    if (e.getChoice() == IceTree.IceTreeChoice.YES) {
                        approvedPaths.add(e);
                    }
                    else {
                        if (e.getChoice() == IceTree.IceTreeChoice.NO) {
                            rejectedPaths.add(e);
                        }
                    }
                }

                bootstrap.setProgressMonitor(new SwingProgressMonitor(
                        EventBuilderFrame.this, "Bootstrapping",
                        "Collecting seeds...",
                        0,
                        5
                ));

               EventBootstrapIterateThread thread = new EventBootstrapIterateThread(bootstrap,
                        approvedPaths,
                        rejectedPaths,
                        EventBuilderFrame.this
                        );
                thread.start();
            }
        });

        // saveButton:  update events
        //
        saveButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent actionEvent) {
		String eventName = bootstrap.eventName;
		IceEvent iceEvent = Ice.getEvent(eventName);
		for (Object o : rankedListModel.toArray()) {
		    IceTree e = (IceTree) o;
		    if (e.getChoice() == IceTree.IceTreeChoice.YES) {
			iceEvent.addTree(e);
                    }
                    if (e.getChoice() == IceTree.IceTreeChoice.NO) {
			iceEvent.addNegTree(e);
		    }
		}
	    // iceEvent.updateTrees();
	    swingEventsPanel.updateEntriesListModel(iceEvent.getTrees());
	    EventBuilderFrame.this.dispose();
            }
        });

        // handle the click of [Exit]
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {


                EventBuilderFrame.this.dispose();
            }
        });

        // handle the click of [x]
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                EventBuilderFrame.this.dispose();

            }
        });

        // adapters

        rankedList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JList l = (JList)e.getSource();
                ListModel m = l.getModel();
                int index = l.locationToIndex(e.getPoint());
                if( index>-1 ) {
                l.setToolTipText(((IceTree)m.getElementAt(index)).getExample());
                }
            }
        });



        // Key bindings
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("Y"), "YES");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("y"), "YES");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("N"), "NO");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("n"), "NO");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("U"), "UNDECIDED");
        rankedList.getInputMap().put(KeyStroke.getKeyStroke("u"), "UNDECIDED");
        rankedList.getActionMap().put("YES", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                yesButton.doClick();
            }
        });
        rankedList.getActionMap().put("NO", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                noButton.doClick();
            }
        });
        rankedList.getActionMap().put("UNDECIDED", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                undecidedButton.doClick();
            }
        });
    }

    public void updateList() {
        DefaultListModel newListModel = new DefaultListModel();
        for (IceTree s : bootstrap.foundPatterns) {
            newListModel.addElement(s);
        }
        rankedListModel = newListModel;
        rankedList.setModel(rankedListModel);
	rankedList.setCellRenderer(new IceCellRenderer());
	rankedList.setPrototypeCellValue(new IceTree("verb nsubj:subject dobj:object"));
    }

}

class EventBootstrappingActionListener implements ActionListener {
    EventBuilderFrame frame;

    EventBootstrappingActionListener(EventBuilderFrame frame) {
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        int idx = frame.rankedList.getSelectedIndex();
        if (idx < 0) return;
        IceTree e = (IceTree) frame.rankedListModel.getElementAt(idx);
        e.setChoice(IceTree.IceTreeChoice.valueOf(actionEvent.getActionCommand()));
        frame.rankedList.revalidate();
        frame.rankedList.repaint();
    }
}

class EventBootstrapIterateThread extends Thread {
    EventBootstrap bootstrap;
    java.util.List<IceTree> approvedPaths;
    java.util.List<IceTree> rejectedPaths;
    EventBuilderFrame frame;

    EventBootstrapIterateThread(EventBootstrap bootstrap,
                           java.util.List<IceTree> approvedPaths,
                           java.util.List<IceTree> rejectedPaths,
                           EventBuilderFrame frame) {
        this.bootstrap = bootstrap;
        this.approvedPaths = approvedPaths;
        this.rejectedPaths = rejectedPaths;
        this.frame = frame;
    }

    public void run() {
        bootstrap.iterate(approvedPaths, rejectedPaths);
        frame.updateList();
        frame.listPane.validate();
        frame.listPane.repaint();
    }


}
