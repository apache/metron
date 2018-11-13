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

# Custom Metron Parsers

We have many stock parsers for normal operations.  Some of these are
networking and cybersecurity focused (e.g. the ASA Parser), some of
these are general purpose (e.g. the CSVParser), but inevitably users
will want to extend the system to process their own data formats.  To
enable this, this is a walkthrough of how to create and use a custom
parser within Metron.

# Writing A Custom Parser
Before we can use a parser, we will need to create a custom parser.  The
parser is the workhorse of Metron ingest.  It provides the mapping
between the raw data coming in via the Kafka value and a `JSONObject`,
the internal data structure provided.

## Implementation

In order to do create a custom parser, we need to do one of the following:
* Write a class which conforms to the `org.apache.metron.parsers.interfaces.MessageParser<JSONObject>` and `java.util.Serializable` interfaces
  * Implement `init()`, `validate(JSONObject message)`, and `List<JSONObject> parse(byte[] rawMessage)`
* Write a class which extends `org.apache.metron.parsers.BasicParser`
  * Provides convenience implementations to `validate` which ensures `timestamp` and `original_string` fields exist.

## Example

In order to illustrate how this might be done, let's create a very
simple parser that takes a comma separated pair and creates a couple of
fields:
* `original_string` -- the raw data
* `timestamp` -- the current time
* `first` -- the first field of the comma separated pair
* `last` -- the last field of the comma separated pair

For this demonstration, let's create a maven project to compile our
project.  We'll call it `extra_parsers`, so in your workspace, let's set
up the maven project:

* Create the maven infrastructure for `extra_parsers` via
    ```
    mkdir -p extra_parsers/src/{main,test}/java
    ```

* Create a pom file indicating how we should build our parsers by
  editing `extra_parsers/pom.xml` with the following content:
    ```
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.3rdparty</groupId>
      <artifactId>extra-parsers</artifactId>
      <packaging>jar</packaging>
      <version>1.0-SNAPSHOT</version>
      <name>extra-parsers</name>
      <url>http://thirdpartysoftware.org</url>
      <properties>
        <!-- The java version to conform to.  Metron works all the way to 1.8 -->
        <java_version>1.8</java_version>
        <!-- The version of Metron that we'll be targetting. -->
        <metron_version>0.4.1</metron_version>
        <!-- To complete the simulation, we'll depend on a common dependency -->
        <guava_version>19.0</guava_version>
        <!-- We will shade our dependencies to create a single jar at the end -->
        <shade_version>2.4.3</shade_version>
      </properties>
      <dependencies>
        <!--
        We want to depend on Metron, but ensure that the scope is "provided"
        as we do not want to include it in our bundle.
        -->
        <dependency>
          <groupId>org.apache.metron</groupId>
          <artifactId>metron-parsers-common</artifactId>
          <version>${metron_version}</version>
          <scope>provided</scope>
        </dependency>
        <dependency>
          <groupId>com.google.guava</groupId>
          <artifactId>guava</artifactId>
          <version>${guava_version}</version>
        </dependency>
        <dependency>
          <groupId>junit</groupId>
          <artifactId>junit</artifactId>
          <version>3.8.1</version>
          <scope>test</scope>
        </dependency>
      </dependencies>
      <build>
        <plugins>
         <!-- We will set up the shade plugin to create a single jar at the
               end of the build lifecycle.  We will exclude some things and
               relocate others to simulate a real situation.
  
               One thing to note is that it's a good practice to shade and
               relocate common libraries that may be dependencies in Metron.
               Your jar will be merged with the parsers jar, so the metron
               version will be included for all overlapping classes.
               So, shade and relocate to ensure that YOUR version of the library is used.
          -->
  
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>${shade_version}</version>
            <configuration>
              <createDependencyReducedPom>true</createDependencyReducedPom>
              <artifactSet>
                <excludes>
                  <!-- Exclude slf4j for no reason other than to illustrate how to exclude dependencies.
                       The metron team has nothing against slf4j. :-)
                   -->
                  <exclude>*slf4j*</exclude>
                </excludes>
              </artifactSet>
            </configuration>
            <executions>
              <execution>
                <phase>package</phase>
                <goals>
                  <goal>shade</goal>
                </goals>
                <configuration>
                  <shadedArtifactAttached>true</shadedArtifactAttached>
                  <shadedClassifierName>uber</shadedClassifierName>
                  <filters>
                    <filter>
                      <!-- Sometimes these get added and confuse the uber jar out of shade -->
                      <artifact>*:*</artifact>
                      <excludes>
                        <exclude>META-INF/*.SF</exclude>
                        <exclude>META-INF/*.DSA</exclude>
                        <exclude>META-INF/*.RSA</exclude>
                      </excludes>
                    </filter>
                  </filters>
                  <relocations>
                    <!-- Relocate guava as it's used in Metron and I really want 0.19 -->
                    <relocation>
                      <pattern>com.google</pattern>
                      <shadedPattern>com.thirdparty.guava</shadedPattern>
                    </relocation>
                  </relocations>
                  <artifactSet>
                    <excludes>
                      <!-- We can also exclude by artifactId and groupId -->
                      <exclude>storm:storm-core:*</exclude>
                      <exclude>storm:storm-lib:*</exclude>
                      <exclude>org.slf4j.impl*</exclude>
                      <exclude>org.slf4j:slf4j-log4j*</exclude>
                    </excludes>
                  </artifactSet>
                </configuration>
              </execution>
            </executions>
          </plugin>
          <!--
          We want to make sure we compile using java 1.8.
          -->
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.5.1</version>
            <configuration>
              <forceJavacCompilerUse>true</forceJavacCompilerUse>
              <source>${java_version}</source>
              <compilerArgument>-Xlint:unchecked</compilerArgument>
              <target>${java_version}</target>
              <showWarnings>true</showWarnings>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </project>
    ```

* Now let's create our parser  `com.thirdparty.SimpleParser` by creating the file `extra-parsers/src/main/java/com/thirdparty/SimpleParser.java` with the following content:
    ```
    package com.thirdparty;

    import com.google.common.base.Splitter;
    import com.google.common.collect.ImmutableList;
    import com.google.common.collect.Iterables;
    import org.apache.metron.parsers.BasicParser;
    import org.json.simple.JSONObject;

    import java.util.List;
    import java.util.Map;

    public class SimpleParser extends BasicParser {
      @Override
      public void init() {

      }

      @Override
      public List<JSONObject> parse(byte[] bytes) {
        String input = new String(bytes);
        Iterable<String> it = Splitter.on(",").split(input);
        JSONObject ret = new JSONObject();
        ret.put("original_string", input);
        ret.put("timestamp", System.currentTimeMillis());
        ret.put("first", Iterables.getFirst(it, "missing"));
        ret.put("last", Iterables.getLast(it, "missing"));
        return ImmutableList.of(ret);
      }

      @Override
      public void configure(Map<String, Object> map) {

      }
    }
    ```
* Compile the parser via `mvn clean package` in `extra_parsers`

* This will create a jar containing your parser and its dependencies (sans Metron dependencies) in `extra-parsers/target/extra-parsers-1.0-SNAPSHOT-uber.jar`

# Deploying Your Custom Parser

In order to deploy your newly built custom parser, you would place the jar file above in the `$METRON_HOME/parser_contrib` directory on the Metron host (i.e. any host you would start parsers from or, alternatively, where the Metron REST is hosted).

## Example

Let's work through deploying the example above.

### Preliminaries

We assume that the following environment variables are set:
* `METRON_HOME` - the home directory for metron
* `ZOOKEEPER` - The zookeeper quorum (comma separated with port specified: e.g. `node1:2181` for full-dev)
* `BROKERLIST` - The Kafka broker list (comma separated with port specified: e.g. `node1:6667` for full-dev)
* `ES_HOST` - The elasticsearch master (and port) e.g. `node1:9200` for full-dev.

Also, this does not assume that you are using a kerberized cluster.  If you are, then the parser start command will adjust slightly to include the security protocol.

### Copy the jar file up

Copy the jar file located in `extra-parsers/target/extra-parsers-1.0-SNAPSHOT-uber.jar` to `$METRON_HOME/parser_contrib` and ensure the permissions are such that the `metron` user can read and execute.

### Restart the REST service in Ambari

In order for new parsers to be picked up, the REST service must be restarted.  You can do that from within Ambari by restarting the `Metron REST` service.

### Create a Kafka Topic

Create a kafka topic, let's call it `test`.
```
KAFKA_HOME=/usr/hdp/current/kafka-broker
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --topic test --partitions 1 --replication-factor 1
```

Note, in a real deployment, that topic would be named something more descriptive and would have replication factor and partitions set to something less trivial.

### Configure Test Parser

Create the a file called `$METRON_HOME/config/zookeeper/parsers/test.json` with the following content:
```
{
  "parserClassName":"com.thirdparty.SimpleParser",
  "sensorTopic":"test"
}
```

### Push the Zookeeper Configs

Now push the config to Zookeeper with the following command.
```
$METRON_HOME/bin/zk_load_configs.sh -m PUSH -i $METRON_HOME/config/zookeeper/ -z $ZOOKEEPER
```

### Start Parser
Now we can start the parser and send some data through:

* Start the parser
    ```
    $METRON_HOME/bin/start_parser_topology.sh -k $BROKERLIST -z $ZOOKEEPER -s test
    ```

* Send example data through:
    ```
    echo "apache,metron" | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list $BROKERLIST --topic test
    ```

* Validate data was written in ES:
    ```
    curl -XPOST "http://$ES_HOST/test*/_search?pretty" -d '
    {
      "_source" : [ "original_string", "timestamp", "first", "last"]
    }
    '
    ```

* This should yield something like:
    ```
    {
      "took" : 23,
      "timed_out" : false,
      "_shards" : {
        "total" : 1,
        "successful" : 1,
        "failed" : 0
      },
      "hits" : {
        "total" : 1,
        "max_score" : 1.0,
        "hits" : [ {
          "_index" : "test_index_2017.10.04.17",
          "_type" : "test_doc",
          "_id" : "3ae4dd4d-8c09-4f2a-93c0-26ec5508baaa",
          "_score" : 1.0,
          "_source" : {
            "original_string" : "apache,metron",
            "last" : "metron",
            "first" : "apache",
            "timestamp" : 1507138373223
          }
        } ]
      }
    }
    ```

### Via the Management UI

As long as the REST service is restarted after new parsers are added to `$METRON_HOME/parser_contrib`, they are available in the UI to creating and deploying parsers.
