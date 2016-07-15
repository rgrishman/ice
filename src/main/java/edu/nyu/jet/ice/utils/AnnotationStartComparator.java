package edu.nyu.jet.ice.utils;

import edu.nyu.jet.tipster.Annotation;

import java.util.Comparator;

/**
 * Compares 2 Jet annotations by their start offsets
 *
 * @author yhe
 * @version 1.0
 */
public class AnnotationStartComparator implements Comparator<Annotation> {
    public int compare(Annotation annotation, Annotation annotation2) {
        return annotation.start() - annotation2.start();
    }
}
