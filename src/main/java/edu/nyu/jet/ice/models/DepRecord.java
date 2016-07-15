package edu.nyu.jet.ice.models;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java Bean record of a typed dependency path
 * Fields include: LDP, lexicalized LDP, example, count
 * Created by yhe on 11/22/15.
 */
public class DepRecord {
    private String ldp;
    private String lexicalLdp;
    private String example;
    private int count;

    public DepRecord(String ldp, String lexicalLdp, String example, int count) {
        this.ldp = ldp;
        this.lexicalLdp = lexicalLdp;
        this.example = example;
        this.count = count;
    }

    public List<DepRecord> loadFromFile(String relationReprFile, TObjectIntHashMap typedRelationCountMap)
            throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(relationReprFile));
        String line = null;
        List<DepRecord> depRecords = new ArrayList<DepRecord>();
        while ((line = r.readLine()) != null) {
            String[] parts = line.split(":::");
            if (parts.length == 3) {
                String ldp = parts[0];
                String lexicalLdp = parts[1];
                String example = parts[2];
                int count = typedRelationCountMap.get(ldp);
                depRecords.add(new DepRecord(ldp, lexicalLdp, example, count));
            }
        }
        r.close();
        return depRecords;
    }

    public String getLdp() {
        return ldp;
    }

    public void setLdp(String ldp) {
        this.ldp = ldp;
    }

    public String getLexicalLdp() {
        return lexicalLdp;
    }

    public void setLexicalLdp(String lexicalLdp) {
        this.lexicalLdp = lexicalLdp;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
