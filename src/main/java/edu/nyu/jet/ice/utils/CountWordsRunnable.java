package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;

import java.util.UUID;

/**
 * Created by pmoore on 2014-07-10.
 */
public class CountWordsRunnable extends IceRunnable {
	public CountWordsRunnable(UUID uuid, IceAPI api, String corpus, int corpusDocuments) {
		super(uuid, "Count Words", corpus, "", api, corpusDocuments);
	}

	@Override
	public void run() {
		getAPI().countWords(super.getCorpus(), this);
	}
}
