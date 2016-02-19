package edu.nyu.jet.ice.relation;// -*- tab-width: 4 -*-

import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.IcePath.IcePathChoice;

import java.util.*;
import java.io.*;

/**
 *  Provides a mechanism for simulated active learning of relations, avoiding the
 *  need to label the same dependency paths repeatedly by hand.
 *  <p>
 *  If there is a local file <CODE>relationOracle</CODE>, use that file to label
 *  candidate paths.  If there is no entry for a particular candidate, ask the
 *  user to label it and record that label for future use in file
 *  <CODE>newRelationOracle</CODE>.
 */

public class RelationOracle {

    static String status = "UNKNOWN";

    // each line has a repr and YES or NO
    static Set<String> knownRelations = new HashSet<String>();

    public static boolean exists () {
        if (status.equals("YES"))
            return true;
        else if (status.equals("NO"))
            return false;
        else try {
            if (new File("relationOracle").exists()) {
                BufferedReader reader = new BufferedReader (new FileReader ("relationOracle"));
                String line;
                while ((line = reader.readLine()) != null) {
                    knownRelations.add(line);
                }
                status = "YES";
                return true;
            } else {
                status = "NO";
                return false;
            }
        } catch (IOException e) {
                System.err.println("IOException in RelationOracle");
                return false;
        }
    }

    /**
     *  If a relation oracle table has been loaded, use that table to label the
     *  candidate paths on <CODE>foundPatterns</CODE>.  If a candidate path
     *  is not in the table, ask the user for a label, apply that label
     *  and record that label for future use.  
     *  <p>
     *  At the end, write a file <CODE>newRelationOracle</CODE> with
     *  an updated table.
     */

    public static void label (List<IcePath> foundPatterns) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            for (IcePath fp : foundPatterns) {
                String repr = fp.getRepr();
                if (knownRelations.contains(repr + " YES")) {
                    fp.setChoice(IcePathChoice.YES);
                    return;
                } else if (knownRelations.contains(repr + " NO")) {
                    fp.setChoice(IcePathChoice.NO);
                    return;
                }
                while (true) {
                    System.out.print (repr + "?");
                    String response = reader.readLine();
                    if (response.equals("Y")) {
                        fp.setChoice(IcePathChoice.YES);
                        knownRelations.add(repr + " YES");
                        return;
                    } else if (response.equals("N")) {
                        fp.setChoice(IcePathChoice.NO);
                        knownRelations.add(repr + " NO");
                        return;
                    } else {
                        System.out.println("Type Y or N");
                    }
                }
            }
            PrintWriter writer = new PrintWriter (new FileWriter ("newRelationOracle"));
            for (String repr : knownRelations) {
                writer.println(repr);
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("IOException in RelationOracle");
        }
    }
}
