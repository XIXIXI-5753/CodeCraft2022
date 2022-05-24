#!/bin/bash
basepath=$(cd `dirname $0`; pwd)
APP_HOME=$basepath

#编译
echo building...
MAKE_FILE=$APP_HOME/CodeCraft-2022/makelist.txt
libs=-Djava.ext.dirs=$APP_HOME/CodeCraft-2022/lib

if [ ! -d $APP_HOME/CodeCraft-2022/src ]
then
    echo "ERROR: can not find $APP_HOME/CodeCraft-2022 directory."
    exit -1
fi

cd "$APP_HOME/CodeCraft-2022/src"
mkdir -p "$APP_HOME/CodeCraft-2022/bin"
javac -source 1.8 -target 1.8 -d $APP_HOME/CodeCraft-2022/bin -encoding UTF-8 $libs @$MAKE_FILE
tmp=$?
if [ ${tmp} -ne 0 ]
then
 echo "ERROR: javac failed:" ${tmp}
 exit -1
fi



#打包
mkdir -p $APP_HOME/bin
echo make jar...
cd "$APP_HOME/CodeCraft-2022/bin"
JAR_NAME=$APP_HOME/bin/CodeCraft-2022.jar
jar -cvf $JAR_NAME *
tmp=$?
if [ ${tmp} -ne 0 ]
then
 echo "ERROR: jar failed:" ${tmp}
 exit -1
fi

cd $APP_HOME


if [ -f CodeCraft-2022.zip ]
then
    rm -f CodeCraft-2022.zip
fi

echo build jar success!
exit
