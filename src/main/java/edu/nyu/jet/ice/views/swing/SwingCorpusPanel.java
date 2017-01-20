package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.controllers.Nice;
import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;


/**
 * The Corpus panel that manages the basic information of the corpus
 *
 * @author yhe
 */
public class SwingCorpusPanel extends JComponent implements Refreshable {
    private JLabel corpusSelectionLabel = new JLabel("Corpus");
    private JComboBox corpusSelectionComboBox = new JComboBox();
    private JButton addButton = new JButton("Add...");
    private JButton deleteButton = new JButton("Delete");
    private JLabel backgroundCorpusLabel = new JLabel("Select Background ");
    private JComboBox backgroundCorpusComboBox = new JComboBox();
    private JLabel directoryLabel = new JLabel("Directory");
    private JButton directoryBrowseButton = new JButton("Browse...");

    private JTextField directoryTextField = new JTextField();
    private JLabel filterLabel = new JLabel("Filter");
    private JTextField filterTextField = new JTextField();
    private JButton preprocessButton = new JButton("Preprocess");
    private JLabel statisticsLabel = new JLabel("Current corpus has 0 documents.");
    private Nice controller;
    private SwingIceStatusPanel iceStatusPanel;

    public SwingCorpusPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(false);
        JPanel foregroundPanel = new JPanel();
        foregroundPanel.setMinimumSize(new Dimension(480, 210));
        foregroundPanel.setMaximumSize(new Dimension(480, 210));
        foregroundPanel.setOpaque(false);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border corpusTitleBorder = BorderFactory.createTitledBorder(etchedBorder, "Corpus");
        foregroundPanel.setBorder(corpusTitleBorder);
        foregroundPanel.setLayout(new MigLayout());
        foregroundPanel.add(corpusSelectionLabel);
        corpusSelectionComboBox.setMinimumSize(new Dimension(240, 20));
        foregroundPanel.add(corpusSelectionComboBox, "wrap");
        foregroundPanel.add(addButton);
        foregroundPanel.add(deleteButton, "wrap");
        foregroundPanel.add(directoryLabel);
        directoryTextField.setMinimumSize(new Dimension(240, 20));
        directoryTextField.setMaximumSize(new Dimension(240, 20));

        directoryTextField.setEditable(false);
        foregroundPanel.add(directoryTextField, "span 3");
        foregroundPanel.add(directoryBrowseButton, "wrap");
        foregroundPanel.add(filterLabel);
        filterTextField.setMinimumSize(new Dimension(240, 20));
        foregroundPanel.add(filterTextField, "span 3");
        foregroundPanel.add(preprocessButton, "wrap");
        foregroundPanel.add(statisticsLabel, "span");

        JPanel backgroundPanel = new JPanel();
        backgroundPanel.setMinimumSize(new Dimension(480, 150));
        backgroundPanel.setMaximumSize(new Dimension(480, 150));
        backgroundPanel.setLayout(new MigLayout());
        backgroundPanel.setOpaque(false);
        Border backgroundTitleBorder =
                BorderFactory.createTitledBorder(etchedBorder, "Background Corpus");
        backgroundPanel.setBorder(backgroundTitleBorder);


        backgroundPanel.add(backgroundCorpusLabel);
        backgroundCorpusComboBox.setMinimumSize(new Dimension(200, 20));
        backgroundPanel.add(backgroundCorpusComboBox, "wrap");
        JLabel hintTitleLabel = new JLabel("Unable to find your background corpus here?");
        SwingIceStatusPanel.bold(hintTitleLabel);
        JLabel hintTextLabel = new JLabel("Before using a corpus as background, run count ");
        JLabel hintTextLabel2 = new JLabel("words and find patterns.");
        backgroundPanel.add(hintTitleLabel, "span");
        backgroundPanel.add(hintTextLabel, "span");
        backgroundPanel.add(hintTextLabel2, "span");

        iceStatusPanel = new SwingIceStatusPanel();
        this.add(foregroundPanel, "cell 0 0");
        this.add(iceStatusPanel, "cell 1 0 1 2");
        this.add(backgroundPanel, "cell 0 1");

        this.controller = controller;

        addButton.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent ev) {
                String corpusName = JOptionPane.showInputDialog("Corpus name");
                for (int i = 0; i < corpusSelectionComboBox.getItemCount(); i++) {
                    String elem = (String)corpusSelectionComboBox.getItemAt(i);
                    if (elem.equals(corpusName)) {
                        JOptionPane.showMessageDialog(SwingCorpusPanel.this,
                                "The name is already used by another corpus.",
                                "Corpus Name Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                if (corpusName != null) {
                    addCorpus(corpusName);
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int input = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to delete corpus [" + Ice.selectedCorpusName + "]?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION);
                if (input == JOptionPane.YES_OPTION) {
                    deleteCorpus(Ice.selectedCorpusName);
                }
            }
        });

        corpusSelectionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox comboBox = (JComboBox)actionEvent.getSource();
                String selectedCorpus = (String)comboBox.getSelectedItem();
                if (selectedCorpus != null) {
                    selectCorpus(selectedCorpus);
                }
            }
        });

        directoryBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    String path = file.getPath();
                    directoryTextField.setText(path);
                    selectDirectory(path);
                }
            }
        });

        preprocessButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Object[] options = {"No",
                        "Yes"};
                setFilter(filterTextField.getText());
                if (Ice.selectedCorpus.getNumberOfDocs()  == 0) {
                    JOptionPane.showMessageDialog(null, "Corpus is empty.", "alert", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int n = JOptionPane.showOptionDialog(null,
                        "Preprocessing is the (only) time consuming step in ICE\n" +
                        "Do you wish to continue?",
                        "Continue with preprocessing?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                if (n == 0) {
                    return;
                }
                IcePreprocessor icePreprocessor = new IcePreprocessor(
                        Ice.selectedCorpus.directory,
                        Ice.iceProperties.getProperty("Ice.IcePreprocessor.parseprops"),
                        Ice.selectedCorpus.docListFileName,
                        filterTextField.getText(),
                        FileNameSchema.getPreprocessCacheDir(Ice.selectedCorpusName)
                );
                SwingProgressMonitor progressMonitor = new SwingProgressMonitor(SwingCorpusPanel.this,
                        "Preprocessing files",
                        "Processing files with Jet...",
                        0,
                        Ice.selectedCorpus.getNumberOfDocs() + 30
                );
                icePreprocessor.setProgressMonitor(progressMonitor);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                icePreprocessor.start();
            }
        });

        backgroundCorpusComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox comboBox = (JComboBox)actionEvent.getSource();
                String selectedBackgroundCorpus = (String)comboBox.getSelectedItem();
                if (selectedBackgroundCorpus != null) {
                    selectBackgroundCorpus(selectedBackgroundCorpus);
                }
            }
        });
    }

    public void setCorporaList(java.util.List<String> names) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
//        int selectedIndex = corpusSelectionComboBox.getSelectedIndex();
        //corpusSelectionComboBox.removeAllItems();
        for (String name : names) {
            model.addElement(name);
        }
        corpusSelectionComboBox.setModel(model);
//        corpusSelectionComboBox.setSelectedIndex(selectedIndex);
    }

    public void setSelectedCorpus(String name) {
        corpusSelectionComboBox.setSelectedItem(name);
    }

    public void setDirectory(String directory) {
        directoryTextField.setText(directory);
    }

    public void setBackgroundList(java.util.List<String> names) {
        backgroundCorpusComboBox.removeAllItems();
//        int oldSelection = backgroundCorpusComboBox.getSelectedIndex();
        for (String name : names) {
            backgroundCorpusComboBox.addItem(name);
        }
//        if (oldSelection >=0 && oldSelection < backgroundCorpusComboBox.getItemCount()) {
//            backgroundCorpusComboBox.setSelectedIndex(oldSelection);
//        }
    }

    public void setUIFilterFromIce(String filterName) {
        filterTextField.setText(filterName);
    }

    public void setSelectedBackground(String name) {
        backgroundCorpusComboBox.setSelectedItem(name);
    }

    public void printCorpusSize(int size) {
        statisticsLabel.setText("Corpus has " + size + " documents.");
    }

    public void refresh() {
        java.util.List<String> names = new ArrayList<String>();
        // names.addAll(Ice.corpora.keySet());
        for (String corpus : Ice.corpora.keySet()) {
            if (!corpus.startsWith(".")) {
                names.add(corpus);
            }
        }
        // String selectedCorpusName = Ice.selectedCorpusName;
        setCorporaList(names);
        if (names.size() > 0 && Ice.selectedCorpusName != null && Ice.selectedCorpusName.startsWith(".")) {
            setSelectedCorpus(names.get(0));
        }
        else {
            setSelectedCorpus(Ice.selectedCorpusName);
        }
        java.util.List<String> backgroundNames = new ArrayList<String>();
        for (String corpus : Ice.corpora.keySet()) {
            if (corpus.equals(Ice.selectedCorpusName))
                continue;
            if (Ice.corpora.get(corpus).wordCountFileName == null)
                continue;
            if (Ice.corpora.get(corpus).relationTypeFileName == null)
                continue;
            if (corpus.startsWith(".")) {
                continue;
            }
            backgroundNames.add(corpus);
        }
        String selectedBackground = "";
        if (Ice.selectedCorpus != null && Ice.selectedCorpus.backgroundCorpus != null) {
            selectedBackground = Ice.selectedCorpus.backgroundCorpus;
        }
        setBackgroundList(backgroundNames);
        setSelectedBackground(selectedBackground);
        String filter = "?";
        String directory = "?";
        if (Ice.selectedCorpus != null && Ice.selectedCorpus.filter != null) {
            filter = Ice.selectedCorpus.filter;
        }
        setUIFilterFromIce(filter);
        if (Ice.selectedCorpus != null && Ice.selectedCorpus.directory != null) {
            directory = Ice.selectedCorpus.directory;
        }
        setDirectory(directory);

        if (Ice.selectedCorpus != null) {
            printCorpusSize(Ice.selectedCorpus.getNumberOfDocs());
        }
        else {
            printCorpusSize(0);
        }
        iceStatusPanel.refresh();
    }

    public void addCorpus(String corpusName) {
        Corpus newCorpus = new Corpus(corpusName);
        Ice.corpora.put(corpusName, newCorpus);
        corpusSelectionComboBox.addItem(corpusName);
        corpusSelectionComboBox.setSelectedItem(corpusName);
        Ice.selectCorpus(corpusName);
        refresh();
    }

    public void deleteCorpus(String corpusName) {
        Ice.corpora.remove(corpusName);
        if (!Ice.corpora.isEmpty())
            Ice.selectCorpus(Ice.corpora.firstKey());
        refresh();
    }

    public void selectCorpus(String selectedCorpus) {
        Ice.selectCorpus(selectedCorpus);
        refresh();
    }

    public void selectDirectory(String directoryName) {
        Ice.selectedCorpus.setDirectory(directoryName);
        Ice.selectedCorpus.writeDocumentList();
    }

    public void setFilter(String filterName) {
        if (filterName.startsWith(".")) {
            filterName = filterName.substring(1);
        }
        Ice.selectedCorpus.setFilter(filterName);
        Ice.selectedCorpus.writeDocumentList();
        // need to update because corpus size may have changed
        // filterTextField.setText(filterName);
        refresh();
    }

    public void selectBackgroundCorpus(String backgroundCorpusName) {
        if (backgroundCorpusName != null) {
            Ice.selectedCorpus.backgroundCorpus = backgroundCorpusName;
        }
    }
}
