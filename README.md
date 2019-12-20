# ICE: Integrated Customization Environment for Information Extraction

Licensed under the Apache 2.0 license.

# Information Extraction

Information extraction is the process of identifying in text all the instances of
specified types of entities, relations, and events.  Building an extraction
system for a new domain involves a substantial effort in text analysis and design.
ICE, the Integrated Customization Environment for Information Extraction,
is designed to ease this task by providing an integrated set of analysis tools. 
ICE is built on top of JET, NYU's Java Extraction Toolkit.

# Running Ice Using Binary Release
 
JET and ICE are avaiable as github repositories (rgrishman/jet and rgrishman/ice) 
and as binary distribution tar files.  To use the binary distributions, simply 
download JET and ICE to separate directories and untar them.  Set the environment variables
ICE_HOME and JET_HOME to point to the root directories of the distributions and put the
bin directory for ICE on the path.

Then ICE can be invoked with

    runice.sh
   
Note that ICE requires that you add two corpora to ICE before it will
let you do anything else.

ICE requires quite a few files, listed in [Files](docs/Files.txt), all accessed
through the ICE_HOME and JET_HOME shell variables.  These files should all be set
up by the binary distibution.  

# Running the Ice Tagger

Using ICE you can build up a set of patterns to capture the information you
want to extract from the text.  For example, if you want to extract data
on the employment of corporate executives, you might have patterns such
as *person* joins *company*  and *company* promoted *person*. After you
have accumulated an initial set of patterns, you can *export* them to JET.
You can then use the JET tagger to extract comparable information from new,
previously unseen text.

To run the tagger, use the command

    runtagger.sh props txtFileList keyFileList apfFileList
    
where *props* is a JET properties file provided as part of the ICE dstribution; 
*txtFileList* is the list of text input files (one per line)
*keyFileList* is the corresponding list of keys,
and *apfFileList* is the list of output files in Ace apf format.

The tagger uses 'perfect entities', which are obtained from the key files, and
extracts relations based on the patterns exported from ICE.

# Building Ice from Source Using ant

We assume that you have git and ant installed on your system.

ICE uses JET to do much of the low-level linguistic processing, and so a copy of JET
is compiled into ICE.  This necessitates a 2-step process whenever JET is updated:
first rebuild JET, then build ICE.

## Building JET

Create an empty directory called *export* under the JET_HOME directory.
Get a copy of *jet-release-script* from the JET git repository and run it.  It will
produce a JET binary distribution (a tar file named jet-all.jar).

## Building ICE

Create another empty directory called *export* under the ICE_HOME directory.
Get a copy pf ice-release-script from the ICE  git repository and run it.  It will
produce an ICE binary distribution (a tar file named ice-all.jar).

# Running maven

Fo those who prefer *maven*, we also provide the necessary *pom.xml* files.  These
build and install JET in the local repository and then build ICE.  
Maven can be invoked with

	mvn package

If everything works, you should find
ICE-0.2.0-jar-with-dependencies.jar (the fatjar) in target/  This
should be renamed ice-all.jar and moved to the ICE_HOME directory.

# User Manual

Please refer to [Iceman](docs/iceman.md) for usage of ICE..
