package edu.nyu.jet.ice.models;

import java.util.*;

/**
 *  a relation defined using ICE. It consists of the types of its
 *  arguments and a set of dependency paths.
 */

public class IceRelation {

	String name = "";

	String arg1type = "";

	String arg2type = "";

	List<String> paths = new ArrayList<String>();

    List<String> negPaths = new ArrayList<String>();

	// ---- property methods -----

	public String getName() {return name;}
	public void setName (String s) {name = s;}
	public List<String> getPaths() {return paths;}
	public void setPaths(List<String> s) {paths = s;}
	public String getArg1type() {return arg1type;}
	public void setArg1type (String s) {arg1type = s;}
	public String getArg2type() {return arg2type;}
	public void setArg2type (String s) {arg2type = s;}

    public List<String> getNegPaths() {
        return negPaths;
    }

    public void setNegPaths(List<String> negPaths) {
        this.negPaths = negPaths;
    }
    // ---- constructors -----

	public IceRelation (String name) {
		this.name = name;
		this.paths = new ArrayList<String>();
	}

	public IceRelation () {
		this ("?");
	}

        public static boolean validPath (String s) {
            return (s.split("--").length == 3);
        }

        public static void reportInvalidPath (String s) {
            if (!validPath(s)) {
                System.err.println ("Invalid path: " + s + " at ");
                (new Throwable()).printStackTrace();
            }
        }

        public boolean invertedPath (String path) {
            if (!validPath(path))
                return false;
            String[] pp = path.split("--");
            String first = pp[0].trim();
            String last = pp[pp.length - 1].trim();
            return first.equals(arg2type) && last.equals(arg1type);
        }

        public boolean isValid () {
            for (String path : paths) {
                if (!validPath(path))
                    return false;
                String[] pp = path.split("--");
                String first = pp[0].trim();
                String last = pp[pp.length - 1].trim();
                if ((first.equals(arg1type) && last.equals(arg2type)) ||
                    (first.equals(arg2type) && last.equals(arg1type)))
                    continue;
                return false;
            }
            return true;
        }

    @Override
    public String toString() {
        return name;
    }

}
