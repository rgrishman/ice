package edu.nyu.jet.ice.entityset;

import edu.nyu.jet.ice.uicomps.EntitySetRankerFrame;

import java.util.List;

/**
 * Wrapper of the EntitySetExpander to run entity reranking in a separate thread
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
        } catch (Exception e) {
            System.err.println ("Exception in EntitySetExpander:\n");
            e.printStackTrace();
        }
    }
}
