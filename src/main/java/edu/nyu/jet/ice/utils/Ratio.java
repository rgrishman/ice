package edu.nyu.jet.ice.utils;

import java.util.*;
import java.io.*;

/**
 * given two document profiles, in the form of word frequencies,
 * dependency triple frequencies, etc., computes
 *      f log f / f'
 * where f is the frequency in corpus 1, and f' the frequency
 * in corpus2
 */

public class Ratio {

	static Map<String, Integer> count1 = new TreeMap<String, Integer>();
	static Map<String, Integer> count2 = new TreeMap<String, Integer>();
	
	public static void main (String[] args) throws IOException {
		String countFile1 = args[0];
		String countFile2 = args[1];
		String ratioFile = args[2];

		readCounts (countFile1, count1);
		readCounts (countFile2, count2);
		computeRatios(new PrintWriter (new FileWriter (ratioFile)));
	}

	public static void readCounts (String file, Map<String, Integer> counts) throws IOException {
		BufferedReader reader = new BufferedReader (new FileReader (file));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] field = line.trim().split("\t");
			if (field.length == 2)
				counts.put(field[1], Integer.valueOf(field[0]));
		}
	}

	public static void computeRatios (PrintWriter writer) throws IOException {
		for (String w : count1.keySet()) {
			Integer f1 = count1.get(w);
			Integer f2 = count2.get(w);
			f1++;
			f2 = (f2 == null) ? 1 : f2 + 1;
			float ratio = (float) f1 / f2 * (float) Math.log((float) f1);
                        writer.printf ("%8.1f\t%s\n", ratio, w);
		}
		writer.close();
	}
}



