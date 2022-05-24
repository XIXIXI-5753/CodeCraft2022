#!/bin/bash

SCRIPT=$(readlink -f "$0")
BASEDIR=$(dirname "$SCRIPT")
cd $BASEDIR

sh build.sh

java -Djava.library.path=./bin -classpath ./bin/CodeCraft-2022.jar com.huawei.java.main.Main 
