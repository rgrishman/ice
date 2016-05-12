package edu.nyu.jet.ice.uicomps;// -*- tab-width: 4 -*-
//Title:        JET-ICE
//Version:      1.72
//Copyright:    Copyright (c) 2014
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool -- Customization Environment

import java.awt.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.List;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.IceEntitySet;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.models.JetEngineBuilder;
import edu.nyu.jet.ice.utils.FileNameSchema;
import org.ho.yaml.*;

/**
 *  provides main frame for ICE along with functionality for defining
 *  new corpora.
 */

public class Ice {

	public static SortedMap<String, Corpus> corpora = new TreeMap<String, Corpus> ();
	public static SortedMap<String, IceEntitySet> entitySets = new TreeMap<String, IceEntitySet>();
	public static SortedMap<String, IceRelation> relations = new TreeMap<String, IceRelation>();
	public static Corpus selectedCorpus = null;
	public static String selectedCorpusName = null;

	private static String configFile = "ice.yml";

	static JTextField directoryField;
	static JTextField filterField;
	public static JFrame mainFrame;

    public static Properties iceProperties = new Properties();

	public static void main (String[] args) {
		if (args.length > 0 && null != args[0] && args[0].length() > 0) {
			configFile = args[0];
		}
		ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
		toolTipManager.setDismissDelay(7500);
		loadConfig(configFile);
		if (!corpora.isEmpty())
			selectCorpus (corpora.firstKey());
		mainFrame = new JFrame();
		Container contentPane = mainFrame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		assembleFrame(contentPane);

		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.pack();
		mainFrame.setVisible(true);
	}

    public static void loadConfig(String configFile) {
		try {
			File yamlFile = new File(configFile);
			System.out.println("Loading configuration from " + yamlFile.getAbsolutePath());
			InputStream yamlInputStream = new FileInputStream(yamlFile);
			YamlDecoder dec = new YamlDecoder(yamlInputStream);
			FileNameSchema.setWD(new String ((String)dec.readObject()));
			String kashrut = FileNameSchema.getCacheRoot();
			System.out.println("Ice.java: " + Integer.toString(new File(kashrut).listFiles().length) + " directories in cache" );
			corpora = new TreeMap((Map) dec.readObject());
			System.out.println("Loaded corpora");
			System.out.println("Ice.java: " + Integer.toString(corpora.size()) + " corpora");
			System.out.println("Ice.java: " + Integer.toString(new File(kashrut).listFiles().length) + " directories in cache" );
			entitySets = new TreeMap((Map) dec.readObject());
			relations = new TreeMap((Map) dec.readObject());
			dec.close();
			System.out.println("Closed yaml file");
			System.out.println("Ice.java: " + Integer.toString(new File(kashrut).listFiles().length) + " directories in cache" );
		} catch (IOException e) {
			System.out.println("Did not load config file " + configFile);
		}
	}

	static void assembleFrame (Container contentPane) {
		Box entityPipeline = Box.createHorizontalBox();
		if (selectedCorpus != null) {
			entityPipeline.add(selectedCorpus.termBox());
			entityPipeline.add(selectedCorpus.entitySetBuilder.makeBox());
		}
		// entityPipeline.add(entityInventoryBox);
		Box relationPipeline = Box.createHorizontalBox();
		if (selectedCorpus != null) {
			relationPipeline.add(selectedCorpus.patternBox());
			// relationPipeline.add(selectedCorpus.relationBox());
			relationPipeline.add(selectedCorpus.relationBuilder.relationSetBox());
		}
		// relationPipeline.add(relationInventoryBox());
		Box pipelines = Box.createVerticalBox();
		pipelines.add(entityPipeline);
		pipelines.add(relationPipeline);
		contentPane.add(corporaBox());
		contentPane.add(pipelines);
		// contentPane.add(buildJetEngine());
	}
		

	public static void selectCorpus (String corpus) {
		selectedCorpusName = corpus;
		selectedCorpus = corpora.get(selectedCorpusName);
        if (mainFrame != null) {
            updateFrame();
        }
	}

	public static void updateFrame () {
		Container contentPane = mainFrame.getContentPane();
		contentPane.removeAll();
		assembleFrame (contentPane);
		mainFrame.validate();
		mainFrame.repaint();
	}

	public static Box corporaBox () {

		Box box = Box.createVerticalBox();
		TitledBorder border = new TitledBorder("Corpora");
                border.setTitleColor(Color.RED);
		box.setBorder(border);

		JButton addCorpusButton = new JButton("add corpus");
		box.add(addCorpusButton);

        Box foreAndBackgroundCorpusBox = Box.createHorizontalBox();

		Box selectCorpusBox = Box.createVerticalBox();
		TitledBorder selectCorpusBorder = new TitledBorder("select corpus");
		selectCorpusBox.setBorder(selectCorpusBorder);
		ButtonGroup bgroup = new ButtonGroup();
		List<JRadioButton> corpusSelectButtons = new ArrayList<JRadioButton>();
		for (String corpus : corpora.keySet()) {
			JRadioButton button = new JRadioButton(corpus);
			bgroup.add(button);
			selectCorpusBox.add(button);
			corpusSelectButtons.add(button);
			button.setActionCommand(corpus);
			button.setSelected(corpus.equals(selectedCorpusName));
		}
		foreAndBackgroundCorpusBox.add(selectCorpusBox);

        Box selectBackgroundCorpusBox = Box.createVerticalBox();
        TitledBorder selectBackgroundCorpusBorder = new TitledBorder("select background corpus");
        selectBackgroundCorpusBox.setBorder(selectBackgroundCorpusBorder);
        List<JRadioButton> backgroundCorpusButtons = new ArrayList<JRadioButton>();
        ButtonGroup bggroup = new ButtonGroup();
        for (String corpus : Ice.corpora.keySet()) {
            if (corpus.equals(selectedCorpusName))
                continue;
            if (Ice.corpora.get(corpus).wordCountFileName == null)
                continue;
            if (Ice.corpora.get(corpus).relationTypeFileName == null)
                continue;
            JRadioButton button = new JRadioButton(corpus);
            bggroup.add(button);
            selectBackgroundCorpusBox.add(button);
            backgroundCorpusButtons.add(button);
            button.setActionCommand(corpus);
        }
        for (JRadioButton b : backgroundCorpusButtons) {
            if (Ice.selectedCorpusName != null
                    && selectedCorpus.backgroundCorpus != null
                    && b.getText().equals(selectedCorpus.backgroundCorpus)) {
                b.setSelected(true);
            }
        }
        foreAndBackgroundCorpusBox.add(selectBackgroundCorpusBox);

        box.add(foreAndBackgroundCorpusBox);

		Box box_1 = Box.createHorizontalBox();
		box_1.add(new JLabel("Directory"));
		directoryField = new JTextField(15);
		if (selectedCorpus != null)
			directoryField.setText(selectedCorpus.directory);
		box_1.add(directoryField);
		JButton browseButton = new JButton("Browse ...");
		box_1.add(browseButton);
		box.add(box_1);

		Box box_2 = Box.createHorizontalBox();
		box_2.add(new JLabel("Filter"));
		filterField = new JTextField(15);
		if (selectedCorpus != null)
			filterField.setText(selectedCorpus.filter);
		box_2.add(filterField);
		box.add(box_2);

		if (selectedCorpus != null) {
			JLabel sizeOfCorpus = new JLabel("Corpus has " + 
				selectedCorpus.numberOfDocs + " documents.");
			box.add(sizeOfCorpus);
		}

		JButton deleteCorpusButton = new JButton("delete corpus");
		box.add(deleteCorpusButton);

		JButton saveButton = new JButton("save");
		box.add(saveButton);

        JButton editEntitySetButton = new JButton("edit entity sets");
        box.add(editEntitySetButton);
        JButton editRelationButton  = new JButton("edit relations");
        box.add(editRelationButton);

        JButton exportButton = new JButton("export to Jet");
        box.add(exportButton);


		// listeners -------------------

        editEntitySetButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                EntitySetEditorFrame frame = new EntitySetEditorFrame("Edit entity sets");
                frame.setPreferredSize(new Dimension(500, 520));
                frame.setMinimumSize(new Dimension(500, 520));
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.entitySetList.revalidate();
                frame.entitySetList.repaint();
            }
        });

        editRelationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                RelationEditorFrame frame = new RelationEditorFrame("Edit relations");
                frame.setPreferredSize(new Dimension(600, 520));
                frame.setMinimumSize(new Dimension(600, 520));
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.relationList.revalidate();
                frame.relationList.repaint();
            }
        });

        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JetEngineBuilder.build();
            }
        });

		addCorpusButton.addActionListener (new ActionListener() {
                        public void actionPerformed (ActionEvent ev) {
				String corpusName = JOptionPane.showInputDialog("corpus name?");
				Corpus newCorpus = new Corpus(corpusName);
				corpora.put(corpusName, newCorpus);	
				selectCorpus(corpusName);
				updateFrame();
			}
		});

		for (JRadioButton b: corpusSelectButtons) {
			b.addActionListener (new ActionListener() {
				public void actionPerformed (ActionEvent ev) {
					selectCorpus(ev.getActionCommand());
					updateFrame();
				}
			});
		}

		directoryField.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				selectedCorpus.setDirectory(directoryField.getText());
				selectedCorpus.writeDocumentList();
			}
		});

		browseButton.addActionListener (new ActionListener() {
                        public void actionPerformed (ActionEvent ev) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					String path = file.getPath();
					selectedCorpus.setDirectory(path);
					directoryField.setText(path);
					selectedCorpus.writeDocumentList();
				}
			}
		});

		filterField.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				selectedCorpus.setFilter(filterField.getText());
				selectedCorpus.writeDocumentList();
				// need to update because corpus size may have changed
				updateFrame();
			}
		});

		deleteCorpusButton.addActionListener (new ActionListener() {
                        public void actionPerformed (ActionEvent ev) {
				int input = JOptionPane.showConfirmDialog(null, 
					   "Are you sure?", "Confirm", JOptionPane.YES_NO_OPTION);
				if (input == JOptionPane.YES_OPTION) {
					corpora.remove (selectedCorpusName);
					if (!corpora.isEmpty())
						selectCorpus (corpora.firstKey());
					updateFrame();
				}
			}
		});

		saveButton.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				saveConfig(configFile);
			}
			});

        for (JRadioButton b : backgroundCorpusButtons) {
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    selectedCorpus.backgroundCorpus = ev.getActionCommand();
                }
            });
        }

		return box;
	}

	protected static void saveConfig(String configFile) {
		try {
			File yamlFile = new File(configFile);
			OutputStream yamlOutputStream = new FileOutputStream(yamlFile);
			YamlEncoder enc = new YamlEncoder(yamlOutputStream);
			enc.writeObject(FileNameSchema.getWD());
			enc.writeObject(corpora);
			enc.writeObject(entitySets);
			enc.writeObject(relations);
			enc.close();
		} catch (IOException e) {
			System.out.println("Error writing config file " + configFile);
		}
	}


}
