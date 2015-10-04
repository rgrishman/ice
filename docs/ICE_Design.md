<!--
IcePreprocessor calls
	parseDocument calls
		parseSentence(doc,span,relations) calls
			parseSentence(doc,span,relations,false)

so .dep file presumably does not include transformations
-->

# ICE Design Document                                    

Oct. 4, 2015

## OVERVIEW

ICE relies on a detailed analysis of the corpora to provide guidance to
the user in building entity sets and relations. In the initial design for ICE,
users initiated various steps of this analysis through the ICE console after
adding a new corpus,  This made for OK demos but proved to be unrealistic for
large corpora because each step took so long (several hours).  It made more
sense to compute as much as possible in advance (through a batch job).

This led us to consider how much could be precomputed, even at the cost of some
additional complexity.  For example, the parser operates on sequences of lexical
items, some of which are multi-word terms identified by the user in the process
of building an extraction model. The parses are needed in turn for defining new
relations. The simplest system structure would reparse the corpus each time the
user added some terms but this would introduce an unacceptable delay into the
user's session each time some terms are added.

We break this dependency by precomputing dependency parses with only single token
lexical items items. If we want to find the dependency path between multi-word 
entities, we use the head word to represent the multi-word entity. Head word
is determined by the IcePreprocessor.findTermHead() function.

<!--
R: ?? do I have this correct  ?? do you update the parses using the terms

Y: Yes you are correct. I added more comments above.
-->

## WHAT IS PRECOMPUTED 

All the information precomputed for corpus X is stored in directory cache/X.

This includes 5 files.

* __ENAMEX tags for each document__:
  stored in file documentName.names,
  one name per line, format:  type \t start \t end

* __POS tags for each document__:
  stored in file documentName.pos,
  one token per line, format:  POS \t start \t end
  
* __the extent of each entity mention in a document__:
  stored in file documentName.jetExtent,
  one entity mention per line, format: MENTION_ID \t start \t end
  Note that we will use names (captured by name tagger), nouns,
  and pronouns for bootstrapping, so it is necessary to keep track
  of all name mentions.
  <!--
   ?? have to explain purpose
-->

* __dependency parse of each document__:
  stored in file documentName.dep
  
* __Ace document produced by AceJet__:
  stored in file documentName.ace
  
## WHAT IS COMPUTED BASED ON PRECOMPUTED INFORMATION

The following files are computed based on the precomputed information.
Note that after preprocessing, ICE will try to generate initial versions
of these files. However, unlike precomputed files that never change after
preprocessing, the user can regenerate the following files any time.

* __the count of each possible term in each document__:

* __aggregate term counts over the corpus__:
  stored in file counts

* __dependency paths over the corpus__:

    - file Relations:  dependency paths between entities,
                     including endpoints, with frequency count
                     
    - file RelationRepr: typed dependency paths with linearization
                     and single example
                     
    - file Relationtypes:  typed dependency paths with frequencies, ranked
    
    - file Relationtypes.source.dict:  typed dependency paths with
                     frequency and single example, ranked 
    
<!--
    R:?? why all four
      ?? when are they generated
      
    Y: At first we only have Relations and Relationtypes. Then to add tooltip, we have
    Relationtypes.source.dict. Finally have have RelationRepr that tries to be a one-stop
    shop for all path information. We should try to at least stop generating 
    Relationtypes.source.dict as it is mostly redundant. Relations and Relationtypes makes 
    Bootstrap cleaner, so we probably want to keep them for now.
-->

<!--
?? context vectors for terms updated when terms are updated
-->

## WHAT IS REPRESENTED INTERNALLY (AND WHEN IT IS COMPUTED)

