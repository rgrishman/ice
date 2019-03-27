package edu.nyu.jet.ice.events;

import java.util.*;

public class IceTreeFactory {

    public static Map<String, IceTree> iceTrees = new HashMap< String, IceTree>();

    /**
     *  performs 4 functions:
     *  - parses the String 's' into a role-based strucure
     *  - standardizes rhe argument order with 'sortArgs'
     *  - generates string repr with normalized role order and spacing
     *  - creates unique copy with this normalized string
     */

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
        sortArgs (argRole, entityType, argValue);
	    return IceTreeFactory.getIceTree (trigger, argRole, entityType, argValue);
	}
    }

    public static IceTree getIceTree (String trigger, String[] argRole, 
	    String[] entityType, String[] argValue) {
        sortArgs (argRole, entityType, argValue);
        String s = IceTree.core(trigger, argRole, entityType, argValue);
        IceTree it = iceTrees.get(s);
        if (it == null) {
            it = new IceTree(trigger, argRole, entityType, argValue);
            iceTrees.put(s, it);;
        }
        return it;
    }

    /**
     *  Creates a normal form for IceTrees in which the roles are
     *  stored in lexicographic order.
     */
    public static void  sortArgs (String[] argRole, String[] argValue, String[] argType) {
        TreeMap<String, String> valueMap = new TreeMap<String, String>();
        TreeMap<String, String> typeMap = new TreeMap<String, String>();
        for (int i = 0; i < argRole.length; i++) {
            valueMap.put(argRole[i], argValue[i]);
            typeMap.put(argRole[i], argType[i]);
        }
        int j = 0;
        for (String role : valueMap.keySet() ) {
            argRole[j] = role;
            argValue[j] = valueMap.get(role);
            argType[j] = typeMap.get(role);
            j++;
        }
    }

}
