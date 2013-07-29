@echo off

IF NOT DEFINED JAVA_HOME (
    echo Error: JAVA_HOME environment variable is not set.
    EXIT /B
) 

%JAVA_HOME%\bin\java -cp resources\;lib\* org.springframework.batch.core.launch.support.CommandLineJobRunner example.LaunchContext personJob
