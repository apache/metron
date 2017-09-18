# Installing Parser Extensions with Swagger

### start full_dev
- run vagrant up from metron-deployment/vagrant/full_dev_platform

### add a slot to storm
- log into ambari on http://node1:8080
- add a new slot to the storm config

### Open swagger
- log into ambari on http://node1:8080
- go into the metron service | quicklinks
- start swagger with user | password

### Install your parser
In swagger, using the parser-extension-controller
- drop down the POST method
- use the file selector to select the created .tar.gz
- execute
The extension should now be installed:
- you should be able to use the other GET methods to review the generated configuration.
  - the configuration will have the extensionID to use for the other {name} operations
- you should be able to view the installed files in hdfs/apps/metron/patterns/{parsername}, hdfs/apps/metron/extensions_alt_lib/

### Start the parser using rest
Still in swagger
- Use the Kafka Controller and the Storm Controller to create a {parserName} topic, and start a storm job for {parserName}
- In the storm UI you should see the storm instance for the parser
You should be able to verify using the rest api and the various controllers that the configurations are there and the topics exist and the storm jobs as well


### Uninstall your parser
In swagger, using the parser-extension-controller
- drop down the DELETE method
- fill in the name parameter with the extensionID from the config
- exectute

When complete you should see that the kafka topic, storm job, the various configurations are all gone, along with the patterns and other things from hdfs
