package edu.nyu.jet.ice.views.swing;

import edu.nyu.jet.ice.models.Corpus;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePreprocessor;
import edu.nyu.jet.ice.terminology.TermCounter;
import edu.nyu.jet.ice.terminology.TermRanker;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.uicomps.ListFilter;
import edu.nyu.jet.ice.uicomps.RelationFilter;
import edu.nyu.jet.ice.uicomps.TermFilter;
import edu.nyu.jet.ice.utils.FileNameSchema;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.entityset.EntityIndexerBox;
import edu.nyu.jet.ice.views.Refreshable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Panel that manages the entity/terminology extraction.
 *
 * Currently, the entity index functionality comes from EntitySetBuilder.makeSwingBox()
 *
 * @author yhe
 */

public class SwingEntitiesPanel extends JPanel implements Refreshable {
    public final SwingIceStatusPanel statusPanel = new SwingIceStatusPanel();
    public final JTextArea textArea = new JTextArea(11, 35);

    /**
     *  create entities panel and display top-ranked entities in response
     *  to "Find Entities" button.
     */

    public SwingEntitiesPanel() {
        super();
        this.setLayout(new MigLayout());
        this.setOpaque(true);
        this.removeAll();
        JPanel termBox = new JPanel(new MigLayout());
        TitledBorder border = new TitledBorder("Entities");
        termBox.setBorder(border);
        termBox.setOpaque(true);
        termBox.setMinimumSize(new Dimension(480, 270));
        JScrollPane scrollPane = new JScrollPane(textArea);
//        if (termFileName != null)
//            displayTerms(termFileName, 100, textArea, termFilter);
        termBox.add(scrollPane, "wrap");
        textArea.setEditable(false);

        JButton findEntitiesButton = new JButton("Find Entities");
        findEntitiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findTerms();
                Ice.selectedCorpus.termFileName = FileNameSchema.getTermsFileName(Ice.selectedCorpusName);
                java.util.List<String> terms =
                        getTerms(FileNameSchema.getTermsFileName(Ice.selectedCorpusName), 100);
                StringBuilder areaTextBuilder = new StringBuilder();
                for (String t : terms) {
                    areaTextBuilder.append(t).append("\n");
                }
                textArea.setText(areaTextBuilder.toString());
            }
        });

        termBox.add(findEntitiesButton);

        EntityIndexerBox eib = new EntityIndexerBox();
        Box indexBox = eib.makeSwingBox();
        this.add(termBox, "cell 0 0");
        this.add(statusPanel, "cell 1 0 1 2");
        this.add(indexBox, "cell 0 1");
        refresh();
    }

    /**
     *  returns a list of (at most <CODE>limit</CODE>) terms from
     *  file <CODE>termFile</CODE>.
     */

    public static java.util.List<String> getTerms(String termFile, int limit) {
        java.util.List<String> topTerms = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(termFile));
            int k = 0;
            while (true) {
                String term = reader.readLine();
                if (term == null) break;
                if (term.length() < 4 || !termIsType(term, "nn")) continue;
                term = term.substring(0, term.length() - 3);
                topTerms.add(term);
                k++;
                if (k >= limit) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return topTerms;
    }

    public void refresh() {
        statusPanel.refresh();
    }

    /**
     *  invokes <CODE>TermRanker</CODE> to rank terms by relative frequency,
     *  writing ranked list to file.
     */

    public void findTerms() {
        String termFileName = FileNameSchema.getTermsFileName(Ice.selectedCorpusName);
        try {
            File f = new File(FileNameSchema.getWordCountFileName(Ice.selectedCorpusName));
            if (!f.exists() || !f.isFile()) {
                if (SwingPathsPanel.preprocessedTextsAvailable(Ice.selectedCorpusName)) {
                    IcePreprocessor.countWords(false);
                } else {
                    JOptionPane.showMessageDialog(Ice.mainFrame, "Source text not available, cannot rebuild term set");
                    return;
                }
            }
            TermRanker.rankTerms(FileNameSchema.getWordCountFileName(Ice.selectedCorpusName),
                    Ice.corpora.get(Ice.selectedCorpus.backgroundCorpus).wordCountFileName, 
		    termFileName);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }
    }

    private static boolean termIsType(String term, String type) {
        String[] parts = term.split("/");
        if (parts.length < 2) return false;
        return parts[1].equals(type);
    }
}
