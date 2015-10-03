package edu.nyu.jet.ice.utils;

import org.apache.commons.exec.*;

import java.util.ArrayList;

/**
 * Spawn and manage processes
 *
 * @author yhe
 * @version 1.0
 */
public class ProcessFarm {
    ArrayList<String>  tasks = new ArrayList<String>();
    ArrayList<DefaultExecuteResultHandler> processes = new ArrayList<DefaultExecuteResultHandler>();

    /**
     * Reset the tasks to be spawned
     */
    synchronized public void reset() {
        tasks = new ArrayList<String>();
        processes = new ArrayList<DefaultExecuteResultHandler>();
    }

    /**
     * Submit the current list for execution
     */
    synchronized public void submit() {
        try {
            for (String line : tasks) {
                CommandLine cmdLine = CommandLine.parse(line);

                DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

                ExecuteWatchdog watchdog = new ExecuteWatchdog(60 * 1000);
                Executor executor = new DefaultExecutor();
                executor.setExitValue(0);
                executor.setWatchdog(watchdog);
                executor.execute(cmdLine, resultHandler);
                processes.add(resultHandler);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hold the host thread until all spawned processes complete execution
     *
     * @return true if all tasks completed successfully, false otherwise
     */
    synchronized public boolean waitFor() {
        boolean success = true;
        for (DefaultExecuteResultHandler p : processes) {
            try {
                p.waitFor();
                int returnVal = p.getExitValue();
                if (returnVal != 0) {
                    success = false;
                }
            }
            catch (InterruptedException e) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Add a shell command to list waiting to be executed. Use submit() to execute
     * all commands in the list
     *
     * @param s A shell command string to be executed
     */
    synchronized public void addTask(String s) {
        tasks.add(s);
    }
}
