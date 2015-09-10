package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;

import java.util.UUID;

public class FindPatternsRunnable extends IceRunnable {
    public FindPatternsRunnable(UUID uuid, IceAPI api, String corpus, int corpusDocuments) {
        super(uuid, "Find Patterns", corpus, "", api, corpusDocuments);
    }

    @Override
    public void run() {
        getAPI().findPatterns(super.getCorpus(), this);
	getAPI().saveStateToDefault();
    }
}
