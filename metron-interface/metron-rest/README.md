# Metron REST and Configuration UI

This UI exposes and aids in sensor configuration.

## Prerequisites

* A running Metron cluster
* A running instance of MySQL
* Java 8 installed
* Storm CLI and Metron topology scripts (start_parser_topology.sh, start_enrichment_topology.sh, start_elasticsearch_topology.sh) installed

## Installation
1. Package the Application with Maven:
    ```
    mvn clean package
    ```

1. Untar the archive in the target directory.  The directory structure will look like:
    ```
    bin
      start.sh
    lib
      metron-rest-version.jar
    ```

1. Install Hibernate by downloading version 5.0.11.Final from (http://hibernate.org/orm/downloads/).  Unpack the archive and set the HIBERNATE_HOME environment variable to the absolute path of the top level directory.
    ```
    export HIBERNATE_HOME=/path/to/hibernate-release-5.0.11.Final
    ```

1. Install the MySQL client by downloading version 5.1.40 from (https://dev.mysql.com/downloads/connector/j/).  Unpack the archive and set the MYSQL_CLIENT_HOME environment variable to the absolute path of the top level directory.
    ```
    export MYSQL_CLIENT_HOME=/path/to/mysql-connector-java-5.1.40
    ```

1. Create a MySQL user for the Config UI (http://dev.mysql.com/doc/refman/5.7/en/adding-users.html).

1. Create a Config UI database in MySQL with this command:
    ```
    CREATE DATABASE IF NOT EXISTS metronrest
    ```

1. Create an `application.yml` file with the contents of [application-docker.yml](src/main/resources/application-docker.yml).  Substitute the appropriate Metron service urls (Kafka, Zookeeper, Storm, etc) in properties containing `${docker.host.address}` and update the `spring.datasource.username` and `spring.datasource.password` properties using the MySQL credentials from step 4.

1. Start the UI with this command:
    ```
    ./bin/start.sh /path/to/application.yml
    ```

## Usage

The exposed REST endpoints can be accessed with the Swagger UI at http://host:port/swagger-ui.html#/.  The default port is 8080 but can be changed in application.yml by setting "server.port" to the desired port.  Users can be added with this SQL statement:
```
use metronrest;
insert into users (username, password, enabled) values ('your_username','your_password',1);
insert into authorities (username, authority) values ('your_username', 'ROLE_USER');
```
Users can be added to additional groups with this SQL statement:
```
use metronrest;
insert into authorities (username, authority) values ('your_username', 'your_group');
```

## API

Request and Response objects are JSON formatted.  The JSON schemas are available in the Swagger UI.

|            |
| ---------- |
| [ `GET /api/v1/globalConfig`](#get-/api/v1/globalconfig)|
| [ `DELETE /api/v1/globalConfig`](#delete-/api/v1/globalconfig)|
| [ `POST /api/v1/globalConfig`](#post-/api/v1/globalconfig)|
| [ `GET /api/v1/grok/list`](#get-/api/v1/grok/list)|
| [ `POST /api/v1/grok/validate`](#post-/api/v1/grok/validate)|
| [ `GET /api/v1/kafka/topic`](#get-/api/v1/kafka/topic)|
| [ `POST /api/v1/kafka/topic`](#post-/api/v1/kafka/topic)|
| [ `GET /api/v1/kafka/topic/{name}`](#get-/api/v1/kafka/topic/{name})|
| [ `DELETE /api/v1/kafka/topic/{name}`](#delete-/api/v1/kafka/topic/{name})|
| [ `GET /api/v1/kafka/topic/{name}/sample`](#get-/api/v1/kafka/topic/{name}/sample)|
| [ `GET /api/v1/sensorEnrichmentConfig`](#get-/api/v1/sensorenrichmentconfig)|
| [ `GET /api/v1/sensorEnrichmentConfig/list/available`](#get-/api/v1/sensorenrichmentconfig/list/available)|
| [ `DELETE /api/v1/sensorEnrichmentConfig/{name}`](#delete-/api/v1/sensorenrichmentconfig/{name})|
| [ `POST /api/v1/sensorEnrichmentConfig/{name}`](#post-/api/v1/sensorenrichmentconfig/{name})|
| [ `GET /api/v1/sensorEnrichmentConfig/{name}`](#get-/api/v1/sensorenrichmentconfig/{name})|
| [ `POST /api/v1/sensorParserConfig`](#post-/api/v1/sensorparserconfig)|
| [ `GET /api/v1/sensorParserConfig`](#get-/api/v1/sensorparserconfig)|
| [ `GET /api/v1/sensorParserConfig/list/available`](#get-/api/v1/sensorparserconfig/list/available)|
| [ `POST /api/v1/sensorParserConfig/parseMessage`](#post-/api/v1/sensorparserconfig/parsemessage)|
| [ `GET /api/v1/sensorParserConfig/reload/available`](#get-/api/v1/sensorparserconfig/reload/available)|
| [ `DELETE /api/v1/sensorParserConfig/{name}`](#delete-/api/v1/sensorparserconfig/{name})|
| [ `GET /api/v1/sensorParserConfig/{name}`](#get-/api/v1/sensorparserconfig/{name})|
| [ `GET /api/v1/sensorParserConfigHistory`](#get-/api/v1/sensorparserconfighistory)|
| [ `GET /api/v1/sensorParserConfigHistory/history/{name}`](#get-/api/v1/sensorparserconfighistory/history/{name})|
| [ `GET /api/v1/sensorParserConfigHistory/{name}`](#get-/api/v1/sensorparserconfighistory/{name})|
| [ `GET /api/v1/storm`](#get-/api/v1/storm)|
| [ `GET /api/v1/storm/client/status`](#get-/api/v1/storm/client/status)|
| [ `GET /api/v1/storm/enrichment`](#get-/api/v1/storm/enrichment)|
| [ `GET /api/v1/storm/enrichment/activate`](#get-/api/v1/storm/enrichment/activate)|
| [ `GET /api/v1/storm/enrichment/deactivate`](#get-/api/v1/storm/enrichment/deactivate)|
| [ `GET /api/v1/storm/enrichment/start`](#get-/api/v1/storm/enrichment/start)|
| [ `GET /api/v1/storm/enrichment/stop`](#get-/api/v1/storm/enrichment/stop)|
| [ `GET /api/v1/storm/indexing`](#get-/api/v1/storm/indexing)|
| [ `GET /api/v1/storm/indexing/activate`](#get-/api/v1/storm/indexing/activate)|
| [ `GET /api/v1/storm/indexing/deactivate`](#get-/api/v1/storm/indexing/deactivate)|
| [ `GET /api/v1/storm/indexing/start`](#get-/api/v1/storm/indexing/start)|
| [ `GET /api/v1/storm/indexing/stop`](#get-/api/v1/storm/indexing/stop)|
| [ `GET /api/v1/storm/parser/activate/{name}`](#get-/api/v1/storm/parser/activate/{name})|
| [ `GET /api/v1/storm/parser/deactivate/{name}`](#get-/api/v1/storm/parser/deactivate/{name})|
| [ `GET /api/v1/storm/parser/start/{name}`](#get-/api/v1/storm/parser/start/{name})|
| [ `GET /api/v1/storm/parser/stop/{name}`](#get-/api/v1/storm/parser/stop/{name})|
| [ `GET /api/v1/storm/{name}`](#get-/api/v1/storm/{name})|
| [ `GET /api/v1/transformation/list`](#get-/api/v1/transformation/list)|
| [ `GET /api/v1/transformation/list/functions`](#get-/api/v1/transformation/list/functions)|
| [ `GET /api/v1/transformation/list/simple/functions`](#get-/api/v1/transformation/list/simple/functions)|
| [ `POST /api/v1/transformation/validate`](#post-/api/v1/transformation/validate)|
| [ `POST /api/v1/transformation/validate/rules`](#post-/api/v1/transformation/validate/rules)|
| [ `GET /api/v1/user`](#get-/api/v1/user)|

### `GET /api/v1/globalConfig`
  * Description: Retrieves the current Global Config from Zookeeper
  * Returns: Current Global Config JSON in Zookeeper

### `DELETE /api/v1/globalConfig`
  * Description: Deletes the current Global Config from Zookeeper
  * Returns: No return value

### `POST /api/v1/globalConfig`
  * Description: Creates or updates the Global Config in Zookeeper
  * Input:
    * globalConfig - The Global Config JSON to be saved
  * Returns: Saved Global Config JSON

### `GET /api/v1/grok/list`
  * Description: Lists the common Grok statements available in Metron
  * Returns: JSON object containing pattern label/Grok statements key value pairs

### `POST /api/v1/grok/validate`
  * Description: Applies a Grok statement to a sample message
  * Input:
    * grokValidation - Object containing Grok statment and sample message
  * Returns: JSON results

### `GET /api/v1/kafka/topic`
  * Description: Retrieves all Kafka topics
  * Returns: A list of all Kafka topics

### `POST /api/v1/kafka/topic`
  * Description: Creates a new Kafka topic
  * Input:
    * topic - Kafka topic
  * Returns: Saved Kafka topic

### `GET /api/v1/kafka/topic/{name}`
  * Description: Retrieves a Kafka topic
  * Input:
    * name - Kafka topic name
  * Returns: Kafka topic

### `DELETE /api/v1/kafka/topic/{name}`
  * Description: Delets a Kafka topic
  * Input:
    * name - Kafka topic name
  * Returns: No return value

### `GET /api/v1/kafka/topic/{name}/sample`
  * Description: Retrieves a sample message from a Kafka topic using the most recent offset
  * Input:
    * name - Kafka topic name
  * Returns: Sample message

### `GET /api/v1/sensorEnrichmentConfig`
  * Description: Retrieves all SensorEnrichmentConfigs from Zookeeper
  * Returns: All SensorEnrichmentConfigs

### `GET /api/v1/sensorEnrichmentConfig/list/available`
  * Description: Lists the available enrichments
  * Returns: List of available enrichments

### `DELETE /api/v1/sensorEnrichmentConfig/{name}`
  * Description: Deletes a SensorEnrichmentConfig from Zookeeper
  * Input:
    * name - SensorEnrichmentConfig name
  * Returns: No return value

### `POST /api/v1/sensorEnrichmentConfig/{name}`
  * Description: Updates or creates a SensorEnrichmentConfig in Zookeeper
  * Input:
    * sensorEnrichmentConfig - SensorEnrichmentConfig
    * name - SensorEnrichmentConfig name
  * Returns: Saved SensorEnrichmentConfig

### `GET /api/v1/sensorEnrichmentConfig/{name}`
  * Description: Retrieves a SensorEnrichmentConfig from Zookeeper
  * Input:
    * name - SensorEnrichmentConfig name
  * Returns: SensorEnrichmentConfig

### `POST /api/v1/sensorParserConfig`
  * Description: Updates or creates a SensorParserConfig in Zookeeper
  * Input:
    * sensorParserConfig - SensorParserConfig
  * Returns: Saved SensorParserConfig

### `GET /api/v1/sensorParserConfig`
  * Description: Retrieves all SensorParserConfigs from Zookeeper
  * Returns: All SensorParserConfigs

### `GET /api/v1/sensorParserConfig/list/available`
  * Description: Lists the available parser classes that can be found on the classpath
  * Returns: List of available parser classes

### `POST /api/v1/sensorParserConfig/parseMessage`
  * Description: Parses a sample message given a SensorParserConfig
  * Input:
    * parseMessageRequest - Object containing a sample message and SensorParserConfig
  * Returns: Parsed message

### `GET /api/v1/sensorParserConfig/reload/available`
  * Description: Scans the classpath for available parser classes and reloads the cached parser class list
  * Returns: List of available parser classes

### `DELETE /api/v1/sensorParserConfig/{name}`
  * Description: Deletes a SensorParserConfig from Zookeeper
  * Input:
    * name - SensorParserConfig name
  * Returns: No return value

### `GET /api/v1/sensorParserConfig/{name}`
  * Description: Retrieves a SensorParserConfig from Zookeeper
  * Input:
    * name - SensorParserConfig name
  * Returns: SensorParserConfig

### `GET /api/v1/sensorParserConfigHistory`
  * Description: Retrieves all current versions of SensorParserConfigs including audit information
  * Returns: SensorParserConfigs with audit information

### `GET /api/v1/sensorParserConfigHistory/history/{name}`
  * Description: Retrieves the history of all changes made to a SensorParserConfig
  * Input:
    * name - SensorParserConfig name
  * Returns: SensorParserConfig history

### `GET /api/v1/sensorParserConfigHistory/{name}`
  * Description: Retrieves the current version of a SensorParserConfig including audit information
  * Input:
    * name - SensorParserConfig name
  * Returns: SensorParserConfig with audit information

### `GET /api/v1/storm`
  * Description: Retrieves the status of all Storm topologies
  * Returns: List of topologies with status information

### `GET /api/v1/storm/client/status`
  * Description: Retrieves information about the Storm command line client
  * Returns: Storm command line client information

### `GET /api/v1/storm/enrichment`
  * Description: Retrieves the status of the Storm enrichment topology
  * Returns: Topology status information

### `GET /api/v1/storm/enrichment/activate`
  * Description: Activates a Storm enrichment topology
  * Returns: Activate response message

### `GET /api/v1/storm/enrichment/deactivate`
  * Description: Deactivates a Storm enrichment topology
  * Returns: Deactivate response message

### `GET /api/v1/storm/enrichment/start`
  * Description: Starts a Storm enrichment topology
  * Returns: Start response message

### `GET /api/v1/storm/enrichment/stop`
  * Description: Stops a Storm enrichment topology
  * Input:
    * stopNow - Stop the topology immediately
  * Returns: Stop response message

### `GET /api/v1/storm/indexing`
  * Description: Retrieves the status of the Storm indexing topology
  * Returns: Topology status information

### `GET /api/v1/storm/indexing/activate`
  * Description: Activates a Storm indexing topology
  * Returns: Activate response message

### `GET /api/v1/storm/indexing/deactivate`
  * Description: Deactivates a Storm indexing topology
  * Returns: Deactivate response message

### `GET /api/v1/storm/indexing/start`
  * Description: Starts a Storm indexing topology
  * Returns: Start response message

### `GET /api/v1/storm/indexing/stop`
  * Description: Stops a Storm enrichment topology
  * Input:
    * stopNow - Stop the topology immediately
  * Returns: Stop response message

### `GET /api/v1/storm/parser/activate/{name}`
  * Description: Activates a Storm parser topology
  * Input:
    * name - Parser name
  * Returns: Activate response message

### `GET /api/v1/storm/parser/deactivate/{name}`
  * Description: Deactivates a Storm parser topology
  * Input:
    * name - Parser name
  * Returns: Deactivate response message

### `GET /api/v1/storm/parser/start/{name}`
  * Description: Starts a Storm parser topology
  * Input:
    * name - Parser name
  * Returns: Start response message

### `GET /api/v1/storm/parser/stop/{name}`
  * Description: Stops a Storm parser topology
  * Input:
    * name - Parser name
    * stopNow - Stop the topology immediately
  * Returns: Stop response message

### `GET /api/v1/storm/{name}`
  * Description: Retrieves the status of a Storm topology
  * Input:
    * name - Topology name
  * Returns: Topology status information

### `GET /api/v1/transformation/list`
  * Description: Retrieves field transformations
  * Returns: List field transformations

### `GET /api/v1/transformation/list/functions`
  * Description: Lists the Stellar functions that can be found on the classpath
  * Returns: List of Stellar functions

### `GET /api/v1/transformation/list/simple/functions`
  * Description: Lists the simple Stellar functions (functions with only 1 input) that can be found on the classpath
  * Returns: List of simple Stellar functions

### `POST /api/v1/transformation/validate`
  * Description: Executes transformations against a sample message
  * Input:
    * transformationValidation - Object containing SensorParserConfig and sample message
  * Returns: Transformation results

### `POST /api/v1/transformation/validate/rules`
  * Description: Tests Stellar statements to ensure they are well-formed
  * Input:
    * statements - List of statements to validate
  * Returns: Validation results

### `GET /api/v1/user`
  * Description: Retrieves the current user
  * Returns: Current user


## License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.
