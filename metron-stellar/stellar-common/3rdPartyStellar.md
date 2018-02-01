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
# Custom Stellar Functions

Metron is fundamentally a programmable, extensible system
and Stellar is the extension language.  We have some great Stellar functions
available out of the box and we'll be adding more over time, but they may
not quite scratch quite your particular itch.  

Of course, we'd love to have your contribution inside of Metron if you think it
general purpose enough, but not every function is general-purpose or it may rely
on libraries those licenses aren't acceptable for an Apache project.  In that case, then you will
be wondering how to add your custom function to a running instance of Metron.

## Building Your Own Function

Let's say that I need a function that returns the current time in milliseconds
since the epoch.  I notice that there's nothing like that currently in Metron,
so I embark on the adventure of adding it for my cluster.

I will presume that you have an installed Metron into your local maven repo via `mvn install` .  In the future, when we publish to a maven repo,
you will not need this.  I will depend on 0.4.2 for the
purpose of this demonstration

### Hack, Hack, Hack

I like to use Maven, so we'll use that for this demonstration, but you can use whatever
build system that you like.  Here's my favorite way to build a project with groupId `com.mycompany.stellar`
and artifactId of `tempus`
`mvn archetype:create -DgroupId=com.mycompany.stellar -DartifactId=tempus -DarchetypeArtifactId=maven-archetype-quickstart`

First, we should depend on `metron-common` and we can do that by adjusting the `pom.xml` just created:
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  
  <groupId>com.mycompany.stellar</groupId>
  <artifactId>tempus</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  
  <name>Stellar Time Functions</name>
  <url>http://mycompany.com</url>
  
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  
  <dependencies>
    <dependency>
      <groupId>org.apache.metron</groupId>
      <artifactId>metron-common</artifactId>
      <version>0.4.2</version>
      <!-- NOTE: We will want to depend on the deployed common on the classpath. -->
      <scope>provided</scope>
    </dependency>
    <dependency>
       <groupId>junit</groupId>
       <artifactId>junit</artifactId>
       <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Let's add our implementation in `src/main/java/com/mycompany/stellar/TimeFunctions.java` with the following content:
```
package com.notmetron.stellar;
    
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
    
import java.util.List;
    
public class TimeFunction {
  @Stellar( name="NOW",
            description = "Right now!",
            params = {},
            returns="Timestamp"
          )
  public static class Now implements StellarFunction {
    
    public Object apply(List<Object> list, Context context) throws ParseException {
      return System.currentTimeMillis();
    }
    
    public void initialize(Context context) { }
    
    public boolean isInitialized() {
      return true;
    }
  }
}
```

Now we can build the project via `mvn package` which will create a `target/tempus-1.0-SNAPSHOT.jar` file.

## Install the Function

Now that we have a jar with our custom function, we must make Metron aware of it.

### Deploy the Jar

First you need to place the jar in HDFS, if we have it on an access node, one way to do that is:
* `hadoop fs -put tempus-1.0-SNAPSHOT.jar /apps/metron/stellar`
This presumes that:
* you've standardized on `/apps/metron/stellar` as the location for custom jars
* you are running the command from an access node with the `hadoop` command installed
* you are running from a user that has write access to `/apps/metron/stellar`

### Set Global Config

You may not need this if your Metron administrator already has this setup.

With that dispensed with, we need to ensure that Metron knows to look at that location.
We need to ensure that the `stellar.function.paths` property in the `global.json` is in place that makes Metron aware
to look for Stellar functions in `/apps/metron/stellar` on HDFS.  

This property looks like, the following for a vagrant install
```
{
  "es.clustername": "metron",
  "es.ip": "node1",
  "es.port": "9300",
  "es.date.format": "yyyy.MM.dd.HH",
  "stellar.function.paths" : "hdfs://node1:8020/apps/metron/stellar/.*.jar",
}
```

The `stellar.function.paths` property takes a comma separated list of URIs or URIs with regex expressions at the end.
Also, note path is prefaced by the HDFS default name, which, if you do not know, can be found by executing,
`hdfs getconf -confKey fs.default.name`, such as
```
[root@node1 ~]# hdfs getconf -confKey fs.default.name
hdfs://node1:8020
```
### Use the Function

Now that we have deployed the function, if we want to use it,
any running topologies that use Stellar will need to be restarted.

Beyond that, let's take a look at it in the REPL:
```
Stellar, Go!
Please note that functions are loading lazily in the background and will be unavailable until loaded fully.
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH, stellar.function.paths=hdfs://node1:8020/apps/metron/stellar/.*.jar, profiler.client.period.duration=1, profiler.client.period.duration.units=MINUTES}
[Stellar]>>> # Get the help for NOW
[Stellar]>>> ?NOW
Functions loaded, you may refer to functions now...
NOW
Description: Right now!
     
Returns: Timestamp
[Stellar]>>> # Try to run the NOW function, which we added:
[Stellar]>>> NOW()
1488400515655
[Stellar]>>> # Looks like I got a timestamp, success!
```
