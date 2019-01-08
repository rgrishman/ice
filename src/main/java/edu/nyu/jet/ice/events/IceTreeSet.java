package edu.nyu.jet.ice.events;

import java.util.*;
import java.io.*;

public class IceTreeSet {

	List<IceTree> list = new ArrayList<IceTree>();
	int count;
    int numTrees = 0;

    IceTreeSet (String fileName, int threshold) {
        try {
            BufferedReader reader = new BufferedReader (new FileReader (fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                int j = line.indexOf("\t");
                if (j >= 0) {
                    count = Integer.parseInt(line.substring(0, j));
                    if (count < threshold)
                        continue;;
                    line = line.substring(j + 1);
                }
                IceTree iceTree = IceTreeFactory.getIceTree(line);
                iceTree.count = count;
                list.add(iceTree);
                numTrees++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
	    }
        System.out.println("loaded " + numTrees + " entries from " + fileName);
    }

    IceTreeSet (String fileName) {
        this (fileName, 1);
    }

}
