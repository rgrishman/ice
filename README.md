# ATTENTION: This is a pre-release version of Ice. Use at your own risk.

Licensed under the Apache 2.0 license.

#Building Ice

##Using git

A [quick guide](http://rogerdudler.github.io/git-guide/).

##Installing Maven

[maven by example](http://books.sonatype.com/mvnex-book/reference/public-book.html)

maven should come preinstalled on OS X, unless you run a version
later than 10.9 Mavericks.

run `mvn --version` to find if maven is installed, if it is not, you can
find tarballs and install instructions here:
<http://maven.apache.org/download.cgi>

We recommend version 3.0.5, or the latest 3.0.x version

## Build

Please run:

	mvn package

If everything works, you should find
ICE-0.2.0-jar-with-dependencies.jar (the fatjar), and ICE-0.2.0.jar in
target/

# Running Ice

## Preparing models

The current version of Ice assumes that it is run from a "working directory", where three Jet property files are located: *props*, *parseprops*, and *onomaprops*. These three files tell Ice where models for Jet are located. These two files are released together with the Java source code: `src/props`.

In theory, Jet model files can sit anywhere. However, to use the property files directly, you can copy `data/`, `acedata/` and `kddData/` directories from Jet into the working directory.

In addition, Ice itself uses two configuration files: *ice.yml* and *iceprops*, which should be put in the working directory as well. 

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

## Running Ice (Built from source)

The easiest way to run Ice is to run from the working directory we prepared in the previous section.

Copy *src/scripts/runice.sh* to the working directory. In *runice.sh*, point $ICE\_HOME to 
the directory containing ICE-0.2.0-jar-with-dependencies.jar (target/), and
$ICE\_LIB\_HOME to the directory containing Jet-1.8.0.11-ICE-jar-with-dependencies.jar (lib/).

Then, from the working directory, run

    ./runice.sh
    
## Running Ice (Binary release)

Alternatively, download the binary distribution and unzip it. In *runice.sh*, point both $ICE\_HOME
and $ICE\_LIB\_HOME to directory of the binary distribution. Both variables are set to . by default.

Then, from the working directory, run

    ./runice.sh

## Using Ice

Please refer to docs/iceman.html for usage.