package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.ice.models.IceEntitySet;
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
public class EntitySetEditorFrame extends JFrame {
    JList entitySetList;
    JList entriesList;
    DefaultListModel entityListModel;
    DefaultListModel entriesListModel;
    JTextField nameTextField;
    IceEntitySet currentEntitySet;

    public EntitySetEditorFrame(String title) {
        super(title);
        JPanel entitySetPanel  = new JPanel(new MigLayout());
        entitySetPanel.setSize(600, 520);

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.setSize(300, 520);
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        leftPanel.setBorder(lowerEtched);
        JLabel entitySetLabel = new JLabel("Entity sets");
        leftPanel.add(entitySetLabel, "wrap");

        entitySetPanel.add(leftPanel);

        JPanel rightPanel = new JPanel(new MigLayout());
        rightPanel.setSize(300, 600);
        rightPanel.setBorder(lowerEtched);
        entitySetPanel.add(rightPanel);
        entityListModel = new DefaultListModel();
        for (String k : Ice.entitySets.keySet()) {
            entityListModel.addElement(Ice.entitySets.get(k));
        }
        entitySetList   = new JList();
        JScrollPane entitySetPane = new JScrollPane(entitySetList);
        entitySetPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entitySetPane.setSize(200, 400);
        entitySetList.setSize(200, 400);
        entitySetList.setMinimumSize(new Dimension(200, 400));
        entitySetPane.setPreferredSize(new Dimension(200, 400));
        leftPanel.add(entitySetPane, "wrap");
        entitySetList.setModel(entityListModel);

        JButton deleteEntitySetButton = new JButton("Delete entity set");
        leftPanel.add(deleteEntitySetButton, "wrap");
        JButton saveEntitySetsButton = new JButton("Save entity sets");
        leftPanel.add(saveEntitySetsButton);

        JLabel nameLabel = new JLabel("Type");
        rightPanel.add(nameLabel, "wrap");
        nameTextField = new JTextField("");
        nameTextField.setMinimumSize(new Dimension(200, 20));
        rightPanel.add(nameTextField, "wrap");
        JLabel itemsLabel = new JLabel("Items");
        rightPanel.add(itemsLabel, "wrap");

        entriesList   = new JList();

        entriesList.setSize(200, 275);
        entriesList.setMinimumSize(new Dimension(200, 275));
        JScrollPane entriesSetPane = new JScrollPane(entriesList);
        entriesSetPane.setSize(200, 275);
        entriesSetPane.setMinimumSize(new Dimension(200, 275));
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

        entitySetList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int idx = entitySetList.getSelectedIndex();
                if (idx < 0) return;
                IceEntitySet entitySet = (IceEntitySet)entityListModel.getElementAt(idx);
                currentEntitySet = entitySet;
                nameTextField.setText(entitySet.getType());
                DefaultListModel newListModel = new DefaultListModel();
                for (String noun : entitySet.getNouns()) {
                    newListModel.addElement(noun);
                }
                entriesListModel = newListModel;
                entriesList.setModel(entriesListModel);
            }
        });

        addEntryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entriesListModel == null) return;
                String s = (String)JOptionPane.showInputDialog(
                        EntitySetEditorFrame.this,
                        "Entry",
                        "Add new entry to entity set",
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
                if (currentEntitySet == null ||
                        nameTextField.getText().trim().length() == 0 ||
                        entriesListModel == null ||
                        entriesListModel.size() == 0) {
                    return;
                }
                currentEntitySet.setType(nameTextField.getText().trim());
                java.util.List<String> nouns = new ArrayList<String>();
                for (Object e : entriesListModel.toArray()) {
                    nouns.add((String)e);
                }
                currentEntitySet.setNouns(nouns);
                entitySetList.revalidate();
                entitySetList.repaint();
            }
        });

        deleteEntitySetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (entityListModel == null) return;
                int idx = entitySetList.getSelectedIndex();
                if (idx < 0) return;
                IceEntitySet e = (IceEntitySet)entityListModel.get(idx);
                int n = JOptionPane.showConfirmDialog(
                        EntitySetEditorFrame.this,
                        String.format("Are you sure to remove the entity set [%s]?", e.getType()),
                        "Confirm entity set deletion",
                        JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    entityListModel.remove(idx);
                    entriesListModel.clear();
                    entriesListModel = null;
                    nameTextField.setText("");
                    currentEntitySet = null;
                }
            }
        });

        saveEntitySetsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Ice.entitySets.clear();
                for (Object e : entityListModel.toArray()) {
                    IceEntitySet es = (IceEntitySet)e;
                    Ice.entitySets.put(es.getType(), es);
                }
                JOptionPane.showMessageDialog(EntitySetEditorFrame.this,
                        "Entity sets in ICE updated. You will still need to save/export to Jet\n" +
                                "if you want to save entity sets to hard disk.");
            }
        });

    }
}
