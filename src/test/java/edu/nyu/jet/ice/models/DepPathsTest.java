package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.models.DepPaths;;
import Jet.Tipster.*;
import Jet.Lisp.FeatureSet;
import Jet.Parser.SyntacticRelationSet;

import java.util.*;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 *  basic tests for DepPaths.buildSyntacticPathOnSpans
 */

public class DepPathsTest {

    @Test
    public void buildSyntacticPathTest1 () throws IOException {
	// Fred_Smith visited Chicago.
	Span arg1 = new Span(0, 11);
	Span arg2 = new Span(19, 27);
	int from = 0;
	int to = 19;
	SyntacticRelationSet relations = new SyntacticRelationSet();
	relations.read(new BufferedReader (new StringReader(
	"nsubj | visited | 11 | VBD | Fred_Smith | 0 | NNP\n" +
	"dobj | visited | 11 | VBD | Chicago | 19 | NNP\n" +
	"punct | visited | 11 | VBD | . | 27 | .\n" +
	"nsubj-1 | Fred_Smith | 0 | ? | visited | 11 | ?\n" +
	"dobj-1 | Chicago | 19 | ? | visited | 11 | ?\n" +
	"punct-1 | . | 27 | ? | visited | 11 | ?")));
	List<Span> localSpans = new ArrayList<Span>();

	DepPath p = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, relations,localSpans);
	assertEquals(p.toString(), "nsubj-1:visit:dobj");
    }

    @Test
    public void buildSyntacticPathTest2 () throws IOException {
	// Fred_Smith has lived in Chicago.
	// test:  transformation, prep
	Span arg1 = new Span(0, 11);
	Span arg2 = new Span(24, 32);
	int from = 0;
	int to = 24;
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
	List<Span> localSpans = new ArrayList<Span>();

	DepPath p = DepPaths.buildSyntacticPathOnSpans (from, to, arg1, arg2, relations,localSpans);
	assertEquals(p.toString(), "nsubj-1:live:prep:in:pobj");
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
