package edu.nyu.jet.ice.events;

import edu.nyu.jet.ice.models.*;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.ProgressMonitorI;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;

import javax.swing.*;
import java.io.IOException;

/**
 * count all dependency paths in corpus.
 */
public class EventFinder extends Thread {

    String[] args;
    String types;
    JTextArea area;
    int numberOfDocs;
    ProgressMonitorI eventProgressMonitor = null;

    public EventFinder(String docListFileName, String directory, String filter,
                   String instances, String types, JTextArea area, int numberOfDocs,
                   ProgressMonitorI eventProgressMonitor) {
        args = new String[7];
        args[0] = "onomaprops";
        args[1] = docListFileName;
        args[2] = directory;
        args[3] = filter;
        args[4] = instances;
        args[5] = "event-temp";   //  <==== lower-level bindings override this
        args[6] = "temp.source.dict";
        this.types = types;
        this.area = area;
        this.numberOfDocs = numberOfDocs;
        this.eventProgressMonitor = eventProgressMonitor;
    }

    public void run() {
        try {
            // force monitor to display during long initialization
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            if (null != eventProgressMonitor) {
                eventProgressMonitor.setProgress(2);
            }
            DepTreeMap depTreeMap = DepTreeMap.getInstance();
            depTreeMap.unpersist();
            DepPaths.progressMonitor = eventProgressMonitor;
            DepPaths.main(args);
       //   Corpus.sort("event-temp", types); //  <=====
            depTreeMap.load(true);
            // if(area != null) {
                // Corpus.displayTerms(types, 40, area, Corpus.eventFilter);
            // }
        } catch (IOException e) {
            System.out.println("IOException in DepTrees " + e);
            e.printStackTrace(System.err);
        }
    }
}
