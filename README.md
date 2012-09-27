Oozie Web
======

A pretty, more usable dashboard for Apache Oozie built with Scala, Scalatra, and Twitter Bootstrap


BASIC OVERVIEW
------
code:           src/main/scala

web resources:  src/main/webapp

config:         src/main/resources

GETTING STARTED
------
1. cp src/main/resources/oozie.properties.example src/main/resources/oozie.properties
2. edit oozie.properties with your oozieUrl of choice
3. ./sbt
4. sbt> container:start
5. enjoy the view at localhost:8080

OOZIE VERSIONS
------
This is built and tested against oozie 2.3.2 from Cloudera's CDH3u3 hadoop distribution.

It has not been tested against oozie 3, nor are there any oozie3 features represented currently.

DEPLOYMENT
------
We use a simple deployment method which runs JettyLauncher#main

To build the jar:

./sbt assembly

To Run the jar:

java jar target/oozie-web.jar 8080

This requires the files in src/main/webapp to still be on the filesystem 
for the jar to find, which isn't ideal, but works for now. 

Ideally it would look inside the jar for the files


SCREENSHOTS
------

![Screenshot 1](http://github.com/foursquare/oozie-web/raw/master/project/screenshots/screenshot1.png)

![Screenshot 2](http://github.com/foursquare/oozie-web/raw/master/project/screenshots/screenshot2.png)


LICENSE
------
[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)