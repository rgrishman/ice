package edu.nyu.jet.ice.models;

import AceJet.AnchoredPath;
import Jet.Lex.Stemmer;
import Jet.Parser.SyntacticRelation;
import Jet.Parser.SyntacticRelationSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import gnu.trove.TIntHashSet;

import java.util.*;

/**
 * A structure to save a dependency path and its lexical representation
 *
 * @author yhe
 * @version 1.0
 */
public class DepPath {

    // To store user-defined String representation. If path is set, toString() will return path,
    // otherwise, the string representation will be computed on-the-fly
    String path = null;

    // Dependency triples on the path
    List<SyntacticRelation> relations = new ArrayList<SyntacticRelation>();

    static Stemmer stemmer = Stemmer.getDefaultStemmer();

    int start = -1;

    int end = -1;

    private DepPath() {
        super();
    }

    /**
     * Create a DepPath, with start and end points set.
     * @param start span.start() of ARG1 (argument at one end of the path)
     * @param end span.start() of ARG1 (argument at another end of the path)
     */
    public DepPath(int start, int end) {
        super();
        this.start = start;
        this.end   = end;
    }

    /**
     * Define the string representation of the DepPath.
     * Suppose a DepPath records a path ARG1:prep:of:pobj:ARG2, if we call toString() without first
     * setPath(), toString() will return ARG1:prep:of:pobj:ARG2.
     * However, we might want toString() to return the transformed path. In that case, we can first
     * setPath("ARG1:prep_of:ARG2"). toString() will return the transformed path thereafter.
     * @param path User-defined string representation of the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Transcribe some dependency labels into words or punctuation marks in the linearized version.
     * @param role The dependency label
     * @return Transcribed dependency label
     */
    public static String lexicalContent(String role) {
        if (role.equals("appos"))
            return ",";
        if (role.startsWith("poss"))
            return "'s";
        if (role.equals("infmod"))  {
            return "to";
        }
//        if (role.startsWith("prep_")) {
//            return role.substring(5);
//        }
        if (role.equals("conj")) {
            return "and";
        }
        if (role.equals("purpcl")) {
            return "to";
        }
        return "";
    }

    /**
     * Create a new instance of DepPath from the current instance,
     * attaching r to the tail of the new instance.
     *
     * @param r A dependency relation between two words
     * @return A new DepPath
     */
    public DepPath extend(SyntacticRelation r) {
        DepPath p = new DepPath();
        p.start = this.start;
        p.end = this.end;
        for (SyntacticRelation rel : this.relations) {
            p.append(rel);
        }
        p.append(r);
        return p;
    }

    public void append(SyntacticRelation r) {
        relations.add(r);
    }

    public void clear() {
        relations.clear();
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int length() {
        return relations.size();
    }

    @Override
    /**
     * Given a DepPath, transcribe it to the string form i.e. label1:word1:label2:word2: ... :labelk
     */
    public String toString() {
        if (path == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < relations.size(); i++) {
                if (i > 0) {
                    sb.append(":");
                }
                sb.append(relations.get(i).type);
                if (i < relations.size() - 1) {
                    sb.append(":").append(relations.get(i).targetWord.replaceAll(":", "_"));
                }
            }
            return AnchoredPath.lemmatizePath(sb.toString());
        }
        else {
            return AnchoredPath.lemmatizePath(path);
        }
    }

    /**
     * Linearize the DepPath, given the Jet Document, the SyntacticRelationSet, and
     * the types of the entities at both ends.
     * @param doc The Jet Document
     * @param relations Dependency relations extracted from the Jet Document
     * @param type1 Entity type of the first argument
     * @param type2 Entity type of the second argument
     * @return Linearized version of the dependency path
     */
    public String linearize(Document doc, SyntacticRelationSet relations,
                            String type1, String type2) {
        // PriorityQueue is maintained as a min-heap, so that the dependency relations
        // in the heap are sorted by the offset of the governed word
        PriorityQueue<SyntacticRelation> nodes = new PriorityQueue<SyntacticRelation>(
                relations.size(),
                new RelationTargetPosnComparator()
        );

        int count = 1;
        // Add a "start relation" so that the entity type of the first argument will
        // be printed out.
        SyntacticRelation startRel =
                new SyntacticRelation(-1 , "", "", "NAMETAG", this.start, type1, "");
        nodes.add(startRel);
        for (SyntacticRelation r : this.relations) {
            if (count == this.relations.size()) {
                // Remove target word (the governed word) from the last dependency relation:
                // the last target word won't be printed out; we will later add an endRel
                // node to print out the entity type of the last target word on the dependency
                // path instead.
                SyntacticRelation fixedR = new SyntacticRelation(r.sourcePosn,
                        r.sourceWord, r.sourcePos, r.type, r.targetPosn,
                        "", r.targetPos);
                nodes.add(fixedR);
            }
            else {
                nodes.add(r);
            }
            // Determine if the relation is "inversed"
            // This mainly helps to decide that if we want to add a word to the linearized path,
            // where we should put the added word. For A:conj:B, "and" should be put after A, but
            // for A:conj-1:B, "and" should be put after B.
            String nodeType = r.type;
            boolean inversed = false;
            if (nodeType.endsWith("-1")) {
                nodeType = nodeType.substring(0, nodeType.length() - 2);
                inversed = true;
            }
            if (nodeType.equals("poss")) {
                inversed = !inversed;
            }
            // If the label can be transcribed to the word, add the transcribed word we obtained
            // from the dependency label to the heap.
            // (all labels will be added to the linearized path later)
            String lexicalContent = lexicalContent(nodeType);
            if (lexicalContent.length() > 0){
                SyntacticRelation prepRel =
                        new SyntacticRelation(-1 , "", "", "NODETYPE",
                                inversed ? r.targetPosn + 1 : r.sourcePosn + 1, lexicalContent, "");
                nodes.add(prepRel);
            }
            count++;
        }
        // Add an "end relation" so that the type of the second argument will be printed out.
        SyntacticRelation endRel   =
                new SyntacticRelation(-1 , "", "", "NAMETAG", this.end, type2, "");
        nodes.add(endRel);
        StringBuilder linearizedPath = new StringBuilder();
        String lastWord = "";
        // Add all target words from the heap to the linearized path. Note that
        // nodes is an ordered heap based on targetPosn
        while (!nodes.isEmpty()) {
            SyntacticRelation node = nodes.poll();
            String targetWord = wordOnPath(node);
            // Avoid "Tom and and Jerry", "Tom and or Jerry" etc.
            if (targetWord.equals("and") ||
                    targetWord.equals("or") ||
                    targetWord.equals(",")) {
                if (!(lastWord.equals(",") ||
                        lastWord.equals("or") ||
                        lastWord.equals("and") ||
                        lastWord.equals(""))) {
                    linearizedPath.append(targetWord).append(" ");
                    lastWord = targetWord.toLowerCase().trim();
                }
            }
            else {
                // add word on the heap to the linearized path
                if (!targetWord.toLowerCase().trim().equals(lastWord)
                        || targetWord.toUpperCase().equals(targetWord)) {
                    linearizedPath.append(targetWord);
                    lastWord = targetWord.toLowerCase().trim();
                    if (targetWord.length() > 0) {
                        linearizedPath.append(" ");
                    }
                }
            }
        }
        return linearizedPath.toString().trim();
    }

    private String wordOnPath(SyntacticRelation node) {
        if (node.type.equals("NAMETAG") || node.type.equals("NODETYPE")) {
            return node.targetWord.trim();
        }
        else if (node.type.equals("dobj-1") &&
                node.sourcePosn < node.targetPosn) {
            return node.targetWord.trim();
        }
        else {
            return stemmer.getStem(node.targetWord.toLowerCase().trim(),
                    node.targetPos).trim();
        }
    }

    /**
     * Compares SyntacticRelations based on their targetPosn
     */
    class RelationTargetPosnComparator implements Comparator<SyntacticRelation> {
        public int compare(SyntacticRelation syntacticRelation, SyntacticRelation t1) {
            return syntacticRelation.targetPosn - t1.targetPosn;
        }

        public boolean equals(Object o) {
            return false;
        }
    }

}
