package edu.nyu.jet.ice.models;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.lex.Stemmer;

import java.util.ArrayList;
import java.util.List;

/**
 * MatcherPath is a dependency path to be matched by the PathMatcher. It is an ordered list of MatcherNodes.
 *
 * @author yhe
 * @version 1.0
 */
public class MatcherPath {
    List<MatcherNode> nodes = new ArrayList<MatcherNode>();
    String arg1Type = "UNK";
    String arg2Type = "UNK";
    String relationType = "NONE";
    Stemmer stemmer = Stemmer.getDefaultStemmer();

    public MatcherPath(String pathString) {
        nodes.clear();
        String[] parts = pathString.split("--");
        if (parts.length == 3) {
            arg1Type = parts[0].trim();
            arg2Type = parts[2].trim();
            parts = parts[1].split(":");
            for (int i = 0; i < (parts.length - 1) / 2; i++) {
                MatcherNode node = new MatcherNode(parts[2*i], stemmer.getStem(parts[2*i + 1],
                        "UNK"));
                nodes.add(node);
            }
            MatcherNode node = new MatcherNode(parts[parts.length - 1], "SYS_PATH_END");
            nodes.add(node);
        }
    }

    public MatcherPath(AnchoredPath path) {
        nodes.clear();
        String pathString = path.toString();
        String[] parts = pathString.split("--");
        if (parts.length == 3) {
            arg1Type = parts[0].trim();
            arg2Type = parts[2].trim();
            parts = parts[1].split(":");
            for (int i = 0; i < (parts.length - 1) / 2; i++) {
                MatcherNode node = new MatcherNode(parts[2*i], stemmer.getStem(parts[2*i + 1],
                        "UNK"));
                nodes.add(node);
            }
            MatcherNode node = new MatcherNode(parts[parts.length - 1], "SYS_PATH_END");
            nodes.add(node);
        }
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationType() {
        return relationType;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public int length() {
        return nodes.size();
    }

    @Override
    public String toString() {
        if (nodes.size() == 0) {
            return arg1Type + "-- --" + arg2Type;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(arg1Type).append("--");
        for (int i = 0; i < nodes.size() - 1; i++) {
            sb.append(nodes.get(i).label).append(":");
            sb.append(nodes.get(i).token).append(":");
        }
        sb.append(nodes.get(nodes.size()-1).label);
        sb.append("--").append(arg2Type);
        return sb.toString();
    }
}
