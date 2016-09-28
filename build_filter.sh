#!/bin/sh

# One required argument: voyager db directory (e.g., /m1/voyager/yourdb)
if [ -n "$1" ]; then
  YOURDB=$1
else
  echo "Usage: $0 your_db_name"
  echo "e.g., for /m1/voyager/yourdb, enter yourdb"
  exit 1
fi

if [ ! -d /m1/voyager/${YOURDB} ]; then
  echo "ERROR: /m1/voyager/${YOURDB} does not exist - exiting"
  exit 1
fi

JAVABIN=/m1/voyager/${YOURDB}/tomcat/java/bin
TOMCAT=/m1/voyager/${YOURDB}/tomcat/catalina_home

if [ ! -d classes ]; then
  mkdir classes
fi

# Experiment
SOURCEDIR=src/edu/ucla/library/libservices/tomcat/filters
# Create another filter - same code except for class name, but will have different config in web.xml
sed 's/BbidHarvestFilter/GeneralSearchFilter/g' ${SOURCEDIR}/BbidHarvestFilter.java > ${SOURCEDIR}/GeneralSearchFilter.java

$JAVABIN/javac \
  -cp .:${TOMCAT}/lib/servlet-api.jar \
  -d classes \
  ${SOURCEDIR}/*.java

JARFILE=ucla-filters.jar
TARGET=/m1/voyager/${YOURDB}/tomcat/vwebv/context/vwebv/WEB-INF/lib/$JARFILE

if [ -f ${JARFILE} ]; then
  rm $JARFILE 
fi

$JAVABIN/jar cvf $JARFILE -C classes edu

if [ -s $JARFILE ]; then
  cp $JARFILE $TARGET
  ls -l $TARGET
fi

