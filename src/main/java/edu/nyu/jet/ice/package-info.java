/**
<h3>Principal Classes of ICE</h3>

<h3>Corpus analysis</h3>

The corpus analysis performed by ICE can be divided into two types:
analysis which is domain-independent and need only be done once,
and analysis which is domain-dependent and may be done repeatedly
as part of a bootstrapping process.
<p>
The domain-independent processing includes
<ul>
<li> part-of-speech tagging </li>
<li> dependency parsing </li>
<li> coreference analysis </li>
<li> name tagging with respect to a set of generic name models
     (for people, places, and organizations) </li>
<li> numeric and time expressions </li>
</ul>
All of this analysis is performed as part of preprocessing
by the <tt>IcePreprocessor</tt> class and stored in the
<tt>cache</tt> directory, which has one subdirectory for
each corpus being preprocessed by ICE.
<p>
<i>Note that this preprocessing could be made more accurate
by making use of domain-specific information, but we do
not do so at this time.</i>
<p>
The domain-specific processing involves finding in
the corpus all dependency paths which connect two
entities (words which are members of an entity set).  A
relation is defined as a set of dependency paths, so
this process collects the candidate paths to be used
in relation bootstrapping.  As the entity sets grow during IE
customization, this set of candidate paths also grows and so needs
to be recomputed.  This analysis is performed by the 
<tt>RelationFinder</tt> class, which invokes <tt>DepPaths</tt>.
To speed processing, <tt>DepPaths</tt> makes use of the
information saved in the cache by preprocessing.
<p>
One additional step of corpus analysis involves the computation
of term context vectors, which record the dependency contexts of
each term in the corpus.  This information, which is used to guide
the creation of entity sets, is computed by class <tt>EntitySetIndexer</tt>.

<h3>Dependency Paths</h3>

<h4>Representation</h4>

ICE relations (class <tt>IceRelation</tt>) are specified in terms of 
the types of its arguments (entity sets) and a set of lexicalized 
dependency paths (LDPs).  An LDP specifies a particular sequence of
words and dependency relations.  For communicating with the user we
want to accept and generate English phrases.  Methods in class 
<tt>DepPath</tt> perform the generation of phrases;  the
correspondence between the internal representation, the phrase,
and a complete sentence with an example of this path is
captured in instances of class <tt>IcePath</tt>.
<p>
We are currently experimenting in Jet with <i>set-generalized</i>
LDPs. where the words are constrained to be members of a set rather
than taking on single values.

<h4>Matching</h4>

Exact match of two LDPs an be done by simple sring match.
To determine whether a document has an instance of an LDP, we
can generate all the LDPs from a document and see if any one
matches.
<p>
For better recall we may want to allow approximate (soft) matching.
Class <tt>PathMatcher</tt> provides edit-distance-based matching
between two LDPs.

<h4>Exporting</h4>

After some entity sets and relations have been defined using Ice,
class <tt>JetEngineBuilder</tt> is used to write thes out in
a format which is accepted  by Jet.  It is represented in Jet
using classes <tt>AnchoredPath</tt> and <tt>AnchoredPathSet</tt>.

<h3>Bootstrapping</h3>

The bootstrapping of relations is managed by class <tt>Bootstrap</tt>.
The basic process starts with a seed provided by the user and ranks
the candidate paths with respect to this seed using an elaborate
combination of scores
<p>
To reduce the manual input required when conducting repeated evaluations
for the same relation, class <tt>RelationOracle</tt> captures the user's
classifications on the initial run and generates automatic responses
to the same queries on subsequent runs.
*/
package edu.nyu.jet.ice;
