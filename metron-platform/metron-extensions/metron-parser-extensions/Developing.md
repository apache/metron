# Developing a new Metron Parser Extension (Metron)

This guide discusses how to add a new Metron Parser Extension to the Metron
codebase.

## Setup
Make sure you have a good, working branch that builds and passes testing
This includes building and installing the bundles-maven-plugin
and the metron-parser-extension-archetype.

## Create a parser extension
```commandline
cd metron-platform/metron-extensions/metron-parser-extensions
run mvn -U archetype:generate -DarchetypeCatalog=local
```
- select Apache Maven Parser Extension Archetype for Metron
-  fill out the archetype variables:
``` commandline
 groupId : org.apache.metron
 artifactId: metron-parser-foo-extension
 version: $METRON_VERSION
 package: org.apache.metron.parsers
 metronVersion: $METRON_VERSION
 parserClassName: name of parser Java classes, such as BasicFoo
 parserName: the parser name as it appears in the system (yaf, asa, bro) foo
```
So you should see the following output:

``` commandline
Choose archetype:
1: local -> org.apache.metron:metron-parser-extension-archetype (Apache Parser Extension Archetype for Metron)
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 1
Define value for property 'groupId': org.apache.metron
Define value for property 'artifactId': metron-parser-foo-extension
Define value for property 'version' 1.0-SNAPSHOT: : 0.4.0
Define value for property 'package' org.apache.metron: : org.apache.metron.parsers
Define value for property 'metronVersion': 0.4.0
Define value for property 'parserClassName' (should match expression '^[A-Z].*$'): BasicFoo
Define value for property 'parserName' (should match expression '^[a-z]+[A-Z,a-z]+$'): foo
Confirm properties configuration:
groupId: org.apache.metron
artifactId: metron-parser-foo-extension
version: 0.4.0
package: org.apache.metron.parsers
metronVersion: 0.4.0
parserClassName: BasicFoo
parserName: foo
```

At this point, you should have a new extension project in the directory named metron-parser-foo-extension with the following files:

``` commandline
.
├── metron-parser-foo
│   ├── README.md
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── config
│       │   │   ├── elasticsearch
│       │   │   │   └── foo_index.template
│       │   │   └── zookeeper
│       │   │       ├── enrichments
│       │   │       │   └── foo.json
│       │   │       ├── indexing
│       │   │       │   └── foo.json
│       │   │       └── parsers
│       │   │           └── foo.json
│       │   ├── java
│       │   │   └── org
│       │   │       └── apache
│       │   │           └── metron
│       │   │               └── parsers
│       │   │                   └── foo
│       │   │                       └── BasicFooParser.java
│       │   └── resources
│       │       ├── META-INF
│       │       │   ├── LICENSE
│       │       │   └── NOTICE
│       │       └── patterns
│       │           └── common
│       └── test
│           ├── java
│           │   └── org
│           │       └── apache
│           │           └── metron
│           │               └── parsers
│           │                   ├── foo
│           │                   │   └── BasicFooParserTest.java
│           │                   └── integration
│           │                       └── BasicFooIntegrationTest.java
│           └── resources
│               ├── config
│               │   └── zookeeper
│               │       ├── bundle.properties
│               │       └── global.json
│               ├── data
│               │   ├── parsed
│               │   │   └── test.parsed
│               │   └── raw
│               │       └── test.raw
│               └── log4j.properties
├── metron-parser-foo-assembly
│   ├── metron-parser-foo-assembly.iml
│   ├── pom.xml
│   └── src
│       └── main
│           └── assembly
│               └── assembly.xml
├── metron-parser-foo-bundle
│   ├── metron-parser-foo-bundle.iml
│   └── pom.xml
├── pom.xml
└── tree.txt

37 directories, 26 files

```

Verify that the metron-parser-extensions/pom.xml references the new module, or add it as such:

```xml
<modules>
    <module>metron-parser-asa-extension</module>
    <module>metron-parser-bro-extension</module>
    <module>metron-parser-cef-extension</module>
    <module>metron-parser-fireeye-extension</module>
    <module>metron-parser-ise-extension</module>
    <module>metron-parser-lancope-extension</module>
    <module>metron-parser-logstash-extension</module>
    <module>metron-parser-paloalto-extension</module>
    <module>metron-parser-snort-extension</module>
    <module>metron-parser-sourcefire-extension</module>
    <module>metron-parser-websphere-extension</module>
    <module>metron-parser-squid-extension</module>
    <module>metron-parser-yaf-extension</module>
    <module>metron-parser-foo-extension</module>
    <module>metron-parser-bundle-tests</module>
  </modules>
```

  Build metron and run tests to ensure you are starting with a working set
  The archetype produces a very simple parser with unit and integration tests
  
  ``` commandline
  [INFO]
  [INFO] --- jacoco-maven-plugin:0.7.9:report (report) @ metron-parser-foo ---
  [INFO] Loading execution data file /Users/you/src/apache/forks/metron/metron-platform/metron-extensions/metron-parser-extensions/metron-parser-foo-extension/metron-parser-foo/target/jacoco.exec
  [INFO] Analyzed bundle 'metron-parser-foo' with 1 classes
  [INFO]
  [INFO] --- maven-jar-plugin:3.0.2:jar (default-jar) @ metron-parser-foo ---
  [INFO]
  [INFO] --- maven-jar-plugin:3.0.2:test-jar (default) @ metron-parser-foo ---
  [INFO]
  [INFO] --- maven-surefire-plugin:2.18:test (integration-tests) @ metron-parser-foo ---
  [INFO] Surefire report directory: /Users/you/src/apache/forks/metron/metron-platform/metron-extensions/metron-parser-extensions/metron-parser-foo-extension/metron-parser-foo/target/surefire-reports

  -------------------------------------------------------
   T E S T S
  -------------------------------------------------------
  Running org.apache.metron.parsers.integration.BasicFooIntegrationTest
  Running Pathed Sample Data Validation on sensorType foo
  Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 43.021 sec - in org.apache.metron.parsers.integration.BasicFooIntegrationTest

  Results :




  Tests run: 1, Failures: 0, Errors: 0, Skipped: 0


  [INFO] metron-parser-foo-extension ........................ SUCCESS [  0.002 s]
  [INFO] metron-parser-foo .................................. SUCCESS [ 44.879 s]
  [INFO] metron-parser-foo-bundle ........................... SUCCESS [  0.011 s]
  [INFO] metron-parser-foo-assembly ......................... SUCCESS [  0.030 s]

```
---------

## Build your parser

  - Edit the configurations for indexing, enrichment, parser
  - You must remove the config/elasticsearch directory created by the archetype!
  - add grok patterns if you want
  - remove the sample parser and write your own
  - etc
  - If your parser is only configuration ( like yaf ), still write the tests etc,
  but get rid of main java code, and edit the metron-parser-foo-assembly/src/main/assembly/assembly.xml as we have done with yaf:

```xml

  <!--  YAF IS CONFIGURATION ONLY AT THIS TIME
    <fileSet>
        <directory>${project.basedir}/../metron-parser-yaf-bundle/target</directory>
        <includes>
            <include>metron-parser-yaf-bundle-${project.version}.bundle</include>
        </includes>
        <outputDirectory>/lib</outputDirectory>
        <useDefaultExcludes>true</useDefaultExcludes>
    </fileSet>
    -->
```
    


## Add your extension to the install
  
  Once you are satisfied with your parser, it is time to add it to the install

  - First, you have to make sure that it is copied from it's build directory to the correct directory for the repo build.

  Open and edit metron-deployment/packaging/docker/rpm-docker/pom.xml
  add a resource entry under the <!-- extensions --> area as such:
  
```xml

  <resource>
    <directory>${metron_dir}/metron-platform/metron-extensions/metron-parser-extensions/metron-parser-foo-extension/metron-parser-foo-assembly/target/</directory>
    <includes>
      <include>*.tar.gz</include>
    </includes>
  </resource>

```

## Add the extension to the rpm spec
  
  Open metron-deployment/packaging/docker/rpm-docker/SPECS/metron.spec

  - Add a source entry such as
  ```commandline
    Source24:       metron-parser-foo-assembly-%{full_version}-archive.tar.gz
  ```

  - Under %install add a mkdir entry such as
  ```commandline
  mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/foo
  ```
  - Under copy source files and untar make an entry such as
  ```commandline
  tar -xzf %{SOURCE24} -C %{buildroot}%{metron_extensions_etc_parsers}/foo
  ```
  - If your parser has code and is not just configuration, under move the bundles from config to extensions lib make an entry as such
  ```commandline
  mv %{buildroot}%{metron_extensions_etc_parsers}/foo/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
  ```
  - Add a package entry for the parser.  Examine the other entries for parser extensions and make sure your entries match
    
```commandline

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        parser-extension-foo
Summary:        Metron Foo Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-foo = %{version}

%description    parser-extension-foo
This package installs the Metron FOO Parser Extension files

%files          parser-extension-foo
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/foo
%dir %{metron_extensions_etc_parsers}/foo/config
%dir %{metron_extensions_etc_parsers}/foo/config/zookeeper
%dir %{metron_extensions_etc_parsers}/foo/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/foo/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/foo/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/foo/patterns
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/foo/config/zookeeper/parsers/foo.json
%{metron_extensions_etc_parsers}/foo/config/zookeeper/enrichments/foo.json
%{metron_extensions_etc_parsers}/foo/config/zookeeper/indexing/foo.json
%{metron_extensions_etc_parsers}/foo/patterns/common
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-foo-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

- Add to the changelog in the spec


## Build the rpms

```commandline
cd metron-deployment/
mvn package -P build-rpms
```

You should see your new rpm:

```commandline

┌[]-(~/src/apache/forks/metron/metron-deployment/packaging/docker/rpm-docker/RPMS/noarch)-[]-
└> ls
metron-common-0.4.0-201706071954.noarch.rpm                      metron-parser-extension-fireeye-0.4.0-201706071954.noarch.rpm    metron-parser-extension-websphere-0.4.0-201706071954.noarch.rpm
metron-config-0.4.0-201706071954.noarch.rpm                      metron-parser-extension-foo-0.4.0-201706071954.noarch.rpm        metron-parser-extension-yaf-0.4.0-201706071954.noarch.rpm
metron-data-management-0.4.0-201706071954.noarch.rpm             metron-parser-extension-ise-0.4.0-201706071954.noarch.rpm        metron-parsers-0.4.0-201706071954.noarch.rpm
metron-elasticsearch-0.4.0-201706071954.noarch.rpm               metron-parser-extension-lancope-0.4.0-201706071954.noarch.rpm    metron-pcap-0.4.0-201706071954.noarch.rpm
metron-enrichment-0.4.0-201706071954.noarch.rpm                  metron-parser-extension-logstash-0.4.0-201706071954.noarch.rpm   metron-profiler-0.4.0-201706071954.noarch.rpm
metron-indexing-0.4.0-201706071954.noarch.rpm                    metron-parser-extension-paloalto-0.4.0-201706071954.noarch.rpm   metron-rest-0.4.0-201706071954.noarch.rpm
metron-parser-extension-asa-0.4.0-201706071954.noarch.rpm        metron-parser-extension-snort-0.4.0-201706071954.noarch.rpm      metron-solr-0.4.0-201706071954.noarch.rpm
metron-parser-extension-bro-0.4.0-201706071954.noarch.rpm        metron-parser-extension-sourcefire-0.4.0-201706071954.noarch.rpm
metron-parser-extension-cef-0.4.0-201706071954.noarch.rpm        metron-parser-extension-squid-0.4.0-201706071954.noarch.rpm

```

## Integrate with the ambari mpack
 - Add to the all_parsers list
 
Edit metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/params/status_params.py
and add your parser to the list, as such:

```commandline
all_parsers = "asa,bro,cef,fireeye,ise,lancope,logstash,paloalto,foo,snort,sourcefire,squid,websphere,yaf"
```

 - Add your parser rpm to the metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/metainfo.xml
```commandline
<package>
    <name>metron-parser-extension-foo</name>
</package>
```

## Run Vagrant full_dev_platform

```commandline
cd metron-deployment/vagrant/full_dev_platform
vagrant up
```
- Verify the parser is installed
    - open http://node1:4200/sensors  ( default user and password are user and password)
    - verify that foo is in the list

You should be able to stop a running sensor and start foo, verify in the storm ui that it is loaded and running




  