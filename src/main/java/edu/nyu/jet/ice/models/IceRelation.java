package edu.nyu.jet.ice.models;

import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;

import java.util.*;

/**
 *  a relation defined using ICE. It consists of the types of its
 *  arguments and a set of dependency paths.
 */

public class IceRelation {

       Logger logger = LoggerFactory.getLogger(IceRelation.class);

       private String name = "";

       private String arg1type = "";

       private String arg2type = "";

       private List<String> reprs = new ArrayList<String>();

       private List<String> paths = new ArrayList<String>();

       private List<String> barePaths = new ArrayList<String>();

       private Map<String, Boolean> inverted = new HashMap<String, Boolean>();;

       private List<String> negPaths = new ArrayList<String>();

       // ---- property methods -----

       /**
        *  Returns the name of the relation.
        */

       public String getName() {return name;}

       /**
        *  Set the name of the relation.
        */

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
         *  including the relation arguments.
         */

       public List<String> getPaths() {
            return paths;}

        /**
         *  Returns the list of paths associated with this relation,
         *  excluding the endpoints of the path (the relation arguments).
         */

        public List<String> getBarePaths () {
            return barePaths;
        }

       public void setPaths(List<String> s) {
            paths.clear();
	    barePaths.clear();
	    inverted.clear();
	    addPaths (s);
        }

       /**
	*  Add <CODE>path</CODE> as one of the paths for this relation.
	*  <CODE>path</CODE> must be a full path, including relation arguments.
	*/

       public void addPath (String path) {
           if (! paths.contains(path)) {
               paths.add(path);
	       logger.info ("Added path {} to relation {}", path, name);
               addBarePath(path);
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
	}

	public IceRelation () {
		this ("?");
	}

        private static boolean validPath (String s) {
            return (s.split("--").length == 3);
        }

        /**
         *  Returns true if 'path' specifies an inverted relation:  if the
         *  argument which comes first in text order is the second argument
         *  to the relation.  'path' must be a bare path,without any subscripts.
         */

        public boolean invertedPath (String path) {
            return inverted.get(path);
        }

        /**
         *  Determines argument order (invertedPath or not) for each path
         *  based on arrgument types and subscripts.
         */

        private void addBarePath (String path) {
	    String[] pp = path.split("--");
	    if (pp.length != 3) {
	        logger.error ("Attempting to add invalid path {} to relation", path);
	        return;
	    }
	    String first = pp[0].trim();
	    String mid = pp[1].trim();
	    String last = pp[2].trim();
	    boolean inv = first.endsWith("(2)") || first.equals(arg2type);
	    if (first.endsWith("(1)") || first.endsWith("(2)"))
		first = first.substring(0, first.length() - 3);
	    if (last.endsWith("(1)") || last.endsWith("(2)"))
		last = last.substring(0, last.length() - 3);
	    String barePath = first + "--" + mid + "--" + last;
	    barePaths.add(barePath);
	    inverted.put(barePath, inv);
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
