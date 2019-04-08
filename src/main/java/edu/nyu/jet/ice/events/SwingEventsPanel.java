package edu.nyu.jet.ice.events;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.IceCellRenderer;
import edu.nyu.jet.ice.events.EventBuilderFrame;
import edu.nyu.jet.ice.events.EventBuilderThread;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.models.WordEmbedding;
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

import org.apache.commons.math3.ml.clustering.*;
import org.apache.commons.math3.ml.distance.*;

/**
 * Panel to bootstrap event extractor.
 *
 * The event extractor maintains a list of event types and, for each
 * event type, a list of trees expressing that event type.  This 
 * information is maintained in two versions:  a persistent version
 * (maintained across ICE sesssions) and a transient version
 * (used to drive the GUI and interpret seletion actions).
 *
 * The persistent version is stored as a set of IceEvents within
 * class Ice and a set of IceTrees within each instance of
 * IceEvent.
 *
 * The transient version is stored in two DefaultListModels,
 * eventListModel (a list of IceEvents) and entriesListModel,
 * a list of the Trees in the currently selected IceEvent.
 *
 */

public class SwingEventsPanel extends JPanel implements Refreshable {

    JList eventList;
    JList entriesList;
    DefaultListModel<IceEvent> eventListModel;
    DefaultListModel<IceTree> entriesListModel;
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

        eventList = new JList();
        entriesList = new JList();
        eventListModel = new DefaultListModel<IceEvent>();
        eventList.setModel(eventListModel);
        entriesListModel = new DefaultListModel<IceTree>();
        entriesList.setModel(entriesListModel);
        for (String k : Ice.events.keySet()) {
            eventListModel.addElement(Ice.events.get(k));
        }
        entriesList.setCellRenderer(new IceCellRenderer(false));
        updateEntriesListModel();

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(210, 366));
        leftPanel.setMaximumSize(new Dimension(210, 366));
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border leftTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Events");
        leftPanel.setBorder(leftTitleBorder);
        JLabel eventLabel = new JLabel("Events");

        this.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setOpaque(false);
        rightPanel.setMinimumSize(new Dimension(266, 366));
        rightPanel.setMaximumSize(new Dimension(266, 366));
        Border rightTitleBorder = BorderFactory.createTitledBorder(lowerEtched, "Members");
        rightPanel.setBorder(rightTitleBorder);
        this.add(rightPanel);

        JScrollPane entitySetPane = new JScrollPane(eventList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(170, 228);
        eventList.setSize(170, 228);
        eventList.setMinimumSize(new Dimension(170, 228));
        entitySetPane.setPreferredSize(new Dimension(170, 228));
        leftPanel.add(entitySetPane, "span");

        JButton addEventButton = new JButton("Add");
        addEventButton.setName("addEventButton");
        leftPanel.add(addEventButton);
        JButton suggestEventButton = new JButton("Suggest");
        leftPanel.add(suggestEventButton, "wrap");
        JButton deleteEventButton = new JButton("Delete");
        leftPanel.add(deleteEventButton);

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
            iceStatusPanel.refresh();
            int idx = eventList.getSelectedIndex();
            if (idx < 0) return;
            currentEvent = eventListModel.get(idx);
            updateEntriesListModel();
            }
        });

        addEventButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String eventName = JOptionPane.showInputDialog("Name of the event");
                if (eventName == null) {
                    return;
                }
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
                eventInstance = DepTreeMap.normalizeRepr(eventInstance);
                List<IceTree> trees = findTreeOrClosest(eventInstance);
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
                    suggestEvent();
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
                        eventInstance = DepTreeMap.normalizeRepr(eventInstance);
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
                    IceTree entry = entriesListModel.getElementAt(i);
                    String repr = entry.getRepr();
                    if (repr == null) continue;
                    if (i > 0) {
                        b.append(":::");
                    }
                    b.append(repr);
                }
                buildEvent(currentEvent.getName(), b.toString());
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = eventList.getSelectedIndex();
                if (idx < 0) {
                    JOptionPane.showMessageDialog(SwingEventsPanel.this,
                        "No event type selected.",
                        "Dependency Tree Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                IceEvent iceEvent = eventListModel.get(idx);
                String eventInstance = JOptionPane.showInputDialog(
                        "Please provide an additional example of the event");
                if (eventInstance == null) {
                    return;
                }
                eventInstance = DepTreeMap.normalizeRepr(eventInstance);
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
                // for persistent recprd
                currentEvent.addTrees(trees);
                // for GUI
                entriesListModel.addElement(trees.get(0));
                updateEntriesListModel();
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                IceTree tree = entriesListModel.elementAt(idx);
                // for persistent record
                currentEvent.removeTree(tree);
                // for GUI
                entriesListModel.remove(idx);
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
                    // for persistent record
                    Ice.removeEvent(e.getName());
                    // for GUI
                    eventListModel.remove(idx);
                    updateEntriesListModel();
                }
            }
        });

    }

    public void updateEntriesListModel () {
        entriesListModel.clear();
        int idx = eventList.getSelectedIndex();
        if (idx < 0) return;
        IceEvent iceEvent = (IceEvent) eventListModel.get(idx);
        List<String> reprs = new ArrayList<String>();
        for (IceTree it : iceEvent.getTrees()) {
            String repr = it.getRepr();
            if (! reprs.contains(repr)) {
                entriesListModel.addElement(it);
                reprs.add(repr);
            }

        }
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
        eventInstance = DepTreeMap.normalizeRepr(eventInstance);
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
        //  for persistent store
        IceEvent iceEvent = new IceEvent(eventName);
        Ice.addEvent(iceEvent);
        iceEvent.setTrees(trees);
        // for GUI
        eventListModel.addElement(iceEvent);
        eventList.setSelectedValue(iceEvent, true);
        updateEntriesListModel();
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
    }

    
    /**
     *  Suggests a pair of trees to use as a seed for building a new event.  It is
     *  intended to select a pair which 
     *  - have different triggers
     *  - have not already been used in an event for this corpus [not yet implemented]
     *  - have not been suggested before (for this corpus)
     *
     *  For efficiency on large corpora, the set of IceTrees is divided
     *  using k-means clustering, and then only checking closeness within a cluster.
     */

    public void suggestEvent() {
        String fileName = FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName);
        IceTreeSet eventtypes = new IceTreeSet(fileName, 5);
        List<IceTree> trees = eventtypes.list;
        // among set of event trees,
        // find closest pairs of trees with different triggers, 
        IceTree besti = null;
        IceTree bestj = null;
        double bestSimilarity = -1;
        int numClusters = trees.size() / 1000 + 1;
        System.out.println("Generating " + numClusters + " clusters");
        KMeansPlusPlusClusterer<IceTree> cl = 
            new KMeansPlusPlusClusterer(numClusters, 10, new EuclideanDistance());
        List<CentroidCluster<IceTree>> clusters = cl.cluster(trees);
        for (CentroidCluster<IceTree> cc : clusters) {
            List<IceTree> points  = cc.getPoints();
            System.out.println("Processing a cluster of "+points.size()+" points");
            for (int i = 0; i < points.size(); i++) {
                for (int j = 0; j < i; j++) {
                    if (points.get(i).trigger.equals(points.get(j).trigger)) continue;
                    // skip phrase if prevoously suggested
                    if (Ice.selectedCorpus.eventsSuggested.contains(points.get(i).getRepr()))
                        continue;
                    if (Ice.selectedCorpus.eventsSuggested.contains(points.get(j).getRepr()))
                        continue;
                    /*
                    // skip if either tree is part of an existing event
                    for (IceEvent event : Ice.events.values()) {
                        for (String repr : event.getReprs()) {
                            if (phrase.equals(repr)) {
                                continue;
                            }
                        }
                    }
                    */
                    double similarity = WordEmbedding.treeSimilarity(points.get(i), points.get(j));
                    if (similarity > bestSimilarity) {
                        besti = points.get(i);
                        bestj = points.get(j);
                        bestSimilarity  = similarity;
                    }
                }
            }
        }
        // select pair with max salience
        String one = besti.getRepr();
        String two = bestj.getRepr();
        System.out.println("We suggest adding event " + one + " with alternate " + two);
        Ice.selectedCorpus.eventsSuggested.add(one);
        Ice.selectedCorpus.eventsSuggested.add(two);
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
/* -------------------------------
To do for clustering and suggesions:
    // 1. convert IceTrees to a set of Clusters
    // 2. define distanc measure = WE distance or shared arg distance
    // 3. cluster into sqrt(n) clusters
    // 4. compare closest in entire corpus with closest in cluster

    public void suggestEvent() {
        String fileName = FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName);
        IceTreeSet eventtypes = new IceTreeSet(fileName);
        List<IceTree> trees = eventtypes.list;
        KMeansPlusPlusClusterer<IceTree> cl = new KMeansPlusPlusClusterer(100, 10, new EuclideanDistance());
        List<CentroidCluster<IceTree>> clusters = cl.cluster(trees);
        for (CentroidCluster<IceTree>  cc : clusters) {
            for (IceTree it : cc.getPoints())
                System.out.println("  " + it.toString());
        System.out.println("=========================");
        }
       
    }
*/
}
