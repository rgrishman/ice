package edu.nyu.jet.ice.events;

import edu.nyu.jet.ice.relation.Bootstrap;
import edu.nyu.jet.ice.events.SwingEventsPanel;

/**
 *  When the 'expand' button om the relation frame is pushed, the SwingEventsPanel 
 *  creates a EventBuilderThread to perform the computations required to
 *  generate a list of candidate relation patterns (which are then reviewed
 *  by the user).
 *
 *  Created by yhe on 10/14/14.
 */
public class EventBuilderThread extends Thread {

    String[] args;
    // RelationBuilder builder;
    EventBootstrap bootstrap;
    String arg1;
    String arg2;
    EventBuilderFrame frame;
    SwingEventsPanel swingEventsPanel;

    public EventBuilderThread (String seed, 
	    String eventInstanceFileName,
	    String pathListFileName, 
	    /* RelationBuildera*/ Object builder,
	    EventBootstrap bootstrap,
	    EventBuilderFrame frame, 
	    SwingEventsPanel swingEventsPanel) {
	args = new String[3];
        args[0] = seed;
        String[] parts = seed.trim().toLowerCase().split(" ");
        if (parts.length > 1) {
            arg1 = parts[0].toUpperCase();
            arg2 = parts[parts.length - 1].toUpperCase();
        }
        args[1] = eventInstanceFileName;
        args[2] = pathListFileName;
        // this.builder = builder;
        this.bootstrap = bootstrap;
        this.frame = frame;
        this.swingEventsPanel = swingEventsPanel;
    }

    public void run() {
        try {
            bootstrap.initialize(args[0], args[1]);
            frame.updateList();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.listPane.revalidate();
            frame.listPane.repaint();
            frame.rankedList.revalidate();
            frame.rankedList.repaint();
        } catch (Exception e) {
            System.err.println("Exception in Jet.RelationAL.Bootstrap: ");
            e.printStackTrace();
        }
    }
}
