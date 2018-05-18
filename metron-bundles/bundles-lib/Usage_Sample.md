<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Sample Usage

The basic use case for any system using the bundle system is loading a bundle as an extension or plugin
that exposes implementations of an interface, and using those instances.

In our example we have an interface `TestInterface`, an will have an integration test that loads and
executes an implementation of that interface from a bundle.

> This document assumes that you have built and installed the bundles-maven-plugin and the bundles lib
> as described in [bundles-lib README](README.md) and [bundles-maven-plugin README](../bundles-maven-plugin/README.md)

What we will set out to do is create a multi-module maven project with the following modules:

- simple-interfaces : This module will define the `TestInterface`
- simple-implementation : This module will define an implementation of `TestInterface` called `TestImplementation`
- simple-bundle : This module will create a bundle of the simple-implementation jar
- simple-integration : This module will use the `BundleSystem` class to load an instance of `TestImplementation` as `TestInterface` and execute it



## Creating the root directory

For our example, I will be using ~/tmp as the location of the project.

1. Create a directory that will hold out project

```bash
-> % mkdir simple-bundle-enabled && cd simple-bundle-enabled
```

`simple-bundle-enabled` will be the top level module directory

2.  Create the root pom.xml

Create a new file called pom.xml in that directory with the following contents ( commented for explanation)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.test</groupId>
  <artifactId>simple-bundle-enabled</artifactId>
  <packaging>pom</packaging>
  <name>simple-bundle-enabled</name>
  <version>1.0.0</version>
  <!-- our modules, commented out for now -->
  <modules>
    <!-- The Interface
    <module>simple-interfaces</module>
    -->
    <!-- The Implementation
    <module>simple-implementation</module>
    -->
    <!-- The Bundle
    <module>simple-bundle</module>
    -->
    <!-- The Integration Test
    <module>simple-integration</module>
    -->
  </modules>
  <build>
    <pluginManagement>
      <plugins>
        <!-- The plugin for building a bundle -->
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
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.5.1</version>
        <configuration>
           <compilerId>javac</compilerId>
           <forceJavacCompilerUse>true</forceJavacCompilerUse>
           <source>1.8</source>
           <compilerArgument>-Xlint:unchecked</compilerArgument>
           <target>1.8</target>
           <showWarnings>true</showWarnings>
           </configuration>
      </plugin>
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

## Creating the simple-interfaces module

We will use the [maven quickstart archetype](http://maven.apache.org/archetypes/maven-archetype-quickstart/) to create a project for the interfaces.
In the simple-bundle-enabled directory run the following command:

```bash
-> % mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.1
```

Then create in interactive mode as follows:

```bash
[INFO] Archetype repository not defined. Using the one from [org.apache.maven.archetypes:maven-archetype-quickstart:1.1] found in catalog remote
Define value for property 'groupId': org.test
Define value for property 'artifactId': simple-interfaces
Define value for property 'version' 1.0-SNAPSHOT: : 1.0.0
Define value for property 'package' org.test: : org.test.interfaces
```

After doing this we will have the following:

```bash
-> % tree .
.
├── pom.xml
└── simple-interfaces
    ├── pom.xml
    └── src
        ├── main
        │   └── java
        │       └── org
        │           └── test
        │               └── interfaces
        │                   └── App.java
        └── test
            └── java
                └── org
                    └── test
                        └── interfaces
                            └── AppTest.java
```
### Creating the TestInterface

1. delete the test directory

```bash
cd simple-interfaces && rm -rd src/test
```
2. add a dependency for the ClassIndex jar
add the following dependency to the simple-interfaces/pom.xml
```xml
  <dependency>
      <groupId>org.atteo.classindex</groupId>
      <artifactId>classindex</artifactId>
      <version>3.3</version>
  </dependency>
```

ClassIndex is used to mark classes and subclasses with attributes that result in the generation of
the service discovery jar meta info at build time.  It also is used for service discovery from jars and
classloaders.  Using ClassIndex increases speed for this type of operation over the built in java service
provider system.  It also removes the necessity to manually manage the service entries.

3. rename App.java to TestInterface.java

```bash
mv src/main/java/org/test/interfaces/App.java src/main/java/org/test/interfaces/TestInterface.java
```

4. edit TestInterface.java such that it has the following contents

```java
package org.test.interfaces;

import org.atteo.classindex.IndexSubclasses;

// the IndexSubclasses attribute marks this interface for the ClassIndex system.
// any class that implements this interface will be registered it it's jar's
// META-INF/services/org.test.interfaces.TestInterface file for service discovery.
// This happens at build time automatically
// If the implementation is not in this module, then the implementation module must also
// have a dependency on ClassIndex
@IndexSubclasses
public interface TestInterface {
  String getFoo();
  String getBar();
  String getBaz();
}

```

We now have our interface module set.

## Creating the simple-implemenation module
from the top level project directory:
We will use the [maven quickstart archetype](http://maven.apache.org/archetypes/maven-archetype-quickstart/) to create a project for the interfaces.
In the simple-bundle-enabled directory run the following command:

```bash
-> % mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.1
```

Then create in interactive mode as follows:

```bash
[INFO] Archetype repository not defined. Using the one from [org.apache.maven.archetypes:maven-archetype-quickstart:1.1] found in catalog remote
Define value for property 'groupId': org.test
Define value for property 'artifactId': simple-implementation
Define value for property 'version' 1.0-SNAPSHOT: : 1.0.0
Define value for property 'package' org.test: : org.test.implementation
```

After doing this we will have the following:

```bash
-> % tree .
.
├── pom.xml
├── simple-implementation
│   ├── pom.xml
│   └── src
│       ├── main
│       │   └── java
│       │       └── org
│       │           └── test
│       │               └── implementation
│       │                   └── App.java
│       └── test
│           └── java
│               └── org
│                   └── test
│                       └── implementation
│                           └── AppTest.java
└── simple-interfaces
    ├── pom.xml
    └── src
        └── main
            └── java
                └── org
                    └── test
                        └── interfaces
                            └── TestInterface.java
```

1. delete the test directory

```bash
cd simple-implementation && rm -rd src/test
```

2. add a dependency for the ClassIndex jar
add the following dependencies to the simple-interfaces/pom.xml
```xml
 <dependency>
      <groupId>org.atteo.classindex</groupId>
      <artifactId>classindex</artifactId>
      <version>3.3</version>
      <scope>provided</scope>
 </dependency>
 <dependency>
       <groupId>org.test</groupId>
       <artifactId>simple-interfaces</artifactId>
       <version>1.0.0</version>
       <scope>provided</scope>
 </dependency>
```

ClassIndex is used to mark classes and subclasses with attributes that result in the generation of
the service discovery jar meta info at build time.  It also is used for service discovery from jars and
classloaders.  Using ClassIndex increases speed for this type of operation over the built in java service
provider system.  It also removes the necessity to manually manage the service entries.

3. rename App.java to TestInterface.java

```bash
mv src/main/java/org/test/implementation/App.java src/main/java/org/test/implementation/TestImplementation.java
```

4. edit TestImplementation.java such that it has the following contents

```java
package org.test.implemenation;

import org.test.interfaces.TestInterface;

// TestInterface is marked with the ClassIndex attribute
// @IndexSubclasses, so this class will automatically be registered
// as a TestInterface service in the jar when built
public class TestImplementation implements TestInterface {

  @Override
  public String getFoo() {
    return "Foo";
  }

  @Override
  public String getBar() {
    return "Bar";
  }

  @Override
  public String getBaz() {
    return "Baz";
  }
}

```

We now have out implementation module.


## Create the bundle module
We have an interface and an implementation.  What we want is to create a bundle for *this* implemenation.

From the top level project directory:

1. create the module directory

```bash
mkdir simple-bundle
```

2. create the pom file simple-bundle/pom.xml with the following contents

```xml
<?xml version="1.0"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.test</groupId>
    <artifactId>simple-bundle-enabled</artifactId>
    <version>1.0.0</version>
  </parent>
  <groupId>org.test</groupId>
  <artifactId>simple-bundle</artifactId>
  <version>1.0.0</version>
  <name>simple-bundle</name>
  <!-- The packaging is bundle, this will bundle of bundle of any dependencies, and any dependencies
   of those dependencies -->
  <packaging>bundle</packaging>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencies>
    <!-- include a dependency on our implementation module -->
    <dependency>
      <groupId>org.test</groupId>
      <artifactId>simple-implementation</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</project>

```

## Test the build

In the top level pom.xml, we should now have uncommented module definitions for simple-interfaces and simple-implemenation.
Edit the module definition such that it is as follows:

```xml
  <modules>
    <module>simple-interfaces</module>
    <module>simple-implementation</module>
    <module>simple-bundle</module>
  </modules>
```

In the top level directory run:

```bash
mvn package
```

And you should see the following :
```bash
Reactor Summary:
[INFO]
[INFO] simple-bundle-enabled .............................. SUCCESS [  0.043 s]
[INFO] simple-interfaces .................................. SUCCESS [  0.740 s]
[INFO] simple-implementation .............................. SUCCESS [  0.111 s]
[INFO] simple-bundle ...................................... SUCCESS [  0.411 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

The bundle will be created in the simple-bundle/target directory as `simple-bundle-1.0.0.bundle`

## Exporing the bundle
- Move to the simple-bundle target directory
- Create a temp directory
- Copy the bundle into it
- unzip the bundle

```bash
-> % cd simple-bundle/target
...
-> % mkdir temp
...
-> % cp simple-bundle-1.0.0.bundle temp
...
-> % cd temp
...
-> % unzip simple-bundle-1.0.0.bundle
```

We will see that the bundle has the following structure:

```bash
-> % tree .
.
└── META-INF
    ├── MANIFEST.MF
    ├── bundled-dependencies
    │   └── simple-implementation-1.0.0.jar
    └── maven
        └── org.test
            └── simple-bundle
                ├── pom.properties
                └── pom.xml
```


## Create the integration module
from the top level project directory:

We will use the [maven quickstart archetype](http://maven.apache.org/archetypes/maven-archetype-quickstart/) to create a project for the interfaces.
In the simple-bundle-enabled directory run the following command:

```bash
-> % mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.1
```

Then create in interactive mode as follows:

```bash
[INFO] Archetype repository not defined. Using the one from [org.apache.maven.archetypes:maven-archetype-quickstart:1.1] found in catalog remote
Define value for property 'groupId': org.test
Define value for property 'artifactId': simple-integration
Define value for property 'version' 1.0-SNAPSHOT: : 1.0.0
Define value for property 'package' org.test: : org.test
```

1. delete the main directory

```bash
cd simple-integration && rm -rd src/main
```

2. add a dependency for the simple-interfaces jar, junit and bundles-lib
add the following dependencies to the simple-integration/pom.xml
```xml
 <dependency>
   <groupId>org.test</groupId>
   <artifactId>simple-interfaces</artifactId>
   <version>1.0.0</version>
 </dependency>
 <dependency>
   <groupId>junit</groupId>
   <artifactId>junit</artifactId>
   <version>4.12</version>
   <scope>test</scope>
 </dependency>
 <dependency>
   <groupId>org.apache.metron</groupId>
   <artifactId>bundles-lib</artifactId>
   <version>0.4.2</version>
 </dependency>
```
3. create the test/resources directory

```bash
-> % mkdir test/resources
```

4. Copy the bundle to the test/resources directory when building
We want to load the bundle from the test/resource directory.  In order to make it available, we need
to create a copy resources entry in our pom.xml file.

Edit the simple-integration/pom.xml and:

- add a parent relative path
  ```xml
  <parent>
    <groupId>org.test</groupId>
    <artifactId>simple-bundle-enabled</artifactId>
    <version>1.0.0</version>
    <relativePath>..</relativePath>
  </parent>
  ```
- add a property for the bundle source
  ```xml
  <properties>
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      <bundle_dir>${project.parent.relativePath}/simple-bundle/target</bundle_dir>
  </properties>
  ```
- add a build entry for copying resources

```xml

<build>
    <plugins>
      <plugin>
        <artifactId>maven-resources-plugin</artifactId>
        <version>3.0.1</version>
        <executions>
          <execution>
            <id>copy-bundles</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.basedir}/src/test/resources</outputDirectory>
              <resources>
                <resource>
                  <directory>${bundle_dir}/</directory>
                  <includes>
                    <include>*.bundle</include>
                  </includes>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

- build the top level project and verify
From the top level project directory:

```bash
-> % mvn clean package
```
Everything should build, and the simple-integration/test directory should be:

```bash
-> % tree simple-integration
simple-integration
├── pom.xml
├── src
│   └── test
│       ├── java
│       │   └── org
│       │       └── test
│       │           └── AppTest.java
│       └── resources
│           └── simple-bundle-1.0.0.bundle
```
5. Create the bundle.properties file.
The bundle system uses a bundle.properties file to configure the system.  We will create this file
in the test/resources directory.

Create a file called bundle.properties in the test/resources directory with the following content

```properties
# this is the directory that the system will look for bundles in
bundle.library.directory=./src/test/resources/
# the extension of bundles is actually configurable, we use bundle
bundle.archive.extension=bundle
# the metatdata prefix is also configurable, we use Bundle
bundle.meta.id.prefix=Bundle
# extension types define the type names and interfaces/classes that the bundle
# system will 'know' about and load
# here we are defining a type named 'test', which will be assignable from TestInterface
bundle.extension.type.test=org.test.interfaces.TestInterface
```
6. edit the AppTest.java file and replace it's contents with the following content

```java 
package org.test;

import org.apache.metron.bundles.BundleSystem;
import org.apache.metron.bundles.BundleSystemBuilder;
import org.apache.metron.bundles.util.BundleProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// test that we can load a bundle and execute methods on the class
// note that we are not passing the class to the BundleSystemBuilder, but rather
// it is read from the properties
public class AppTest {

  // it is best practices in tests that use the BundleSystem to reset it
  // before and after, since the maven execution context is not known
  @Before
  public void before() {
    BundleSystem.reset();
  }

  @After
  public void after() {
    BundleSystem.reset();
  }

  @Test
  public void test() throws Exception {

    // Load the bundle properties
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("src/test/resources/bundle.properties", null);

    // build the BundleSystem from the properties.  BundleSystem is a simple interface
    // over the bundles-lib classes.
    BundleSystem bundleSystem = new BundleSystemBuilder().withBundleProperties(properties)
        .build();

    // create an instance by the name of the type
    TestInterface result = bundleSystem.createInstance("org.test.implementation.TestImplementation",
        TestInterface.class);

    // call the methods
    Assert.assertEquals("Foo", result.getFoo());
    Assert.assertEquals("Bar", result.getBar());
    Assert.assertEquals("Baz", result.getBaz());

  }
}

```
7. Ensure the top level pom includes the new integration module:
The top level project pom.xml's modules should look like this:
```xml
<modules>
  <module>simple-interfaces</module>
  <module>simple-implementation</module>
  <module>simple-bundle</module>
  <module>simple-integration</module>
</modules>
```
8. Run the tests!

From the top level project directory :

```bash
mvn clean package
```


You can play with the system by using your own interfaces and implementations

