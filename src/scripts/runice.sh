#!/bin/bash
java -Xmx4g -Dfile.encoding=UTF-8 -cp "$ICE_HOME/ice-all.jar" -DjetHome=$JET_HOME -DiceHome=$ICE_HOME edu.nyu.jet.ice.controllers.Nice
