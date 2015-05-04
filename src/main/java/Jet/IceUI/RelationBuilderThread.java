package Jet.IceUI;

import Jet.RelationAL.Bootstrap;
import edu.nyu.jet.ice.views.swing.SwingRelationsPanel;

/**
 * Created by yhe on 10/14/14.
 */
public class RelationBuilderThread extends Thread {

    String[] args;
    RelationBuilder builder;
    Bootstrap bootstrap;
    String arg1;
    String arg2;
    RelationBuilderFrame frame;
    SwingRelationsPanel swingRelationsPanel;

    public RelationBuilderThread(String seed, String relationInstanceFileName,
                          String pathListFileName, RelationBuilder builder, Bootstrap bootstrap,
                          RelationBuilderFrame frame, SwingRelationsPanel swingRelationsPanel) {
        args = new String[3];
        args[0] = seed;
        String[] parts = seed.trim().toLowerCase().split(" ");
        if (parts.length > 1) {
            arg1 = parts[0].toUpperCase();
            arg2 = parts[parts.length - 1].toUpperCase();
        }
        args[1] = relationInstanceFileName;
        System.out.println("plfn = " + pathListFileName);
        args[2] = pathListFileName;
        this.builder = builder;
        this.bootstrap = bootstrap;
        this.frame = frame;
        this.swingRelationsPanel = swingRelationsPanel;
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
            System.out.println("Exception in Jet.RelationAL.Bootstrap " + e);
        }
    }
}
