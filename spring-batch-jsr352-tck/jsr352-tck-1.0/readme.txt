Batch Applications for the Java Platform TCK
------------

The Batch Applications for the Java Platform specification (JSR-352) describes the job specification language,
Java programming model, and runtime  environment for batch applications for the Java platform.

This is the TCK for JSR-352.

This distribution, as a whole, is licensed under the terms of the Apache Public License (see LICENSE.TXT).


This distribution consists of:

artifacts/
   -- TCK binaries and source, packaged as jars
   -- TestNG suite.xml file for running the TCK

doc/
   -- Reference guide for the TCK

lib/
   -- Dependencies for running the TCK
   
build.xml
   -- Ant build file used to run (and optionally build from source) the TCK

jsr352-tck.properties
   -- Specify the location of required properties here