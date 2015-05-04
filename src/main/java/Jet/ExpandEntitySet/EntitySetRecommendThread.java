package Jet.ExpandEntitySet;


import Jet.IceUI.EntitySetBuilderFrame;

/**
 * Created by yhe on 2/18/14.
 */
public class EntitySetRecommendThread extends Thread {
    private EntitySetExpander expander;
    private EntitySetBuilderFrame frame;
    private boolean showWindow;

    public EntitySetRecommendThread (EntitySetExpander expander, EntitySetBuilderFrame frame, boolean showWindow) {
        this.expander = expander;
        this.frame    = frame;
        this.showWindow = showWindow;
    }

    public EntitySetRecommendThread (EntitySetExpander expander, EntitySetBuilderFrame frame) {
        this.expander = expander;
        this.frame    = frame;
        this.showWindow = false;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            expander.recommend();
            frame.updateLists(expander.getPositives(), expander.getNegatives());
            if (showWindow) {
                frame.setSize(800, 540);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.positiveList.revalidate();
                frame.negativeList.revalidate();
                frame.positiveList.repaint();
                frame.negativeList.repaint();
            }
            //count++;
        } catch (Exception e) {
            System.err.println ("Exception in EntitySetExpander:\n");
            e.printStackTrace();
        }
    }

}
