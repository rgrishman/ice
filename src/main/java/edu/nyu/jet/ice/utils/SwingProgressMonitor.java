package edu.nyu.jet.ice.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: joelsieh
 * Date: 7/9/14
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SwingProgressMonitor extends ProgressMonitor implements ProgressMonitorI {

   public SwingProgressMonitor(Component parentComponent,
                          Object message,
                          String note,
                          int min,
                          int max) {
       super(parentComponent, message, note, min, max);
   }
}
