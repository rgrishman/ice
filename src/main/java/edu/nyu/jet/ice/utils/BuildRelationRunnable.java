package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;
import edu.nyu.jet.ice.models.IcePath;

import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: joelsieh
 * Date: 8/13/14
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class BuildRelationRunnable extends IceRunnable {

    private List<IcePath> relationPaths = null;
    private String seed = null;

    public BuildRelationRunnable(UUID uuid, IceAPI api, String corpus, String seed, int corpusDocuments) {
   		super(uuid, "Build Relation", corpus, "", api, corpusDocuments);
        this.seed = seed;
   	}

   	@Override
   	public void run() {
   		// call api and set required fields on result, updating progress as req'd
   		relationPaths = getAPI().buildRelation(super.getCorpus(), seed, this);
   	}

    public List<IcePath> getRelationPaths() {
        return relationPaths;
    }

}
