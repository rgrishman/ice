package edu.nyu.jet.ice.models;

import java.util.*;

/**
 *  an entity type defined through ICE
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

	public List<String> getNames () {
		return names;
	}

	public void setNames (List<String> ls) {
		names = ls;
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
