package edu.nyu.jet.ice.utils;

import edu.nyu.jet.ice.controllers.IceAPI;
import edu.nyu.jet.ice.utils.ProgressMonitorI;

import java.util.UUID;

public abstract class IceRunnable implements Runnable, ProgressMonitorI {
    private final UUID uuid;
    private final String name;
    private final String corpus;
    private final String bgCorpus;
    private final IceAPI api;
    private int maxProgress = -1;
    private int progress = 0;			// indicates this hasn't started yet
    private String note = "Not started";
    private boolean isAlive = false;

    public IceRunnable(UUID uuid, String name, String corpus, String bgCorpus, IceAPI api, int maxProgress) {
	this.uuid = uuid;
        this.name = name;
	this.corpus = corpus;
	this.bgCorpus = bgCorpus;
	this.api = api;
	this.maxProgress = maxProgress;
	this.isAlive = true;
	this.note = "Not started";
    }

    public UUID getUUID() {
	return uuid;
    }
    
    public IceAPI getAPI() {
	return api;
    }

    public synchronized int getProgress() {
	return progress;
    }

    public String getName() {
        return name;
    }

    public String getCorpus() { return corpus; }
    
    public String getBGCorpus() { return bgCorpus; }
    
    public String getNote() { return note; }

    @Override
	public synchronized void setProgress(int progress) {
	    synchronized (this) {
		this.progress = progress;
		this.notifyAll();
	    }
	}
    
    @Override
	public boolean isCanceled() {
	return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAlive() {
	return isAlive;
    }
    public void setAlive(boolean a) {
	isAlive = a;
    }

    @Override
	public void setNote(String note) { this.note = note; }
    
    @Override
	public synchronized int getMaximum() { return maxProgress; }
    
    @Override
	public void setMaximum(int maximum) { this.maxProgress = maximum; }
}
