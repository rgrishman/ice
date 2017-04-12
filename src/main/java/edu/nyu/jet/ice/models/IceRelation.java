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

	private List<String> reprs = new ArrayList<String>();

	List<String> paths = new ArrayList<String>();

	List<String> cleanPaths = new ArrayList<String>();

        private Map<String, Boolean> inverted;

        private boolean valid;

        List<String> negPaths = new ArrayList<String>();

	// ---- property methods -----

	/**
	 *  Returns the name of the relation.
	 */

	public String getName() {return name;}

	public void setName (String s) {name = s;}

	/**
	 *  Add 'repr' to the set of English phrases representing the relation.
	 */

	public void addRepr (String repr) {
	    if (! reprs.contains(repr))
		reprs.add(repr);
	}

	public void removeRepr (String repr) {
	    reprs.remove(repr);
	}

	public List<String> getReprs() {
	    return reprs;
	}

	public void updatePaths() {
	    DepPathMap depPathMap = DepPathMap.getInstance();
	    depPathMap.load();
	    paths.clear();
	    for (String repr : reprs) {
		List<String> paths = depPathMap.findPath(repr);
		addPaths(paths);
	    }
	}

        /**
         *  Returns the list of paths associated with this relation,
         *  including any subscripts required to indicate argument order.
         */

	public List<String> getPaths() {
            return paths;}

        /**
         *  Returns the list of paths associated with this relation,
         *  excluding subscripts required to indicate argument order.
         */

        public List<String> getCleanPaths () {
            return cleanPaths;
        }

	public void setPaths(List<String> s) {
            analyzeSubscripts (s);
        }

	public void addPath (String path) {
	    if (! paths.contains(path)) {
		paths.add(path);
		analyzeSubscripts(paths);
		System.out.println("Added path " + path + " to relation " + name); // <<<
	    }
	}

	public void addPaths (List<String> paths) {
	    for (String p : paths)
		addPath(p);
	}

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

        /**
         *  Returns true if 'path' specifies an inverted relation:  if the
         *  argument which comes first in text order is the second argument
         *  to the relation.
         */

        public boolean invertedPath (String path) {
            if (inverted == null)
                analyzeSubscripts(paths);
            Boolean r = inverted.get(path);
            if (r == null) return false;
            return r;
        }

        /**
         *  Returns true if no error was detected in constructing the IceRelation.
         */

        public boolean isValid () {
            return valid;
        }

        /**
         *  Determines argument order (invertedPath or not) for each path
         *  based on argument types and subscripts.
         */

        private void analyzeSubscripts (List<String> rawPaths) {
            paths = rawPaths;
            valid = true;
            cleanPaths = new ArrayList<String>();
            inverted = new HashMap<String, Boolean>();
            for (String path : rawPaths) {
                if (!validPath(path)) {
                    valid = false;
                    break;
                }
                String[] pp = path.split("--");
                String first = pp[0].trim();
                String mid = pp[1].trim();
                String last = pp[2].trim();
                boolean inv = first.endsWith("(2)") || first.equals(arg2type);
                if (first.endsWith("(1)") || first.endsWith("(2)"))
                    first = first.substring(0, first.length() - 3);
                if (last.endsWith("(1)") || last.endsWith("(2)"))
                    last = last.substring(0, last.length() - 3);
                if (!((first.equals(arg1type) && last.equals(arg2type)) ||
                      (first.equals(arg2type) && last.equals(arg1type))))
                    valid = false;
                String fullPath = first + " -- " + mid + " -- " + last;
                cleanPaths.add(fullPath);
                inverted.put(fullPath, inv);
            }
        }

        @Override
            public String toString() {
                return name;
            }

        public String report () {
            String r = name + "(" + arg1type + ", " + arg2type + ")\n";
            for (String path : paths)
                r += path + "\n";
                return r;
        }
}
