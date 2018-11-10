package edu.nyu.jet.aceJet;

import edu.nyu.jet.ice.events.IceTree;
import edu.nyu.jet.ice.events.IceTreeFactory;
import java.util.*;
import java.io.*;

public class AnchoredTreeSet implements Iterable<IceTree> {

    ArrayList<IceTree> paths = new ArrayList<IceTree>();
    Map<String, List<IceTree>> pathIndex = new HashMap<String, List<IceTree>>();
    Map<String, List<IceTree>> argIndex = new HashMap<String, List<IceTree>>();
    int count;

    public AnchoredTreeSet (String fileName) throws IOException {
	BufferedReader reader = new BufferedReader (new FileReader (fileName));
	String line;
	count = 0;
	while ((line = reader.readLine()) != null) {
	    add (line);
	}
	System.out.println ("Loaded " + count + " trees.");
    }

    public void add (String line) {
	int j = line.indexOf("\t");
	if (j >= 0) {
	    line = line.substring(j + 1);
	}
	IceTree p = IceTreeFactory.getIceTree(line);
	if (p == null)
	    return;
	paths.add(p);
	String trigger = p.getTrigger();
	if (pathIndex.get(trigger) == null)
	    pathIndex.put(trigger, new ArrayList<IceTree>());
	pathIndex.get(trigger).add(p);
	String args = p.getArgValue("nsubj") + ":" + p.getArgValue("dobj");
	if (argIndex.get(args) == null)
	    argIndex.put(args, new ArrayList<IceTree>());
	argIndex.get(args).add(p);
	count++;
    }

    public List<IceTree> getByPath (String path) {
	return pathIndex.get(path);
    }

	public List<IceTree> getByArgs (String arg1, String arg2) {
		return getByArgs (arg1 + ":" + arg2);
	}

	public List<IceTree> getByArgs (String args) {
		return argIndex.get(args);
	}
	
        /**
         *      returns an Iterator over the paths in the AnchoredTreeSet.
         */

        @Override
            public Iterator<IceTree> iterator() {
                return paths.iterator();
            }
}
