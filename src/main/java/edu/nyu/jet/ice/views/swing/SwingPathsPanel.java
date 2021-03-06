package edu.nyu.jet.ice.views.swing;  // -*- tab-width: 4 -*-

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.RelationFinder;
import edu.nyu.jet.ice.models.IcePathFactory;;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.uicomps.RelationFilter;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;
import edu.nyu.jet.ice.events.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Panel that extracts dependency paths or trees from the current corpus
 * These are displayed in a scrolling window and also made available as
 * files in the cache. 
 * 
 * @author yhe
 */

public class SwingPathsPanel extends JPanel implements Refreshable {

    private SwingIceStatusPanel iceStatusPanel;
    private JList patternList;
    private boolean events;
    JTextField filterTextField = new JTextField(30);

/**
 *  Create panel.
 */

    public SwingPathsPanel() {
	super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        this.removeAll();
        JPanel patternBox = new JPanel(new MigLayout());
        patternBox.setOpaque(false);
        patternBox.setMinimumSize(new Dimension(480, 366));
        TitledBorder border = new TitledBorder("Phrases");
        patternBox.setBorder(border);
        this.add(patternBox);

	patternList = new JList();
        JScrollPane scrollPane = new JScrollPane(patternList);
	scrollPane.setSize(new Dimension(350, 360));
	scrollPane.setPreferredSize(new Dimension(350, 360));
	scrollPane.setMinimumSize(new Dimension(350, 360));
	scrollPane.setMaximumSize(new Dimension(350, 360));

        patternBox.add(scrollPane, "span");
        JRadioButton relationsButton = new JRadioButton("relations");
        JRadioButton eventsButton = new JRadioButton("events");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(relationsButton);
        buttonGroup.add(eventsButton);
        
        patternBox.add(relationsButton);
        patternBox.add(eventsButton, "wrap");

        JLabel filterLabel = new JLabel("containing");
        filterTextField = new JTextField(25);
        patternBox.add(filterLabel);
        patternBox.add(filterTextField);

    relationsButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		ProgressMonitorI progressMonitor = 
		new SwingProgressMonitor(Ice.mainFrame, "Extracting relation phrases",
		    "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
		events = false;
        checkForAndFindRelations(progressMonitor);
		}});

	eventsButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		ProgressMonitorI progressMonitor = 
		new SwingProgressMonitor(Ice.mainFrame, "Extracting events phrases",
		    "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
		events = true;
		checkForAndFindRelations(progressMonitor);
		}});

	filterTextField.addActionListener (new ActionListener () {
		public void actionPerformed(ActionEvent e) {
		ProgressMonitorI progressMonitor = 
		new SwingProgressMonitor(Ice.mainFrame, "Extracting event phrases",
		    "Initializing Jet", 0, Ice.selectedCorpus.numberOfDocs + 30);
		checkForAndFindRelations(progressMonitor);
		// PathExtractionThread thread = new PathExtractionThread(true,
		    // events, filterTextField.getText(), patternList, progressMonitor);
		// thread.start();
		}});
	
	patternList.addMouseMotionListener(new MouseMotionAdapter() {
		@Override
		public void mouseMoved(MouseEvent e) {
		JList l = (JList)e.getSource();
		ListModel m = l.getModel();
		int index = l.locationToIndex(e.getPoint());
        if( index > -1 ) {
		    String repr = (String) m.getElementAt(index);
            int i = repr.indexOf("\t");
            if (i >= 0) {
                repr = repr.substring(i + 1);
            }
            String example = "";;
            if (events) {
                java.util.List<IceTree> trees = DepTreeMap.findTree(repr);
                if (trees != null && trees.size() > 0) {
                    IceTree tree = trees.get(0);
                    example = tree.getExample();
                }
            } else {
                java.util.List<IcePath> paths = DepPathMap.findPath(repr);
                if (paths != null && paths.size() > 0) {
                    IcePath path = paths.get(0);
                    example = path.getExample();
                }
            }
			String toolTip = IceUtils.splitIntoLine(example, 80);
			toolTip = "<html>" + toolTip.replaceAll("\\n", "<\\br>");
			l.setToolTipText(toolTip);
		    }
		} });
	iceStatusPanel = new SwingIceStatusPanel(); 
	this.add(iceStatusPanel);
	fullRefresh();
	}

    public void refresh() {
        fullRefresh();
    }

    public void fullRefresh() {
        iceStatusPanel.refresh();
    }

    /** Select file 'file' to display.
      */

    public void checkForAndFindRelations(ProgressMonitorI progressMonitor) {
        String relationsFileName = 
            FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);    //name + "Relations";
        String relationTypesFileName = 
            FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName);//name + "RelationTypes";
        String eventsFileName = 
            FileNameSchema.getEventsFileName(Ice.selectedCorpusName);       //name + "Events";
        String eventTypesFileName = 
            FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName);   //name + "EventTypes";
        String instanceFileName = events ? eventsFileName : relationsFileName;
        String typesFileName = events ? eventTypesFileName : relationTypesFileName;
        File file = new File(typesFileName);
        boolean fileExists = file.exists() && !file.isDirectory();
        boolean shouldReuse = false;
        if (preprocessedTextsAvailable(Ice.selectedCorpusName)) {
            if (fileExists) {
                int n = JOptionPane.showConfirmDialog(
                        Ice.mainFrame,
                        "Extracted patterns already exist. Show existing patterns without recomputation?",
                        "Patterns exist",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) { // reuse existing paths
                    shouldReuse = true;
                }
            }
        } else {
            if (fileExists) {
                shouldReuse = true;
            } else {
                 JOptionPane.showMessageDialog(Ice.mainFrame, "Source text not available, cannot rebuild pattern set");
            }
        }
        PathExtractionThread thread = new PathExtractionThread(shouldReuse,
                events, filterTextField.getText(), patternList, progressMonitor);
        thread.start();
    }

    public static boolean preprocessedTextsAvailable (String corpusName) {
        return (new File(FileNameSchema.getPreprocessCacheDir(corpusName))).exists();
    }
}

/**
  *  Thread which displays top-ranked patterns (paths or trees)
  */

class PathExtractionThread extends Thread {

    private boolean shouldReuse = true;
    private ProgressMonitorI progressMonitor;
    private String filter = "";;
    private JList patternList;
    private boolean   events;
    private static final int SIZE_LIMIT = 100;

    public PathExtractionThread(boolean shouldReuse,
            boolean events,
            String filter,
            JList patternList,
            ProgressMonitorI progressMonitor) {
        this.shouldReuse = shouldReuse;
        this.events = events;
        this.filter = filter;
        this.patternList    = patternList;
        this.progressMonitor = progressMonitor;
    }

    @Override
    public void run() {

// recompute files to display (using RelationFinder or EventFinder)  

    if (!shouldReuse) {
	    if (!events) {
		RelationFinder finder = new RelationFinder(
			Ice.selectedCorpus.docListFileName, Ice.selectedCorpus.directory,
			Ice.selectedCorpus.filter, FileNameSchema.getRelationsFileName(Ice.selectedCorpusName),
			FileNameSchema.getRelationTypesFileName(Ice.selectedCorpusName), null,
			Ice.selectedCorpus.numberOfDocs,
			progressMonitor);
		finder.run();
		Ice.selectedCorpus.relationsFileName =
		    FileNameSchema.getRelationsFileName(Ice.selectedCorpusName);
		Ice.selectedCorpus.relationTypesFileName =
		    FileNameSchema.getRelationTypesFileName(Ice.selectedCorpus.name);
	    } else /* events */ {
		EventFinder finder = new EventFinder(
			Ice.selectedCorpus.docListFileName, Ice.selectedCorpus.directory,
			Ice.selectedCorpus.filter, FileNameSchema.getEventsFileName(Ice.selectedCorpusName),
			FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName), null,
			Ice.selectedCorpus.numberOfDocs,
			progressMonitor);
		finder.run();
		Ice.selectedCorpus. eventsFileName = 
		    FileNameSchema.getEventsFileName(Ice.selectedCorpusName);
		Ice.selectedCorpus. eventTypesFileName = 
		    FileNameSchema.getEventTypesFileName(Ice.selectedCorpusName);
	    }
	}
        progressMonitor.setNote("Postprocessing...");

        // rank paths
        String fileWithRankings;
        if (!events) {
            fileWithRankings = Ice.selectedCorpus.rankRelations();
            DepTreeMap depTreeMap = DepTreeMap.getInstance();
            depTreeMap.loadTrees(false);
        } else /* events */ {
            fileWithRankings = Ice.selectedCorpus.rankEvents(); 
            DepPathMap depPathMap = DepPathMap.getInstance();
            depPathMap.loadPaths(false);
        }

    // filter and show paths
	//   
    try {
        BufferedReader reader = new BufferedReader(new FileReader(fileWithRankings));
        int k = 0;
        Vector<String> pathsToDisplay = new Vector<String>();
        while (k <= SIZE_LIMIT) {
            String line = reader.readLine();
            if (line == null) break;
            String[] parts = line.split("\\t");
            if (parts.length < 2) {
                continue;
            }
            String repr;
            if (events) {
                IceTree it = IceTreeFactory.getIceTree(parts[1]);
                if (it.getTrigger().equals("?")) continue;
                repr = it.getRepr();
            } else /* relation */ {
                IcePath ip = IcePathFactory.getIcePath(parts[1]);
                repr = ip.getRepr();
            }
            if (repr == null) continue;
            if (!filter.equals("") && repr.indexOf(filter) < 0)
                continue;
            pathsToDisplay.add(parts[0].trim() + "\t" + repr);
            k++;
        }
        reader.close();
        if (progressMonitor == null || !progressMonitor.isCanceled()) {
            patternList.setListData(pathsToDisplay);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    progressMonitor.setProgress(progressMonitor.getMaximum());
    }
}
