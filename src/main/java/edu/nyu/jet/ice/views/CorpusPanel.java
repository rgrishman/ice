package edu.nyu.jet.ice.views;

import java.util.List;

/**
 * Created by yhe on 10/12/14.
 */
public interface CorpusPanel {
    public void setCorporaList(List<String> names);
    public void setSelectedCorpus(String name);
    public void setDirectory(String directory);
    public void setFilter(String type);
    public void setBackgroundList(List<String> names);
    public void setSelectedBackground(String name);
    public void printCorpusSize(int size);
    public void addCorpus(String corpusName);
}
