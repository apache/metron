# Metron Maven Parser Extension Archetype

This archetype can be used to create a project to create, build and deploy one or more metron parsers
By filling out the parameters when creating the archetype, the produced project will be completely setup
to build.

## Steps Required

### Build Metron
`cd incubator-metron`
`mvn clean install`

This will build and install the archetype locally 
    TODO: deploy archetype to maven repo

### Create your project 
Create a directory where ever you would like for your project
`mkdir myParser && cd myParser`

### Create the project with maven
`mvn archetype:generate -DarchetypeCatalog=local`

Select the metron-maven-parser-extension-archtype

##### Fill out the parameters
NOTES:
- Currently, the version number _must match the current version of Metron ( 0.3.1 )
  When prompted for a version you must enter this or your parser will fail to start in storm
    
- The parserName must be lowercase, the parserClassName must start be Capital case, the name should match
  for consistancy's sake, but do not have to.  the parserName will be the sensor name used in the system ( the ES index etc )
   whereas the parserClassName will be used for java class names in generated sources

##### Examine the generated project, code your parser and tests and then build
`mvn install`

###Install
    TODO