package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.ice.models.IceRelation;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;


/**
 * Created by yhe on 4/14/14.
 */
public class RelationEditorFrame extends JFrame {
    JList relationList;
    JList entriesList;
    DefaultListModel relationListModel;
    DefaultListModel entriesListModel;
    JTextField nameTextField;
    JTextField arg1TextField;
    JTextField arg2TextField;
    IceRelation currentRelation;

    public RelationEditorFrame(String title) {
        super(title);
        JPanel entitySetPanel  = new JPanel(new MigLayout());
        entitySetPanel.setSize(800, 520);

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setSize(300, 520);
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        leftPanel.setBorder(lowerEtched);
        JLabel entitySetLabel = new JLabel("Relations");
        leftPanel.add(entitySetLabel, "wrap");

        entitySetPanel.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setSize(400, 520);
        rightPanel.setBorder(lowerEtched);
        entitySetPanel.add(rightPanel);
        relationListModel = new DefaultListModel();
        for (String k : Ice.relations.keySet()) {
            relationListModel.addElement(Ice.relations.get(k));
        }
        relationList = new JList();
        JScrollPane entitySetPane = new JScrollPane(relationList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(200, 400);
        relationList.setSize(200, 400);
        relationList.setMinimumSize(new Dimension(200, 400));
        entitySetPane.setPreferredSize(new Dimension(200, 400));
        leftPanel.add(entitySetPane, "wrap");
        relationList.setModel(relationListModel);

        JButton deleteEntitySetButton = new JButton("Delete relation");
        leftPanel.add(deleteEntitySetButton, "wrap");
        JButton saveEntitySetsButton = new JButton("Save relations");
        leftPanel.add(saveEntitySetsButton);

        JLabel nameLabel = new JLabel("Type");
        rightPanel.add(nameLabel, "wrap");

        nameTextField = new JTextField("");
        nameTextField.setMinimumSize(new Dimension(300, 20));
        rightPanel.add(nameTextField, "wrap");

        JLabel arg1Label = new JLabel("Arg1");
        rightPanel.add(arg1Label, "wrap");

        arg1TextField = new JTextField("");
        arg1TextField.setMinimumSize(new Dimension(300, 20));
        rightPanel.add(arg1TextField, "wrap");

        JLabel arg2Label = new JLabel("Arg2");
        rightPanel.add(arg2Label, "wrap");

        arg2TextField = new JTextField("");
        arg2TextField.setMinimumSize(new Dimension(300, 20));
        rightPanel.add(arg2TextField, "wrap");
        JLabel itemsLabel = new JLabel("Items");
        rightPanel.add(itemsLabel, "wrap");

        entriesList   = new JList();

        entriesList.setSize(300, 175);
        entriesList.setMinimumSize(new Dimension(300, 175));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(300, 175);
        entriesSetPane.setMinimumSize(new Dimension(300, 175));
        rightPanel.add(entriesSetPane, "wrap");
        JButton addEntryButton = new JButton("Add");
        rightPanel.add(addEntryButton, "wrap");
        JButton deleteEntryButton = new JButton("Delete");
        rightPanel.add(deleteEntryButton, "wrap");
        JButton saveEntitySetButton = new JButton("Save");
        rightPanel.add(saveEntitySetButton);
        entriesSetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        this.add(entitySetPanel);

        // listeners

        relationList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int idx = relationList.getSelectedIndex();
                if (idx < 0) return;
                IceRelation iceRelation = (IceRelation) relationListModel.getElementAt(idx);
                currentRelation = iceRelation;
                nameTextField.setText(iceRelation.getName());
                arg1TextField.setText(iceRelation.getArg1type());
                arg2TextField.setText(iceRelation.getArg2type());
                DefaultListModel newListModel = new DefaultListModel();
                for (String path : iceRelation.getPaths()) {
                    newListModel.addElement(path);
                }
                entriesListModel = newListModel;
                entriesList.setModel(entriesListModel);
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                String s = (String)JOptionPane.showInputDialog(
                        RelationEditorFrame.this,
                        "Entry",
                        "Add new path to relation",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");

                if ((s != null) && (s.length() > 0)) {
                    entriesListModel.addElement(s);
                }
            }
        });

        deleteEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                int idx = entriesList.getSelectedIndex();
                if (idx < 0) return;
                entriesListModel.remove(idx);
            }
        });

        saveEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (currentRelation == null ||
                        nameTextField.getText().trim().length() == 0 ||
                        entriesListModel == null ||
                        entriesListModel.size() == 0) {
                    return;
                }
                currentRelation.setName(nameTextField.getText().trim());
                currentRelation.setArg1type(arg1TextField.getText());
                currentRelation.setArg2type(arg2TextField.getText());
                java.util.List<String> paths = new ArrayList<String>();
                for (Object e : entriesListModel.toArray()) {
                    paths.add((String) e);
                }
                currentRelation.setPaths(paths);
                relationList.revalidate();
                relationList.repaint();
            }
        });

        deleteEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (relationListModel == null) return;
                int idx = relationList.getSelectedIndex();
                if (idx < 0) return;
                IceRelation e = (IceRelation) relationListModel.get(idx);
                int n = JOptionPane.showConfirmDialog(
                        RelationEditorFrame.this,
                        String.format("Are you sure to remove the relation [%s]?", e.getName()),
                        "Confirm relation deletion",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    relationListModel.remove(idx);
                    entriesListModel.clear();
                    entriesListModel = null;
                    nameTextField.setText("");
                    arg1TextField.setText("");
                    arg2TextField.setText("");
                    currentRelation = null;
                }
            }
        });

        saveEntitySetsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Ice.relations.clear();
                for (Object e : relationListModel.toArray()) {
                    IceRelation es = (IceRelation)e;
                    Ice.relations.put(es.getName(), es);
                }
                JOptionPane.showMessageDialog(RelationEditorFrame.this,
                        "User relations in ICE updated. You will still need to save/export to Jet\n" +
                                "if you want to save relations to hard disk.");
            }
        });

    }
}
