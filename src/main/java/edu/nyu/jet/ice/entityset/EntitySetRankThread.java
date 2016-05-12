package edu.nyu.jet.ice.entityset;

import edu.nyu.jet.ice.uicomps.EntitySetRankerFrame;

/**
 * Wrapper of the EntitySetExpander to run it in a separate Thread
 *
 * @author yhe
 * @version 1.0
 */
public class EntitySetRankThread extends Thread  {
    private EntitySetExpander expander;
    private EntitySetRankerFrame frame;
    private boolean showWindow;

    public EntitySetRankThread (EntitySetExpander expander, EntitySetRankerFrame frame, boolean showWindow) {
        this.expander = expander;
        this.frame    = frame;
        this.showWindow = showWindow;
    }

    public EntitySetRankThread (EntitySetExpander expander, EntitySetRankerFrame frame) {
        this.expander = expander;
        this.frame    = frame;
        this.showWindow = false;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            showWindow = expander.rank();
            frame.updateList();
            //frame.updateLists(expander.getPositives(), expander.getNegatives());
            if (showWindow) {
                // frame.setSize(400, 525);
                frame.setAlwaysOnTop(true);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.listPane.revalidate();
                frame.listPane.repaint();
                frame.rankedList.revalidate();
                frame.rankedList.repaint();
//                frame.positiveList.revalidate();
//                frame.negativeList.revalidate();
//                frame.positiveList.repaint();
//                frame.negativeList.repaint();
            }
            //count++;
        } catch (Exception e) {
            System.err.println ("Exception in EntitySetExpander:\n");
            e.printStackTrace();
        }
    }
}
