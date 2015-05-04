package edu.nyu.jet.ice.controllers;

import edu.nyu.jet.ice.views.CorpusPanel;
import edu.nyu.jet.ice.views.swing.SwingEntitiesPanel;
import edu.nyu.jet.ice.views.swing.SwingEntitySetPanel;
import edu.nyu.jet.ice.views.swing.SwingPathsPanel;
import edu.nyu.jet.ice.views.swing.SwingRelationsPanel;

/**
 * Created by yhe on 10/13/14.
 */
public interface IceController {
    public void refreshAll();

    public void setCorpusPanel(CorpusPanel corpusPanel);
    public void setEntitiesPanel(SwingEntitiesPanel entitiesPanel);
    public void setPathsPanel(SwingPathsPanel pathsPanel);
    public void setEntitySetPanel(SwingEntitySetPanel entitySetPanel);
    public void setRelationsPanel(SwingRelationsPanel relationsPanel);

    public void addCorpus(String corpusName);
    public void deleteCorpus(String corpusName);
    public void selectCorpus(String selectedCorpus);
    public void selectDirectory(String directoryName);
    public void setFilter(String filterName);
    public void selectBackgroundCorpus(String backgroundCorpusName);
    public void saveProgress();
    public void export();
}
