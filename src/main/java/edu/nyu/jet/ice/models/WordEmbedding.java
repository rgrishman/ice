package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.events.IceTree;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WordEmbedding provides the basic functionality for loading, accessing,
 * and computing similarities using word embeddings.
 */

public class WordEmbedding {

    private static Map<String, double[]> embeddings = null;
    private static int dim = 0;

    /**
     *  Load word embeddings from <CODE>embeddingFile</CODE>.
     *  Each line of the file consists of a word and the coordinates of
     *  word, separated by a single blank.
     */

    public static void loadWordEmbedding(String embeddingFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(embeddingFile));
        String line;
        embeddings = new HashMap<String, double[]>();
        int count = 1;
        System.err.println("Load embeddings...");
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(" ");
            dim = parts.length - 1;
            double[] embedding = new double[dim];
            for (int i = 1; i < parts.length; i++) {
                embedding[i - 1] = Double.valueOf(parts[i]);
            }
            embeddings.put(parts[0], embedding);
            if (count % 10000 == 0) {
                System.err.print(count + "...");
            }
            count++;
        }
        System.err.println();
        br.close();
    }

    /**
     *  Returns <CODE>true</CODE> if a file of word embeddings has been loaded.
     */

    public static boolean isLoaded () {
        return embeddings != null;
    }

    /**
     *  Returns the word embedding of <CODE>word</CODE>, or <CODE>null</CODE>
     *  if the embedding is undefined.
     */

    public static double[] embed (String word) {
        return embeddings.get(word.toLowerCase());
    }

    /**
     *  Returns the word embedding of <CODE>phrase</CODE>, computed as the
     *  sum of the embeddings of the constituent words.  Returns <CODE>null</CODE>
     *  if any words has an undefined embedding.
     */

    public static double[] embed (String[] phrase) {
        double[] v = new double[dim];
        for (int i = 0; i < phrase.length; i++) {
            double[] wordEmbedding = embed(phrase[i]);
            if (wordEmbedding == null)
                return null;
            for (int k = 0; k < dim; k++) {
                v[k] += wordEmbedding[k];
            }
        }
        return v;
    }

    /**
     *  Returns the cosine similarity of <CODE>token1</CODE> and <CODE>token2</CODE>
     *  based on tneir word embeddings.  Returns 0 if either embedding is
     *  undefined.  Returns 1 if the arguments are equal, whether or not their
     *  embedding is defined.
     */

    public static double similarity(String token1, String token2) {
        if (token1 == null || token2 == null) return 0.;
        if (token1.equalsIgnoreCase(token2)) return 1.;
        return similarity(embed(token1), embed(token2));
    }

    /**
     *  Returns the similarity of two paths, defined as the product of the similarities
     *  of the corresponding lexical items in the two paths.  If the paths are of
     *  different lengths, 0 is returned.
     */

    public static double pathSimilarity (String path1, String path2) {
	String[] seq1 = path1.split(":");
	String[] seq2 = path2.split(":");
	if (seq1.length != seq2.length) return 0;
	double sim = 1;
	for (int i = 1; i < seq1.length; i+=2) {
	    sim = sim * similarity(seq1[i], seq2[i]);
	}
	return sim;
    }

    /**
     *  Returns the similarity of two IceTree trees.  This is computed based on the
     *  the similarity of the triggers and the arguments, equally weighted.
     *  <p>
     *  More preciswely, we use the word embeddings of the triggers and the values
     *  of the arguments.  For arguments, we us all pairs of aeguments (one from
     *  each tree) with the same role label.
     */

    public static double treeSimilarity (IceTree tree1, IceTree tree2) {
        int numargs1 = tree1.numArgs();
        int numargs2 = tree2.numArgs();
        int minNumArgs = Math.min(numargs1, numargs2);
        double triggerSimilarity = similarity(tree1.getTrigger(), tree2.getTrigger());
        double argSimilarity = 0;
        for (int i = 0; i < numargs1; i++) {
            for (int j = 0; j < numargs2; j++) {
                if (tree1.getArgRole(i).equals(tree2.getArgRole(j))) {
                    argSimilarity += similarity(tree1.getEntityType(i), tree2.getEntityType(j));
                } 
            }
        }
        argSimilarity /= minNumArgs;   
        return (triggerSimilarity + argSimilarity) / 2;
    }

    /**
     *  Returns the cosine similarity of vectors<CODE>embedding1</CODE> and <CODE>embedding2</CODE>.
     *  If either vector is null, returns 0.
     */

    public static double similarity(double[] embedding1, double[] embedding2) {
        if (embedding1 == null || embedding2 == null) return 0;
        return dotProduct(embedding1, embedding2) /
                (norm(embedding1) * norm(embedding2));
    }
 
    /**
     *  Returns the dot product of vectors <CODE>v1</CODE> and <CODE>v2</CODE>.
     */

    public static double dotProduct(double[] v1, double[] v2) {
        double result = 0;
        for (int i = 0; i < v1.length && i < v2.length; i++) {
            result += v1[i] * v2[i];
        }
        return result;
    }

    /**
     *  Returns the norm (length) of vector <CODE>v</CODE>.
     */

    public static double norm(double[] v) {
        double result = 0;
        for (int i = 0; i < v.length; i++) {
            result += v[i] * v[i];
        }
        return Math.sqrt(result);
    }

}
