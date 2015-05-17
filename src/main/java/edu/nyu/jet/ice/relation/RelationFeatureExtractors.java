package edu.nyu.jet.ice.relation;

import AceJet.*;
import Jet.Lex.Tokenizer;
import Jet.Tipster.Span;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class RelationFeatureExtractors {
    /**
     * generate the class label for a relation mention
     * @param rm relation mention
     * @return classLabel
     */
    public static String generateClassLabel(AceRelationMention rm, String granuality) {
        String classLabel = "";
            if (granuality.equals("type")) {
                classLabel = classLabel + rm.relation.type;
            } else if (granuality.equals("subtype")) {
                classLabel = classLabel + rm.relation.type + "--" + rm.relation.subtype;
            }
        return classLabel;
    }

    /**
     * phrase := headOfE1 * headOfE2
     * @param rm
     * @param fileText
     * @return
     * @throws Exception
     */
    public static String generatePhraseFeature(AceRelationMention rm, String fileText) throws Exception {
        String outString = new String();
        int beginIndex = rm.arg1.head.start() < rm.arg2.head.start() ? rm.arg1.head.start(): rm.arg2.head.start();
        int endIndex = rm.arg1.head.end() < rm.arg2.head.end() ? rm.arg2.head.end() + 1 : rm.arg1.head.end() + 1;
        String phrase = fileText.substring(beginIndex, endIndex);
        String[] tokens = Tokenizer.tokenize(phrase);
        boolean lowerHead1 = rm.arg1.head.start() < rm.arg2.head.start() ?  rm.arg1.type.equals("NAM") : rm.arg2.type.equals("NAM");
        boolean lowerHead2 = rm.arg1.head.start() > rm.arg2.head.start() ?  rm.arg1.type.equals("NAM") : rm.arg2.type.equals("NAM");
        if (tokens.length < 6) {
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                if (t.equals("abc") || t.equals("nbc") || t.equals("cnn") || t.equals("usa") || t.equals("u.s") || t.equals("u.s.")) {
                    t = t.toUpperCase();
                }
                if ( (lowerHead1 && i == 0 && Character.isLowerCase(t.charAt(0))) ||
                        (lowerHead2 && i == tokens.length - 1 && Character.isLowerCase(t.charAt(0)))) {
                    t = t.substring(0, 1).toUpperCase() + t.substring(1);
                }
                outString = outString + t + " ";
            }

        }
        outString = outString.isEmpty() ? "nil" : outString;
        return "phrase=" + outString.trim().replaceAll(" |\n", "_");
    }


    /**
     * generate head of both mentions.
     * @param rm
     * @param fileText
     * @return
     * @throws Exception
     */
    public static List<String> generateEntityHeadFeatures(AceRelationMention rm, String fileText) throws Exception {
        // use the ACE span to create words between features
        Span spanM1 = new Span(rm.arg1.head.start(), rm.arg1.head.end()+1);
        Span spanM2 = new Span(rm.arg2.head.start(), rm.arg2.head.end()+1);

        String orderOfMentions = new String(); //"orderM=1"; //
        if (spanM2.end() <= spanM1.start())		// M2 M1
            orderOfMentions = "orderM=2";
        else if (spanM1.end() <= spanM2.start())	// M1 M2
            orderOfMentions = "orderM=1";

        //	RelationMention relMention = replacePronoun(rm);
        // use singapore's mention definition for deriving head and bag of words features,
        AceRelationMention relMention = rm;
        // create new Span for mentions
        spanM1 = new Span(relMention.arg1.extent.start(), relMention.arg1.head.end()+1);
        spanM2 = new Span(relMention.arg2.extent.start(), relMention.arg2.head.end()+1);

        // head of M1
        String HM1 = getHeadOfMention(relMention.arg1);
        // head of M2
        String HM2 = getHeadOfMention(relMention.arg2);
        // combination of hm1 and hm2
        String HM12 = getHeadOfMention(relMention.arg1) + "--" + getHeadOfMention(relMention.arg2);

        ArrayList<String> wordFeas = new ArrayList<String>();
        wordFeas.add(orderOfMentions);
        wordFeas.add("HM1=" + HM1);
        wordFeas.add("HM2=" + HM2);
        wordFeas.add("HM12=" + HM12);
//        String outString = new String();
//        for (String fea : wordFeas){
//            if (! fea.isEmpty()) {
//                outString = outString + fea.replace("\n", " ").replace(" ", "_") + " ";
//            }
//        }
        return wordFeas;
    }

    /**
     * word feature of mentions
     *
     */
    public static List<String> generateWordFeatures(AceRelationMention rm, String fileText) throws Exception {
        // use the ACE span to create words between features
//		Span spanM1 = new Span(rm.mention1.head.start(), rm.mention1.head.end()+1);
//		Span spanM2 = new Span(rm.mention2.head.start(), rm.mention2.head.end()+1);
        Span spanM1 = new Span(rm.arg1.extent.start(), rm.arg1.head.end()+1);
        Span spanM2 = new Span(rm.arg2.extent.start(), rm.arg2.head.end()+1);
        // words in between
        String WBNULL = new String(); 	// if no word
        String WBFL = new String();		// only one in between
        String WBF = new String();		// 1st word in between
        String WBL = new String();		// last word
        String WBO = new String();		// other words
        String NUMWB = "NUMWB=0";
        String TPatternET = new String(); // token based pattern with entity type
        String TPatternEH = new String(); // token based pattern with entity head
        ArrayList<String> BagWBO = new ArrayList<String>();
        ArrayList<String> bigrams = new ArrayList();
        String wordsBetween = getWordsInBetweenMentions(spanM1, spanM2, fileText);
        ArrayList<String> wordFeas = new ArrayList<String>();
        if (wordsBetween.isEmpty()) {
            WBNULL = "WBNULL=" + "true";
        } else {
            String[] words = Tokenizer.tokenize(wordsBetween);
            NUMWB = "NUMWB=" + String.valueOf(words.length);
            if (words.length == 1) {
                WBFL = "WBFL=" + words[0];
            } else if (words.length == 2) {
                WBF = "WBF=" + words[0];
                WBL = "WBL=" + words[1];
            } else if (words.length > 2) {
                WBF = "WBF=" + words[0];
                WBL = "WBL=" + words[words.length - 1];
                for (int i = 1; i < words.length -1; i++) {
                    if (! BagWBO.contains("WBO" + "_" + words[i] + "=TRUE"))
                        BagWBO.add("WBO" + "_" +words[i] + "=TRUE");
                }
                for (int i = 0; i < words.length; i++) {
                    if (i + 1 < words.length) {
                        if (! bigrams.contains("BIGRAM" +  "_" + words[i] + words[i+1] + "=TRUE"))
                            bigrams.add("BIGRAM" +  "_" + words[i] + words[i+1] + "=TRUE");
                    }
                }
            }
            // only link all the tokens if there are at most 5 tokens in between and combine with entity types
            if (words.length < 6) {
                //easy to neglect!!
                //TPatternET used as backoff when phrase is short and doesn't exist in similarity table
                String ngramPat = new String();
                for (int i = 0; i < words.length; i++) {
                    TPatternET = TPatternET + words[i] + "_";
                    ngramPat = ngramPat + words[i] + " ";
                }
                ngramPat = ngramPat.trim();
			/*	if (useNgramPatCluster && npat2cluster.containsKey(ngramPat)) {
					wordFeas.add("pcluster=" + npat2cluster.get(ngramPat));
				}*/
                String phrase = generatePhrase(rm, fileText); // [head_e1, head_e2]
                if (rm.arg1.head.start() < rm.arg2.head.start()) {
                    TPatternET = "TPatternET=" + rm.arg1.entity.type + "_" + TPatternET + rm.arg2.entity.type;
                    //	TPatternEH = "TPatternEH=" + getHeadOfMention(rm.mention1) + "_" + TPatternET + getHeadOfMention(rm.mention2);
                }
                else {
                    TPatternET = "TPatternET=" + rm.arg2.entity.type + "_" + TPatternET + rm.arg1.entity.type;
                    //	TPatternEH = "TPatternEH=" + getHeadOfMention(rm.mention2) + "_" + TPatternET + getHeadOfMention(rm.mention1);
                }
            }
        }
        String orderOfMentions = new String(); //"orderM=1"; //
        if (spanM2.end() <= spanM1.start())		// M2 M1
            orderOfMentions = "orderM=2";
        else if (spanM1.end() <= spanM2.start())	// M1 M2
            orderOfMentions = "orderM=1";

//		RelationMention relMention = replacePronoun(rm);
        AceRelationMention relMention = rm;
        // use singapore's mention definition for deriving head and bag of words features,
        // create new Span for mentions
        spanM1 = new Span(relMention.arg1.extent.start(), relMention.arg1.head.end()+1);
        spanM2 = new Span(relMention.arg2.extent.start(), relMention.arg2.head.end()+1);

        // bag of words of M1 in lower case, using singpore's definition of mention [extent.start(), head.end()]
        String WM1 =  fileText.substring(spanM1.start(), spanM1.end());
        List<String> BagWM1 = getBagOfMentionWords(WM1, "BagWM1");
        // head of M1
//		String HM1 = "HM1=" + getHeadOfMention(relMention.mention1);
        // bag of words in M2
        String WM2 = fileText.substring(spanM2.start(), spanM2.end());
        List<String> BagWM2 = getBagOfMentionWords(WM2, "BagWM2");
        // head of M2
//		String HM2 = "HM2=" + getHeadOfMention(relMention.mention2);
        // combination of hm1 and hm2
//		String HM12 = "HM12=" + getHeadOfMention(relMention.mention1) + "--" + getHeadOfMention(relMention.mention2);

        // words before M1 or after M2...  no distinguish between M1 M2 and M2 M1...
        String BM1F = new String();	String BM1L = new String();
        String AM2F = new String(); String AM2L = new String();
        int left = rm.arg1.extent.start() < rm.arg2.extent.start() ? rm.arg1.extent.start() : rm.arg2.extent.start();
        int right = rm.arg1.extent.end() > rm.arg2.extent.end() ? rm.arg1.extent.end() + 1 : rm.arg2.extent.end() + 1;
        String[] words = Tokenizer.tokenize(fileText.substring(0, left).trim());
        if (words.length > 1) {
            BM1F = "BM1F=" + words[words.length -1];
            BM1L = "BM1L=" + words[words.length -2];
        } else if (words.length == 1) {BM1F = "BM1F=" + words[0];}
        words = Tokenizer.tokenize(fileText.substring(right).trim());
        if (words.length > 1) {
            AM2F = "AM2F=" + words[0];
            AM2L = "AM2L=" + words[1];
        } else if (words.length == 1) {AM2F = "AM2F=" + words[0];}

        //wordFeas.add(orderOfMentions);
        wordFeas.addAll(BagWM1);//wordFeas.add(HM1);
        wordFeas.addAll(BagWM2);//wordFeas.add(HM2);
        //wordFeas.add(HM12);
        wordFeas.add(WBNULL); wordFeas.add(WBFL);wordFeas.add(WBF);wordFeas.add(WBL); wordFeas.addAll(BagWBO);
        wordFeas.addAll(bigrams); wordFeas.add(BM1F);wordFeas.add(BM1L); wordFeas.add(AM2F);wordFeas.add(AM2L);
        wordFeas.add(NUMWB); wordFeas.add(TPatternET); //wordFeas.add(TPatternEH);
//        String outString = new String();
//        for (String fea : wordFeas){
//            if (! fea.isEmpty()) {
//                outString = outString + fea.replace("\n", " ").replace(" ", "_") + " ";
//            }
//        }
//        return outString;
        return wordFeas;
    }

    public static String generatePhrase(AceRelationMention rm, String fileText) throws Exception {
        String outString = new String();
        int beginIndex = rm.arg1.head.start() < rm.arg2.head.start() ? rm.arg1.head.start(): rm.arg2.head.start();
        int endIndex = rm.arg1.head.end() < rm.arg2.head.end() ? rm.arg2.head.end() + 1 : rm.arg1.head.end() + 1;
        String phrase = fileText.substring(beginIndex, endIndex);
        String[] tokens = Tokenizer.tokenize(phrase);
        boolean lowerHead1 = rm.arg1.head.start() < rm.arg2.head.start() ?  rm.arg1.type.equals("NAM") : rm.arg2.type.equals("NAM");
        boolean lowerHead2 = rm.arg1.head.start() > rm.arg2.head.start() ?  rm.arg1.type.equals("NAM") : rm.arg2.type.equals("NAM");
        if (tokens.length < 6) {
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                if (t.equals("abc") || t.equals("nbc") || t.equals("cnn") || t.equals("usa") || t.equals("u.s") || t.equals("u.s.")) {
                    t = t.toUpperCase();
                }
                if ( (lowerHead1 && i == 0 && Character.isLowerCase(t.charAt(0))) ||
                        (lowerHead2 && i == tokens.length - 1 && Character.isLowerCase(t.charAt(0)))) {
                    t = t.substring(0, 1).toUpperCase() + t.substring(1);
                }
                outString = outString + t + " ";
            }

        }
        outString = outString.trim();
        return outString.trim().replaceAll(" |\n", "_");
    }

    /**
     * generate overlap feature, number of words in between is generated in generateWordFeatures()
     * @param aceDoc
     * @param relMention
     * @param fileText
     * @return
     * @throws java.io.IOException
     */
    public static List<String> generateOverlapFeature(AceDocument aceDoc,
                                                         AceRelationMention relMention,
                                                         String fileText) throws IOException{
        String ovfea = new String();
        //Span spanM1 = new Span(relMention.mention1.extent.start(), relMention.mention1.head.end()+1);
        //Span spanM2 = new Span(relMention.mention2.extent.start(), relMention.mention2.head.end()+1);
        Span spanM1 = relMention.arg1.extent;
        Span spanM2 = relMention.arg2.extent;

        String NUMMB = "NUMMB=" + String.valueOf(getNumberOfMentionsBetween(relMention, aceDoc));

        // combination of ET12, HM12 and M1<M2, M2<M1
        String M1inM2 = "false";
        String M2inM1 = "false";
        if (spanM2.start() <= spanM1.start() && spanM1.end() <= spanM2.end()) {
            M1inM2 = "true";
        }
        if (spanM1.start() <= spanM2.start() && spanM2.end() <= spanM1.end()){
            M2inM1 = "true";
        }
        // combination of ET12, HM12 and M1<M2, M2<M1
        String ET12 = relMention.arg1.entity.type + "--" + relMention.arg2.entity.type;
        String ET12M1inM2 = "ET12M1inM2=" + ET12 + "--" + M1inM2;
        String ET12M2inM1 = "ET12M2inM1=" + ET12 + "--" + M2inM1;
        String HM12 = getHeadOfMention(relMention.arg1) + "--" + getHeadOfMention(relMention.arg2);
        HM12 = HM12.replace("\n", " ").replace(" ", "_");
        String HM12M1inM2 = "HM12M1inM2=" + HM12 + "--" + M1inM2;
        String HM12M2inM1 = "HM12M2inM1=" + HM12 + "--" + M2inM1;
//        ovfea = NUMMB + " " + ET12M1inM2 + " " + ET12M2inM1 + " " + HM12M1inM2 + " " + HM12M2inM1 + " ";
//        return ovfea;
        List<String> features = new ArrayList<String>();
        features.add(NUMMB);
        features.add(ET12M1inM2);
        features.add(ET12M2inM1);
        features.add(HM12M1inM2);
        features.add(HM12M2inM1);
        return features;
    }

    public static int getNumberOfMentionsBetween(AceRelationMention relMention, AceDocument aceDoc) {
        int minIndex = relMention.arg1.head.start() < relMention.arg2.head.start()
                ? relMention.arg1.head.end() : relMention.arg2.head.end();
        int maxIndex = relMention.arg1.head.end() > relMention.arg2.head.end()
                ? relMention.arg1.head.start() : relMention.arg2.head.start();
        int numOfMentions = 0;
        for (AceEntity e : aceDoc.entities) {
            for (int i = 0; i < e.mentions.size(); i++) {
                AceEntityMention m = (AceEntityMention) e.mentions.get(i);
                // if in between
                if (minIndex < m.head.start() && m.head.end() < maxIndex ) {
                    numOfMentions++;
                }
            }
        }
        return numOfMentions;
    }

    /**
     * Mention Level feature: NOM, NAM or PRO
     *
     */
    public static List<String> generateMLFeature(AceRelationMention rm) {
        List<String> result = new ArrayList<String>();
        result.add("ML1=" + rm.arg1.type);
        result.add("ML2=" + rm.arg2.type);
        result.add("ML12=" + rm.arg1.type + "--" + rm.arg2.type);
        return result;
    }
    /**
     * Entity Type feature
     */
    public static List<String> generateETFeature(AceRelationMention rm) {
        List<String> result = new ArrayList<String>();
        result.add("ET1=" + rm.arg1.entity.type);
        result.add("ET2=" + rm.arg2.entity.type);
        result.add("ET12=" + rm.arg1.entity.type + "--" + rm.arg2.entity.type);
        result.add("EST12=" + rm.arg1.entity.subtype + "--" + rm.arg2.entity.subtype);
        return result;
    }


    /* Parser based features are not included for now
    /**
     * generate features of the following:
     * 1) Base Phrase Chunking; 2) Dependency Tree; 3) Parse Tree.
     * @param rm
     * @param pdoc
     * @return
     * @throws Throwable
    * /

    protected static String generateParsingFeature (RelationMention rm, Rel_ParsedStanfordDoc pdoc, int sid, String featuretype) throws Throwable {
        String feature = new String();
        // chunk feature
        String fea_between = new String();
        String fea_before = new String();
        String fea_after = new String();
        // dependency tree feature
        String fea_dtree = new String();
        // parse tree feature
        String fea_ptree = new String();
        //dependency path
        String fea_dpath = new String();

        if (pdoc.sid2ParsedSen.containsKey(sid)) {
            Rel_ParsedSen ps = pdoc.sid2ParsedSen.get(sid);

            if (ps.wordStartToChunkIndex.containsKey(rm.mention1.head.start())
                    && ps.wordStartToChunkIndex.containsKey(rm.mention2.head.start()) ) {
                // to do : try to use the last word of head
                //		int indexOfMention1 = ps.wordStartToIndex.get(rm.mention1.head.start());
                //		System.out.println(ps.words.get(indexOfMention1) + "\t" + rm.mention1.head.start() + "--" + indexOfMention1);
                //		int indexOfMention2 = ps.wordStartToIndex.get(rm.mention2.head.start());
                int indexOfMention1 = ps.wordStartToChunkIndex.get(getStartOfMentionHead(rm.mention1.head.start(), rm.mention1.headText));
                int indexOfMention2 = ps.wordStartToChunkIndex.get(getStartOfMentionHead (rm.mention2.head.start(), rm.mention2.headText));
                //		System.out.println(ps.words.get(indexOfMention1) + "\t" + rm.mention1.head.start() + "--" + indexOfMention1);

                if (indexOfMention1 != -1 && indexOfMention2 != -1) {
                    int beginIndex = indexOfMention1 < indexOfMention2 ?
                            indexOfMention1 : indexOfMention2;
                    int endIndex = indexOfMention1 > indexOfMention2 ?
                            indexOfMention1 : indexOfMention2;
                    fea_between = getChunkHeadsInBetween(rm, ps, beginIndex, endIndex);
                    // not really before M1, actually before the first mention (whose span is smaller)
                    fea_before = getChunkHeadsBefore(ps, beginIndex);
                    fea_after = getChunkHeadsAfter(ps, endIndex);
                    fea_dtree = getDependencyTree(rm, ps, beginIndex, endIndex);
                    fea_ptree = getParseTree(ps, beginIndex, endIndex);
                    indexOfMention1 = ps.wordStartToStanfordIndex.get(getStartOfMentionHead(rm.mention1.head.start(), rm.mention1.headText));
                    indexOfMention2 = ps.wordStartToStanfordIndex.get(getStartOfMentionHead (rm.mention2.head.start(), rm.mention2.headText));
                    fea_dpath = "DPathET=" + pdoc.sid2dptree.get(sid).getPathForTwoTokens(rm, indexOfMention1, indexOfMention2).replace(" ", "_") + " ";
                    //	fea_dpath = fea_dpath + "DPathEH=" + pdoc.sid2dptree.get(sid).getPathForTwoTokens(rm, indexOfMention1, indexOfMention2, getHeadOfMention(rm.mention1), getHeadOfMention(rm.mention2)).replace(" ", "_") + " ";
                    if (featuretype.equals("all")) feature =  fea_dpath + fea_dtree + fea_ptree + fea_before + fea_between + fea_after;
                    if (featuretype.equals("parse"))  feature =  fea_ptree + fea_before + fea_between + fea_after;
                    if (featuretype.equals("dependency")) feature =  fea_dpath + fea_dtree;
                }
            } else {
			//	System.err.println("Mentions are not correctly aligned in sentence " + sid + rm.mention1.head + rm.mention1.headText + "\t" + rm.mention2.head + rm.mention2.headText);
			//	if (! ps.wordStartToIndex.containsKey(rm.mention1.head.start())) System.err.println("M1 is not aligned");
			//	if (! ps.wordStartToIndex.containsKey(rm.mention2.head.start())) System.err.println("M2 is not aligned");
            }
        }
        return feature;
    }

    private static String getDependencyTree(RelationMention rm, Rel_ParsedSen ps, int begin, int end) throws Throwable {
        String feature = new String();
        // m1 ahead of m2
        boolean M1M2 = rm.mention1.head.start() < rm.mention2.head.start() ? true : false;
        String ET1 = rm.mention1.entity.type; 	String ET2 = rm.mention2.entity.type;
        String ET12 = ET1 + "--" + ET2 + "--";
        // the head in ps.heads are the words on which the Mention.head is dependent
        String DW1 = M1M2 ? ps.heads.get(begin) : ps.heads.get(end);
        String DW2 = M1M2 ? ps.heads.get(end) : ps.heads.get(begin);
        // since we only consider the head word, indices begin/end are for mentions.head
        String H1 = M1M2 ? ps.words.get(begin) : ps.words.get(end);
        String H2 = M1M2 ? ps.words.get(end) : ps.words.get(begin);
        String ET1DW1 = "ET1DW1=" + ET1 + "--" + DW1 + " ";
        String ET2DW2 = "ET2DW2=" + ET2 + "--" + DW2 + " ";
        String H1DW1 = "H1DW1=" + H1 + "--" + DW1 + " ";
        String H2DW2 = "H2DW2=" + H2 + "--" + DW2 + " ";
        int np = 0; int pp = 0; int vp = 0;
        // not in the same chunk if either M1 or M2 is not in any NP/PP/VP
        String beginIOBs = ps.IOBChains.get(begin); String endIOBs = ps.IOBChains.get(end);
        if (beginIOBs.contains("NP") && endIOBs.contains("NP")) np = 1;
        if (beginIOBs.contains("PP") && endIOBs.contains("PP")) pp = 1;
        if (beginIOBs.contains("VP") && endIOBs.contains("VP")) vp = 1;
        for (int i = begin + 1; i <= end; i++) {
            String bio = ps.IOBChunks.get(i);
            if ( (bio.equals("O") || bio.equals("B-NP")) && np == 1) np++;
            if ( (bio.equals("O") || bio.equals("B-PP")) && pp == 1) pp++;
            if ( (bio.equals("O") || bio.equals("B-VP")) && vp == 1) vp++;
        }
        boolean sameNP = np == 1 ? true : false;
        boolean samePP = pp == 1 ? true : false;
        boolean sameVP = vp == 1 ? true : false;
        String ET12SameNP = "ET12SameNP=" + ET12 + sameNP + " ";
        String ET12SamePP = "ET12SamePP=" + ET12 + samePP + " ";
        String ET12SameVP = "ET12SameVP=" + ET12 + sameVP + " ";

        feature = ET1DW1 + ET2DW2 + H1DW1 + H2DW2 + ET12SameNP + ET12SamePP + ET12SameVP;
//		feature = ET12SameNP;
        return feature;
    }

    private static String getParseTree(Rel_ParsedSen ps, int begin, int end) {
        String PTP = new String(); String PTPH = new String();
        ArrayList<String> labels = new ArrayList<String>(); // for removing duplicate
        String leftChain = ps.IOBChains.get(begin);
        String rightChain = ps.IOBChains.get(end);
        String[] leftLabels = leftChain.split("/");
        String[] rightLabels = rightChain.split("/");
        int leastCommonIndex = leftLabels.length <= rightLabels.length ? leftLabels.length : rightLabels.length;
        String topPhrase = new String(); String topPhraseHead = new String();
        int indexOfTopPh = -1;
        for (int i = 0; i < leastCommonIndex; i++) {
            String leftPhrase = leftLabels[i].split("-")[1];
            String rightPhrase = rightLabels[i].split("-")[1];
            if (! leftPhrase.equals(rightPhrase)) {
                topPhrase = leftLabels[i - 1].split("-")[1];
                indexOfTopPh = i - 1;
                break;
            }
        }
        // if topPhrase is empty
        if (topPhrase.isEmpty()) {
            topPhrase = leftLabels[leftLabels.length -1].split("-")[1];
            // if m1 and m2 are in the same chunk, path would only be the chunk label
            if (leftLabels.length == rightLabels.length) {
                PTP = topPhrase;
                for (int i = begin -1; i >= 0; i--) {
                    if (! ps.functions.get(i).equals("NOFUNC")) {
                        for (String l : ps.functions.get(i).split("/")) {
                            if (l.equals(topPhrase))
                                topPhraseHead = ps.words.get(i);
                            PTPH = topPhrase + "--" + topPhraseHead;
                        }
                        if (! topPhraseHead.isEmpty())
                            break;
                    }
                }
            } else { // cases like m1: S/VP/NP m2: S/VP/
                for (int i = begin; i <= end; i++) {
                    if (topPhraseHead.isEmpty()) {
                        String function = ps.functions.get(i);
                        for (String f : function.split("/")) {
                            if (f.equals(topPhrase))
                                topPhraseHead = ps.words.get(i);
                        }
                    } else {
                        break;
                    }
                }
                // if topPhraseHead is still empty, then it's not in between; do left search
                if (topPhraseHead.isEmpty()) {
                    for (int i = begin -1; i >= 0 ; i--) {
                        String function = ps.functions.get(i);
                        for (String f : function.split("/")) {
                            if (f.equals(topPhrase))
                                topPhraseHead = ps.words.get(i);
                        }
                        if (! topPhraseHead.isEmpty())
                            break;
                    }
                }
                if (leftLabels.length > leastCommonIndex) {
                    for (int i = leftLabels.length - 1; i >= leastCommonIndex - 1; i--) {
                        String l = leftLabels[i];
                        if (! l.equals("NOFUNC")) {
                            l = l.replace("B-", ""); l = l.replace("I-", "");
                            if (! labels.contains(l)) {
                                labels.add(l);
                                PTP = PTP + l + "----";
                                PTPH = PTPH + l + "----";
                            }
                        }
                    }
                    if (! topPhraseHead.isEmpty()) PTPH = PTPH + "--" + topPhraseHead + "----";

                }
                if (rightLabels.length > leastCommonIndex) {
                    PTP = topPhrase + "----";
                    if (! topPhraseHead.isEmpty()) PTPH = topPhrase + "--" + topPhraseHead + "----";
                    for (int i = leastCommonIndex; i < rightLabels.length; i++) {
                        String l = rightLabels[i];
                        if (! l.equals("NOFUNC")) {
                            l = l.replace("B-", ""); l = l.replace("I-", "");
                            if (! labels.contains(l)) {
                                labels.add(l);
                                PTP = PTP + l + "----";
                                PTPH = PTPH + l + "----";
                            }
                        }
                    }
                }
                PTP = PTP.substring(0, PTP.length() - 4); PTPH = PTPH.substring(0, PTPH.length() - 4);
            }

        } else {
            //System.out.println("topPhrase " + topPhrase);
            for (int i = begin; i <= end; i++) {
                if (topPhraseHead.isEmpty()) {
                    String function = ps.functions.get(i);
                    for (String f : function.split("/")) {
                        if (f.equals(topPhrase))
                            topPhraseHead = ps.words.get(i);
                    }
                } else {
                    break;
                }
            }
            // if topPhraseHead is still empty, then it's not in between; do left search
            if (topPhraseHead.isEmpty()) {
                for (int i = begin -1; i >= 0 ; i--) {
                    String function = ps.functions.get(i);
                    for (String f : function.split("/")) {
                        if (f.equals(topPhrase))
                            topPhraseHead = ps.words.get(i);
                    }
                    if (! topPhraseHead.isEmpty())
                        break;
                }
            }
            //	System.out.println("topPhrase\t" + topPhrase + "\ttopPhraseHead\t" + topPhraseHead);
            // left
            for (int i = leftLabels.length - 1; i > indexOfTopPh; i--) {
                String l = leftLabels[i];
                if (! l.equals("NOFUNC")) {
                    l = l.replace("B-", ""); l = l.replace("I-", "");
                    if (! labels.contains(l)) {
                        labels.add(l);
                        PTP = PTP + l + "----";
                        PTPH = PTPH + l + "----";
                    }
                }
            }
            // top phrase
            if (! topPhraseHead.isEmpty())
                PTPH = PTPH + topPhrase + "--" + topPhraseHead + "----";
            // right
            for (int i = indexOfTopPh + 1; i < rightLabels.length; i++) {
                String l = rightLabels[i];
                if (! l.equals("NOFUNC")) {
                    l = l.replace("B-", ""); l = l.replace("I-", "");
                    if (! labels.contains(l)) {
                        labels.add(l);
                        PTP = PTP + l + "----";
                        PTPH = PTPH + l + "----";
                    }
                }
            }
            PTP = PTP.substring(0, PTP.length() - 4); PTPH = PTPH.substring(0, PTPH.length() - 4);
        }
        PTP = "PTP=" + PTP;
        if (! PTPH.isEmpty()) {
            PTPH = "PTPH=" + PTPH;
        }
        String parsingFeature = PTP + " " + PTPH;
        parsingFeature = parsingFeature.trim();

        return parsingFeature + " ";
    }
    /*
     * generate first and last phrase heads after M2
     * /
    private static String getChunkHeadsAfter(Rel_ParsedSen ps, int endIndexOfMention) throws Throwable {
        String feature = new String();
        ArrayList<String> wordsAfter = new ArrayList<String>();
        for (int i = endIndexOfMention + 1; i < ps.words.size(); i++) {
            if (! ps.functions.get(i).equals("NOFUNC")) {
                wordsAfter.add(ps.words.get(i));
                if (wordsAfter.size() == 2) break;
            }
        }
        if (wordsAfter.size() == 1) {
            feature = "CPHAM2F=" + wordsAfter.get(0) + " ";
        } else if (wordsAfter.size() == 2) {
            feature =  "CPHAM2F=" + wordsAfter.get(0) + " "
                    + "CPHAM2L=" + wordsAfter.get(1) + " ";
        }
        return feature;
    }
    /*
     * generate first and last phrase heads before M1
     * /
    private static String getChunkHeadsBefore(Rel_ParsedSen ps, int beginIndexOfMention) throws Throwable {
        String feature = new String();
        ArrayList<String> wordsBefore = new ArrayList<String>();
        for (int i = beginIndexOfMention -1; i >=0; i -- ) {
            if (! ps.functions.get(i).equals("NOFUNC")) {
                wordsBefore.add(ps.words.get(i));
                if (wordsBefore.size() == 2) break;
            }
        }

        if (wordsBefore.size() == 1) {
            feature = "CPHBM1F=" + wordsBefore.get(0) + " ";
        } else if (wordsBefore.size() == 2) {
            feature =  "CPHBM1F=" + wordsBefore.get(wordsBefore.size() -2) + " "
                    + "CPHBM1L=" + wordsBefore.get(wordsBefore.size() -1) + " ";
        }
        return feature;
    }
    /*
     * also generate CPP (path of phrase labels) and CPPH (with heads) features
     * /
    private static String getChunkHeadsInBetween(RelationMention rm, Rel_ParsedSen ps, int begin, int end) throws Throwable {
        String CPHBNULL = new String(); // no phrase in between
        String CPHBFL = new String();	// only one
        String CPHBF = new String(); 	// first
        String CPHBL = new String(); 	// last
        String CPHBO = new String();	// other
        String CPP = new String(); 		// phrase labels path
        String CPPH = new String();		// path with head (if at most 2 phrases in between)
        String CPatternET = new String();	// link all the chunk heads in between with entity types of mentions
        String CPatternEH = new String(); 	// link all the chunk heads in between with strings of mentions
        String feature = new String();	// output this one
        ArrayList<Integer> headIndicesBetween = new ArrayList<Integer>();
        // only these will be used for CPPH
        ArrayList<Integer> headIndicesBetweenForCPPH = new ArrayList<Integer>();

        for (int i = begin + 1; i < end; i++) {
            if (! ps.functions.get(i).equals("NOFUNC")) { // head!!!!
                headIndicesBetween.add(i);
            }
        }
        if (headIndicesBetween.isEmpty()) { // + CPHNULL
            CPHBNULL = "CPHBNULL=true";
        } else {
            if (headIndicesBetween.size() < 5) {
                for (Integer i : headIndicesBetween) {
                    CPatternET = CPatternET + ps.words.get(i) + "_";
                }
                if (rm.mention1.head.start() < rm.mention2.head.start()) {
                    CPatternET = "CPatternET=" + rm.mention1.entity.type + "_" + CPatternET + rm.mention2.entity.type;
                    //	CPatternEH = "CPatternEH=" + getHeadOfMention(rm.mention1) + "_" + CPatternET + getHeadOfMention(rm.mention2);
                }
                else {
                    CPatternET = "CPatternET=" + rm.mention2.entity.type + "_" + CPatternET + rm.mention1.entity.type;
                    //	CPatternEH = "CPatternEH=" + getHeadOfMention(rm.mention2) + "_" + CPatternET + getHeadOfMention(rm.mention1);
                }
            }
            if (headIndicesBetween.size() == 1) { // + CPHBFL
                int index = headIndicesBetween.get(0);
                CPHBFL = "CPHBFL=" + ps.words.get(index);
                headIndicesBetweenForCPPH.add(index);
            } else if (headIndicesBetween.size() >= 2) { // + CPHBF + CPHBL + CPHBO
                int first = headIndicesBetween.get(0);
                int last = headIndicesBetween.get(headIndicesBetween.size() -1);
                CPHBF = "CPHBF=" + ps.words.get(first);
                CPHBL= "CPHBL=" + ps.words.get(last);
                if (headIndicesBetween.size() > 2) { // + CPHBO
                    ArrayList<String> otherChunkHeads = new ArrayList();
                    for (int i = 1; i < headIndicesBetween.size() -1; i++) {
                        int index = headIndicesBetween.get(i);
                        String head = ps.words.get(index);
                        String oneHeadFeature = "CPHBO" + "_" + head + "=TRUE ";
                        if (! otherChunkHeads.contains(oneHeadFeature)) {
                            otherChunkHeads.add(oneHeadFeature);
                            CPHBO = CPHBO + oneHeadFeature;
                        }
                    }
                } else if (headIndicesBetween.size() == 2) { // + CPPH
                    int index1 = headIndicesBetween.get(0); int index2 = headIndicesBetween.get(1);
                    headIndicesBetweenForCPPH.add(index1); headIndicesBetweenForCPPH.add(index2);
                }
            }
        }
        // all cases have CPP feature
        for (int i = begin; i <= end; i++) {
            if (! ps.functions.get(i).equals("NOFUNC"))
                CPP = CPP + ps.functions.get(i) + "----";
        }
        if (! CPP.isEmpty()) {
            CPP = CPP.substring(0, CPP.length() -4);
            CPP = "CPP=" + CPP + " ";
        }
        // generate CPPH only is there are at most two phrases in between
        if (headIndicesBetween.size() <= 2) {
            // check if m1 and m2 are head of chunks first
            boolean isM1CH = ps.functions.get(begin).equals("NOFUNC") ? false : true;
            boolean isM2CH = ps.functions.get(end).equals("NOFUNC") ? false : true;
            if (isM1CH)
                headIndicesBetweenForCPPH.add(0, begin); // insert it to be the first
            if (isM2CH)
                headIndicesBetweenForCPPH.add(end);
            for (int i : headIndicesBetweenForCPPH) {
                CPPH = CPPH + ps.functions.get(i) + "--" + ps.words.get(i) + "----";
            }
            if (! CPPH.isEmpty()) {
                CPPH = CPPH.substring(0, CPPH.length() -4);
                CPPH = "CPPH=" + CPPH + " ";
            }
        }
        if (! CPHBNULL.isEmpty()) feature = feature + CPHBNULL.trim() + " ";
        if (! CPHBFL.isEmpty()) feature = feature + CPHBFL.trim() + " ";
        if (! CPHBF.isEmpty()) feature = feature + CPHBF.trim() + " ";
        if (! CPHBO.isEmpty()) feature = feature + CPHBO.trim() + " ";
        if (! CPHBL.isEmpty()) feature = feature + CPHBL.trim() + " ";
        if (! CPP.isEmpty()) feature  = feature + CPP.trim() + " ";
        if (! CPPH.isEmpty()) feature  = feature + CPPH.trim() + " ";
        if (! CPatternET.isEmpty()) feature = feature + CPatternET + " ";
        if (! CPatternEH.isEmpty()) feature = feature + CPatternEH + " ";
        return feature;
    }

    **** Parser based features are not included for now */

    /**
     * 1) generate head of  both M1 and M2 according to Singpore's definition
     * 	generally set head as the last word of the mention
     * 	when there is a preposition, set it as the last word before preposition
     * 2) shrink names into one token
     * @throws java.io.IOException
     * @throws
     */
    public static String getHeadOfMention(AceEntityMention em) throws IOException  {
        String head = new String();
        //	if (em.type.equals("NAM")) {
        // shrink names into one token
        //		head = em.headText.replace("\n", " ").replace(" ", "_");
        //	} else {
        StringTokenizer st = new StringTokenizer(em.getHeadText());
        ArrayList<String> tokens = new ArrayList();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            // added: if there is a preposition like "University of ABC", set University as head
            if (nextToken.equals("of")) {
                //System.out.println(headText);
                break;
            } else {
                tokens.add(nextToken);
            }
        }
        head = tokens.get(tokens.size() - 1);
        if (em.type.equals("NAM") && Character.isLowerCase(head.charAt(0))) {
            head = head.substring(0, 1).toUpperCase() + head.substring(1);
        }
        //	}
        return head;
    }


    private static int getStartOfMentionHead(int startOfHeadText, String headText) throws IOException  {
        int beginIndex = startOfHeadText;
        StringTokenizer st = new StringTokenizer(headText);
        ArrayList<String> tokens = new ArrayList();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            // added: if there is a preposition like "University of ABC", set University as head
            if (nextToken.equals("of")) {
                //System.out.println(headText);
                break;
            } else {
                tokens.add(nextToken);
            }
        }
        //String head = tokens.get(tokens.size() - 1);
        //
        for (int i = 0; i < tokens.size() - 1; i++) {
            beginIndex = beginIndex + tokens.get(i).length() + 1;
        }
        return beginIndex;
    }

    public static List<String> getBagOfMentionWords(String str, String feaName) throws IOException {
        ArrayList<String> words = new ArrayList<String>();
        for (String w : Tokenizer.tokenize(str.trim())) {
            if (! w.isEmpty()) {
                String word = feaName + "_" + w + "=TRUE";
                if (! words.contains(word)) {
                    words.add(word);
                }
            }
        }
        return words;
    }

    public static String getWordsInBetweenMentions(Span spanM1, Span spanM2, String fileText) {
        String words = new String();
        int beginOfMention1 = spanM1.start(); int endOfMention1 = spanM1.end();
        int beginOfMention2 = spanM2.start(); int endOfMention2 = spanM2.end();
        if (endOfMention1 < beginOfMention2)
            words = fileText.substring(endOfMention1, beginOfMention2);
        if (endOfMention2 < beginOfMention1)
            words = fileText.substring(endOfMention2, beginOfMention1);
        return words.trim();
    }

    public static String getWordsInBetween(AceRelationMention relMention, String fileText) {
        String words = new String();
        int beginOfMention1 = relMention.arg1.extent.start();
        int beginOfMention2 = relMention.arg2.extent.start();
        int endOfMention1 = relMention.arg1.extent.end();
        int endOfMention2 = relMention.arg2.extent.end();
        if (beginOfMention1 < beginOfMention2) {
            if (endOfMention1 < beginOfMention2) {
                words = fileText.substring(endOfMention1, beginOfMention2);
            }
        } else {
            if (endOfMention2 < beginOfMention1) {
                words = fileText.substring(endOfMention2, beginOfMention1);
            }
        }
        return words.trim();
    }

    /**
     * procedure for replacing pronoun mention to its most recent non-pronoun antecedent
     */

    private static AceRelationMention replacePronoun(AceRelationMention relMention) {
        boolean proM1 = relMention.arg1.type.equals("PRO") ? true : false;
        boolean proM2 = relMention.arg2.type.equals("PRO") ? true : false;
        if (proM1) {
            int minDistance = 10000; AceEntityMention aem1 = null;
            for (Object m : relMention.arg1.entity.mentions) {
                AceEntityMention aem = (AceEntityMention) m;
                if (! aem.type.equals("PRO") && minDistance > (relMention.arg1.extent.start() - aem.extent.end())
                        && (relMention.arg1.extent.start() - aem.extent.end()) > 0) {
                    aem1 = aem;	minDistance = relMention.arg1.extent.start() - aem.extent.end();
                }
            }
            if (aem1 != null) relMention.arg1 = aem1;

        }
        if (proM2) {
            int minDistance = 10000; AceEntityMention aem2 = null;
            for (Object m : relMention.arg2.entity.mentions) {
                AceEntityMention aem = (AceEntityMention) m;
                if (! aem.type.equals("PRO") && minDistance > (relMention.arg2.extent.start() - aem.extent.end())
                        && (relMention.arg2.extent.start() - aem.extent.end()) > 0) {
                    aem2 = aem;
                    minDistance = relMention.arg2.extent.start() - aem.extent.end();
                }
            }
            if (aem2 != null) relMention.arg2 = aem2;

        }
        return relMention;
    }
    /**
     * get relation mentions from an AceDocument
     */
    public static List<AceRelationMention> getAllRelMentions(AceDocument ad) {
        ArrayList<AceRelationMention> rmAry = new ArrayList<AceRelationMention>();
        ArrayList relations = ad.relations;
        for (int i=0; i<relations.size(); i++) {
            AceRelation relation = (AceRelation) relations.get(i);
            String relationClass = relation.relClass;
            if (relationClass.equals("IMPLICIT")) continue;
            ArrayList relationMentions = relation.mentions;
            for (int j=0; j<relationMentions.size(); j++) {
                AceRelationMention relationMention = (AceRelationMention) relationMentions.get(j);
                // create new RelationMention with type & subtype
//                AceRelationMention acerel = new AceRelationMention (relation.type, relation.subtype);
//                acerel.setArg(1, relationMention.arg1);
//                acerel.setArg(2, relationMention.arg2);
//                //			acerel.setLexicalCon(relationMention.lexicalCon);
//                rmAry.add(acerel);
                rmAry.addAll(relationMentions);
            }
        }
        return rmAry;
    }
    /**
     *
     * @param rm
     * @return
     * @throws Throwable
     */
    public static List<String> generateCountryFeature(AceRelationMention rm, List<String> nationalities)
            throws Throwable {
        List<String> features = new ArrayList<String>();
        String h1 = getHeadOfMention(rm.arg1);
        String h2 = getHeadOfMention(rm.arg2);
        if (Collections.binarySearch(nationalities, h1.toLowerCase()) >= 0) {
            features.add("CountryET2=true--" + rm.arg2.entity.type);
        }
        if (Collections.binarySearch(nationalities, h2.toLowerCase()) >= 0) {
            features.add("ET1Country=true--" + rm.arg1.entity.type);
        }
        return features;
    }
    /**
     *
     * @param rm
     * @return
     * @throws Throwable
     */
    public static List<String> generateSocialTriggerFeature(AceRelationMention rm,
                                                         Map<String, List<String>> soctrigger2class) throws Throwable {
        List<String> features = new ArrayList<String>();
        String h1 = getHeadOfMention(rm.arg1);
        String h2 = getHeadOfMention(rm.arg2);
        if (soctrigger2class.containsKey(h1)) {
            String ET2 = rm.arg2.entity.type;
            for (String c : soctrigger2class.get(h1)) {
                //feature = feature + "SC1ET2=" + ET2 + "--" + c + " ";
                features.add("SC1ET2=" + ET2 + "--" + c);
            }
        }
        if (soctrigger2class.containsKey(h2)) {
            String ET1 = rm.arg1.entity.type;
            for (String c : soctrigger2class.get(h2)) {
                //feature = feature + "ET1SC2=" + ET1 + "--" + c + " ";
                features.add("ET1SC2=" + ET1 + "--" + c);
            }
        }
        return features;
    }

    public static Map<String, NomContext> readEmploymentNominal(String employmentNominalContextFile) throws IOException {
        Map<String, NomContext> nomToNC = new HashMap();
        File ncFile = new File(employmentNominalContextFile);
        BufferedReader reader = new BufferedReader(new FileReader(ncFile));
        String sline = new String();
        String nomLine = new String();
        String patternLine = new String();

        while ( (sline = reader.readLine()) != null ) {
            if (sline.contains("nominal: ")) {
                nomLine = sline;
            } else if (sline.contains("ngram patterns: ")) {
                patternLine = sline;
                //nominal: executive	total
                String nom = nomLine.substring(nomLine.indexOf(" ") + 1).trim();

                // make sure to delete next line when use one stage's result
                nom = nom.substring(0, nom.indexOf("\t"));
                //ngram patterns:
                String patterns = patternLine.substring("ngram patterns: ".length());
                String[] pas = patterns.split("\t");
                if (! nomToNC.containsKey(nom)) {
                    NomContext NC = new NomContext(nom);
                    NC.addingContexts(pas);
                    nomToNC.put(nom, NC);
                } else {
                    NomContext NC = nomToNC.get(nom);
                    NC.addingContexts(pas);
                    nomToNC.put(nom, NC);
                }
            }
        }
        return nomToNC;
    }
    /**
     * generate head of  both M1 and M2 according to Singpore's definition
     * generally set head as the last word of the mention
     * when there is a preposition, set it as the last word before preposition
     * @throws java.io.IOException
     * @throws
     */
    public static String getHeadOfMention(String headText) throws IOException  {
        StringTokenizer st = new StringTokenizer(headText);
        ArrayList<String> tokens = new ArrayList();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            // added: if there is a preposition like "University of ABC", set University as head
            if (nextToken.equals("of")) {
                //System.out.println(headText);
                break;
            } else {
                tokens.add(nextToken);
            }
        }
        String head = tokens.get(tokens.size() - 1);
        return head;
    }
    /**
     *
     * @param rm
     * @param nomToNC
     * @param fileText
     * @return
     * @throws java.io.IOException
     */
    public static List<String> generateNCFeature(AceRelationMention rm, Map<String, NomContext> nomToNC, String fileText) throws IOException {
        String NCINfeature = new String();
        String NCOVfeature = new String();
        String NCMDfeature = new String();
        //	if ( (rm.mention1.entity.type.equals("PERSON") && rm.mention2.entity.type.equals("ORGANIZATION"))
        //		||(rm.mention1.entity.type.equals("PERSON") && rm.mention2.entity.type.equals("GPE"))) {
        //String headOfMention1 = rm.mention1.headText.toLowerCase();
        String headOfMention1 = getHeadOfMention(rm.arg1.getHeadText().toLowerCase());

        if (headOfMention1.substring(headOfMention1.length() -1, headOfMention1.length()).equals("s")) {
            headOfMention1 = headOfMention1.substring(0, headOfMention1.length() -1);
        }
        if (nomToNC.containsKey(headOfMention1)) {
            int confidence = nomToNC.get(headOfMention1).EVPatterns.size() +
                    nomToNC.get(headOfMention1).VEPatterns.size();
            if (confidence > 2 ) {

                //		NCINfeature = "NCIN=true ";

                // word overlapping between middle tokens of mentions and learned nominals
                ArrayList<String> overlappedWords = new ArrayList<String>();
                ArrayList<String> overlappedMods = new ArrayList<String>();
                int start1 = rm.arg1.head.start();
                int end1 = rm.arg1.head.end();
                int start2 = rm.arg2.head.start();
                int end2 = rm.arg2.head.end();
                String order = new String();
                String entityType = new String();
                if ( (start1 > -1 && start1 < fileText.length()) &&
                        (start2 > -1 && start2 < fileText.length())) {

                }
                if (start1 > start2){
                    order="VE";
                    entityType = rm.arg2.entity.type + "--" + rm.arg1.entity.type + "--";
                    if (end2 + 1 != start1) {
                        String strWords = fileText.substring(end2 + 1, start1).replace("\n", " ").trim();
                        String[] words = Tokenizer.tokenize(strWords);
                        for (String word : words) {
                            if (nomToNC.get(headOfMention1).VEMidTokens.contains(word)) {
                                if (! overlappedWords.contains(word)) {
                                    overlappedWords.add(word);
                                }
                            }
                        }
                    }
                } else {
                    order="EV";
                    entityType = rm.arg1.entity.type + "--" + rm.arg2.entity.type + "--";
                    if (end1 + 1 != start2) {
                        try {
                            String strWords = fileText.substring(end1 + 1, start2).replace("\n", " ").trim();
                            String[] words = Tokenizer.tokenize(strWords);
                            for (String word : words) {
                                if (nomToNC.get(headOfMention1).EVMidTokens.contains(word)) {
                                    if (! overlappedWords.contains(word)) {
                                        overlappedWords.add(word);
                                    }
                                }
                            }
                        } catch (StringIndexOutOfBoundsException e)  {
                            //
                        }
                    }
                    if (rm.arg1.extent.start() < rm.arg1.head.start()) {
                        try {
                            String strMods = fileText.substring(rm.arg1.extent.start(), rm.arg1.head.start());
                            String[] modifiers = Tokenizer.tokenize(strMods);
                            for (String modifier : modifiers) {
                                if (! overlappedMods.contains(modifier)) {
                                    overlappedMods.add(modifier);
                                }
                            }
                        } catch (StringIndexOutOfBoundsException e) {
                            //
                        }
                    }
                }
                NCINfeature = "NCIN=" + order + "--" + entityType + "--true ";
                for (int i = 0; i < overlappedWords.size(); i++) {
                    //	NCOVfeature = NCOVfeature + order + "NCOV" + String.valueOf(i+1) + "=" + overlappedWords.get(i) + " ";
                    NCOVfeature = NCOVfeature + order + "NCOV" + "=" + entityType + overlappedWords.get(i) + " ";
                }
                for (String mod : overlappedMods) {
                    NCMDfeature = NCMDfeature + order + "NCMD" + "=" + mod + " ";
                }
            }
        }
        //}
        //String feature = NCINfeature + NCOVfeature + NCMDfeature; //  +
        List<String> features = new ArrayList<String>();
        features.add(NCINfeature.trim());
        features.add(NCOVfeature.trim());
        features.add(NCMDfeature.trim());
        return features;
    }
    /**
     * generate nationality semantic resources.
     * @param nationalityFile
     * @return
     * @throws java.io.IOException
     */
    public static List<String> readInNationality(String nationalityFile) throws IOException {
        ArrayList<String> ns = new ArrayList<String>();
        File in = new File(nationalityFile);
        BufferedReader reader = new BufferedReader(new FileReader(in));
        String sline = new String();
//		int i = 0;
        while ( (sline = reader.readLine()) != null) {
            if (sline.contains("nationality")) {
                int beginIndex = sline.indexOf("\"") + 1;
                int endIndex = sline.lastIndexOf("\"");
                String n = sline.substring(beginIndex, endIndex);
                // use lower case
                ns.add(n.toLowerCase());
                //	if (i < 5) {
                //		System.out.println(n.toLowerCase());
                //		i++;
                //	}
            }
        }
        Collections.sort(ns);
        return ns;
    }
    /**
     * generate social triggers from file
     * @param socialTriggerFile
     * @return
     * @throws java.io.IOException
     */
    public static Map<String, List<String>> readInSocialTriggers(String socialTriggerFile) throws IOException {
        Map<String, List<String>> m = new HashMap();
        File in = new File(socialTriggerFile);
        BufferedReader rdr = new BufferedReader(new FileReader(in));
        String sline = new String();
        while ( (sline = rdr.readLine()) != null) {
            String[] elements = sline.split(" ");
            if (elements.length == 2) {
                String trigger = elements[0]; String semClass = elements[1];
                if (! m.containsKey(trigger)) {
                    List<String> classes = new ArrayList<String>();
                    classes.add(semClass);
                    m.put(trigger, classes);
                } else {
                    if (! m.get(trigger).contains(semClass)) {
                        m.get(trigger).add(semClass);
                    }
                }
            }
        }
        return m;
    }

//    public static Map<Integer, Integer> buildOffsetTable (StringBuffer fileTextWithXML) {
//        Map<Integer, Integer> xmlTosgmOffsets = new HashMap<Integer, Integer>();
//        boolean inTag = false;
//        int length = fileTextWithXML.length();
//        StringBuffer fileText = new StringBuffer();
//        int sgmIndex = 0;
//        for (int i=0; i<length; i++) {
//            xmlTosgmOffsets.put(i, sgmIndex);
//            char c = fileTextWithXML.charAt(i);
//            if(c == '<') inTag = true;
//            if (!inTag) {
//                fileText.append(c);
//                sgmIndex++;
//            }
//            if(c == '>') inTag = false;
//        }
//        return xmlTosgmOffsets;
//    }



}
