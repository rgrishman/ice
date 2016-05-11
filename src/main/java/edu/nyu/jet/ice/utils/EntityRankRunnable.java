package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;
import edu.nyu.jet.ice.entityset.Entity;
import edu.nyu.jet.ice.entityset.EntitySetExpander;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityRankRunnable extends IceRunnable {
    private final EntitySetExpander expander;
    private final List<Entity> entities;

    public EntityRankRunnable(UUID uuid, IceAPI api, String corpusName, EntitySetExpander expander) {
	super(uuid, "Expand Entity Set", corpusName, "", api, expander.getEntityFeatureDictSize());
	this.expander = expander;
	this.entities = null;
    }

    public EntityRankRunnable(UUID uuid, IceAPI api, String corpusName, EntitySetExpander expander, List<Entity> entities) {
	super(uuid, "Expand Entity Set", corpusName, "", api, expander.getEntityFeatureDictSize());
	this.expander = expander;
	this.entities = entities;
    }

    @Override
   	public void run() {
        expander.setProgressMonitor(this);
	if (null == entities) {
	    expander.rank();
	} else {
	    expander.rerank(entities);
	}
    }
}
