#!/bin/bash
ICE_HOME=.
ICE_LIB_HOME=.
java -Xmx4g -Dfile.encoding=UTF-8 -cp "$ICE_HOME/ICE-0.2.0-jar-with-dependencies.jar" AceJet.IceTagger $1 $2 $3
