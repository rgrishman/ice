package edu.nyu.jet.ice.models;

import edu.nyu.jet.lex.Stemmer;
import edu.nyu.jet.parser.SyntacticRelation;

import java.util.HashSet;
import java.util.Set;

/**
 *  Regularizer for DepPath:  deletes quantity constructs such as 'pound of X'
 *  from dependency paths.
 *
 * @author yhe
 * @version 1.0
 */
public class DepPathRegularizer {

    /**
     *  set of quantity words
     */

    public Set<String> quantifiers = new HashSet<String>();

    private Stemmer stemmer = Stemmer.getDefaultStemmer();

    {
        quantifiers.add("ounce");
        quantifiers.add("gram");
        quantifiers.add("kilogram");
        quantifiers.add("quantity");
        quantifiers.add("kilo");
        quantifiers.add("pound");
        quantifiers.add("amount");
    }

    /**
     *  Returns dependency path 'p' with quantity phrase 'Q of X' reduced to 'X'.
     */

    public DepPath regularize(DepPath p) {
        DepPath result =  p.copy();

        SyntacticRelation prevRelation = null;
        for (SyntacticRelation r : p.getRelations()) {
            // prep_of: when using transformation
            // prep: when not using transformation
            if ((r.type.equals("prep_of") || r.type.equals("prep")) &&
                    quantifiers.contains(
                            stemmer.getStem(r.sourceWord.trim().toLowerCase(), "NN")) &&
                    prevRelation != null) {
                prevRelation.targetPos = r.targetPos;
                prevRelation.targetPosn = r.targetPosn;
                prevRelation.targetWord = r.targetWord;
            }
            else {
                if (prevRelation != null) {
                    result.append(prevRelation);
                }
                if ((r.type.equals("prep_of-1") || r.type.equals("prep-1")) &&
                        quantifiers.contains(
                                stemmer.getStem(r.targetWord.trim().toLowerCase(), "NN"))) {
                    prevRelation = null;
                }
                else {
                    prevRelation = r;
                }
            }
        }
        if (prevRelation != null) {
            result.append(prevRelation);
        }
        return result;
    }

    public String regularize(String p) {
        String result = p;
        for (String w : quantifiers) {
            result = result.replaceAll(":" + w + ":prep_of", "");
        }
        result = result.replaceAll("rcmod:\\d+:", "");
        if (!p.equals(result)) {
//            System.err.println("Before regularization:" + p);
//            System.err.println("After  regularization:" + result);
        }
        return result;
    }

}
