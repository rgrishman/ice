#!/bin/bash
ICE_HOME=.
ICE_LIB_HOME=.
java -Xmx4g -Dfile.encoding=UTF-8 -cp "$ICE_HOME/ice-all.jar" AceJet.IceTagger $1 $2 $3
