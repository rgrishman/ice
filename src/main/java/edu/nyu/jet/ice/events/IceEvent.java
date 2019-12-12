package edu.nyu.jet.ice.events;

import edu.nyu.jet.Logger;
import edu.nyu.jet.LoggerFactory;
import edu.nyu.jet.ice.models.*;

import java.util.*;

/**
 *  An event defined using ICE. It consists of the types of its
 *  arguments and a set of dependency trees.
 */

public class IceEvent {

       static final Logger logger = LoggerFactory.getLogger(IceEvent.class);

       private String name = "";

       private List<String> reprs = new ArrayList<String>();

       private List<IceTree> trees = new ArrayList<IceTree>();

       private List<IceTree> negTrees = new ArrayList<IceTree>();


       // ---- property methods -----

       /**
        *  Returns the name of the event.
        */

       public String getName() {return name;}

       /**
        *  Set the name of the event.
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

       public void updateTrees() {
           DepTreeMap depTreeMap = DepTreeMap.getInstance();
           trees = new ArrayList<IceTree>();
           depTreeMap.loadTrees();
           for (String repr : reprs) {
               List<IceTree> trees = depTreeMap.findTree(repr);
               addTrees(trees);
           }
       }

        /**
         *  Returns the list of trees associated with this event.
         */

       public List<IceTree> getTrees() {
            return trees;
       }

       public void setTrees (List<IceTree> it) {
           trees = it;
       }

       public void removeTree (IceTree tree) {
           trees.remove(tree);
       }

   /**
	*  Add <CODE>tree</CODE> as one of the trees for this event.
	*/

	public void addTrees (List<IceTree> trees) {
	    if (trees != null)
		for (IceTree p : trees)
		    addTree(p);
	}

        public List<IceTree> getNegTrees() {
            return negTrees;
        }

        public void setNegTrees(List<IceTree> negTrees) {
            this.negTrees = negTrees;
        }

        public void addNegTree (IceTree negTree) {
            negTrees.add(negTree);
        }

    // ---- constructors -----

	public IceEvent (String name) {
		this.name = name;
	}

	public IceEvent () {
		this ("?");
	}

        @Override
            public String toString() {
                return name;
            }

        public String report () {
            String r = name + " (event)\n";
            for (IceTree tree : trees)
                r += tree + "\n";
                return r;
        }

    public void addTree (IceTree it) {
        trees.add(it);
        logger.info ("Added tree {} to event {}", it, name);
    }

}
