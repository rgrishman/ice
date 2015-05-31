package edu.nyu.jet.ice.utils;

/**
 * Created with IntelliJ IDEA.
 * User: joelsieh
 * Date: 7/9/14
 * Time: 4:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ProgressMonitorI {

    boolean isCanceled();

    void setProgress(int docCount);

	void setMaximum(int maximum);

	int getMaximum();

    void setNote(String s);
}
