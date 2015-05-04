package edu.nyu.jet.ice.controllers;

import Jet.IceModels.Corpus;
import Jet.IceModels.DepPaths;
import Jet.IceUtils.FileNameSchema;
import Jet.IceUtils.IceUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

/**
 * Created by yhe on 12/14/14.
 */
public class NiceExtractor  {
    public static JFrame mainFrame;
    public static final String cover = "     __        __   \n" +
            "    |__| _____/  |_ \n" +
            "    |  |/ __ \\   __\\\n" +
            "    |  \\  ___/|  |  \n" +
            "/\\__|  |\\___  >__|  \n" +
            "\\______|    \\/      \n";
    public static final String unicodeMessage = "  \u6E05\u98A8\u5F90\u4F86  \u6C34\u6CE2\u4E0D\u8208";

    private static JLabel directoryLabel = new JLabel("  Directory");
    private static JButton directoryBrowseButton = new JButton("Browse...");
    private static JButton loadButton = new JButton("Load");
    private static JLabel statusLabel = new JLabel("  Welcome to Jet Extraction Interface.");
    private static JComboBox typeComboBox = new JComboBox();
    private static String fileType = "txt";
    private static String directory = "";

    private static JTextField directoryTextField = new JTextField();
    private static String workDirectory = "./tmp";
    private static String inputListFile = workDirectory + File.separator + "filelist";

    private static JTextArea pathsArea = new JTextArea();
    private static JButton extractPathsButton = new JButton("Extract");
    private static JButton exportPathsButton  = new JButton("Export");
    private static String pathsFileName = workDirectory + File.separator + "paths";

    private static JTextArea relationsArea = new JTextArea();
    private static JButton extractRelationsButton = new JButton("Extract");
    private static JButton exportRelationsButton  = new JButton("Export");
    private static String relationsFileName = workDirectory + File.separator + "relations";


    public static void main(String[] args) throws Exception {
        System.err.println(cover);
        System.err.println(unicodeMessage);

        String path = NiceExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        //JOptionPane.showMessageDialog(null, decodedPath);

        File workingDirFile = new File(workDirectory);
        if (workingDirFile.exists()) {
            if (!workingDirFile.isDirectory()) {
                System.err.println("Unable to create working directory. Exit.");
                System.exit(-1);
            }
        }
        else {
            workingDirFile.mkdirs();
        }

        mainFrame = new JFrame();
        mainFrame.setLayout(new MigLayout());
        mainFrame.setTitle("Jet Extraction Interface");
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setMinimumSize(new Dimension(640, 520));
        mainFrame.setMaximumSize(new Dimension(640, 520));
        mainFrame.setResizable(false);

        assemble();

        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusPanel.setPreferredSize(new Dimension(mainFrame.getWidth(), 16));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);
        mainFrame.getContentPane().add(statusPanel, "south");

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }

    public static void assemble() {
        Container contentPane = mainFrame.getContentPane();
        contentPane.removeAll();
        //mainFrame.removeAll();
        contentPane.setLayout(new MigLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        contentPane.add(tabbedPane, "span");
        //mainFrame.add(tabbedPane, "span");

        JComponent pathPanel = new JPanel(new MigLayout());
        tabbedPane.addTab("Paths", null, pathPanel,
                "Paths from the corpus");
        pathPanel.setMinimumSize(new Dimension(600, 380));

        pathsArea = new JTextArea(20, 45);
        pathsArea.setEditable(false);
        JScrollPane pathScrollPane = new JScrollPane(pathsArea);
        pathPanel.add(pathScrollPane, "span");
        extractPathsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    DepPaths.main(new String[]{"onomaprops",
                            inputListFile,
                            "",
                            fileType,
                            pathsFileName,
                            pathsFileName + ".types",
                            pathsFileName + ".source.dict"});
                    pathsArea.setText(IceUtils.readFileAsString(pathsFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        pathPanel.add(extractPathsButton);
        exportPathsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
                int returnVal = fc.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        FileUtils.copyFile(new File(pathsFileName), fc.getSelectedFile());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        pathPanel.add(exportPathsButton);

        JComponent relationPanel = new JPanel(new MigLayout());
        tabbedPane.addTab("Relations", null, relationPanel,
                "Entities from the corpus");
        relationPanel.setMinimumSize(new Dimension(600, 380));

        relationsArea = new JTextArea(20, 45);
        relationsArea.setEditable(false);
        JScrollPane relationScrollPane = new JScrollPane(relationsArea);
        relationPanel.add(relationScrollPane, "span");

        extractRelationsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    DepPaths.extractPathRelations(inputListFile, relationsFileName, fileType, "parseprops");
                    relationsArea.setText(IceUtils.readFileAsString(relationsFileName));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        relationPanel.add(extractRelationsButton);
        exportRelationsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
                int returnVal = fc.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        FileUtils.copyFile(new File(relationsFileName), fc.getSelectedFile());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        relationPanel.add(exportRelationsButton);

        directoryTextField.setMinimumSize(new Dimension(240, 20));
        directoryTextField.setMaximumSize(new Dimension(240, 20));
        directoryTextField.setEditable(false);

        typeComboBox.addItem("TXT");
        typeComboBox.addItem("SGM");
        typeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                fileType = typeComboBox.getSelectedItem().toString().toLowerCase();
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
                }
            }
        });


        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                directory = directoryTextField.getText().trim();
                if (directory.length() > 0 && fileType.length() > 0) {
                    java.util.List<String> fileNames =
                            Corpus.findAllFiles(directory, fileType);
                    boolean success = writeFileList(inputListFile, fileNames, directory, fileType);
                    if (success) {
                        updateStatus("Success writing file list.");
                    }
                    else {
                        updateStatus("Error writing file list.");
                    }
                }
            }
        });

        contentPane.add(directoryLabel);
        contentPane.add(directoryTextField);
        contentPane.add(typeComboBox);
        contentPane.add(directoryBrowseButton);
        contentPane.add(loadButton, "wrap push");
    }

    private static boolean writeFileList(String docListFileName,
                                      java.util.List<String> docs,
                                      String directory,
                                      String filter) {
        try {
            File docListFile = new File(docListFileName);
            PrintWriter writer = new PrintWriter(new FileWriter(docListFile));
            for (String doc : docs) {

                if ("*".equals(filter.trim())) {
                    writer.println(doc);
                } else {
                    String nameWithoutSuffix =
                            directory + File.separator + doc.substring(0, doc.length() - filter.length() - 1);
                    writer.println(nameWithoutSuffix);
                }
            }
            writer.close();
            return true;
        } catch (IOException e) {
            System.err.println("Error writing fileList.");
            e.printStackTrace();
        }
        return false;
    }

    public static void updateStatus(String s) {
        statusLabel.setText("  " + s);
    }


}
