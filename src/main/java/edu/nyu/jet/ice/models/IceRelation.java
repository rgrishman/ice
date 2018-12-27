package edu.nyu.jet.ice.models;

import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;

import java.util.*;

/**
 *  a relation defined using ICE. It consists of the types of its
 *  arguments and a set of dependency paths. The argumenta may
 *  include subscripts;  if the subscripts have been removed, it
 *  is a 'bare path';  if the arguments have been instantiated, it is
 *  an anchored path (see the AnchoredPath class).`
 */

public class IceRelation {

    Logger logger = LoggerFactory.getLogger(IceRelation.class);

    private String name = "";

    private String arg1type = "";

    private String arg2type = "";

    private IcePath[] icePaths = new IcePath[0];

    private Map<String, Boolean> inverted = new HashMap<String, Boolean>();;

    private IcePath[] negPaths;

    private IcePath[] barePaths;

    // ---- property methods -----

    public IcePath[] getPaths () {
        return icePaths;
    }

    public IcePath getPaths (int i) {
        return icePaths[i];
    }

    public void setPaths (IcePath[] ip) {
        icePaths = ip;
    }

    public void setPaths (IcePath[] ip, int i) {
        icePaths[i] = ip[i];
    }

    public IcePath[] getBarePaths () {
        return barePaths;
    }

    /**
     *  Returns the name of the relation.
     */

    public String getName() {return name;}

    /**
     *  Set the name of the relation.
     */

    public void setName (String s) {name = s;}

    /* previously called by SwingRelationsPanel

       public void updatePaths() {
           DepPathMap.load();
           paths.clear();
           for (String repr : reprs) {
               List<String> paths = depPathMap.findPath(repr);
               addPaths(paths);
           }
       }
       */

    /**
     *  Add <CODE>path</CODE> as one of the paths for this relation.
     *  <CODE>path</CODE> must be a full path, including relation arguments.
     */

    public void addPath (IcePath p) {
        int numPaths = icePaths.length;
        for (int i = 0; i < numPaths; i++) {
            if (icePaths[i].equals(p)) {
                return;
            }
        }
        IcePath[] enlarged = Arrays.copyOf(icePaths, numPaths + 1);
        enlarged[numPaths] = p;
        icePaths = enlarged;
        logger.info ("Added path {} to relation {}", p, name);
	}

    public void addPaths (List<IcePath> sip) {
        for (IcePath ip : sip)
            addPath (ip);
    }

    /**
     *  Deletes from the relation all paths with a given repr
     *  (i.e., which are rendered to the user using the same text.
     */

    public void deletePaths (String repr) {
        List<IcePath> remaining = new ArrayList<IcePath>();
        for (IcePath ip : icePaths)
            if (!ip.getRepr().equals(repr))
                remaining.add(ip);
        icePaths = remaining.toArray(new IcePath[0]);
    }
    
	public String getArg1type() {return arg1type;}
	public void setArg1type (String s) {arg1type = s;}
	public String getArg2type() {return arg2type;}
	public void setArg2type (String s) {arg2type = s;}

    public IcePath[] getNegPaths() {
        return negPaths;
    }

    public void setNegPaths(IcePath[] negPaths) {
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

        private void addBarePath (IcePath path) {
            String[] pp = path.getPathString().split("--");
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
            String barePathString = first + "--" + mid + "--" + last;
            IcePath barePath = new IcePath(barePathString);
         // barePaths.add(barePath);
            int numPaths = barePaths.length;
            IcePath[] r = new IcePath[numPaths + 1];
            for (int i = 0; i < numPaths; i++){
                r[i] = barePaths[i];
            }
            icePaths[numPaths + 1] = barePath;;
            barePaths = r;
            inverted.put(barePath.getPathString(), inv);
	}

        @Override
            public String toString() {
                return name;
            }

        public String report () {
            String r = name + "(" + arg1type + ", " + arg2type + ")\n";
            for (IcePath p : icePaths)
                r += p.toString() + "\n";
                return r;
        }
}
