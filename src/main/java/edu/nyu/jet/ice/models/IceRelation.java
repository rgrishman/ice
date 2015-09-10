package edu.nyu.jet.ice.models;

import java.util.*;

/**
 *  a relation defined using ICE. It consists of the types of its
 *  arguments and a set of dependency paths.
 */

public class IceRelation {

	String name;

	String arg1type;

	String arg2type;

	List<String> paths;

    List<String> negPaths;

    List<IcePath> icePaths;

	// ---- property methods -----

	public String getName() {return name;}
	public void setName (String s) {name = s;}
	public List<String> getPaths() {return paths;}
	public void setPaths(List<String> s) {paths = s;}
	public String getArg1type() {return arg1type;}
	public void setArg1type (String s) {arg1type = s;}
	public String getArg2type() {return arg2type;}
	public void setArg2type (String s) {arg2type = s;}

    public List<IcePath> getIcePaths() {
	return icePaths;
    }

    public void setIcePaths (List<IcePath> paths) {
	icePaths = paths;
    }

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

    @Override
    public String toString() {
        return name;
    }

}
