# Using ICE

*Feburary 24, 2015*

## Introduction

JET is NYU's general information extraction engine, capable of extracting entities, relations and events.  It is distributed with
corpus-trained models for the ACE 2005 entity, relation, and event subtypes.  Although these types provide quite good
coverage for general news, there is a frequent need to add new types for specific domains.  In principle these could be
added by annotating all instances of the new type in the training collection.  Unless the type is very rare, this is
likely to be effective.  However, it is also likely to be quite slow and expensive.

ICE, the integrated customization environment, is intended to provide a fast but limited capability for extending JET to
new types.  It allows the user to add new entity types and relation types;  work on adding events is in progress.
Entity types are defined in terms of sets of names and common nouns; relation types are defined in terms of sets of
dependency paths (words and syntactic relations).  The user is asked to provide a few examples (typically 1 to 3) of a
new entity type, or one example of a new relation type.  The system then uses distributional analysis and bootstrapping
strategies to fill out the set, subject to user review. This approach is likely to produce much more complete sets than
if the user came up with all the set entries themselves.

We go through the steps of adding a new corpus to the system, finding entities, building new entity sets, finding a new
phrases, and building new relations. Each function can be completed in a panel of the Ice Swing GUI. Ice will bring us
to the Corpus panel when started.

## Starting Ice

Unzip the package and run:

    ./runice.sh

![The corpus panel](ice1.png)

## The Corpus Panel

__Adding a new corpus__

 A corpus is defined by a directory and a file extension.  The set of non-directory files dominated by that directory and ending in the specified extension constitute an ICE corpus. 

To add a new corpus, first click *Add corpus* , then either enter the path of the
root directory of the corpus or click *browse* to select the directory where the
files are located. You can apply a filter on the extension of the filename (e.g. sgm) 
in the corresponding text filter;  You need to click "Apply" to apply the filter.

Clicking "Preprocess" will start the preprocessing process, which is rather **slow**.
This process will perform dependency parsing, name tagging, and coref resolution, and
will save the result of these steps for use by the following components.

As the first step of preprocessing, we remove characters not recognized by Ice and copy
all documents to the cache directory. We process documents in the cache directory 
in later steps. 

__The background corpus__

Some of the corpus analysis you will perform compares the corpus you are
analyzing against a reference corpus, referred to as the *background corpus*.  
The background corpus is selected here.

__The status monitor__

The status monitor provides status information on the corpus you are
currently analyzing.  A set of red and green status messages indicate what
pre-processing steps have been performed on the corpus.  The *refresh*
button updates these messages.

Two more buttons complete this panel.  The *persist* button saves the 
current status of ICE, including the corpus that you worked on, as well
as extracted entity sets and relations.  The *export* button saves the
information on entity sets and relations in a format that can be loaded
into Jet.

![The entities panel](ice2.png)

## Finding entities

__Finding salient terms__

The first step in analyzing the corpus is to identify the salient terms --
those which occur more frequently in this corpus than in a 'background 
corpus'.  In the Entities panel, click *Find Entities*.  The result will be a list of terms
ranked by their relative frequency in the two corpora (those with the 
highest frequency appearing first).

Note that, you can also index entities in this panel. This is necessary
for building entity sets. Please refer to the next section for more information.

![The entity sets panel](ice3.png)

## Entity sets

__Basic approach__

You may have some prior notion from the domain of the texts or from 
the analysis task as to what entity sets should be added.  Alternatively, 
the top ranked unassigned terms from the term finder may suggest a needed
class.  Either way, you are ready to start building a new entity class 
(this is in addition to the classes {person, organization, geo-political entity, 
location, facility, vehicle, weapon} predefined by ACE.

The set builder is based on distributional similarity:  Words which appear 
in the same contexts (e.g., as the subject of the same set of verbs) are likely
to be semantically similar.  You start the process by giving some seeds -- typical
members of the set.  The tool then ranks the other words in the corpus based on
their similarity to the seeds.  You then mark some of these words as correct set
members or not.  After marking some words, you may rerank the term list based on
similarity to the seeds plus all the words marked correct.  After a few iterations,
you will have a substantial set of words marked as correct set members.  Finally
you save these words as the definition of a new entity set.

__Indexing__

To make the process efficient, ICE keeps an index of the words appearing 
at least n times in a given context.  Before you build the first entity
set from a new corpus, you have to build this index.  Go to the *Entities*
panel, set the cutoff (minimum value of n, default = 1, we recommend n \> 3)
and select *Index*; this will take a while.

__Building a set__

You are now ready to build an entity set.  Select the *Add* button on the 
left side of the *Entity Set* panel and provide a name for the entity set.
Next add at least two seeds to the set, using the *Add* button on the
right side, under "Members". Click *Suggest* if you want Ice to suggest
seeds for you. (Currently *Suggest* will always suggest the same seeds
even if you click it multiple times.)

Once you have entered your seeds, select *Expand*.  ICE will compute
similarities as described above and display a list of terms, ranked
by their similarity to the seeds. You mark items as correct or
incorrect, rerank if you want, and select save when satisfied with the set.

![Ranking entities](rankentities.png)

In Figure 2, you can judge whether a displayed word in the ranked list belongs
to an entity set or not. To choose among Yes, No Undecided, use the radio button
or keyboard shortcuts (Y for Yes, N for No and U for undecided). If
Yes or No is chosen for an entity, the decision will be shown next to the entity.
After several entities are judged, click *Rerank* to rerank the list using the
information you just provided. When finished, click *Save* and *Exit*.

After returning to the Entity Sets panel, click the *Save* button on the right of
 the entity set panel and then the *Save* button on the left of the entity set panel
 to keep it in the Ice environment.

Finally, click *Persist* in the status panel to save the newly-built entities set to
the ice.yml file, so that these entities will be available after Ice is closed and
 re-opened. Click *Export* to export all entity sets in the ice.yml file to Jet.

![The phrases panel](ice4.png)

## Finding Phrases

There is an analogous process for defining new types of relations and events:  you first find the
most salient patterns in a corpus and then use ICE to create clusters of these patterns.

A pattern is a sequence of words connecting two entities of specified types. (Actually, internally a pattern also
specifies the grammatical relation between these words, but this level of detail is hidden from the user.)  Because the
pattern must connect two entities, defining new entity types can lead to new patterns connecting these entities.

To find the most common phrases in a corpus, use the *ALl phrases* button. (
If *Sentential phrases* is clicked, only patterns of the subject - verb - object form
are displayed.  This is useful for finding events.)
Phrases will be ranked based on the ratio between their frequency in the current
corpus and their frequency in the background corpus. This is similar to what the *Find entities*
button does for entities.

![The relations panel](ice5.png)

## Building Relations

Building relations is just like building entity sets.  Select the *Add* button on the
left side of the *Relations* panel and provide a name for the relation.
Next add at least one seed to the set, using the *Add* button on the
right side, under "Members". Click *Suggest* if you want Ice to suggest a seed pattern
for you. (Currently *Suggest* will always suggest the same seeds
 even if you click it multiple times.)

Once you have entered your seeds, select *Expand*.  ICE will bootstrap
patterns that distribute similarly to your seed pattern. You mark items as correct or
incorrect, rerank if you want, and select save when satisfied with extracted
phrases.

![Ranking relations](rankrelations.png)

Like building entity sets, you can choose whether you want to accept or reject a pattern
 using radio buttons or keyboard shortcuts. You could click *Iterate* to search for more
 patterns using the pattens you just approved. When finished, click *Save* and exit. If you
 are uncertain about what a pattern phrase means, hover your mouse over the phrase, and
 a tooltip will appear, which shows the sentence from which the pattern phrase is extracted.

 After returning to the Relations panel, click the *Save* button on the right of
  the entity set panel and then the *Save* button on the left of the entity set panel
  to keep it in the Ice environment. If you manually edit the relations, you will
  also need to click the right *Save* to save it to the relations, and click left
  *Save* to save the relation to the system.

 Finally, click *Persist* in the status panel to save the newly-built entities set to
 the ice.yml file, so that these entities will be available after Ice is closed and
  re-opened. Click *Export* to export all entity sets in the ice.yml file to Jet.

