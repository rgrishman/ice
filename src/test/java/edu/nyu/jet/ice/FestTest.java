package edu.nyu.jet.ice;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

import edu.nyu.jet.ice.models.JetEngineBuilder;
import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.ice.views.swing.SwingCorpusPanel;

// import org.fest.swing.*;
import org.fest.swing.core.*;
import org.fest.swing.finder.*;
import org.fest.swing.fixture.*;
import org.junit.*;
import static org.junit.Assert.assertTrue;

/**
 *  System-level tests of ICE using FEST
 *
 *  Initial tests:  defining new relations
 */

public class FestTest {
 
	private FrameFixture demo;
        JPanelFixture relationsPanel;
	
        @Before
 	public void setUp() throws IOException, InterruptedException {
	    //
	    // start up
	    //
            if (!(new File("ace.yml").exists())) {
                System.out.println ("ace.yml file missing");
                System.exit(-1);
            }
            System.out.println("Copying ace.yml to ice.yml");
            Process p = Runtime.getRuntime().exec("cp ace.yml ice.yml");
            p.waitFor();
	    System.out.println("Starting Nice.");
	    Nice.main(new String[0]);
	    System.out.println("Finished main.");
	    demo = new FrameFixture(Nice.mainFrame);
	    System.out.println("Created demo.");
	    demo.show();
	    System.out.println("Showing demo.");
            demo.target.toFront();
        }

        @After
        public void tearDown() {
            demo.cleanUp();
        }

        @Test
        public void test(){
	    
	    JTabbedPaneFixture tabbedPane = findTabbedPane();
	    System.out.println("Found tabbed pane " + tabbedPane);
	    tabbedPane.selectTab("Relations");
	    System.out.println("Selected relations tab");
	    relationsPanel = findPanel("relations panel");
	    System.out.println("Found relations panel " + relationsPanel);
            //
            //  define new relation:  different types
            //
            addRelation ("CITIZEN", "GPE PERSON");
            addRelationMember ("PERSON of GPE");
	    JButtonFixture saveRelationButton = findButtonNamed("saveRelationButton");
	    saveRelationButton.click();
	    System.out.println("Clicked saveRelationButton " + saveRelationButton);
            //
            //  define new relation:  same type
            //
            addRelation ("PAIR", "PERSON(1) PERSON(2)");
            addRelationMember ("PERSON(2) , PERSON(1)");
	    saveRelationButton.click();
	    System.out.println("Clicked saveRelationButton " + saveRelationButton);
            //
            //  save relations, export, and exit
            //
	    JButtonFixture saveRelationsButton = findButtonNamed("saveRelationsButton");
	    saveRelationsButton.click();
	    System.out.println("Clicked saveRelationsButton " + saveRelationsButton);
	    JOptionPaneFixture jOptionPane = JOptionPaneFinder.findOptionPane().using(demo.robot);
	    System.out.println("Found jOptionPane " + jOptionPane + " (message)");
	    JButtonFixture button2 = jOptionPane.okButton();
	    button2.click();
	    System.out.println("Clicked OK button " + button2);
            captureJetEngine();
	    JButtonFixture exportButton = findButton(relationsPanel, "Export");
	    exportButton.click();
	    System.out.println("Clicked exportButton " + exportButton);
            reportRelationPatterns();
            // demo.close();
        }

        public void addRelation (String relation, String example) {
            //
            // add a relation
            //
	    JButtonFixture addRelationButton = findButtonNamed(relationsPanel, "addRelationButton");
	    addRelationButton.click();
	    System.out.println("Clicked addRelationButton " + addRelationButton);
	    JOptionPaneFixture jOptionPane = JOptionPaneFinder.findOptionPane().withTimeout(20000).using(demo.robot);
	    System.out.println("Found jOptionPane " + jOptionPane + " (request for relation name)");
	    JTextComponentFixture jtcf = jOptionPane.textBox();
	    jtcf.enterText(relation);
	    System.out.println("Entered relation name:  " + relation);
	    JButtonFixture button2 = jOptionPane.okButton();
	    button2.click();
	    System.out.println("Clicked OK button " + button2);
	    jOptionPane = JOptionPaneFinder.findOptionPane().using(demo.robot);
	    System.out.println("Found jOptionPane " + jOptionPane + " (request for example)");
	    jtcf = jOptionPane.textBox();
	    jtcf.enterText(example);
	    System.out.println("Entered example:  " + example);
	    button2 = jOptionPane.okButton();
	    button2.click();
	    System.out.println("Clicked OK button " + button2);
        }

        public void addRelationMember (String example) {
            //
            // add a relation member (additional path)
            //
	    JButtonFixture addRelationMemberButton = findButtonNamed(relationsPanel, "addRelationMemberButton");
	    addRelationMemberButton.click();
	    System.out.println("Clicked addRelationMemberButton " + addRelationMemberButton);
	    JOptionPaneFixture jOptionPane = JOptionPaneFinder.findOptionPane().using(demo.robot);
	    System.out.println("Found jOptionPane " + jOptionPane + " (request for example)");
	    JTextComponentFixture jtcf = jOptionPane.textBox();
	    jtcf.enterText(example);
	    System.out.println("Entered example:  " + example);
	    JButtonFixture button2 = jOptionPane.okButton();
	    button2.click();
	    System.out.println("Clicked OK button " + button2);
        }


	    /*---------
	    //
	    // add corpus
	    //
	    JButtonFixture addButton = findButton("Add...");
	    addButton.click();
	    System.out.println("Clicked addButton " + addButton);

	    JOptionPaneFixture jOptionPane = JOptionPaneFinder.findOptionPane().using(demo.robot);
	    System.out.println("Found jOptionPane " + jOptionPane);
	    JTextComponentFixture jtcf = jOptionPane.textBox();
	    System.out.println("Found text component " + jtcf);
	    jtcf.enterText("corpus1");

	    JButtonFixture button2 = jOptionPane.okButton();
	    button2.click();
	    System.out.println("Clicked OK button " + button2);
	    //
	    // start file browser
	    //
	    JButtonFixture browseButton = findButton("Browse...");
	    browseButton.click();
	    System.out.println("Clicked browseButton " + browseButton);
	    JFileChooserFixture jfcf = findFileChooser();
	    jfcf.selectFile(new File("/Users"));
	    // jfcf.approve();
	    // findButton("Choose").click();
	    // JTextComponentFixture directory= findTextBox("Directory");
	    // JTextComponentFixture filter= findTextBox("?");
	    */

	JButtonFixture findButton (final String text) {
    		return demo.button(new GenericTypeMatcher<JButton>(JButton.class){
		    @Override protected boolean isMatching(final JButton r){
			return text.equals(r.getText());
		    }
		});
	}

	JButtonFixture findButton (JPanelFixture panel, final String text) {
    		return panel.button(new GenericTypeMatcher<JButton>(JButton.class){
		    @Override protected boolean isMatching(final JButton r){
			return text.equals(r.getText());
		    }
		});
	}

	JButtonFixture findButtonNamed (final String name) {
    		return demo.button(new GenericTypeMatcher<JButton>(JButton.class){
		    @Override protected boolean isMatching(final JButton r){
			return name.equals(r.getName());
		    }
		});
	}

	JButtonFixture findButtonNamed (JPanelFixture panel, final String name) {
            return panel.button(name);}
            /*
    		return panel.button(new GenericTypeMatcher<JButton>(JButton.class){
		    @Override protected boolean isMatching(final JButton r){
			return name.equals(r.getName());
		    }
		});
	} */

	/*
	JButtonFixture findButton2 (ComponentFixture f, final String text) {
    		return f.button(new GenericTypeMatcher<JButton>(JButton.class){
		    @Override protected boolean isMatching(final JButton r){
			return text.equals(r.getText());
		    }
		});
	}
	*/

	JTextComponentFixture findTextBox (final String text) {
    		return demo.textBox(new GenericTypeMatcher<JTextComponent>(JTextComponent.class){
		    @Override protected boolean isMatching(final JTextComponent r){
			return text.equals(r.getText());
		    }
		});
	}

	JFileChooserFixture findFileChooser () {
    		return demo.fileChooser(new GenericTypeMatcher<JFileChooser>(JFileChooser.class){
		    @Override protected boolean isMatching(final JFileChooser r){
			return true;
		    }
		});
	}

	JTabbedPaneFixture findTabbedPane () {
    		return demo.tabbedPane(new GenericTypeMatcher<JTabbedPane>(JTabbedPane.class){
		    @Override protected boolean isMatching(final JTabbedPane r){
			return true;
		    }
		});
	}

	JPanelFixture findPanel (final String name) {
    		return demo.panel(new GenericTypeMatcher<JPanel>(JPanel.class){
		    @Override protected boolean isMatching(final JPanel r){
			return name.equals(r.getName());
		    }
		});
	}

        StringWriter sw3;

        void captureJetEngine () {
            StringWriter sw1 = new StringWriter();
            JetEngineBuilder.setCommonNounWriter(sw1);
            StringWriter sw2 = new StringWriter();
            JetEngineBuilder.setProperNounWriter(sw2);
            sw3 = new StringWriter();
            JetEngineBuilder.setRelationPatternWriter(sw3);
            StringWriter sw4 = new StringWriter();
            JetEngineBuilder.setNegatedPatternWriter(sw4);
        }

        void reportRelationPatterns () {
            sw3.flush();
            String p = sw3.toString();
            System.out.println("Patterns: \n" + p);
            assertTrue("missing path GPE--amod-1--PERSON	CITIZEN", p.contains("GPE--amod-1--PERSON	CITIZEN"));
            assertTrue("missing path GPE--nn-1--PERSON	CITIZEN",         p.contains("GPE--nn-1--PERSON	CITIZEN"));
            assertTrue("missing path PERSON--prep:of:pobj--GPE	CITIZEN-1", p.contains("PERSON--prep:of:pobj--GPE	CITIZEN-1"));
            assertTrue("missing path PERSON--nn-1--PERSON	PAIR",    p.contains("PERSON--nn-1--PERSON	PAIR"));
            assertTrue("missing path PERSON--appos--PERSON	PAIR",    p.contains("PERSON--appos--PERSON	PAIR"));
        }
}
