package edu.nyu.jet.ice.events;

import edu.nyu.jet.tipster.*;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.aceJet.EDTtype;
import edu.nyu.jet.ice.models.WordEmbedding;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class IceTree implements Comparable <IceTree>, Clusterable {

    String trigger;
    String[] argRole;
    String [] argValue;
    String[] entityType;

    List<Integer> argPosn;
    int triggerPosn;
    List<MentionType> mentionType;

    String repr;

    String example;

    /**
     *  'The coordinates' (embedding) of this IceTree, used to
     *  compute the distance of this IceTree from other IceTreea
     *  (for clustering, etc.).  (Currently the sum of the
     *  word embeddings of the trigger and arguments.)
     */

    public double[] getPoint() {
        String s = toString().replaceAll("="," ").replaceAll(":"," ");
        double[] we = WordEmbedding.embed(s.split(" "));
        // clustering requires non-null values;  I pick one arbitrarily.
        if (we == null) we = WordEmbedding.embed("the");
        return we;
    }

    /**
     *  The score assigned by event bootstrapping, used to rank the trees
     *  presented to the user for review.
     */
    double score;

    /**
     *  the number of instances of this tree in the corpus.
     */
    int count;

    public IceTree () {
	System.out.println("? constructing IceTree");
    }

    public IceTree (String trigger, String[] argRole, String[] entityType, String[] argValue) {
        String s = IceTree.core(trigger, argRole, entityType, argValue);
        IceTreeFactory.getIceTree(s);
    }

    static String pattern = "^([A-Za-z_]+)(:([A-Za-z0-9',.$%_-]+))?(=([A-Za-z0-9',.$%_-]+))?$";
    
    static Pattern r = Pattern.compile(pattern);

    /**
     *  The label assigned to the IceTree by the user during event bootstrapping.
     */

    public enum IceTreeChoice {
        NO, YES, UNDECIDED
    }
    
    IceTreeChoice choice = IceTreeChoice.UNDECIDED;

    public IceTreeChoice getChoice() {
        return choice;
    }

    public void setChoice(IceTreeChoice choice) {
        this.choice = choice;
    }

    /**
     *  The syntactic type of an argument, as determined by the 'extract' method.
     */

    public enum MentionType {
        PRONOUN, NOMINAL, NAME, UNKNOWN
    }
    
    public MentionType getMentionType (int i) {
        return mentionType.get(i);
    }

    //
    //  ---- property methods for core properties
    //       (these are required in order that the properties be recognized as Java
    //       beans which will be read and written by yaml.
    //

    public String getTrigger () {
        return trigger;
    }

    public void setTrigger(String s) {
        trigger = s;
    }

    public String[] getArgRole () {
        return argRole;
    }

    public String getArgRole (int i) {
        return argRole[i];
    }

    public void setArgRole (String[] s) {
        argRole = s;
    }

    public void setArgRole (int i, String s) {
        argRole[i] = s;
    }

    public String[] getEntityType () {
        return entityType;
    }

    public String getEntityType(int i) {
        return entityType[i];
    }

    public void setEntityType (String[] s) {
        entityType = s;
    }

    public void setEntityType(int i, String s) {
        argRole[i] = s;
    }

    public String[] getArgValue () {
        return argValue;
    }

    public String getArgValue (int i) {
        return argValue[i];
    }

    public void setArgValue (String[] s) {
        argValue = s;
    }

    public void setArgValue (int i, String s) {
        argValue[i] = s;
    }

    public String getArgValueForRole (String role) {
        for (int i = 0; i < argRole.length; i++) {
	    if (argRole[i].equals(role))
		return argValue[i];
	}
	return null;
    }

    public IceTree clearArgValues () {
	String[] nulls = new String[argRole.length];
	for (int i = 0; i < argRole.length; i++) {
	    nulls[i] = null;
	}
	return IceTreeFactory.getIceTree(trigger, argRole, entityType, nulls);
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

    public String getExample () {
        return example;
    }

    public void setExample (String t) {
        example = t;
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
	    List<String> argRoleList = new ArrayList<String>();
	    List<String> argValueList = new ArrayList<String>();
	    List<String> entityTypeList = new ArrayList<String>();
	    mentionType = new ArrayList<MentionType>();
	
	    for (int i = 1; i < triggerAndArgs.length; i++) {
		Matcher m = r.matcher(triggerAndArgs[i]);
		if (m.find()) {
		    String role = m.group(1);
		    String type = m.group(3);
		    String value = m.group(5);
		    argRoleList.add(role);
		    argValueList.add(value);
		    entityTypeList.add(type);
		    mentionType.add(MentionType.UNKNOWN);
		} else {
		    System.out.println("invalid event:  " + s);
	// 	    Thread.dumpStack();
		    break;
		}
	    }
	    argRole = argRoleList.toArray(new String[0]);
	    argValue = argValueList.toArray(new String[0]);
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
		    String s = core(trigger, localArgRole.toArray(new String[0]), 
                                   localEntityType.toArray(new String[0]), 
				   localArgValue.toArray(new String[0]));
		    IceTree it = IceTreeFactory.getIceTree(s);
		    result.add(it);
		    it.triggerPosn = triggerPosn;
		    it.argPosn = localArgPosn;
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

    IceTree (String trigger, int triggerPosn, String[] argRole, List<Integer> argPosn, String[] argValue,
	    List<MentionType> mentionType, String[] entityType) {
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
        for (int i = 0; i < argRole.length; i++) {
            s += " " + argRole[i];
            if (entityType != null && entityType[i] != null) 
                s += ":" + entityType[i];
            if (argValue[i] != null)
                s += "=" + argValue[i];
        }
        return s;
    }

    public static String core (String trigger, String[] argRole, 
            String[] entityType, String[] argValue) {
        String s = trigger;
        for (int i = 0; i < argRole.length; i++) {
            s += " " + argRole[i];
            if (entityType[i] != null) 
                s += ":" + entityType[i];
            if (argValue[i] != null)
                s += "=" + argValue[i];
        }
        return s;
    }

    public IceTree keySignature () {
        int numargs = argRole.length;
        String[] newEntityType = new String[numargs];
        String[] nulls = new String[numargs];
        for (int i = 0; i < numargs; i++) {
            if (entityType[i] == null || entityType[i].equals("OTHER")) {
                newEntityType[i] = argValue[i]; 
            } else {
                newEntityType[i] = entityType[i];
            }
            nulls[i] = null;
        }
        IceTree it = IceTreeFactory.getIceTree(trigger, argRole, newEntityType, nulls);
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
        String[] argValues = argStems.toArray(new String[0]);
        IceTree it = IceTreeFactory.getIceTree(triggerStem, argRole, entityType, argValues);
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
        for (int i = 0; i < argRole.length; i++) {
            if (argRole[i].startsWith("prep_")) {
                sb.append(" ");
                addRole(sb, argRole[i].substring(5).toLowerCase(), i);
            }
        }
        return sb.toString().trim();
    }

    private void addRole (StringBuffer sb, String role, String separator) {
        for (int i = 0; i < argRole.length; i++)
            if (argRole[i].equals(role)) {
                sb.append(separator);
                if (entityType == null || entityType[i] == null || entityType[i].equals("OTHER"))
                    sb.append(argValue[i]);
                else
                    sb.append(entityType[i]);
                return;
            } 
    }

    private void addRole (StringBuffer sb, String role, int index) {
        if (index >= 0) {
            sb.append(" ");
            sb.append(role);
            sb.append(" ");
            if (entityType == null || entityType[index] == null || entityType[index].equals("OTHER"))
                sb.append(argValue[index]);
            else
                sb.append(entityType[index]);
        } 
    }

    public String getRole (String role) {
        for (int i = 0; i < argRole.length; i++)
            if (argRole[i].equals(role)) {
                if (entityType[i] == null || entityType[i].equals("OTHER"))
                    return argValue[i];
                else
                    return entityType[i];
            } 
        return "";
    }

    public int numArgs () {
        return argRole.length;
    }

    public String argPair () {
	return getArgValueForRole("nsubj") + ":" + getArgValueForRole("dobj");
    }

    public EventBootstrap.BootstrapAnchoredPathType type;
 }
