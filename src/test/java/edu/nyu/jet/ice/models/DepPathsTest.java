package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.models.DepPaths;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.parser.DepTransformer;

import java.util.*;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 *  basic tests for DepPaths.buildSyntacticPathOnSpans and linearize:
 *        minimal SVO;
 *        perfect and PP;
 *        passive.
 */

public class DepPathsTest {

    private static DepTransformer transformer;
    private static DepPathRegularizer depPathRegularizer;

    @BeforeClass
    public static void depPathsTest () {
	transformer = new DepTransformer("yes");
	transformer.setUsePrepositionTransformation(false);
	depPathRegularizer = new DepPathRegularizer();
    }

    @Test
    public void buildSyntacticPathTest1 () throws IOException {
	Document doc = new Document("Fred Smith visited Chicago.");
	Span arg1 = new Span(0, 11);
	Span arg2 = new Span(19, 27);
	int from = 0;
	int to = 19;
	SyntacticRelationSet relations = new SyntacticRelationSet();
	relations.read(new BufferedReader (new StringReader(
	"nsubj | visited | 11 | VBD | Fred_Smith | 0 | NNP\n" +
	"dobj | visited | 11 | VBD | Chicago | 19 | NNP\n" +
	"punct | visited | 11 | VBD | . | 27 | .\n"))); 
	SyntacticRelationSet transformedRelations = 
	    transformer.transform(relations.deepCopy(), doc.fullSpan());
	relations = transformedRelations;
	relations.addInverses();
	List<Span> localSpans = new ArrayList<Span>();

	DepPath path = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, relations,localSpans);
	assertEquals(path.toString(), "nsubj-1:visit:dobj");

	DepPath regularizedPath = depPathRegularizer.regularize(path);
	String type1 = "PERSON";
	String type2 = "GPE";
	String linearizedPath = regularizedPath.linearize(doc, relations, type1, type2);
	System.out.println(" linearized pah:  " + linearizedPath);
    }

    @Test
    public void buildSyntacticPathTest2 () throws IOException {
	Document doc = new Document("Fred Smith has lived in Chicago.");
	// test:  transformation, prep
	Span arg1 = new Span(0, 11);
	Span arg2 = new Span(24, 32);
	int from = 0;
	int to = 24;
	SyntacticRelationSet relations = new SyntacticRelationSet();
	relations.read(new BufferedReader (new StringReader(
	// "nsubj | lived | 15 | VBZ | Fred_Smith | 0 | NNP\n" +
	"nsubj | has | 11 | VBZ | Fred_Smith | 0 | NNP\n" +
	"vch | has | 11 | VBZ | lived | 15 | VBN\n" +
	"prep | lived | 15 | VBN | in | 21 | IN\n" +
	"pobj | in | 21 | IN | Chicago | 24 | NNP\n" +
	// "punct | lived | 15 | VBZ | . | 32 | .\n")));
	"punct | has | 11 | VBZ | . | 32 | .\n")));
	SyntacticRelationSet transformedRelations = 
	    transformer.transform(relations.deepCopy(), doc.fullSpan());
	relations = transformedRelations;
	relations.addInverses();
	List<Span> localSpans = new ArrayList<Span>();

	DepPath path = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, relations,localSpans);
	assertEquals(path.toString(), "nsubj-1:live:prep:in:pobj");

	DepPathRegularizer depPathRegularizer = new DepPathRegularizer();
	DepPath regularizedPath = depPathRegularizer.regularize(path);
	String type1 = "PERSON";
	String type2 = "GPE";
	String linearizedPath = regularizedPath.linearize(doc, relations, type1, type2);
	System.out.println(" linearized pah:  " + linearizedPath);
    }

    @Test
    public void buildSyntacticPathTest3 () throws IOException {
	Document doc = new Document("Fred Smith was shot in Chicago.");
	// test:  transformation, prep
	Span arg1 = new Span(0, 11);
	Span arg2 = new Span(23, 30);
	int from = 0;
	int to = 23;
	SyntacticRelationSet relations = new SyntacticRelationSet();
	relations.read(new BufferedReader (new StringReader(
	"nsubj | was | 11 | VBD | Fred_Smith | 0 | NNP\n" +
	"vch | was | 11 | VBD | shot | 15 | VBN\n" +
	"prep | shot | 15 | VBN | in | 20 | IN\n" +
	"pobj | in | 20 | IN | Chicago | 23 | NNP\n" +
	"punct | was | 11 | VBZ | . | 30 | .\n")));
	SyntacticRelationSet tRelations = 
	    transformer.transform(relations.deepCopy(), doc.fullSpan());
	relations.addInverses();
	tRelations.addInverses();
	List<Span> localSpans = new ArrayList<Span>();

	DepPath tPath = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, tRelations,localSpans);
// 	assertEquals(path.toString(), "nsubj-1:be:vch:shoot:prep:in:pobj");
	System.out.println("tPath = " + tPath.toString());

	DepPath path = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, relations,localSpans);
	DepPathRegularizer depPathRegularizer = new DepPathRegularizer();
	DepPath regularizedPath = depPathRegularizer.regularize(path);
	String type1 = "PERSON";
	String type2 = "GPE";
	String linearizedPath = regularizedPath.linearize(doc, relations, type1, type2);
	System.out.println(" linearized pah:  " + linearizedPath);
    }
/*  -- unit test to be completed
    @Test
    public void collectPathsTest () throws IOException {
	Document doc = new Document ("Fred_Smith has lived in Chicago.  ");
	doc.annotate("sentence", new Span(0, 33), null);
	doc.annotate("ENAMEX", new Span(0, 11), new FeatureSet("TYPE", "PERSON")); 
	doc.annotate("ENAMEX", new Span(24, 32), new FeatureSet("TYPE", "GPE")); 
	SyntacticRelationSet relations = new SyntacticRelationSet();
	relations.read(new BufferedReader (new StringReader(
	"nsubj | lived | 15 | VBZ | Fred_Smith | 0 | NNP\n" +
	"prep | lived | 15 | VBN | in | 21 | IN\n" +
	"pobj | in | 21 | IN | Chicago | 24 | NNP\n" +
	"punct | lived | 15 | VBZ | . | 32 | .\n" +
	"nsubj-1 | Fred_Smith | 0 | ? | lived | 15 | ?\n" +
	"prep-1 | in | 21 | ? | lived | 15 | ?\n" +
	"pobj-1 | Chicago | 24 | ? | in | 21 | ?\n" +
	"punct-1 | . | 32 | ? | lived | 15 | ?\n")));
	DepPaths.collectPaths(doc, relations, relations);
    }
*/
}
