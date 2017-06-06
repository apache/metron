# Metron Maven Parser Extension Archetype

This archetype can be used to create a project to create, build and deploy one or more metron parsers
By filling out the parameters when creating the archetype, the produced project will be completely setup
to build.


### USAGE
Creating a parser and deploying it... and deleting it using the rest api in full dev

## Preparation
In order to build the parser, we need to build and install the maven plugin to build the bundles, as well as the maven archetype itself.
At this time, since they are not published to apache maven, this will be a requirement.

- Build the bundle plugin
```
cd bundles-maven-plugin && mvn -q install && cd ..
```
- Build the archetype
```
cd metron-maven-archetypes/metron-parser-extension-archetype
mvn install
cd ../..
```

### Build Metron
```
cd metron
mvn clean install
```

## Create and build a new parser extension
- Make a directory and cd into it
- Create from archetype
```
mvn -U archetype:generate -DarchetypeCatalog=local
```
   - choose the org.apache.metron:metron-maven-parser-extension-archetype (Apache Maven Parser Extension Archetype for Metron) option
   - Fill out the information (information on the parameters is in the README for the archetype)
- cd into the created directory and build the parser extension
```
mvn package
```

This will result in the tar.gz being created inside the {NAME}-parser-assembly project/target directory

## start full_dev
- run vagrant up from metron-deployment/vagrant/full_dev_platform

## add a slot to storm
- log into ambari on http://node1:8080
- add a new slot to the storm config

## Open swagger
- log into ambari on http://node1:8080
- go into the metron service | quicklinks
- start swagger with user | password

## Install your parser
In swagger, using the parser-extension-controller
- drop down the POST method
- use the file selector to select the created .tar.gz
- execute
The extension should now be installed:
- you should be able to use the other GET methods to review the generated configuration.
  - the configuration will have the extensionID to use for the other {name} operations
- you should be able to view the installed files in hdfs/apps/metron/patterns/{parsername}, hdfs/apps/metron/extensions_alt_lib/

## Start the parser using rest
Still in swagger
- Use the Kafka Controller and the Storm Controller to create a {parserName} topic, and start a storm job for {parserName}
- In the storm UI you should see the storm instance for the parser
You should be able to verify using the rest api and the various controllers that the configurations are there and the topics exist and the storm jobs as well


## Uninstall your parser
In swagger, using the parser-extension-controller
- drop down the DELETE method
- fill in the name parameter with the extensionID from the config
- exectute

When complete you should see that the kafka topic, storm job, the various configurations are all gone, along with the patterns and other things from hdfs

