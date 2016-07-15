package edu.nyu.jet.ice.relation;

import edu.nyu.jet.aceJet.AceDocument;
import edu.nyu.jet.aceJet.AceEntityMention;
import edu.nyu.jet.aceJet.AceRelationMention;
import edu.nyu.jet.aceJet.EventSyntacticPattern;
import edu.nyu.jet.ice.utils.LexUtils;
import edu.nyu.jet.parser.SyntacticRelation;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A Feature extractor using Bunescu-Mooney 2005 features for supervised/simulated active learning relation extraction.
 *
 * This class is not used by ICE GUI/CLI.
 *
 * @author yhe
 * @version 1.0
 */
public class BunescuMooneyFeatureExtractor implements RelationFeatureExtractor {

    final static Logger logger = LoggerFactory.getLogger(BunescuMooneyFeatureExtractor.class);

    /**
     *  returns the syntactic path from the anchor to an argument.
     */

    public static List<List<String>> buildBunescuPath
    (int anchorStart, int argumentPosn, SyntacticRelationSet relations, Document doc) {
        Map<Integer, List<List<String>>> path = new HashMap<Integer, List<List<String>>>();
        int variable = 0;
        LinkedList<Integer> todo = new LinkedList<Integer>();
        todo.add(new Integer(anchorStart));
        path.put(new Integer(anchorStart), new ArrayList<List<String>>());
        while (todo.size() > 0) {
            Integer from = todo.removeFirst();
            logger.trace ("from = " + from);
            SyntacticRelationSet fromSet = relations.getRelationsFrom(from.intValue());
            logger.trace ("fromSet = " + fromSet);
            for (int ifrom = 0; ifrom < fromSet.size(); ifrom++) {
                SyntacticRelation r = fromSet.get(ifrom);
                int to = r.targetPosn;
                // avoid loops
                if (path.get(to) != null) continue;
                logger.trace ("to = " + to);
                // if 'to' is target
                if (to == argumentPosn) {
                    logger.trace ("TO is an argument");
                    List<List<String>> p = path.get(from);
                    List<String> type = new ArrayList<String>();
                    type.add(r.type);
                    p.add(type);
                    return p;
                } else {
                    // 'to' is another node
                    List<List<String>> p = path.get(from);
                    List<String> type = new ArrayList<String>();
                    type.add(r.type);
                    List<String> forms = new ArrayList<String>();
                    StringBuilder b = new StringBuilder();
                    forms.add(b.append("W=").append(LexUtils.normalize(r.targetWord.replaceAll("\\s+", " "))).toString());
                    b.setLength(0);
                    //FeatureSet[] features = Lexicon.lookUp(new String[]{r.targetWord});
                    //String pos = features != null && features.length > 0 && features[0].get("cat") != null ?
                    //        (String)features[0].get("cat") : "n";
                    String pos = "nn";
                    List<Annotation> constits = doc.annotationsAt(r.targetPosn, "tagger");
                    if (constits != null) {
                        pos = ((String)constits.get(0).get("cat")).toLowerCase();
                    }
                    forms.add(b.append("POS=").append(pos).toString());
                    b.setLength(0);
                    p.add(type);
                    p.add(forms);
                    path.put(to, p);
                }
                todo.add(to);
            }
        }
        return new ArrayList<List<String>>();
    }

    public Event extractFeatures(AceEntityMention m1,
                                 AceEntityMention m2,
                                 AceRelationMention r,
                                 Annotation sentence,
                                 SyntacticRelationSet paths,
                                 List<AceEntityMention> mentions,
                                 AceDocument aceDoc,
                                 Document doc) {
        System.err.println("Working on:" + m1.text + " ::: " + m2.text);
        String label = r == null ? "NONE" : r.relation.type;
        int h1 = m1.getJetHead().start();
        int h2 = m2.getJetHead().start();
        if (h1 >= h2) {
            int tmp = h1;
            h1 = h2;
            h2 = tmp;
        }
        List<List<String>> path     = buildBunescuPath(h1, h2, paths, doc);
        String surfacePath          = EventSyntacticPattern
                .buildSyntacticPath(h1, h2, paths);
        surfacePath = surfacePath == null ? "" : surfacePath.replaceAll("\\s+", "_");
        List<String> extractedPaths = new ArrayList<String>();
        if (path.size() <= 20) {
            extractedPaths = extractPossiblePaths(path, 0);
        }
        //System.err.println("After extractedPaths");
        List<String> contextList    = new ArrayList<String>();
        String arg1Surface = LexUtils.normalize(m1.text.replaceAll("\\s+" , " "));
        String arg2Surface = LexUtils.normalize(m2.text.replaceAll("\\s+" , " "));
        String arg1Type    = m1.entity.type;
        String arg2Type    = m2.entity.type;
        String[] starts    = new String[]{arg1Surface, arg1Type};
        String[] ends      = new String[]{arg2Surface, arg2Type};
        StringBuilder b    = new StringBuilder();
        for (int i = 0; i < starts.length; i++) {
            for (int j = 0; j < ends.length; j++) {
                for (int k = 0; k < extractedPaths.size(); k++) {
                    b.append("BM=")
                            .append(starts[i])
                            .append("|||")
                            .append(extractedPaths.get(k))
                            .append("|||")
                            .append(ends[j]);
                    contextList.add(b.toString());
                    b.setLength(0);
                }
            }
        }
        b.setLength(0);
        contextList.add(b.append("ARG1=").append(arg1Surface).toString());
        b.setLength(0);

        contextList.add(b.append("ARG2=").append(arg2Surface).toString());
        b.setLength(0);

        contextList.add(b.append("ARG1Type=").append(arg1Type).toString());
        b.setLength(0);

        contextList.add(b.append("ARG2Type=").append(arg2Type).toString());
        b.setLength(0);

        contextList.add(b.append("ARG1ARG2Type=").append(arg1Type).append(":::").append(arg2Type).toString());
        b.setLength(0);

        for (int i = 0; i < starts.length; i++) {
            for (int j = 0; j < ends.length; j++) {
                for (int k = 0; k < extractedPaths.size(); k++) {
                    b.append("BM_ARG1ARG2Type=")
                            .append(starts[i])
                            .append("|||")
                            .append(extractedPaths.get(k))
                            .append("|||")
                            .append(ends[j])
                            .append("===")
                            .append(arg1Type).append(":::").append(arg2Type).toString();
                    contextList.add(b.toString());
                    b.setLength(0);
                }
            }
        }

        contextList.add(b.append("surfacePath=").append(surfacePath).toString());
        b.setLength(0);

        return new Event(label, contextList.toArray(new String[contextList.size()]));
    }

    public List<String> extractPossiblePaths(List<List<String>> lattice, int pos) {
        if (lattice == null || lattice.size() == 0) {
            return new ArrayList<String>();
        }
        else if (pos == lattice.size()-1) {
            return lattice.get(lattice.size() - 1);
        }
        else {
            List<String> pathsAfter = extractPossiblePaths(lattice, pos + 1);
            List<String> result     = new ArrayList<String>();
            List<String> forms      = lattice.get(pos);
            StringBuilder b = new StringBuilder();
            for (String form : forms) {
                for (String pathAfter : pathsAfter) {
                    b.append(form).append("|||").append(pathAfter);
                    result.add(b.toString());
                    b.setLength(0);
                }
            }
            return result;
        }
    }

}
