package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;
import edu.nyu.jet.ice.models.*;
import edu.nyu.jet.ice.utils.IceInfoStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IndexCorpusRunnable extends IceRunnable {
    private final Index index;
    private final Corpus corpus;

    public IndexCorpusRunnable(UUID uuid, IceAPI api, String corpusName, Corpus corpus, Index index, int corpusDocuments) {
	super(uuid, "Index Corpus for Entities", corpusName, "", api, corpusDocuments);
	this.index = index;
	this.corpus = corpus;
    }

    @Override
   	public void run() {
	index.setStatus(IceInfoStatus.PROCESSING);
	getAPI().indexCorpusForEntities(super.getCorpus(), index.getName(), index.getType(), index.getCutoff(), this);

	corpus.addIndex(index.getName(), index);
	getAPI().saveStateToDefault();
	index.setStatus(IceInfoStatus.PROCESSED);
    }
}
