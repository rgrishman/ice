package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;

import java.util.UUID;

/**
 * Created by grvsmth, August 15, 2015
 */
public class PreprocessRunnable extends IceRunnable {
    private String filter;

    public PreprocessRunnable(UUID uuid, IceAPI api, String corpus, int corpusDocuments, String filter) {
	super(uuid, "Preprocess", corpus, "", api, corpusDocuments);
	this.filter = filter;
    }

    @Override public void run() {
	// call api and set required fields on result, updating progress as req'd
	getAPI().preprocess(super.getCorpus(), this.filter, this);
    }
}
