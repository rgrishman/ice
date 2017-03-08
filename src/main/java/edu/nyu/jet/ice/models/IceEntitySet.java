package edu.nyu.jet.ice.models;

import java.util.*;

/**
 *  An entity type defined through ICE, consisting of a set of names
 *  and a set of nouns.  (At present, only the nouns are used.)
 */

public class IceEntitySet {

	String type;

	List<String> nouns;

	List<String> names;

	// ----- property methods -----

	public String getType () {
		return type;
	}

	public void setType (String tp) {
		type = tp;
	}

	public List<String> getNouns () {
		return nouns;
	}

	public void setNouns (List<String> ls) {
		nouns = ls;
	}

	public void addNoun (String noun) {
	    	if (!nouns.contains(noun))
		    	nouns.add(noun);
	}

	public void removeNoun (String noun) {
	    	nouns.remove(noun);
	}

	public List<String> getNames () {
		return names;
	}

	public void setNames (List<String> ls) {
		names = ls;
	}

	public void addName (String name) {
	    	if (!names.contains(name))
		    	names.add(name);
	}

	public void removeName (String name) {
	    	names.remove(name);
	}
	// ----- constructors -----

	public IceEntitySet (String s) {
		type = s;
		nouns = new ArrayList<String>();
		names = new ArrayList<String>();
	}

	public IceEntitySet () {
		this("?");
	}

	public String toString() {
	    return type;
	}
}
