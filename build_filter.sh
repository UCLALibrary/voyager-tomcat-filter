#!/bin/sh

JAVABIN=/m1/shared/java/jdk1.7.0_03/bin

if [ ! -d classes ]; then
  mkdir classes
fi

$JAVABIN/javac \
  -cp .:/m1/shared/Tomcat/7.0.26/lib/servlet-api.jar \
  -d classes \
  src/edu/ucla/library/libservices/tomcat/filters/BbidHarvestFilter.java

JARFILE=ucla-filters.jar
TARGET=/m1/voyager/ucladb/tomcat/vwebv/context/vwebv/WEB-INF/lib/$JARFILE

rm $JARFILE
$JAVABIN/jar cvf $JARFILE -C classes edu

if [ -s $JARFILE ]; then
  cp $JARFILE $TARGET
  ls -l $TARGET
fi

