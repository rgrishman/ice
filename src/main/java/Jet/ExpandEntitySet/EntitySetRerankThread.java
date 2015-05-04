package Jet.ExpandEntitySet;

import Jet.IceUI.EntitySetRankerFrame;

import java.awt.*;
import java.util.List;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class EntitySetRerankThread extends Thread  {
    private EntitySetExpander expander;
    private EntitySetRankerFrame frame;
    private List<Entity> entities;


    public EntitySetRerankThread(EntitySetExpander expander, EntitySetRankerFrame frame, List<Entity> entities) {
        this.expander = expander;
        this.frame    = frame;
        this.entities = entities;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            expander.rerank(entities);
            frame.updateList();
            frame.listPane.validate();
            frame.listPane.repaint();
//            frame.setVisible(false);
//            frame.listPane.setSize(new Dimension(350, 300));
//            frame.listPane.setPreferredSize(new Dimension(350, 300));
//            frame.setSize(400, 525);
//            frame.setAlwaysOnTop(true);
//            frame.setVisible(true);
//
//            frame.listPane.revalidate();
//            frame.listPane.repaint();
//            frame.rankedList.revalidate();
//            frame.rankedList.repaint();
//            frame.listPane.revalidate();
//            frame.listPane.repaint();
//            frame.rankedList.revalidate();
//            frame.rankedList.repaint();
        } catch (Exception e) {
            System.err.println ("Exception in EntitySetExpander:\n");
            e.printStackTrace();
        }
    }
}
