package edu.nyu.jet.aceJet;

import edu.nyu.jet.aceJet.*;
import edu.nyu.jet.tipster.ExternalDocument;

import java.util.*;

public class TypedRelationExtractorScorer {

    int correctEntityMentions = 0;
    int missingEntityMentions = 0;
    int spuriousEntityMentions = 0;
    int correctRelationMentions = 0;
    int missingRelationMentions = 0;
    int spuriousRelationMentions = 0;
    int relationMentionTypeErrors = 0;

    Map<String, String> entityAlignment = new HashMap<String, String>();

    Set<String> symmetricTypes = new HashSet<String>();

    String type = "NONE";

    {
        // symmetricTypes.add("PHYS.Located");       // symmetric in 2005 scorer but not a symmetric relation
        symmetricTypes.add("PHYS:Near");             // 2004 and 2005
        symmetricTypes.add("PER-SOC:Business");      // 2004 and 2005
        symmetricTypes.add("PER-SOC:Family");        // 2004 and 2005
        symmetricTypes.add("PER-SOC:Other");         // 2004
        symmetricTypes.add("PER-SOC:Lasting-Personal"); // 2005
        symmetricTypes.add("EMP-ORG:Partner");       // 2004
        symmetricTypes.add("EMP-ORG:Other");         // 2004
        symmetricTypes.add("OTHER-AFF:Other");       // 2004
    }


    public TypedRelationExtractorScorer(String type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void scoreDocument(String sourceFile, String responseFile, String keyFile) {

        ExternalDocument doc = new ExternalDocument("sgml", sourceFile);
        doc.setAllTags(true);
        doc.open();
        AceDocument response = new AceDocument(sourceFile, responseFile);
        AceDocument key = new AceDocument(sourceFile, keyFile);

        // collect key entity mentions
        Set<AceEntityMention> keyEntityMentions = new HashSet<AceEntityMention>();
        for (AceEntity entity : key.entities)
            keyEntityMentions.addAll(entity.mentions);
        Map<Integer, AceEntityMention> keyEntityMentionMap = new HashMap<Integer, AceEntityMention>();
        for (AceEntityMention mention : keyEntityMentions) {
            int end = mention.head.end();
            if (doc.charAt(end) == '.') end--;
            keyEntityMentionMap.put(end, mention);
        }

        // align response entity mentions
        for (AceEntity entity : response.entities) {
            for (AceEntityMention mention : (ArrayList<AceEntityMention>) entity.mentions) {
                int end = mention.head.end();
                if (doc.charAt(end) == '.') end--;
                AceEntityMention keyMention = keyEntityMentionMap.get(end);
                if (keyMention == null) {
                    System.out.println("Spurious mention " + mention.text);
                    spuriousEntityMentions++;
                } else if (!keyEntityMentions.contains(keyMention)) {
                    System.out.println("Spurious mention (duplicate head) " + mention.text);
                    spuriousEntityMentions++;
                } else {
                    entityAlignment.put(mention.id, keyMention.id);
                    keyEntityMentions.remove(keyMention);
                    correctEntityMentions++;
                }
            }
        }
        for (AceEntityMention keyMention : keyEntityMentions) {
            System.out.println("Missing  mention " + keyMention.text + " [head = " + keyMention.headText + "]");
            missingEntityMentions++;
        }

        // collect key relation mentions
        Set<AceRelationMention> keyRelationMentions = new HashSet<AceRelationMention>();
        for (AceRelation relation : key.relations) {
            if (relation.type.equals(type)) {
                keyRelationMentions.addAll(relation.mentions);
            }
        }

        // score response relation mentions
        for (AceRelation relation : response.relations) {
            if (!relation.type.equals(type)) {
                continue;
            }
            relation_loop:
            for (AceRelationMention mention : (ArrayList<AceRelationMention>) relation.mentions) {
                String type = mention.relation.type;
                String subtype = mention.relation.subtype;
                String responseArg1 = mention.arg1.id;
                String mappedArg1 = entityAlignment.get(responseArg1);
                String responseArg2 = mention.arg2.id;
                String mappedArg2 = entityAlignment.get(responseArg2);
                // look for key relation with those args
                for (AceRelationMention keyMention : keyRelationMentions) {
                    String keyArg1 = keyMention.arg1.id;
                    String keyArg2 = keyMention.arg2.id;
                    String keyType = keyMention.relation.type;
                    String keySubtype = keyMention.relation.subtype;
                    boolean symmetric = symmetricTypes.contains(keyType + ":" + keySubtype);
                    if ((keyArg1.equals(mappedArg1) && keyArg2.equals(mappedArg2)) ||
                            (symmetric && keyArg1.equals(mappedArg2) && keyArg2.equals(mappedArg1))) {
                        // check for matching type
                        if (type.equals(keyType))
                            correctRelationMentions++;
                        else
                            relationMentionTypeErrors++;
                        keyRelationMentions.remove(keyMention);
                        continue relation_loop;
                    }
                }
                // none:  spurious
                spuriousRelationMentions++;
                System.out.println("Spurious relation (" + type + ":" + subtype + "): " + mention.text);
            }
        }
        for (AceRelationMention keyMention : keyRelationMentions) {
            String type = keyMention.relation.type;
            String subtype = keyMention.relation.subtype;
            System.out.println("Missing  relation (" + type + ":" + subtype + "): " + keyMention.text);
            missingRelationMentions++;
        }
    }

    public void reportScores() {
        System.out.println();
        System.out.println("Correct entity mentions:    " + correctEntityMentions);
        System.out.println("Missing entity mentions:    " + missingEntityMentions);
        System.out.println("Spurious entity mentions:   " + spuriousEntityMentions);
        System.out.println();
        System.out.println("Correct relation mentions:  " + correctRelationMentions);
        System.out.println("Relation mention type errs: " + relationMentionTypeErrors);
        System.out.println("Missing relation mentions:  " + missingRelationMentions);
        System.out.println("Spurious relation mentions: " + spuriousRelationMentions);
        int responseCount = correctRelationMentions + relationMentionTypeErrors + spuriousRelationMentions;
        int keyCount = correctRelationMentions + relationMentionTypeErrors + missingRelationMentions;
        float precision = (float) correctRelationMentions / responseCount;
        float recall = (float) correctRelationMentions / keyCount;
        float f = 2 * precision * recall / (precision + recall);
        System.out.println();
        System.out.printf("Precision = %4.1f\n", 100 * precision);
        System.out.printf("Recall =    %4.1f\n", 100 * recall);
        System.out.printf("F =         %4.1f\n", 100 * f);
    }
}
