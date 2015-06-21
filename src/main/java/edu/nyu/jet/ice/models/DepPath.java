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

    List<SyntacticRelation> relations = new ArrayList<SyntacticRelation>();

    static Stemmer stemmer = Stemmer.getDefaultStemmer();

    int start = -1;

    int end = -1;

    private DepPath() {
        super();
    }

    public DepPath(int start, int end) {
        super();
        this.start = start;
        this.end   = end;
    }

    public static String lexicalContent(String role) {
        if (role.equals("appos"))
            return ",";
        if (role.startsWith("poss"))
            return "'s";
        if (role.equals("infmod"))  {
            return "to";
        }
        if (role.startsWith("prep_")) {
            return role.substring(5);
        }
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

    public boolean hasEdgeStartsFrom(int posn) {
        for (SyntacticRelation r : relations) {
            if (r.sourcePosn == posn) return true;
        }
        return start == posn;
    }

    public int length() {
        return relations.size();
    }

    @Override
    public String toString() {
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

    public String linearize(Document doc, SyntacticRelationSet relations,
                            String type1, String type2) {
        PriorityQueue<SyntacticRelation> nodes = new PriorityQueue<SyntacticRelation>(
                relations.size(),
                new RelationTargetPosnComparator()
        );
        TIntHashSet targets = new TIntHashSet();
        targets.add(this.start);
        targets.add(this.end);
        Annotation startToken = doc.annotationsAt(this.start, "token") != null ?
                doc.annotationsAt(this.start, "token").get(0) : null;
        String startName      = "UNK";
        if (startToken != null) {
            startName = doc.text(startToken).trim();
        }

        for (SyntacticRelation r : this.relations) {
            targets.add(r.targetPosn);
        }
        int count = 1;
        SyntacticRelation startRel =
                new SyntacticRelation(-1 , "", "", "NAMETAG", this.start, type1, "");
        nodes.add(startRel);
        for (SyntacticRelation r : this.relations) {
            int sourcePosn = r.sourcePosn;
            SyntacticRelationSet fromSet = relations.getRelationsFrom(sourcePosn);
//            SyntacticRelation subj = fromSet.getRelation(sourcePosn, "nsubj");
//            if (subj != null) {
//                if (!targets.contains(subj.targetPosn)) {
//                    nodes.add(subj);
//                    targets.add(subj.targetPosn);
//                }
//            }
//            SyntacticRelation obj = fromSet.getRelation(sourcePosn, "dobj");
//            if (obj != null) {
//                if (!targets.contains(obj.targetPosn)) {
//                    nodes.add(obj);
//                    targets.add(obj.targetPosn);
//                }
//            }
            if (count == this.relations.size()) {
                // r.targetWord = "";
                SyntacticRelation fixedR = new SyntacticRelation(r.sourcePosn,
                        r.sourceWord, r.sourcePos, r.type, r.targetPosn,
                        "", r.targetPos);
                nodes.add(fixedR);
            }
            else {
                nodes.add(r);
            }
            String nodeType = r.type;
            boolean inversed = false;
            if (nodeType.endsWith("-1")) {
                nodeType = nodeType.substring(0, nodeType.length() - 2);
                inversed = true;
            }
            if (nodeType.equals("poss")) {
                inversed = !inversed;
            }
            String lexicalContent = lexicalContent(nodeType);
            if (lexicalContent.length() > 0){
                SyntacticRelation prepRel =
                        new SyntacticRelation(-1 , "", "", "NODETYPE",
                                inversed ? r.targetPosn + 1 : r.sourcePosn + 1, lexicalContent, "");
                nodes.add(prepRel);
            }
            count++;
        }
        SyntacticRelation endRel   =
                new SyntacticRelation(-1 , "", "", "NAMETAG", this.end, type2, "");
        nodes.add(endRel);
        //Collections.sort(nodes, new RelationTargetPosnComparator());
        StringBuilder linearizedPath = new StringBuilder();
        String lastWord = "";
        while (!nodes.isEmpty()) {
            SyntacticRelation node = nodes.poll();

//            String lexicalContent = lexicalContent(nodeType);
//
//            if (lexicalContent.equals("and") &&
//                    !(lastWord.equals("and") || lastWord.equals("or"))) {
//                linearizedPath.append("and ");
//                lastWord = "and";
//            }
//            else {
//                if (lexicalContent.length() > 0) {
//                    if (!lexicalContent.toLowerCase().trim().equals(lastWord)) {
//                        linearizedPath.append(lexicalContent).append(" ");
//                        lastWord = lexicalContent.toLowerCase().trim();
//                    }
//                }
//            }
            //if (i < nodes.size() - 1) {
            String targetWord = wordOnPath(node);
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
                if (!targetWord.toLowerCase().trim().equals(lastWord)
                        || targetWord.toUpperCase().equals(targetWord)) {
                    linearizedPath.append(targetWord);
                    lastWord = targetWord.toLowerCase().trim();
                    if (targetWord.length() > 0) {
                        linearizedPath.append(" ");
                    }
                }
            }

            //}
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

    String getPrep(String relationName) {
        if (relationName.startsWith("prep_")) {
            return relationName.substring(5);
        }
        else {
            return "";
        }
    }

    class RelationTargetPosnComparator implements Comparator<SyntacticRelation> {
        public int compare(SyntacticRelation syntacticRelation, SyntacticRelation t1) {
            return syntacticRelation.targetPosn - t1.targetPosn;
        }

        public boolean equals(Object o) {
            return false;
        }
    }

}
