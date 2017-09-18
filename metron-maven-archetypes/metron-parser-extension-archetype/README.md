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

- [Installing Parser Extensions with Swagger](Swagger.md)
- [Installing Parser Extensions with Metron Configuration Management UI](UI.md)


