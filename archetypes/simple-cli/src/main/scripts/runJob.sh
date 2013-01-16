#!/bin/bash

if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME environment variable is not set."
  exit 1
fi

$JAVA_HOME/bin/java -cp resources/:lib/* org.springframework.batch.core.launch.support.CommandLineJobRunner classpath:/launch-context.xml job1
