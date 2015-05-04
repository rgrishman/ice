#!/bin/bash
ICE_HOME=~/code/ICE/target
ICE_LIB_HOME=~/code/ICE/lib
java -Xmx4g -cp "$ICE_HOME/ICE-0.2.0-jar-with-dependencies.jar:$ICE_LIB_HOME/Jet-1.8.0.11-ICE-jar-with-dependencies.jar" edu.nyu.jet.ice.controllers.Nice
