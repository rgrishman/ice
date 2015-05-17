package edu.nyu.jet.ice.relation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class NomContext {
    public String nominal = new String();
    // E:= entity (Person) == mention 1 in ACE
    // V:= value (ORG) == mention 2 in ACE
    public ArrayList<String> EVPatterns = new ArrayList<String>();
    public ArrayList<String> VEPatterns = new ArrayList<String>();
    public ArrayList<String> EVMidTokens = new ArrayList<String>();
    public ArrayList<String> VEMidTokens = new ArrayList<String>();
    public ArrayList<String> EVModifiers = new ArrayList<String>();
    public ArrayList<String> VEModifiers = new ArrayList<String>();
    public int totalFreq = 0;

    public NomContext(String learnedNominal) {
        this.nominal = learnedNominal;
    }

    public void addingContexts(String[] fatPatterns) {
        for (String fatPa : fatPatterns) {
            // example: took over as *NN* of (103 EV)
            try {
                int indexOfParen = fatPa.lastIndexOf("(");
                //	System.out.println(fatPa);
                String pattern = fatPa.substring(0, indexOfParen).trim();
                String freqDirection = fatPa.substring(indexOfParen + 1);
                String[]  buf = freqDirection.split(" ");
                int freq = Integer.parseInt(buf[0]);
                this.totalFreq = this.totalFreq + freq;
                String direction = buf[1].replace(")", "");

                if (direction.equals("EV")) {
                    // , the chief *NN* of (2558 EV)
                    if (! this.EVPatterns.contains(pattern)) {
                        this.EVPatterns.add(pattern);
                    }
                    int beginIndex = pattern.indexOf("*NN*") + "*NN*".length();
                    if (beginIndex != pattern.length()) {
                        String wordsInBetween = pattern.substring(beginIndex).trim();
                        String[] words = wordsInBetween.split(" ");
                        for (String word : words) {
                            if (! this.EVMidTokens.contains(word)) {
                                this.EVMidTokens.add(word);
                            }
                        }
                    }
                    String wordsBefore = pattern.substring(0, pattern.indexOf("*NN*")).trim();
                    String[] words = wordsBefore.split(" ");
                    for (String word : words) {
                        if (! this.EVModifiers.contains(word)) {
                            this.EVModifiers.add(word);
                        }
                    }
                } else if (direction.equals("VE")) {
                    // 's chief *NN* , (114 VE)
                    if (! this.VEPatterns.contains(pattern)) {
                        this.VEPatterns.add(pattern);
                    }
                    int beginIndex = pattern.indexOf("*NN*");
                    String wordsInBetween = pattern.substring(0, beginIndex).trim();
                    if (pattern.contains(" *NN*")) {
                        String[] words = wordsInBetween.split(" ");
                        for (String word : words) {
                            if (! this.VEMidTokens.contains(word)) {
                                this.VEMidTokens.add(word);
                            }
                        }
                    }
                    // how to choose modifers for VE patterns???
                } else {
                    System.out.println("wrong entity value (mention order)");
                }
            } catch ( StringIndexOutOfBoundsException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File in = new File("E:\\Data\\ACE\\relation_eval\\ORG_EMP\\nominal_context\\MI\\nominalAndcontext_log.txt");
        BufferedReader reader = new BufferedReader(new FileReader(in));
        String sline = new String();
        String nomLine = new String();
        String patternLine = new String();
        Map<String, NomContext> nomToNC = new HashMap();

        while ( (sline = reader.readLine()) != null ) {
            if (sline.contains("nominal: ")) {
                nomLine = sline;
            } else if (sline.contains("ngram patterns: ")) {
                patternLine = sline;
                //nominal: executive	total
                String nom = nomLine.substring(nomLine.indexOf(" ") + 1).trim();
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

        ArrayList<String> ary = new ArrayList<String>();

        for (String nom : nomToNC.keySet()) {
            int numOfPas = nomToNC.get(nom).EVPatterns.size() + nomToNC.get(nom).VEPatterns.size();
            ary.add(String.valueOf(numOfPas) + "<-->" + nom);
        }

        class ScoreComparator implements Comparator<String> {
            public int compare(String str1, String str2)
            {
                int i1 = Integer.parseInt(str1.substring(0, str1.indexOf("<-->")));
                int i2 = Integer.parseInt(str2.substring(0, str2.indexOf("<-->")));
                return (i2 - i1);
            }
        }
        ScoreComparator sc = new ScoreComparator();

        Collections.sort(ary, sc);

        File out = new File("E:\\Data\\ACE\\relation_eval\\ORG_EMP\\nominal_context\\MI\\sorted_nominalAndcontext.txt");
        FileWriter writer = new FileWriter(out);
        File outNomOnly = new File("E:\\Data\\ACE\\relation_eval\\ORG_EMP\\nominal_context\\MI\\MI_NOM_ONLY.txt");
        FileWriter writeNom = new FileWriter(outNomOnly);
        int i = 0;
        for (String s : ary) {
            String[] buf = s.split("<-->");
            int numOfPatterns = Integer.parseInt(buf[0]);
            if ( numOfPatterns != 1) {
                i++;
                String nom = buf[1];
                writeNom.write(nom +  "," + "\t" + numOfPatterns + "\n");
            }
            String nom = buf[1];

            writer.write("nominal: " + nom + "\t" + "number of patterns: " + buf[0] + "\n");
            NomContext NC = nomToNC.get(nom);
            writer.write("EVPatterns: \n");
            for (String pattern : NC.EVPatterns) {
                writer.write(pattern + "\n");
            }
            writer.write("\nVEPatterns: \n");
            for (String pattern : NC.VEPatterns) {
                writer.write(pattern + "\n");
            }
            writer.write("\n\n");
        }
        System.out.println(i);
        writer.close();
        writeNom.close();
    }
}