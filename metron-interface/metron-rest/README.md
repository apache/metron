# Metron REST

This module provides a RESTful API for interacting with Metron.

## Prerequisites

* A running Metron cluster
* Java 8 installed
* Storm CLI and Metron topology scripts (start_parser_topology.sh, start_enrichment_topology.sh, start_elasticsearch_topology.sh) installed

## Installation
1. Package the application with Maven:
```
mvn clean package
```

1. Untar the archive in the target directory.  The directory structure will look like:
```
bin
  start_metron_rest.sh
lib
  metron-rest-$METRON_VERSION.jar
```

1. Create an `application.yml` file with the contents of [application-docker.yml](src/main/resources/application-docker.yml).  Substitute the appropriate Metron service urls (Kafka, Zookeeper, Storm, etc) in properties containing `${docker.host.address}` and update the `spring.datasource.*` properties as needed (see the [Security](#security) section for more details).

1. Start the application with this command:
```
./bin/start_metron_rest.sh /path/to/application.yml
```

## Usage

The exposed REST endpoints can be accessed with the Swagger UI at http://host:port/swagger-ui.html#/.  The default port is 8080 but can be changed in application.yml by setting "server.port" to the desired port.

## Security

The metron-rest module uses [Spring Security](http://projects.spring.io/spring-security/) for authentication and stores user credentials in a relational database.  The H2 database is configured by default and is intended only for development purposes.  The "dev" profile can be used to automatically load test users:
```
./bin/start_metron_rest.sh /path/to/application.yml --spring.profiles.active=dev
```

For [production use](http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-connect-to-production-database), a relational database should be configured.  For example, configuring MySQL would be done as follows:

1. Create a MySQL user for the Metron REST application (http://dev.mysql.com/doc/refman/5.7/en/adding-users.html).

1. Connect to MySQL and create a Metron REST database:
```
CREATE DATABASE IF NOT EXISTS metronrest
```

1. Add users:
```
use metronrest;
insert into users (username, password, enabled) values ('your_username','your_password',1);
insert into authorities (username, authority) values ('your_username', 'ROLE_USER');
```

1. Replace the H2 connection information in the application.yml file with MySQL connection information:
```
spring:
  datasource:
        driverClassName: com.mysql.jdbc.Driver
        url: jdbc:mysql://mysql_host:3306/metronrest
        username: metron_rest_user
        password: metron_rest_password
        platform: mysql
```

1. Add a dependency for the MySQL JDBC connector in the metron-rest pom.xml:
```
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <version>${mysql.client.version}</version>
</dependency>
```

1. Follow the steps in the [Installation](#installation) section

## API

Request and Response objects are JSON formatted.  The JSON schemas are available in the Swagger UI.

|            |
| ---------- |
| [ `GET /api/v1/global/config`](#get-apiv1globalconfig)|
| [ `DELETE /api/v1/global/config`](#delete-apiv1globalconfig)|
| [ `POST /api/v1/global/config`](#post-apiv1globalconfig)|
| [ `GET /api/v1/grok/list`](#get-apiv1groklist)|
| [ `POST /api/v1/grok/validate`](#post-apiv1grokvalidate)|
| [ `POST /api/v1/hdfs`](#post-apiv1hdfs)|
| [ `GET /api/v1/hdfs`](#get-apiv1hdfs)|
| [ `DELETE /api/v1/hdfs`](#delete-apiv1hdfs)|
| [ `GET /api/v1/hdfs/list`](#get-apiv1hdfslist)|
| [ `GET /api/v1/kafka/topic`](#get-apiv1kafkatopic)|
| [ `POST /api/v1/kafka/topic`](#post-apiv1kafkatopic)|
| [ `GET /api/v1/kafka/topic/{name}`](#get-apiv1kafkatopic{name})|
| [ `DELETE /api/v1/kafka/topic/{name}`](#delete-apiv1kafkatopic{name})|
| [ `GET /api/v1/kafka/topic/{name}/sample`](#get-apiv1kafkatopic{name}sample)|
| [ `GET /api/v1/sensor/enrichment/config`](#get-apiv1sensorenrichmentconfig)|
| [ `GET /api/v1/sensor/enrichment/config/list/available`](#get-apiv1sensorenrichmentconfiglistavailable)|
| [ `DELETE /api/v1/sensor/enrichment/config/{name}`](#delete-apiv1sensorenrichmentconfig{name})|
| [ `POST /api/v1/sensor/enrichment/config/{name}`](#post-apiv1sensorenrichmentconfig{name})|
| [ `GET /api/v1/sensor/enrichment/config/{name}`](#get-apiv1sensorenrichmentconfig{name})|
| [ `GET /api/v1/sensor/indexing/config`](#get-apiv1sensorindexingconfig)|
| [ `DELETE /api/v1/sensor/indexing/config/{name}`](#delete-apiv1sensorindexingconfig{name})|
| [ `POST /api/v1/sensor/indexing/config/{name}`](#post-apiv1sensorindexingconfig{name})|
| [ `GET /api/v1/sensor/indexing/config/{name}`](#get-apiv1sensorindexingconfig{name})|
| [ `POST /api/v1/sensor/parser/config`](#post-apiv1sensorparserconfig)|
| [ `GET /api/v1/sensor/parser/config`](#get-apiv1sensorparserconfig)|
| [ `GET /api/v1/sensor/parser/config/list/available`](#get-apiv1sensorparserconfiglistavailable)|
| [ `POST /api/v1/sensor/parser/config/parseMessage`](#post-apiv1sensorparserconfigparsemessage)|
| [ `GET /api/v1/sensor/parser/config/reload/available`](#get-apiv1sensorparserconfigreloadavailable)|
| [ `DELETE /api/v1/sensor/parser/config/{name}`](#delete-apiv1sensorparserconfig{name})|
| [ `GET /api/v1/sensor/parser/config/{name}`](#get-apiv1sensorparserconfig{name})|
| [ `POST /api/v1/stellar/apply/transformations`](#post-apiv1stellarapplytransformations)|
| [ `GET /api/v1/stellar/list`](#get-apiv1stellarlist)|
| [ `GET /api/v1/stellar/list/functions`](#get-apiv1stellarlistfunctions)|
| [ `GET /api/v1/stellar/list/simple/functions`](#get-apiv1stellarlistsimplefunctions)|
| [ `POST /api/v1/stellar/validate/rules`](#post-apiv1stellarvalidaterules)|
| [ `GET /api/v1/storm`](#get-apiv1storm)|
| [ `GET /api/v1/storm/client/status`](#get-apiv1stormclientstatus)|
| [ `GET /api/v1/storm/enrichment`](#get-apiv1stormenrichment)|
| [ `GET /api/v1/storm/enrichment/activate`](#get-apiv1stormenrichmentactivate)|
| [ `GET /api/v1/storm/enrichment/deactivate`](#get-apiv1stormenrichmentdeactivate)|
| [ `GET /api/v1/storm/enrichment/start`](#get-apiv1stormenrichmentstart)|
| [ `GET /api/v1/storm/enrichment/stop`](#get-apiv1stormenrichmentstop)|
| [ `GET /api/v1/storm/indexing`](#get-apiv1stormindexing)|
| [ `GET /api/v1/storm/indexing/activate`](#get-apiv1stormindexingactivate)|
| [ `GET /api/v1/storm/indexing/deactivate`](#get-apiv1stormindexingdeactivate)|
| [ `GET /api/v1/storm/indexing/start`](#get-apiv1stormindexingstart)|
| [ `GET /api/v1/storm/indexing/stop`](#get-apiv1stormindexingstop)|
| [ `GET /api/v1/storm/parser/activate/{name}`](#get-apiv1stormparseractivate{name})|
| [ `GET /api/v1/storm/parser/deactivate/{name}`](#get-apiv1stormparserdeactivate{name})|
| [ `GET /api/v1/storm/parser/start/{name}`](#get-apiv1stormparserstart{name})|
| [ `GET /api/v1/storm/parser/stop/{name}`](#get-apiv1stormparserstop{name})|
| [ `GET /api/v1/storm/{name}`](#get-apiv1storm{name})|
| [ `GET /api/v1/user`](#get-apiv1user)|

### `GET /api/v1/global/config`
  * Description: Retrieves the current Global Config from Zookeeper
  * Returns:
    * 200 - Returns current Global Config JSON in Zookeeper
    * 404 - Global Config JSON was not found in Zookeeper

### `DELETE /api/v1/global/config`
  * Description: Deletes the current Global Config from Zookeeper
  * Returns:
    * 200 - Global Config JSON was deleted
    * 404 - Global Config JSON was not found in Zookeeper

### `POST /api/v1/global/config`
  * Description: Creates or updates the Global Config in Zookeeper
  * Input:
    * globalConfig - The Global Config JSON to be saved
  * Returns:
    * 200 - Global Config updated. Returns saved Global Config JSON
    * 201 - Global Config created. Returns saved Global Config JSON

### `GET /api/v1/grok/list`
  * Description: Lists the common Grok statements available in Metron
  * Returns:
    * 200 - JSON object containing pattern label/Grok statements key value pairs

### `POST /api/v1/grok/validate`
  * Description: Applies a Grok statement to a sample message
  * Input:
    * grokValidation - Object containing Grok statement and sample message
  * Returns:
    * 200 - JSON results

### `POST /api/v1/hdfs`
  * Description: Writes contents to an HDFS file.  Warning: this will overwite the contents of a file if it already exists.
  * Input:
    * path - Path to HDFS file
    * contents - File contents
  * Returns:
    * 200 - Contents were written

### `GET /api/v1/hdfs`
  * Description: Reads a file from HDFS and returns the contents
  * Input:
    * path - Path to HDFS file
  * Returns:
    * 200 - Returns file contents

### `DELETE /api/v1/hdfs`
  * Description: Deletes a file from HDFS
  * Input:
    * path - Path to HDFS file
    * recursive - Delete files recursively
  * Returns:
    * 200 - File was deleted
    * 404 - File was not found in HDFS

### `GET /api/v1/hdfs/list`
  * Description: Reads a file from HDFS and returns the contents
  * Input:
    * path - Path to HDFS directory
  * Returns:
    * 200 - Returns file contents

### `GET /api/v1/kafka/topic`
  * Description: Retrieves all Kafka topics
  * Returns:
    * 200 - Returns a list of all Kafka topics

### `POST /api/v1/kafka/topic`
  * Description: Creates a new Kafka topic
  * Input:
    * topic - Kafka topic
  * Returns:
    * 200 - Returns saved Kafka topic

### `GET /api/v1/kafka/topic/{name}`
  * Description: Retrieves a Kafka topic
  * Input:
    * name - Kafka topic name
  * Returns:
    * 200 - Returns Kafka topic
    * 404 - Kafka topic is missing

### `DELETE /api/v1/kafka/topic/{name}`
  * Description: Delets a Kafka topic
  * Input:
    * name - Kafka topic name
  * Returns:
    * 200 - Kafka topic was deleted
    * 404 - Kafka topic is missing

### `GET /api/v1/kafka/topic/{name}/sample`
  * Description: Retrieves a sample message from a Kafka topic using the most recent offset
  * Input:
    * name - Kafka topic name
  * Returns:
    * 200 - Returns sample message
    * 404 - Either Kafka topic is missing or contains no messages

### `GET /api/v1/sensor/enrichment/config`
  * Description: Retrieves all SensorEnrichmentConfigs from Zookeeper
  * Returns:
    * 200 - Returns all SensorEnrichmentConfigs

### `GET /api/v1/sensor/enrichment/config/list/available`
  * Description: Lists the available enrichments
  * Returns:
    * 200 - Returns a list of available enrichments

### `DELETE /api/v1/sensor/enrichment/config/{name}`
  * Description: Deletes a SensorEnrichmentConfig from Zookeeper
  * Input:
    * name - SensorEnrichmentConfig name
  * Returns:
    * 200 - SensorEnrichmentConfig was deleted
    * 404 - SensorEnrichmentConfig is missing

### `POST /api/v1/sensor/enrichment/config/{name}`
  * Description: Updates or creates a SensorEnrichmentConfig in Zookeeper
  * Input:
    * sensorEnrichmentConfig - SensorEnrichmentConfig
    * name - SensorEnrichmentConfig name
  * Returns:
    * 200 - SensorEnrichmentConfig updated. Returns saved SensorEnrichmentConfig
    * 201 - SensorEnrichmentConfig created. Returns saved SensorEnrichmentConfig

### `GET /api/v1/sensor/enrichment/config/{name}`
  * Description: Retrieves a SensorEnrichmentConfig from Zookeeper
  * Input:
    * name - SensorEnrichmentConfig name
  * Returns:
    * 200 - Returns SensorEnrichmentConfig
    * 404 - SensorEnrichmentConfig is missing

### `GET /api/v1/sensor/indexing/config`
  * Description: Retrieves all SensorIndexingConfigs from Zookeeper
  * Returns:
    * 200 - Returns all SensorIndexingConfigs

### `DELETE /api/v1/sensor/indexing/config/{name}`
  * Description: Deletes a SensorIndexingConfig from Zookeeper
  * Input:
    * name - SensorIndexingConfig name
  * Returns:
    * 200 - SensorIndexingConfig was deleted
    * 404 - SensorIndexingConfig is missing

### `POST /api/v1/sensor/indexing/config/{name}`
  * Description: Updates or creates a SensorIndexingConfig in Zookeeper
  * Input:
    * sensorIndexingConfig - SensorIndexingConfig
    * name - SensorIndexingConfig name
  * Returns:
    * 200 - SensorIndexingConfig updated. Returns saved SensorIndexingConfig
    * 201 - SensorIndexingConfig created. Returns saved SensorIndexingConfig

### `GET /api/v1/sensor/indexing/config/{name}`
  * Description: Retrieves a SensorIndexingConfig from Zookeeper
  * Input:
    * name - SensorIndexingConfig name
  * Returns:
    * 200 - Returns SensorIndexingConfig
    * 404 - SensorIndexingConfig is missing

### `POST /api/v1/sensor/parser/config`
  * Description: Updates or creates a SensorParserConfig in Zookeeper
  * Input:
    * sensorParserConfig - SensorParserConfig
  * Returns:
    * 200 - SensorParserConfig updated. Returns saved SensorParserConfig
    * 201 - SensorParserConfig created. Returns saved SensorParserConfig

### `GET /api/v1/sensor/parser/config`
  * Description: Retrieves all SensorParserConfigs from Zookeeper
  * Returns:
    * 200 - Returns all SensorParserConfigs

### `GET /api/v1/sensor/parser/config/list/available`
  * Description: Lists the available parser classes that can be found on the classpath
  * Returns:
    * 200 - Returns a list of available parser classes

### `POST /api/v1/sensor/parser/config/parseMessage`
  * Description: Parses a sample message given a SensorParserConfig
  * Input:
    * parseMessageRequest - Object containing a sample message and SensorParserConfig
  * Returns:
    * 200 - Returns parsed message

### `GET /api/v1/sensor/parser/config/reload/available`
  * Description: Scans the classpath for available parser classes and reloads the cached parser class list
  * Returns:
    * 200 - Returns a list of available parser classes

### `DELETE /api/v1/sensor/parser/config/{name}`
  * Description: Deletes a SensorParserConfig from Zookeeper
  * Input:
    * name - SensorParserConfig name
  * Returns:
    * 200 - SensorParserConfig was deleted
    * 404 - SensorParserConfig is missing

### `GET /api/v1/sensor/parser/config/{name}`
  * Description: Retrieves a SensorParserConfig from Zookeeper
  * Input:
    * name - SensorParserConfig name
  * Returns:
    * 200 - Returns SensorParserConfig
    * 404 - SensorParserConfig is missing

### `POST /api/v1/stellar/apply/transformations`
  * Description: Executes transformations against a sample message
  * Input:
    * transformationValidation - Object containing SensorParserConfig and sample message
  * Returns:
    * 200 - Returns transformation results

### `GET /api/v1/stellar/list`
  * Description: Retrieves field transformations
  * Returns:
    * 200 - Returns a list field transformations

### `GET /api/v1/stellar/list/functions`
  * Description: Lists the Stellar functions that can be found on the classpath
  * Returns:
    * 200 - Returns a list of Stellar functions

### `GET /api/v1/stellar/list/simple/functions`
  * Description: Lists the simple Stellar functions (functions with only 1 input) that can be found on the classpath
  * Returns:
    * 200 - Returns a list of simple Stellar functions

### `POST /api/v1/stellar/validate/rules`
  * Description: Tests Stellar statements to ensure they are well-formed
  * Input:
    * statements - List of statements to validate
  * Returns:
    * 200 - Returns validation results

### `GET /api/v1/storm`
  * Description: Retrieves the status of all Storm topologies
  * Returns:
    * 200 - Returns a list of topologies with status information

### `GET /api/v1/storm/client/status`
  * Description: Retrieves information about the Storm command line client
  * Returns:
    * 200 - Returns storm command line client information

### `GET /api/v1/storm/enrichment`
  * Description: Retrieves the status of the Storm enrichment topology
  * Returns:
    * 200 - Returns topology status information
    * 404 - Topology is missing

### `GET /api/v1/storm/enrichment/activate`
  * Description: Activates a Storm enrichment topology
  * Returns:
    * 200 - Returns activate response message

### `GET /api/v1/storm/enrichment/deactivate`
  * Description: Deactivates a Storm enrichment topology
  * Returns:
    * 200 - Returns deactivate response message

### `GET /api/v1/storm/enrichment/start`
  * Description: Starts a Storm enrichment topology
  * Returns:
    * 200 - Returns start response message

### `GET /api/v1/storm/enrichment/stop`
  * Description: Stops a Storm enrichment topology
  * Input:
    * stopNow - Stop the topology immediately
  * Returns:
    * 200 - Returns stop response message

### `GET /api/v1/storm/indexing`
  * Description: Retrieves the status of the Storm indexing topology
  * Returns:
    * 200 - Returns topology status information
    * 404 - Topology is missing

### `GET /api/v1/storm/indexing/activate`
  * Description: Activates a Storm indexing topology
  * Returns:
    * 200 - Returns activate response message

### `GET /api/v1/storm/indexing/deactivate`
  * Description: Deactivates a Storm indexing topology
  * Returns:
    * 200 - Returns deactivate response message

### `GET /api/v1/storm/indexing/start`
  * Description: Starts a Storm indexing topology
  * Returns:
    * 200 - Returns start response message

### `GET /api/v1/storm/indexing/stop`
  * Description: Stops a Storm enrichment topology
  * Input:
    * stopNow - Stop the topology immediately
  * Returns:
    * 200 - Returns stop response message

### `GET /api/v1/storm/parser/activate/{name}`
  * Description: Activates a Storm parser topology
  * Input:
    * name - Parser name
  * Returns:
    * 200 - Returns activate response message

### `GET /api/v1/storm/parser/deactivate/{name}`
  * Description: Deactivates a Storm parser topology
  * Input:
    * name - Parser name
  * Returns:
    * 200 - Returns deactivate response message

### `GET /api/v1/storm/parser/start/{name}`
  * Description: Starts a Storm parser topology
  * Input:
    * name - Parser name
  * Returns:
    * 200 - Returns start response message

### `GET /api/v1/storm/parser/stop/{name}`
  * Description: Stops a Storm parser topology
  * Input:
    * name - Parser name
    * stopNow - Stop the topology immediately
  * Returns:
    * 200 - Returns stop response message

### `GET /api/v1/storm/{name}`
  * Description: Retrieves the status of a Storm topology
  * Input:
    * name - Topology name
  * Returns:
    * 200 - Returns topology status information
    * 404 - Topology is missing

### `GET /api/v1/user`
  * Description: Retrieves the current user
  * Returns:
    * 200 - Current user

## Testing

Profiles are includes for both the metron-docker and Quick Dev environments.

### metron-docker

Start the [metron-docker](../../metron-docker) environment.  Build the metron-rest module and start it with the Spring Boot Maven plugin:
```
mvn clean package
mvn spring-boot:run -Drun.profiles=docker,dev
```
The metron-rest application will be available at http://localhost:8080/swagger-ui.html#/.

### Quick Dev

Start the [Quick Dev](../../metron-deployment/vagrant/quick-dev-platform) environment.  Build the metron-rest module and start it with the Spring Boot Maven plugin:
```
mvn clean package
mvn spring-boot:run -Drun.profiles=vagrant,dev
```
The metron-rest application will be available at http://localhost:8080/swagger-ui.html#/.

To run the application locally on the Quick Dev host, package the application and scp the archive to node1:
```
mvn clean package
scp ./target/metron-rest-$METRON_VERSION-archive.tar.gz root@node1:~/
```
Login to node1 and unarchive the metron-rest application.  Start the application on a different port to avoid conflicting with Ambari:
```
java -jar ./lib/metron-rest-$METRON_VERSION.jar --spring.profiles.active=vagrant,dev --server.port=8082
```
The metron-rest application will be available at http://node1:8082/swagger-ui.html#/.

## License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.
