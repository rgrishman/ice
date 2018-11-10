package edu.nyu.jet.ice.events;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.events.EventBuilderFrame;
import edu.nyu.jet.ice.events.EventBuilderThread;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.events.EventBootstrap;
import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.views.Refreshable;
import edu.nyu.jet.ice.views.swing.SwingIceStatusPanel;
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
 * Panel to bootstrap event extractor
 *
 * @author yhe
 */
public class SwingEventsPanel extends JPanel implements Refreshable {
    JList eventList;
    JList entriesList;
    DefaultListModel eventListModel;
    DefaultListModel entriesListModel;
    IceEvent currentEvent;
    SwingIceStatusPanel iceStatusPanel;
    // public List<String> negTrees = new ArrayList<String>();
    public Map<String, List<String>> negTrees = new TreeMap<String, List<String>>();
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
    public SwingEventsPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(210, 366));
        leftPanel.setMaximumSize(new Dimension(210, 366));
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border leftTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Events");
        leftPanel.setBorder(leftTitleBorder);
        JLabel entitySetLabel = new JLabel("Events");

        this.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setOpaque(false);
        rightPanel.setMinimumSize(new Dimension(266, 366));
        rightPanel.setMaximumSize(new Dimension(266, 366));
        Border rightTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Members");
        rightPanel.setBorder(rightTitleBorder);
        this.add(rightPanel);
        eventListModel = new DefaultListModel();
        for (String k : Ice.events.keySet()) {
            eventListModel.addElement(Ice.events.get(k));
        }
        eventList = new JList();

        JScrollPane entitySetPane = new JScrollPane(eventList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(170, 228);
        eventList.setSize(170, 228);
        eventList.setMinimumSize(new Dimension(170, 228));
        entitySetPane.setPreferredSize(new Dimension(170, 228));
        leftPanel.add(entitySetPane, "span");
        eventList.setModel(eventListModel);

        JButton addEventButton = new JButton("Add");
        addEventButton.setName("addEventButton");
        leftPanel.add(addEventButton);
        JButton suggestEventButton = new JButton("Suggest");
        leftPanel.add(suggestEventButton, "wrap");
        JButton deleteEventButton = new JButton("Delete");
        leftPanel.add(deleteEventButton);

        entriesList   = new JList();

        entriesList.setSize(235, 228);
        entriesList.setMinimumSize(new Dimension(235, 228));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(235, 228);
        entriesSetPane.setMinimumSize(new Dimension(235, 228));
        rightPanel.add(entriesSetPane, "span");
        JButton addEntryButton = new JButton("Add");
        addEntryButton.setName("addEventMemberButton");
        rightPanel.add(addEntryButton);
        JButton expandEntriesButton = new JButton("Expand");
        rightPanel.add(expandEntriesButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton);
        entriesSetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        iceStatusPanel = new SwingIceStatusPanel();
        this.add(iceStatusPanel);

        // listeners

        eventList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                refresh();
            }
        });

        addEventButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String eventName = JOptionPane.showInputDialog("Name of the event");
                if (eventName == null) {
                    return;
                }
                eventName = eventName.toUpperCase();
                for (Object existingEvent : eventListModel.toArray()) {
                    if (existingEvent.toString().equals(eventName)) {
                        JOptionPane.showMessageDialog(SwingEventsPanel.this,
                                "The name is already in use for another event.",
                                "Event Name Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                String eventInstance = JOptionPane.showInputDialog(
                        "Please provide an example of the event");
                if (eventInstance == null) {
                    return;
                }
                List<IceTree> trees = findTreeOrClosest (eventInstance);
                if (trees != null) {
                    installSeedEvent(eventName, eventInstance, trees);
                }
            }
        });

        suggestEventButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.Timer timer = switchToBusyCursor(SwingEventsPanel.this);
                String eventInstance = null;
                try {
                    eventInstance = suggestEvent();
                }
                finally {
                    switchToNormalCursor(SwingEventsPanel.this, timer);
                }
                if (eventInstance != null) {
                    int n = JOptionPane.showConfirmDialog(
                            SwingEventsPanel.this,
                            String.format("Do you want to start with path [%s]?", eventInstance),
                            "Path Suggestion",
                            JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
                        String eventName = JOptionPane.showInputDialog("Name of the event");
                        if (eventName == null) {
                            return;
                        }
                        eventName = eventName.toUpperCase();
                        for (Object existingEvent : eventListModel.toArray()) {
                            if (existingEvent.toString().equals(eventName)) {
                                JOptionPane.showMessageDialog(SwingEventsPanel.this,
                                        "The name is already used for another event.",
                                        "Event Name Error",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        DepTreeMap depTreeMap = DepTreeMap.getInstance();
                        List<IceTree> trees = depTreeMap.findTree(eventInstance);
                        if (trees == null) {
                            JOptionPane.showMessageDialog(SwingEventsPanel.this,
                                    "The provided path does not appear in the corpus.",
                                    "Dependency Tree Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        installSeedEvent(eventName, eventInstance, trees);
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
                buildEvent(currentEvent.getName(), b.toString());
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                String eventInstance = JOptionPane.showInputDialog(
                        "Please provide an example of the event");
                if (eventInstance == null) {
                    return;
                }
                List<IceTree> trees = findTreeOrClosest (eventInstance);
                if (trees == null) {
                    return;
                }
                for (Object entry : entriesListModel.toArray()) {
                    if (entry.toString().equals(eventInstance)) {
                        JOptionPane.showMessageDialog(SwingEventsPanel.this,
                                "The provided path exists in the path set.",
                                "Dependency Tree Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                entriesListModel.addElement(closestPhrase);
		currentEvent.addRepr(closestPhrase);
                currentEvent.addTrees(trees);
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                String repr = (String) entriesListModel.elementAt(idx);
                entriesListModel.remove(idx);
		currentEvent.removeRepr(repr);
		currentEvent.updateTrees();
            }
        });

        deleteEventButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (eventListModel == null) return;
                int idx = eventList.getSelectedIndex();
                if (idx < 0) return;
                IceEvent e = (IceEvent) eventListModel.get(idx);
                int n = JOptionPane.showConfirmDialog(
                        SwingEventsPanel.this,
                        String.format("Are you sure you want to remove the event [%s]?", e.getName()),
                        "Confirm event deletion",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    eventListModel.remove(idx);
                    entriesListModel.clear();
                    entriesListModel = null;
                    currentEvent = null;
                }
            }
        });

    }

    public String closestPhrase;

    /**
     *  Finds the LDPs which corresponds to the English phrase 'eventInstance'. If
     *  'relationInstance' is not derived from any LDP in the corpus, search for the
     *  most similar phrase (in edit distance) which is derived from an LDP in the
     *  corpus, ask the user if they want to use this phrase instead, and return
     *  the LDPs associated with this phrase.
     *  <br>
     *  As a side effect, sets 'closestPhrase' to the corresponding English phrase.
     */

    public List<IceTree> findTreeOrClosest (String eventInstance) {
        DepTreeMap depTreeMap = DepTreeMap.getInstance();
        depTreeMap.load();
        List<IceTree> trees = depTreeMap.findTree(eventInstance);
        if (trees != null) {
            closestPhrase = eventInstance;
            return trees;
        }
        closestPhrase = depTreeMap.findClosestTree(eventInstance);
        if (closestPhrase == null) {
            System.err.println("*** findTreeOrClosest failed.");
            System.err.println("*** eventInstance = " + eventInstance);
            return null;
        }
        int n = JOptionPane.showConfirmDialog(
                SwingEventsPanel.this,
                String.format("No example in the corpus for [%s]\nDo you want to use [%s]?", 
                    eventInstance, closestPhrase),
                "Tree Suggestion",
                JOptionPane.YES_NO_OPTION);
        if (n == JOptionPane.NO_OPTION) 
            return null;
        trees = depTreeMap.findTree(closestPhrase);
        if (trees == null) {
            System.err.println("*** findTreeOrClosest failed.");
            return null;
        }
        return trees;
    }

    /**
     *  Creates a new IceEvent.  This method is invoked in response to
     *  the AddEvent and Suggest buttons.
     *
     *  @param  eventName        the name of the event
     *  @param  eventInstance    an ERnglish phrase representing the relatio
     *  @param  trees            a list of dependency trees for the relation
     */

    private void installSeedEvent(String eventName, String eventInstance, List<IceTree> trees) {
        IceEvent iceEvent = new IceEvent(eventName);
	Ice.addEvent(iceEvent);
	iceEvent.addRepr(eventInstance);

        iceEvent.setTrees(trees);
        eventListModel.addElement(iceEvent);
        eventList.setSelectedValue(iceEvent, true);
        // selecting new element forces refresh
        currentEvent = iceEvent;
    }

    /**
     *  Use bootstrapping to build a model for a relation.  This
     *  method is invoked by pressing the 'EXPAND' button on the
     *  relation panel.
     *
     *  @param  inName  the name of the event
     *  @param  inSeed  the seed for bootstrapping this event,
     *                  consisting of one or more Englist expressions
     *                  separated by ':::'
     */

    public void buildEvent(String inName, String inSeed) {

        String name = inName;

        String pathListFileName = "";  // not used anyway
        ProgressMonitorI  progressMonitorI = new SwingProgressMonitor(
                Ice.mainFrame, "Initializing Event Builder",
                "Collecting seeds...",
                0,
                5
        );
        String bootstrapperName =
                Ice.iceProperties.getProperty("Ice.Bootstrapper.name") != null ?
                        Ice.iceProperties.getProperty("Ice.Bootstrapper.name") : "Bootstrap";
        EventBootstrap bootstrap = EventBootstrap.makeBootstrap(bootstrapperName, progressMonitorI, name);
        EventBuilderFrame frame = new EventBuilderFrame("Bootstrap events",
        //        null,
                bootstrap,
                SwingEventsPanel.this);

        frame.setSize(400, 580);
        frame.setAlwaysOnTop(true);
        String seed = inSeed;
	System.out.println("Ice.selectedCorpus.eventInstanceFileName = " + Ice.selectedCorpus.eventInstanceFileName);
	System.out.println("Ice.selectedCorpus= " + Ice.selectedCorpus);
        EventBuilderThread builder = new EventBuilderThread(
                seed,
		FileNameSchema.getEventsFileName(Ice.selectedCorpusName),
                // "cache/ACE-nw/Events", //Ice.selectedCorpus.eventInstanceFileName,
                pathListFileName,
                null,
                bootstrap,
                frame,
                SwingEventsPanel.this);
        builder.start();
    }

    /**
     *  Refreshes the display of the currently selected event.
     */

    public void refresh() {
        iceStatusPanel.refresh();
        int idx = eventList.getSelectedIndex();
        if (idx < 0) return;
        IceEvent event = (IceEvent)eventListModel.getElementAt(idx);
        if (Ice.events.containsKey(event.getName())) {
            event = Ice.events.get(event.getName());
            eventListModel.setElementAt(event, idx);
            eventList.setSelectedIndex(idx);
        }
        currentEvent = event;
        DefaultListModel newListModel = new DefaultListModel();
        DepTreeMap depTreeMap = DepTreeMap.getInstance();
        depTreeMap.load();
        for (IceTree tree : event.getTrees()) {
             //String repr = depTreeMap.findRepr(tree);
	    String repr = tree.getRepr();
	    if (repr == null)
                System.err.println("Repr not found for path:" + tree);
	    else if (! newListModel.contains(repr)) {
                newListModel.addElement(repr);
            }
        }

        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    public void updateEntriesListModel(java.util.List<IceTree> trees) {
        DefaultListModel newListModel = new DefaultListModel();
        for (IceTree tree : trees) {
	    String repr = tree.getRepr();
	    if (! newListModel.contains(repr)) {
                newListModel.addElement(repr);
	    }
        }
        entriesListModel = newListModel;
        entriesList.setModel(entriesListModel);
    }

    /**
     *  Suggests a path to use as a seed for building a new event.  It selects
     *  a path which is a sentential (subject-verb-object) structure, has not
     *  been suggested before (for this corpus), and is not a member of an
     *  existing relation.
     */

    public String suggestEvent() {
        String result = "";
        try {
            String[] lines =
                    IceUtils.readLines(FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName));
            DepTreeMap depTreeMap = DepTreeMap.getInstance();
            depTreeMap.load();
	    /*
      loop: for (String line : lines) {
                String[] parts = line.split("\t");
                String path = parts[1];
                if (!path.matches(".*nsubj-1:.*:dobj.*")) 
                    continue;
                String phrase = depTreeMap.findRepr(path);
                if (phrase == null)
                    continue;
                // skip phrase if prevoously suggested
                if (Ice.selectedCorpus.eventsSuggested.contains(phrase))
                    continue;
                // skip phrase if it is part of an existing event
                for (IceEvent event : Ice.events.values()) {
                    for (String repr : event.getReprs()) {
                        if (phrase.equals(repr)) {
                            continue loop;
                        }
                    }
                }
                Ice.selectedCorpus.eventsSuggested.add(phrase);
                return phrase;
            }
	    */
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
