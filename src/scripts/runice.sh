#!/bin/bash
ICE_HOME=.
ICE_LIB_HOME=.
java -Xmx4g -Dfile.encoding=UTF-8 -cp "$ICE_HOME/ice-all.jar" -DjetHome=$JET_HOME edu.nyu.jet.ice.controllers.Nice
