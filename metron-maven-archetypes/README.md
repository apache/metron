# Metron Maven Archetypes

These are [Apache Maven Archetypes](http://maven.apache.org/archetype/index.html) for use in creating extension components for the Apache Metron system
Archetypes are helpful in creating maven based projects with the correct setup and dependency configurations for the target system, including providing sample implementations.

### metron-maven-parser-extension-archetype
This is an archetype for creating an Apache Metron Parser Extension

#### Use

Build and install the archetype
```
$mvn install
```

Create a directory to host your extension code

```
$ mkdir ~/src/my-parser-extension
$ cd ~/src/my-parser-extension
```

Use the archetype to create your project

```
$mvn archetype:generate -DarchetypeCatalog=local
[Select the org.apache.metron:metron-maven-parser-extension-archetype (Apache Maven Parser Extension Archetype for Metron) entry]
```

Configure the project properties.  Ending up with something like this:

Confirm properties configuration:
groupId: org.someorg
artifactId: someparser
version: 1.0-SNAPSHOT
package: org.someorg.parsers
metronVersion: 0.4.0
parserClassName: Nice
parserName: nice

This will produce a project:

![Project](project.png)


#### Project description

#####metron-parser-nice ( the Parser Project)
This project contains the sample parser code, configuration, and tests

#####metron-parser-nice-bundle ( the Bundle Project )
The project contains builds the Bundle file

#####metron-parser-nice-assembly ( the Assembly Project)
The project that builds a tar.gz assembly of the bundle and configuration.
This is the final, installable product.
