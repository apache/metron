<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
      https://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Apache Metron Bundle Maven Plugin

Apache Metron Bundles Maven Plugin helps to build Bundles Archives to support the classloader isolation model.

## Table of Contents

- [Requirements](#requirements)
- [Building](#building)
- [Getting Started](#getting-started)
- [Quickstart](#quickstart)
- [Settings and configuration](#settings-and-configuration)
- [Getting Help](#getting-help)
- [License](#license)

## Requirements
* JDK 1.7 or higher
* Apache Maven 3.1.0 or higher

## Building 

Building the bundles-maven-plugin module should be rare since it will be released infrequently compared to
the main 'metron' code tree.

- Build with `mvn clean install`
- Presuming you need to make use of changes to the bundles-maven-plugin module, you should next
  go to the [metron](../metron) directory and follow its instructions. 

## Getting Started

While it is most likely
that a maven archetype is being utilized to create bundles, as part of a toolkit etc, you may want to create on manually, or may need to create a project for use in an archetype.

The plugin is utilized by setting the packaging of a maven module to 'bundle'.

```xml
<packaging>bundle</packaging>
```

This means that when you package this module, any of it's non-provided dependencies will be packaged into the produced bundle ( and all of their non-provided dependencies as well).
Since a library may not always be distributed as part of a bundle with all it's dependencies, the bundle module
shall be a separate module from the actual classes and dependencies to be bundled.

A very simple example layout for a project that utilizes bundles would be:

```bash
├── README.md
├── pom.xml
├── testapp
│   ├── pom.xml
│   ├── src
│   │   ├── main
│   │   │   └── java
│   │   │       └── org
│   │   │           └── apache
│   │   │               └── test
│   │   │                   └── App.java
│   │   └── test
│   │       └── java
│   │           └── org
│   │               └── apache
│   │                   └── test
│   │                       └── AppTest.java
└── testappbundle
    ├── pom.xml
```
Where testappbundle is the bundle module that creates a bundle of testapp, and contains the following pom.xml:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <parent>
    <artifactId>test.bundles.plugin</artifactId>
    <groupId>org.apache.test</groupId>
    <version>1.0-SNAPSHOT</version>
  </parent>
  <modelVersion>4.0.0</modelVersion>

  <artifactId>test.app.bundle</artifactId>

  <!-- Packaging is bundle -->
  <packaging>bundle</packaging>
  
  <!-- All dependencies of this module, and all the dependencies of THAT dependency will
  be included in the produced bundle -->
  <dependencies>
    <dependency>
      <groupId>org.apache.test</groupId>
      <artifactId>test.app</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>

  <build>
  <!-- OUR PLUGIN REFERENCES -->
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.metron</groupId>
          <artifactId>bundles-maven-plugin</artifactId>
          <version>0.4.2</version>
          <extensions>true</extensions>
          <configuration>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.metron</groupId>
        <artifactId>bundles-maven-plugin</artifactId>
        <version>0.4.2</version>
        <extensions>true</extensions>
      </plugin>
    </plugins>
  </build>
</project>
```
When the module is packaged, it packages all of it's  non-provided dependencies into the bundles /bundled-dependencies directory.
Thus, to create a bundle of a module's jar and that jar's non-provided dependencies, you add that module to your
bundle modules dependencies.  You can unzip and examine the bundle in the target directory, and verify 
it's contents, which should be similar to :

```bash
-> % tree .
.
└── META-INF
    ├── MANIFEST.MF
    ├── bundled-dependencies
    │   ├── log4j-1.2.17.jar
    │   ├── metron-common-0.4.1.jar
    │   ├── slf4j-api-1.7.7.jar
    │   ├── slf4j-log4j12-1.7.7.jar
    │   └── test.app-1.0-SNAPSHOT.jar
    └── maven
        └── org.apache.test
            └── test.app.bundle
                ├── pom.properties
                └── pom.xml
```

This reflects the testapp project, which has these dependencies :

```xml
<dependencies>
    <dependency>
      <groupId>org.apache.metron</groupId>
      <artifactId>metron-common</artifactId>
      <version>0.4.1</version>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
```
metron-common itself is a shaded jar, but it depends on log4j and slf4j, so those libraries are pulled in..

## Quickstart

* Create a new multi module maven project (if you do not have one already)
* Add a new module for your bundle, it needs only to have a pom.xml
* Create the pom.xml as above, with the correct plugin and packaging entries, and add dependencies
for the module you want to bundle.
* `mvn package`

## Settings and configuration

There are several properties that can be set to change the behavior of this plugin.
Two of special interest are:

#### packageType
packageType defines the type of archive to be produced and evaluated for special dependencies.  The default packageType is bundle.  This property should be changed if you have need to 
customize the file extension of archives produced by this plugin.  This plugin will build archives with .bundle extentions, and look for othe .bundle dependencies by definition.
Changing this value, for example to 'foo', will have the effect of having the plugin produce archives with .foo as the extension, and look for .foo files
as bundle dependencies.
 
#### packageIDPrefix 
The archives produced by this plugin have the following entries added to the manifest ( where packageIDPrefix defaults to 'Bundle'):

-  {packageIDPrefix}-Id
-  {packageIDPrefix}-Group
-  {packageIDPrefix}-Version
-  {packageIDPrefix}-Dependency-Group
-  {packageIDPrefix}-Dependency-Id
-  {packageIDPrefix}-Dependency-Version

This property can be used to change the name of these manifest entries.  One convention being to continue as the original by capitalizing the
type, such as:
Bundle to bundle

 

## Getting Help
If you have questions, you can reach out to our mailing list: dev@metron.apache.org
We're also often available in IRC: #apache-metron on
[irc.freenode.net](https://webchat.freenode.net/?channels=#apache-metron).


## License

Except as otherwise noted this software is licensed under the
[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

