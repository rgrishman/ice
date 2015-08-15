package edu.nyu.jet.ice.utils;

import javax.swing.*;
import java.awt.*;

public class DummyMonitor implements ProgressMonitorI {
    @Override
	public boolean isCanceled() {
	return false;
    }

    @Override
	public void setProgress(int docCount) {

    }

    @Override
	public void setMaximum(int maximum) {

    }

    @Override
	public int getMaximum() {
	return 0;
    }

    @Override
	public void setNote(String s) {

    }

    public String getNote() { return ""; }
}