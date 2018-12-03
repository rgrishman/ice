package edu.nyu.jet.ice.events;

import java.util.*;

public class IceTreeFactory {

    public static Map<String, IceTree> iceTrees = new HashMap< String, IceTree>();

    public static IceTree getIceTree (String s)  {
	// analyze String into Tree
	String[] triggerAndArgs = s.split(" ");
	String trigger;
	if (triggerAndArgs.length < 2) {
	    trigger = "?";
	    System.out.println("invalid event:  " + s);
	    // Thread.dumpStack();
	    return null;
	} else {
	    trigger = triggerAndArgs[0];
	    List<String> argRoleList = new ArrayList<String>();
	    List<String> argValueList = new ArrayList<String>();
	    List<String> entityTypeList = new ArrayList<String>();
	    List<IceTree.MentionType> mentionType = new ArrayList<IceTree.MentionType>();
	
	    for (int i = 1; i < triggerAndArgs.length; i++) {
		String arg = triggerAndArgs[i];
		String role;
		String type;
		String value;
		int colon = arg.indexOf(":");
		if (colon >= 0) {
		    role = arg.substring(0, colon);
		    String typeAndValue = arg.substring(colon + 1);
		    int equal = typeAndValue.indexOf("=");
		    if (equal >= 0) {
			type = typeAndValue.substring(0, equal);
			value = typeAndValue.substring(equal + 1);
		    } else {
			type = typeAndValue;
			value = null;
		    }
		} else {
		    int equal = arg.indexOf("=");
		    if (equal >= 0) {
			role = arg.substring(0, equal);
			type = null;
			value = arg.substring(equal + 1);
		    } else {
			System.out.println("invalid event:  " + s);
			Thread.dumpStack();
			break;
		    }
		}
		argRoleList.add(role);
		argValueList.add(value);
		entityTypeList.add(type);
		mentionType.add(IceTree.MentionType.UNKNOWN);
	    }
	    String[] argRole = argRoleList.toArray(new String[0]);
	    String[] argValue = argValueList.toArray(new String[0]);
	    String[] entityType = entityTypeList.toArray(new String[0]);
	    return IceTreeFactory.getIceTree (trigger, argRole, entityType, argValue);
	}
    }

     public static IceTree getIceTree (String trigger, String[] argRole, 
	    String[] entityType, String[] argValue) {
	String s = IceTree.core(trigger, argRole, entityType, argValue);
	if (iceTrees.get(s) == null) {
	    iceTrees.put(s, new IceTree(s));
	    // System.out.println("creating IceTree " + s);
	}
	IceTree it = iceTrees.get(s);
	it.trigger = trigger;
	it.argRole = argRole;
	it.entityType = entityType;
	it.argValue = argValue;
	return it;
    }

}
