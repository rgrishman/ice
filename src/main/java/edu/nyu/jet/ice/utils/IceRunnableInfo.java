package edu.nyu.jet.ice.utils;

import java.util.UUID;

public class IceRunnableInfo {
	private UUID id;
	private String name;
	private String corpus;
	private String bgCorpus;
	private int progress;
	private String note;
    private boolean isAlive;

	public IceRunnableInfo(IceRunnable source) {
		this.id = source.getUUID();
		this.name = source.getName();
		this.corpus = source.getCorpus();
		this.bgCorpus = source.getBGCorpus();
		this.progress = source.getProgress();
		this.note = source.getNote();
		this.isAlive = source.isAlive();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCorpus() {
		return corpus;
	}

	public void setCorpus(String corpus) {
		this.corpus = corpus;
	}

	public String getBGCorpus() {
		return bgCorpus;
	}

	public void setBGCorpus(String bgCorpus) {
		this.bgCorpus = bgCorpus;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getNote() {
		return note;
	}

    public boolean isAlive() {
	return isAlive;
    }

    public void setAlive(boolean a) {
	isAlive = a;
    } 

	public void setNote(String note) {
		this.note = note;
	}
}
