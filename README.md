# ICE: Integrated Customization Environment for Information Extraction

Licensed under the Apache 2.0 license.

# Running Ice Using Binary Release

Alternatively, download the binary distribution and unzip it. In *runice.sh*, point both $ICE\_HOME
and $ICE\_LIB\_HOME to directory of the binary distribution. Both variables are set to . by default.

Then, from the working directory, run

    ./runice.sh
    
# Running the Ice Tagger

Ice bundles a relation tagger based on Jet, which tags mentions of relations in text files, using
the models that you build with Ice. Note that before the Ice tagger can find the relations,
you have to use *Export* in Ice to export them to the underlying Jet tagger.

To run the tagger, from the working directory, run

    ./runtagger.sh propertyFile txtFileList apfFileList
    
where propertyFile is the Jet properties file, usually it is parseprops; txtFileList are
the list of text input files, and apfFileList is the list of output files in Ace apf
format.

#Building and Running Ice from Source

We assume that you have git and maven installed on your system.

## Build

Please run:

	mvn package

If everything works, you should find
ICE-0.2.0-jar-with-dependencies.jar (the fatjar), and ICE-0.2.0.jar in
target/

## Preparing models

Ice relies on Jet and its models. We provide the Jet binary and necessary models in the
binary distribution. However, if you are building from source, you might also want to
obtain Jet from: <http://cs.nyu.edu/grishman/jet/jet.html>

The current version of Ice assumes that it is run from a "working directory", where three 
Jet property files are located: *props*, *parseprops*, and *onomaprops*. These three files 
tell Ice where models for Jet are located. These files are released together with the 
Java source code in the `src/props` directory.

In theory, Jet model files can sit anywhere. However, to use the property files directly, 
you can copy `data/` and `acedata/` directories from Jet into the working directory.

In addition, Ice itself uses two configuration files: *ice.yml* and *iceprops*, which should be 
put in the working directory as well. 

After these steps, the working directory we have prepared will look like this:

    working_dir/
        props - Jet property file 1
        parseprops - Jet property file 2
        onomaprops - Jet property file 3
        ice.yml - Ice configuration file 1
        iceprops - Ice configuration file 2
        data/ - model files, including parseModel.gz
        acedata/ - model files

With these files, we should be ready to go. 

## Starting the GUI

The easiest way to run Ice is to run from the working directory we prepared in the previous section.

Copy *src/scripts/runice.sh* to the working directory. In *runice.sh*, point $ICE\_HOME to 
the directory containing ICE-0.2.0-jar-with-dependencies.jar (target/), and
$ICE\_LIB\_HOME to the directory containing Jet-1.8.0.11-ICE-jar-with-dependencies.jar (lib/).

Then, from the working directory, run

    ./runice.sh

# User Manual

Please refer to [Iceman](docs/iceman.md) for usage.

