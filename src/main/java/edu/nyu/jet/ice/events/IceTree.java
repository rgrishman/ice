package edu.nyu.jet.ice.events;

import edu.nyu.jet.tipster.*;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.aceJet.EDTtype;
import java.util.*;
import java.util.regex.*;

public class IceTree implements Comparable <IceTree> {

    String trigger;
    int triggerPosn;
    List<String> argRole;
    List<Integer> argPosn;
    List<String> argValue;
    List<MentionType> mentionType;
    List<String> entityType;
    String repr;

    String toolTip;
    double score;
    int count;

    public IceTree () {
	System.out.println("?");
    }

    public IceTree (String trigger, List<String> argRole, List<String> entityType, List<String> argValue) {
	String s = IceTree.core(trigger, argRole, entityType, argValue);
	IceTreeFactory.getIceTree(s);
    }

    static String pattern = "^([A-Za-z_]+)(:([A-Za-z0-9._]+))?(=([A-Za-z0-9._]+))?$";
    
    static Pattern r = Pattern.compile(pattern);

    public enum IceTreeChoice {
	NO, YES, UNDECIDED
    }
    
    public IceTreeChoice getChoice() {
	return choice;
    }

    IceTreeChoice choice = IceTreeChoice.UNDECIDED;

    public void setChoice(IceTreeChoice choice) {
	this.choice = choice;
    }

    public enum MentionType {
	PRONOUN, NOMINAL, NAME, UNKNOWN
    }
    

    public MentionType getMentionType (int i) {
	return mentionType.get(i);
    }

    public String getTrigger () {
	return trigger;
    }

    public String getArgValue (String role) {
	for (int i = 0; i < argRole.size(); i++) {
	    if (argRole.get(i).equals(role))
		return argValue.get(i);
	}
	return null;
    }

    public static IceTree clearArgValues (IceTree it) {
	List<String> newArgValue = new ArrayList<String>();
	for (int i = 0; i < it.argRole.size(); i++) {
	    newArgValue.add(null);
	}
	return IceTreeFactory.getIceTree(it.trigger, it.argRole, it.entityType, newArgValue);
    }

    public int getTriggerPosn () {
	return triggerPosn;
    }

    public String getRepr() {
	if (repr == null)
	    repr = linearize();
	return repr;
    }

    public void setRepr (String r) {
	repr = r;
    }

    public String getToolTip () {
	return toolTip;
    }

    public void setToolTip (String t) {
	toolTip = t;
    }
    public double getScore() {
	return score;
    }

    public void setScore (double d) {
	score = d;
    }

    public IceTree (IceTree it) {
	trigger = it.trigger;
	triggerPosn = it.triggerPosn;
	argRole = it.argRole;
	argPosn = it.argPosn;
	argValue = it.argValue;
    }

    public IceTree (String s) {
	// analyze String into Tree
	String[] triggerAndArgs = s.split(" ");
	if (triggerAndArgs.length < 2) {
	    trigger = "?";
	    System.out.println("invalid event:  " + s);
	    // Thread.dumpStack();
	} else {
	    trigger = triggerAndArgs[0];
	    argRole = new ArrayList<String>();
	    argValue = new ArrayList<String>();
	    entityType = new ArrayList<String>();
	    mentionType = new ArrayList<MentionType>();
	
	    for (int i = 1; i < triggerAndArgs.length; i++) {
		Matcher m = r.matcher(triggerAndArgs[i]);
		if (m.find()) {
		    String role = m.group(1);
		    String type = m.group(3);
		    String value = m.group(5);
		    argRole.add(role);
		    argValue.add(value);
		    entityType.add(type);
		    mentionType.add(MentionType.UNKNOWN);
		} else {
		    System.out.println("invalid event:  " + s);
	// 	    Thread.dumpStack();
		    break;
		}
	    }
	}
    }
     
    public int compareTo (IceTree iceTree) {
	if (this.score < iceTree.score) return 1;
	if (this.score > iceTree.score) return -1;
	return 0;
    }

    @Override
   public boolean equals (Object other) {
       boolean result = false;
       if (other instanceof IceTree) {
	   IceTree iceTree = (IceTree) other;
	   result = this.core().equals(iceTree.core());
       }
       return result;
   }

    @Override
   public int hashCode () {
       return this.core().hashCode();
   }

    /**
      *  Extract all clausal dependency trees from a document.  
      */

    public static List<IceTree>  extract (Document doc, Span span, SyntacticRelationSet relations) {
	List<IceTree> result = new ArrayList<IceTree>();
	// create index: map from source position in SyntacticRelationSet to a list of all
	// syntactic relations sarting at that position
	Map<Integer,List<SyntacticRelation>> index = new HashMap<Integer, List<SyntacticRelation>>();
	SyntacticRelationSet localSRS = new SyntacticRelationSet();
	for (SyntacticRelation rel : relations) {
	    int p = rel.sourcePosn;
	    if (p < span.start() || p >= span.end()) continue;
	    localSRS.add(rel);
	    if (index.get(p) == null) {
		index.put(p, new ArrayList<SyntacticRelation>());
	    }
	    index.get(p).add(rel);
	}
	addPrepLinks(localSRS, index);
      loop:
	for (int triggerPosn : index.keySet()) {
	    List<SyntacticRelation> rr = index.get(triggerPosn);
	    if (rr.get(0).sourcePos.startsWith("V")) {
		String trigger =  rr.get(0).sourceWord;
		List<String> localArgRole = new ArrayList<String>();
		List<Integer> localArgPosn = new ArrayList<Integer>();
		List<String> localArgValue = new ArrayList<String>();
		List<MentionType> localMentionType = new ArrayList<MentionType>();
		List<String> localEntityType = new ArrayList<String>();
		for (SyntacticRelation r : rr) {
		    if (! roleOfInterest(r.type)) continue;
		    localArgRole.add(r.type);
		    localArgPosn.add(r.targetPosn);
		    Vector<Annotation> v = doc.annotationsAt(r.targetPosn, "ENAMEX");
		    if (v != null) {
			Annotation name = v.get(0);
			String type = (String) name.get("TYPE");
			String text = doc.text(name).trim().replaceAll("\\s+", "_");
			localMentionType.add(MentionType.NAME);
			localArgValue.add(text);
			localEntityType.add(type);
		    } else if (r.targetPos.startsWith("PRP") ||
			       r.targetPos.startsWith("W")) {
			localArgValue.add(r.targetWord);
			localMentionType.add(MentionType.PRONOUN);
			localEntityType.add(null);
			continue loop;
		    } else {
			localArgValue.add(r.targetWord);
			localMentionType.add(MentionType.NOMINAL);
			int start = r.targetPosn;
			String eTypeSubtype = EDTtype.getTypeSubtypeFromHead(r.targetWord);
			String eType = EDTtype.bareType(eTypeSubtype);
			localEntityType.add(eType);
		    }
		}
		if (localArgRole.contains("nsubj") && (localArgRole.contains("dobj"))) {
		    String s = core(trigger, localArgRole, localEntityType, localArgValue);
		    IceTree it = IceTreeFactory.getIceTree(s);
		    result.add(it);
		    it.triggerPosn = triggerPosn;
		    it.argPosn = localArgPosn;
		    it.entityType = localEntityType;
		    it.mentionType = localMentionType;
		}
	    }
	}
	return result;
    }

    static private boolean roleOfInterest (String role) {
	if (role.equals("nsubj") || role.equals("dobj") || role.equals("iobj")) return true;
	// inverse relations excluded
	if (role.endsWith("-1")) return false;
	if (role.startsWith("prep_")) return true;
	return false;
    }

    /**
      *  Modifies the internal representation of a phrase to collapse prepositional phrases.
      *  THe structure  --prep--> [preposition, p] --pobj--->  with two links and an intermediate node
      *  is reduced to a single link of type prep_p.  This makes it easier to identify repositional
      *  phrase arguments.
      */

    static private void addPrepLinks (SyntacticRelationSet relations, Map<Integer,List<SyntacticRelation>> index) {
	List<SyntacticRelation> newrels = new ArrayList<SyntacticRelation>();
	for (SyntacticRelation rel : relations) {
	    if (rel.type.equals("prep")) {
		int posn = rel.targetPosn;
		if (index.get(posn) != null) {
		    for (SyntacticRelation rel2 : index.get(posn)) {
			if (rel2.type.equals("pobj")) {
			    SyntacticRelation newrel =
				new SyntacticRelation (rel.sourcePosn, rel.sourceWord, rel.sourcePos,
					"prep_" + (rel.targetWord.toLowerCase()),
					rel2.targetPosn, rel2.targetWord, rel2.targetPos);
			    newrels.add(newrel);
			    index.get(rel.sourcePosn).add(newrel);
			    break;
			}
		    }
		}
	    }
	}
	relations.addAll(newrels);
    }

    IceTree (String trigger, int triggerPosn, List<String> argRole, List<Integer> argPosn, List<String> argValue,
	    List<MentionType> mentionType, List<String> entityType) {
	this.trigger = trigger;
	this.triggerPosn = triggerPosn;
	this.argRole = argRole;
	this.argValue = argValue;
	this.argPosn = argPosn;
	this.mentionType = mentionType;
	this.entityType = entityType;
    }

    public String core () {
       String s = trigger;
       for (int i = 0; i < argRole.size(); i++) {
	   s += " " + argRole.get(i);
	   if (entityType.get(i) != null) 
	       s += ":" + entityType.get(i);
	   if (argValue.get(i) != null)
	       s += "=" + argValue.get(i);
       }
       return s;
    }

    public static String core (String trigger, List<String> argRole, 
	    List<String> entityType, List<String> argValue) {
       String s = trigger;
       for (int i = 0; i < argRole.size(); i++) {
	   s += " " + argRole.get(i);
	   if (entityType.get(i) != null) 
	       s += ":" + entityType.get(i);
	   if (argValue.get(i) != null)
	       s += "=" + argValue.get(i);
       }
       return s;
    }

    public IceTree keySignature () {
	List<String> type = new ArrayList<String>();
	List<String> nulls = new ArrayList<String>();
       for (int i = 0; i < argRole.size(); i++) {
	   String v;
	   if (entityType.get(i) == null || entityType.get(i).equals("OTHER"))  
	      v = argValue.get(i);
	   else
	      v = entityType.get(i);
	   type.add(v);
	   nulls.add(null);
       }
	IceTree it = IceTreeFactory.getIceTree(trigger, argRole, type, nulls);
	return it;
    }

    @Override
	public String toString() {
	    return core();
	}
 
    public static Stemmer stemmer=new Stemmer().getDefaultStemmer();

    /**
      *  Returna the lemmatized form of a tree, where the trigger and all
      *  argument and abe replaced by their root forms.
      */

    public IceTree lemmatize () {
	String triggerStem = stemmer.getStem(trigger, "V");
	List<String> argStems = new ArrayList<String>();
	for (String a : argValue)
	    if (a != null)
		argStems.add(stemmer.getStem(a, "NNS"));
	    else
		argStems.add(null);
	IceTree it = IceTreeFactory.getIceTree(triggerStem, argRole, entityType, argStems);
	it.triggerPosn = triggerPosn;
	it.argPosn = argPosn;
	it.mentionType = mentionType;
	return it;
    }

    /**
      * Returns the linearized (near-English) form of a tree. 
      */

    public String linearize () {
	StringBuffer sb = new StringBuffer();
	addRole(sb, "nsubj", "");
	sb.append(" ");
	sb.append(trigger);
	addRole(sb, "dobj", " ");
	addRole(sb, "iobj", " ");
	for (int i = 0; i < argRole.size(); i++) {
	    if (argRole.get(i).startsWith("prep_")) {
		sb.append(" ");
		addRole(sb, argRole.get(i).substring(5).toLowerCase(), i);
	    }
	}
	return sb.toString().trim();
    }

    private void addRole (StringBuffer sb, String role, String separator) {
	int index = argRole.indexOf(role);
	if (index >= 0) {
	    sb.append(separator);
	    if (entityType == null || entityType.get(index) == null || entityType.get(index).equals("OTHER"))
		sb.append(argValue.get(index));
	    else
		sb.append(entityType.get(index));
	} 
    }

    private void addRole (StringBuffer sb, String role, int index) {
	if (index >= 0) {
	    sb.append(" ");
	    sb.append(role);
	    sb.append(" ");
	    if (entityType == null || entityType.get(index) == null || entityType.get(index).equals("OTHER"))
		sb.append(argValue.get(index));
	    else
		sb.append(entityType.get(index));
	} 
    }
    public String getRole (String role) {
	int index = argRole.indexOf(role);
	if (index >= 0) {
	    if (entityType.get(index) == null || entityType.get(index).equals("OTHER"))
		return argValue.get(index);
	    else
		return entityType.get(index);
	} else {
	    return "";
	}
    }

    public String argPair () {
	return getArgValue("nsubj") + ":" + getArgValue("dobj");
    }

    public EventBootstrap.BootstrapAnchoredPathType type;
 }
