package edu.nyu.jet.ice.models;

import java.util.*;

/**
 *  Generates instances of IcePath so that there is a unique instance for each
 *  value of the path string.
 */

public class IcePathFactory {

    public static Map<String, IcePath> icePaths = new HashMap< String, IcePath>();

    public static IcePath getIcePath (String s)  {
        if (icePaths.get(s) == null) {
            icePaths.put(s, new IcePath(s));
        }
        return icePaths.get(s);
        // System.out.println("creating IcePath " + s);
    }

}
