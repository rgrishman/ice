package edu.nyu.jet.ice.controllers;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.JetEngineBuilder;
import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;
import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.views.Refreshable;
import edu.nyu.jet.ice.views.swing.*;
import edu.nyu.jet.ice.events.SwingEventsPanel; // <<
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.concepts.ConceptHierarchy;
import edu.nyu.jet.concepts.ConceptHierarchyWindow;
import edu.nyu.jet.LoggerFactory;
import net.miginfocom.swing.MigLayout;
import org.ho.yaml.YamlDecoder;
import org.ho.yaml.YamlEncoder;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Nice is a (N)ew Ice GUI in Swing.
 *
 * The main Nice window is a tabbed pane that contains panels that serve actual functionalities
 *
 * @author yhe
 */
public class Nice {
    public static JFrame mainFrame;
    public SwingCorpusPanel corpusPanel;
    public SwingEntitiesPanel entitiesPanel;
    public SwingPathsPanel pathsPanel;
    public SwingEntitySetPanel entitySetPanel;
    public SwingRelationsPanel relationsPanel;
    public SwingEventsPanel eventsPanel;
    public static Nice instance;

    public static void saveStatus(String branch) throws FileNotFoundException {
        File yamlFile = new File(branch + ".yml");
        OutputStream yamlOutputStream = new FileOutputStream(yamlFile);
        YamlEncoder enc = new YamlEncoder(yamlOutputStream);
        enc.writeObject(Ice.corpora);
        enc.writeObject(Ice.entitySets);
        enc.writeObject(Ice.relations);
        enc.writeObject(Ice.events);
        enc.writeObject(Ice.selectedCorpusName);
        enc.close();
    }

    public static void saveStatus() throws FileNotFoundException {
	saveStatus("ice");
    }

    public void setCorpusPanel(SwingCorpusPanel corpusPanel) {
        this.corpusPanel = corpusPanel;
    }

    public void setEntitiesPanel(SwingEntitiesPanel entitiesPanel) {
        this.entitiesPanel = entitiesPanel;
    }

    public void setPathsPanel(SwingPathsPanel pathsPanel) {
        this.pathsPanel = pathsPanel;
    }

    public void setRelationsPanel(SwingRelationsPanel relationsPanel) {
        this.relationsPanel = relationsPanel;
        relationsPanel.setName("relations panel");
    }

    public void setEventsPanel(SwingEventsPanel eventsPanel) {
        this.eventsPanel = eventsPanel;
        eventsPanel.setName("events panel");
    }

    public void setEntitySetPanel(SwingEntitySetPanel entitySetPanel) {
        this.entitySetPanel = entitySetPanel;
	entitySetPanel.setName("entity set panel");
    }

    /**
      *  Saves the current state of the ICE model, including corpora,
      *  entity sets, relations and events, to the yml file.


    public void saveProgress() {
        try {
            File yamlFile = new File("ice.yml");
            OutputStream yamlOutputStream = new FileOutputStream(yamlFile);
            YamlEncoder enc = new YamlEncoder(yamlOutputStream);
            enc.writeObject(Ice.corpora);
            enc.writeObject(Ice.entitySets);
            enc.writeObject(Ice.relations);
            enc.writeObject(Ice.events);
            enc.writeObject(Ice.selectedCorpusName);
            enc.close();
            if (entitiesPanel == null || pathsPanel == null) {
                reassemble();
            }
        } catch (IOException e) {
            System.out.println("Error writing ice.yml.");
        }
    }

    public void export() {
        JetEngineBuilder.build();
    }

    /**
     *  Where everything starts:  the entry point for the entire ICE system.
     *  Performs various initializationa.
     */

    public static void main(String[] args) throws IOException {
        printCover();
        LoggerFactory.setLoggers();
        Properties iceProperties = loadIceProperties();
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setDismissDelay(60000);
        initIce();
        loadPathMatcher(iceProperties);
        String ontologyFileName = iceProperties.getProperty("Ice.Ontology.fileName");
        if (ontologyFileName != null) {
            File ontologyFile = new File(ontologyFileName);
            Ice.ontology = new ConceptHierarchy(ontologyFile);
            new ConceptHierarchyWindow(Ice.ontology, ontologyFile);
        }
        mainFrame = new JFrame();
        mainFrame.setLayout(new MigLayout());
        mainFrame.setTitle("Integrated Customization Environment (ICE) v0.2demo");
        mainFrame.setLocationRelativeTo(null); // center main window

        reassemble();

        // verify that essential files are present
        locateFile("parseprops");
        // locateFile("onomaprops");
    }

    /**
     *  Loads the yaml file, which contains information about the
     *  current state of the model, including corpora, entity sets,
     *  relations and events.
     */

    public static void initIce(String branch) {
        String selectedCorpusName = null;;
        try {
            File yamlFile = new File(branch + ".yml");
            InputStream yamlInputStream = new FileInputStream(yamlFile);
            YamlDecoder dec = new YamlDecoder(yamlInputStream);
            Ice.corpora = new TreeMap((Map) dec.readObject());
            Ice.entitySets = new TreeMap((Map) dec.readObject());
            Ice.relations = new TreeMap((Map) dec.readObject());
            Ice.events = new TreeMap((Map) dec.readObject());
            selectedCorpusName = (String) dec.readObject();
            dec.close();
        } catch (IOException e) {
            System.err.println("Did not load " + branch + ".yml.");
        }
        if (!Ice.corpora.isEmpty()) {
            if (selectedCorpusName != null &&
                Ice.corpora.get(selectedCorpusName) != null)
                Ice.selectCorpus(selectedCorpusName);
            else
                Ice.selectCorpus(Ice.corpora.firstKey());
        }
    }

    public static void initIce() {
	initIce("ice");
    }

    public static void loadPathMatcher(Properties iceProperties) {
        Bootstrap.pathMatcher = new PathMatcher();
        if (iceProperties.getProperty("Ice.DepEmbeddings.fileName") != null) {
            try {
                WordEmbedding.loadWordEmbedding(
                        iceProperties.getProperty("Ice.DepEmbeddings.fileName").trim());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties loadIceProperties() {
        File icePropFile = new File("iceprops");
        Properties iceProperties = new Properties();
        Ice.iceProperties.getProperty("null");
        if (icePropFile.exists() && !icePropFile.isDirectory()) {
            try {
                iceProperties.load(new FileReader(icePropFile));
            }
            catch (Exception e) {
                System.err.println("Unable to load properties file: " + icePropFile.getAbsolutePath());
            }

            for (Object k : iceProperties.keySet()) {
                String val = iceProperties.getProperty((String) k);
                Ice.iceProperties.setProperty((String)k, val);
            }
        }
        return iceProperties;
    }

    /**
     *  If property <i>property</i> was assigned an imteger value
     *  in iceprops, return that value, else returns <i>defaultValue</i>
     */

    public static int iceIntegerProperty (String propertyName, int defaultValue) {
	if (Ice.iceProperties.getProperty(propertyName) != null) {
	    try {
		return Integer.valueOf(Ice.iceProperties.getProperty(propertyName));
	    } catch (NumberFormatException e) {
		return defaultValue;
	    }
	}
	else 
	    return defaultValue;
    }
							        

    /**
      *  If file 'fname' exists either in the working diretory or in the iceHome directory,
      *  return that file, else issue a message and quit.
      */

    public static File locateFile (String fname) {
        File f = new File(fname);
        if (f.exists())
            return f;
        String iceHome = System.getProperty("iceHome");
        if (iceHome == null || iceHome.equals("")) {
            System.err.println("jetHome property not set, cannot proceed");
            System.exit(1);
        }
        f = new File(iceHome, fname);
        if (f.exists())
            return f;
        System.err.println("Cannot locate file " + fname + ", cannot proceed");
        // System.exit(1);
        return null;   // required by the compiler
    }

    public static void printCover() {
        String cover = "                  ___           ___     \n" +
                "    ___          /  /\\         /  /\\    \n" +
                "   /  /\\        /  /:/        /  /:/_   \n" +
                "  /  /:/       /  /:/        /  /:/ /\\  \n" +
                " /__/::\\      /  /:/  ___   /  /:/ /:/_ \n" +
                " \\__\\/\\:\\__  /__/:/  /  /\\ /__/:/ /:/ /\\\n" +
                "    \\  \\:\\/\\ \\  \\:\\ /  /:/ \\  \\:\\/:/ /:/\n" +
                "     \\__\\::/  \\  \\:\\  /:/   \\  \\::/ /:/ \n" +
                "     /__/:/    \\  \\:\\/:/     \\  \\:\\/:/  \n" +
                "     \\__\\/      \\  \\::/       \\  \\::/   \n" +
                "                 \\__\\/         \\__\\/ v0.2\n\n";
        System.err.println(cover);
        String unicodeMessage = "          \u5BD2\u96E8\u9023\u6C5F\u591C\u5165\u5433 \u5E73\u660E\u9001\u5BA2\u695A\u5C71\u5B64\n";
        System.err.println(unicodeMessage);
    }

    public static void reassemble() {
        Container contentPane = mainFrame.getContentPane();
        contentPane.removeAll();
        contentPane.setLayout(new MigLayout());

        Nice nice = new Nice();
        Nice.instance = nice;
        SwingCorpusPanel swingCorpusPanel = new SwingCorpusPanel();
        nice.setCorpusPanel(swingCorpusPanel);
        SwingEntitiesPanel swingEntitiesPanel = null;
        SwingPathsPanel swingPathsPanel = null;
        SwingEntitySetPanel swingEntitySetPanel = null;
        SwingRelationsPanel swingRelationsPanel = null;
        SwingEventsPanel swingEventsPanel = null;
        if (IceUtils.numOfWordCountedCorpora() > 1) {
            swingEntitiesPanel = new SwingEntitiesPanel();
            swingPathsPanel = new SwingPathsPanel();
            swingEntitySetPanel = new SwingEntitySetPanel();
            swingRelationsPanel = new SwingRelationsPanel();
            swingEventsPanel = new SwingEventsPanel();
            nice.setEntitiesPanel(swingEntitiesPanel);
            nice.setPathsPanel(swingPathsPanel);
            nice.setEntitySetPanel(swingEntitySetPanel);
            nice.setRelationsPanel(swingRelationsPanel);
            nice.setEventsPanel(swingEventsPanel);
        }
        assembleTabs(contentPane, swingCorpusPanel, swingEntitiesPanel, swingPathsPanel,
                swingEntitySetPanel, swingRelationsPanel, swingEventsPanel);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new SaveStatusWindowAdapter());
        mainFrame.setMinimumSize(new Dimension(640, 480));
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        mainFrame.validate();
        mainFrame.repaint();
        swingCorpusPanel.refresh();
        if (IceUtils.numOfWordCountedCorpora() == 1) {
            JOptionPane.showMessageDialog(null,
                    "You just processed your first corpus. \n" +
                            "We need a second corpus to use as background.\n"+
                            "Please click [Add...] to add another corpus and\n" +
                            "then [Preprocess].");
        }
    }

    public static void assembleTabs(Container container,
                                    JComponent corpusPanel,
                                    final SwingEntitiesPanel entitiesPanel,
                                    final SwingPathsPanel pathsPanel,
                                    final SwingEntitySetPanel entitySetPanel,
                                    final SwingRelationsPanel relationsPanel,
                                    final SwingEventsPanel eventsPanel) {
        // Nice niceAPI = new Nice();
        final JTabbedPane tabbedPane = new JTabbedPane();

        //JComponent corpusPanel = new SwingCorpusPanel(niceAPI);
        tabbedPane.addTab("Corpus", null, corpusPanel,
                "Basic information about the corpus");
        corpusPanel.setMinimumSize(new Dimension(600, 420));
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        if (IceUtils.numOfWordCountedCorpora() > 1) {
            tabbedPane.addTab("Entities", null, entitiesPanel,
                    "Collect entities in the corpus to support entity set construction");
            entitiesPanel.setMinimumSize(new Dimension(600, 420));
            tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

            JComponent panel3 = entitySetPanel;
            tabbedPane.addTab("Entity sets", null, panel3,
                    "Add or edit entity sets");
            panel3.setMinimumSize(new Dimension(600, 420));
            tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

            tabbedPane.addTab("Phrases", null, pathsPanel,
                    "Collect dependency paths in the corpus to support relation construction");
            pathsPanel.setMinimumSize(new Dimension(600, 420));
            tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

            JComponent panel5 = relationsPanel;// makeTextPanel(
            // "Panel #5 (has a preferred size of 410 x 50).");
            tabbedPane.addTab("Relations", null, panel5,
                    "Add or edit relations");
            panel5.setMinimumSize(new Dimension(600, 420));
            tabbedPane.setMinimumSize(new Dimension(620, 440));
            tabbedPane.setMnemonicAt(4, KeyEvent.VK_5);

            JComponent panel6 = eventsPanel;
            // "Panel #5 (has a preferred size of 410 x 50).");
            tabbedPane.addTab("Events", null, panel6,
                    "Add or edit events");
            panel6.setMinimumSize(new Dimension(600, 420));
            tabbedPane.setMinimumSize(new Dimension(620, 440));
            tabbedPane.setMnemonicAt(5, KeyEvent.VK_6);
        }

        container.add(tabbedPane);

        // Listeners

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                if (tabbedPane.getSelectedComponent() instanceof Refreshable) {
                    ((Refreshable)tabbedPane.getSelectedComponent()).refresh();
                }
            }
        });
        //container.setMinimumSize(new Dimension(515, 450));
    }

    static class SaveStatusWindowAdapter extends WindowAdapter {
        public void windowClosing(WindowEvent e)
        {
            JFrame frame = (JFrame)e.getSource();

            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Do you want to save the current status of Ice before exit?",
                    "Exit Ice",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    saveStatus();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
