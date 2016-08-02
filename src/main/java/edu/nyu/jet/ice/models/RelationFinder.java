package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;

import javax.swing.*;
import java.io.IOException;

/**
 * count all dependency paths in corpus.
 */
public class RelationFinder extends Thread {

    String[] args;
    String types;
    JTextArea area;
    int numberOfDocs;
    ProgressMonitorI relationProgressMonitor = null;

    public RelationFinder(String docListFileName, String directory, String filter,
                   String instances, String types, JTextArea area, int numberOfDocs,
                   ProgressMonitorI relationProgressMonitor) {
        args = new String[7];
        args[0] = "onomaprops";
        args[1] = docListFileName;
        args[2] = directory;
        args[3] = filter;
        args[4] = instances;
        args[5] = "temp";
        args[6] = "temp.source.dict";
        this.types = types;
        this.area = area;
        this.numberOfDocs = numberOfDocs;
        this.relationProgressMonitor = relationProgressMonitor;
    }

    public void run() {
        try {
            // force monitor to display during long initialization
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            if (null != relationProgressMonitor) {
                relationProgressMonitor.setProgress(2);
            }
            DepPathMap depPathMap = DepPathMap.getInstance();
            depPathMap.unpersist();
            DepPaths.progressMonitor = relationProgressMonitor;
            DepPaths.main(args);
            Corpus.sort("temp", types);
            depPathMap.forceLoad();
            if(area != null) {
                Corpus.displayTerms(types, 40, area, Corpus.relationFilter);
            }
        } catch (IOException e) {
            System.out.println("IOException in DepPaths " + e);
            e.printStackTrace(System.err);
        }
    }
}
