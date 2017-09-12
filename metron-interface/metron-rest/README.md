# Metron REST

This module provides a RESTful API for interacting with Metron.

## Prerequisites

* A running Metron cluster
* Java 8 installed
* Storm CLI and Metron topology scripts (start_parser_topology.sh, start_enrichment_topology.sh, start_elasticsearch_topology.sh) installed
* A relational database

## Installation

### From Source

1. Package the application with Maven:
  ```
mvn clean package
  ```

1. Untar the archive in the $METRON_HOME directory.  The directory structure will look like:
  ```
config
  rest_application.yml
bin
  metron-rest
lib
  metron-rest-$METRON_VERSION.jar
  ```

1. Copy the `$METRON_HOME/bin/metron-rest` script to `/etc/init.d/metron-rest`

### From Package Manager

1. Deploy the RPM at `/metron/metron-deployment/packaging/docker/rpm-docker/target/RPMS/noarch/metron-rest-$METRON_VERSION-*.noarch.rpm`

1. Install the RPM with:
  ```
rpm -ih metron-rest-$METRON_VERSION-*.noarch.rpm
  ```

## Configuration

The REST application depends on several configuration parameters:

### REQUIRED
No optional parameter has a default.

| Environment Variable                  | Description
| ------------------------------------- | -----------
| METRON_JDBC_DRIVER                    | JDBC driver class
| METRON_JDBC_URL                       | JDBC url
| METRON_JDBC_USERNAME                  | JDBC username
| METRON_JDBC_PLATFORM                  | JDBC platform (one of h2, mysql, postgres, oracle
| ZOOKEEPER                             | Zookeeper quorum (ex. node1:2181,node2:2181)
| BROKERLIST                            | Kafka Broker list (ex. node1:6667,node2:6667)
| HDFS_URL                              | HDFS url or `fs.defaultFS` Hadoop setting (ex. hdfs://node1:8020)

### Optional - With Defaults
| Environment Variable                  | Description                                                       | Required | Default
| ------------------------------------- | ----------------------------------------------------------------- | -------- | -------
| METRON_USER                           | Run the application as this user                                  | Optional | metron
| METRON_LOG_DIR                        | Directory where the log file is written                           | Optional | /var/log/metron/
| METRON_PID_DIR                        | Directory where the pid file is written                           | Optional | /var/run/metron/
| METRON_REST_PORT                      | REST application port                                             | Optional | 8082
| METRON_JDBC_CLIENT_PATH               | Path to JDBC client jar                                           | Optional | H2 is bundled
| METRON_TEMP_GROK_PATH                 | Temporary directory used to test grok statements                  | Optional | ./patterns/temp
| METRON_DEFAULT_GROK_PATH              | Defaults HDFS directory used to store grok statements             | Optional | /apps/metron/patterns
| SECURITY_ENABLED                      | Enables Kerberos support                                          | Optional | false

### Optional - Blank Defaults
| Environment Variable                  | Description                                                       | Required
| ------------------------------------- | ----------------------------------------------------------------- | --------
| METRON_JVMFLAGS                       | JVM flags added to the start command                              | Optional
| METRON_SPRING_PROFILES_ACTIVE         | Active Spring profiles (see [below](#spring-profiles))            | Optional
| METRON_SPRING_OPTIONS                 | Additional Spring input parameters                                | Optional
| METRON_PRINCIPAL_NAME                 | Kerberos principal for the metron user                            | Optional
| METRON_SERVICE_KEYTAB                 | Path to the Kerberos keytab for the metron user                   | Optional

These are set in the `/etc/sysconfig/metron` file.

## Database setup

The REST application persists data in a relational database and requires a dedicated database user and database (see https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html for more detail).

### Development

The REST application comes with embedded database support for development purposes (https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html#boot-features-embedded-database-support).

For example, edit these variables in `/etc/sysconfig/metron` before starting the application to configure H2:
```
METRON_JDBC_DRIVER="org.h2.Driver"
METRON_JDBC_URL="jdbc:h2:file:~/metrondb"
METRON_JDBC_USERNAME="root"
METRON_JDBC_PASSWORD='root"
METRON_JDBC_PLATFORM="h2"
```

### Production

The REST application should be configured with a production-grade database outside of development.

For example, the following configures the application for MySQL:

1. Install MySQL if not already available (this example uses version 5.7, installation instructions can be found [here](https://dev.mysql.com/doc/refman/5.7/en/linux-installation-yum-repo.html))

1. Create a metron user and REST database and permission the user for that database:
  ```
CREATE USER 'metron'@'node1' IDENTIFIED BY 'Myp@ssw0rd';
CREATE DATABASE IF NOT EXISTS metronrest;
GRANT ALL PRIVILEGES ON metronrest.* TO 'metron'@'node1';
  ```

1. Install the MySQL JDBC client onto the REST application host and configurate the METRON_JDBC_CLIENT_PATH variable:
  ```
cd $METRON_HOME/lib
wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.41.tar.gz
tar xf mysql-connector-java-5.1.41.tar.gz
  ```

1. Edit these variables in `/etc/sysconfig/metron` to configure the REST application for MySQL:
  ```
METRON_JDBC_DRIVER="com.mysql.jdbc.Driver"
METRON_JDBC_URL="jdbc:mysql://mysql_host:3306/metronrest"
METRON_JDBC_USERNAME="metron"
METRON_JDBC_PLATFORM="mysql"
METRON_JDBC_CLIENT_PATH=$METRON_HOME/lib/mysql-connector-java-5.1.41/mysql-connector-java-5.1.41-bin.jar
  ```

## Usage

After configuration is complete, the REST application can be managed as a service:
```
service metron-rest start
```

If a production database is configured, the JDBC password should be passed in as the first argument on startup:
```
service metron-rest start Myp@ssw0rd
```

The REST application can be accessed with the Swagger UI at http://host:port/swagger-ui.html#/.  The default port is 8082.

## Security

### Authentication

The metron-rest module uses [Spring Security](http://projects.spring.io/spring-security/) for authentication and stores user credentials in the relational database configured above.  The required tables are created automatically the first time the application is started so that should be done first.  For example (continuing the MySQL example above), users can be added by connecting to MySQL and running:
```
use metronrest;
insert into users (username, password, enabled) values ('your_username','your_password',1);
insert into authorities (username, authority) values ('your_username', 'ROLE_USER');
```

### Kerberos

Metron REST can be configured for a cluster with Kerberos enabled.  A client JAAS file is required for Kafka and Zookeeper and a Kerberos keytab for the metron user principal is required for all other services.  Configure these settings in the `/etc/sysconfig/metron` file:
```
SECURITY_ENABLED=true
METRON_JVMFLAGS="-Djava.security.auth.login.config=$METRON_HOME/client_jaas.conf"
METRON_PRINCIPAL_NAME="metron@EXAMPLE.COM"
METRON_SERVICE_KEYTAB="/etc/security/keytabs/metron.keytab"
```

## Spring Profiles

The REST application comes with a few [Spring Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html) to aid in testing and development.

| Profile                  | Description                                   |
| ------------------------ | --------------------------------------------- |
| test                     | sets variables to in-memory services, only used for integration testing |
| dev                      | adds a test user to the database with credentials `user/password`       |
| vagrant                  | sets configuration variables to match the Metron vagrant environment    |
| docker                   | sets configuration variables to match the Metron docker environment     |

Setting active profiles is done with the METRON_SPRING_PROFILES_ACTIVE variable.  For example, set this variable in `/etc/sysconfig/metron` to configure the REST application for the Vagrant environment and add a test user:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
```

## API

Request and Response objects are JSON formatted.  The JSON schemas are available in the Swagger UI.

|            |
| ---------- |
| [ `POST /api/v1/alert/escalate`](#get-apiv1alertescalate)|
| [ `GET /api/v1/global/config`](#get-apiv1globalconfig)|
| [ `DELETE /api/v1/global/config`](#delete-apiv1globalconfig)|
| [ `POST /api/v1/global/config`](#post-apiv1globalconfig)|
| [ `GET /api/v1/grok/get/statement`](#get-apiv1grokgetstatement)|
| [ `GET /api/v1/grok/list`](#get-apiv1groklist)|
| [ `POST /api/v1/grok/validate`](#post-apiv1grokvalidate)|
| [ `POST /api/v1/hdfs`](#post-apiv1hdfs)|
| [ `GET /api/v1/hdfs`](#get-apiv1hdfs)|
| [ `DELETE /api/v1/hdfs`](#delete-apiv1hdfs)|
| [ `GET /api/v1/hdfs/list`](#get-apiv1hdfslist)|
| [ `GET /api/v1/kafka/topic`](#get-apiv1kafkatopic)|
| [ `POST /api/v1/kafka/topic`](#post-apiv1kafkatopic)|
| [ `GET /api/v1/kafka/topic/{name}`](#get-apiv1kafkatopicname)|
| [ `DELETE /api/v1/kafka/topic/{name}`](#delete-apiv1kafkatopicname)|
| [ `GET /api/v1/kafka/topic/{name}/sample`](#get-apiv1kafkatopicnamesample)|
| [ `POST /api/v1/search/search`](#get-apiv1searchsearch)|
| [ `POST /api/v1/search/group`](#get-apiv1searchgroup)|
| [ `GET /api/v1/search/findOne`](#get-apiv1searchfindone)|
| [ `GET /api/v1/search/column/metadata`](#get-apiv1searchcolumnmetadata)|
| [ `GET /api/v1/search/column/metadata/common`](#get-apiv1searchcolumnmetadatacommon)|
| [ `GET /api/v1/sensor/enrichment/config`](#get-apiv1sensorenrichmentconfig)|
| [ `GET /api/v1/sensor/enrichment/config/list/available/enrichments`](#get-apiv1sensorenrichmentconfiglistavailableenrichments)|
| [ `GET /api/v1/sensor/enrichment/config/list/available/threat/triage/aggregators`](#get-apiv1sensorenrichmentconfiglistavailablethreattriageaggregators)|
| [ `DELETE /api/v1/sensor/enrichment/config/{name}`](#delete-apiv1sensorenrichmentconfigname)|
| [ `POST /api/v1/sensor/enrichment/config/{name}`](#post-apiv1sensorenrichmentconfigname)|
| [ `GET /api/v1/sensor/enrichment/config/{name}`](#get-apiv1sensorenrichmentconfigname)|
| [ `GET /api/v1/sensor/indexing/config`](#get-apiv1sensorindexingconfig)|
| [ `DELETE /api/v1/sensor/indexing/config/{name}`](#delete-apiv1sensorindexingconfigname)|
| [ `POST /api/v1/sensor/indexing/config/{name}`](#post-apiv1sensorindexingconfigname)|
| [ `GET /api/v1/sensor/indexing/config/{name}`](#get-apiv1sensorindexingconfigname)|
| [ `POST /api/v1/sensor/parser/config`](#post-apiv1sensorparserconfig)|
| [ `GET /api/v1/sensor/parser/config`](#get-apiv1sensorparserconfig)|
| [ `GET /api/v1/sensor/parser/config/list/available`](#get-apiv1sensorparserconfiglistavailable)|
| [ `POST /api/v1/sensor/parser/config/parseMessage`](#post-apiv1sensorparserconfigparsemessage)|
| [ `GET /api/v1/sensor/parser/config/reload/available`](#get-apiv1sensorparserconfigreloadavailable)|
| [ `DELETE /api/v1/sensor/parser/config/{name}`](#delete-apiv1sensorparserconfigname)|
| [ `GET /api/v1/sensor/parser/config/{name}`](#get-apiv1sensorparserconfigname)|
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
| [ `GET /api/v1/storm/parser/activate/{name}`](#get-apiv1stormparseractivatename)|
| [ `GET /api/v1/storm/parser/deactivate/{name}`](#get-apiv1stormparserdeactivatename)|
| [ `GET /api/v1/storm/parser/start/{name}`](#get-apiv1stormparserstartname)|
| [ `GET /api/v1/storm/parser/stop/{name}`](#get-apiv1stormparserstopname)|
| [ `GET /api/v1/storm/{name}`](#get-apiv1stormname)|
| [ `GET /api/v1/storm/supervisors`](#get-apiv1stormsupervisors)|
| [ `PATCH /api/v1/update/patch`](#patch-apiv1updatepatch)|
| [ `PUT /api/v1/update/replace`](#patch-apiv1updatereplace)|
| [ `GET /api/v1/user`](#get-apiv1user)|

### `POST /api/v1/alert/escalate`
  * Description: Escalates a list of alerts by producing it to the Kafka escalate topic
  * Input:
    * alerts - The alerts to be escalated
  * Returns:
    * 200 - Alerts were escalated

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

### `GET /api/v1/grok/get/statement`
  * Description: Retrieves a Grok statement from the classpath
  * Input:
    * path - Path to classpath resource
  * Returns:
    * 200 - Grok statement

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
  * Description: Writes contents to an HDFS file.  Warning: this will overwrite the contents of a file if it already exists. Permissions must be set for all three groups if they are to be set. If any are missing, the default permissions will be used, and if any are invalid an exception will be thrown.
  * Input:
    * path - Path to HDFS file
    * contents - File contents
    * userMode - [optional] symbolic permission string for user portion of the permissions to be set on the file written. For example 'rwx' or read, write, execute. The symbol '-' is used to exclude that permission such as 'rw-' for read, write, no execute
    * groupMode - [optional] symbolic permission string for group portion of the permissions to be set on the file written. For example 'rwx' or read, write, execute. The symbol '-' is used to exclude that permission such as 'rw-' for read, write, no execute
    * otherMode - [optional] symbolic permission string for other portion of the permissions to be set on the file written. For example 'rwx' or read, write, execute. The symbol '-' is used to exclude that permission such as 'rw-' for read, write, no execute
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
  * Description: Lists an HDFS directory
  * Input:
    * path - Path to HDFS directory
  * Returns:
    * 200 - HDFS directory list

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
  * Description: Deletes a Kafka topic
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

### `POST /api/v1/search/search`
  * Description: Searches the indexing store
  * Input:
      * searchRequest - Search request
  * Returns:
    * 200 - Search response
    
### `POST /api/v1/search/group`
  * Description: Searches the indexing store and returns field groups. Groups are hierarchical and nested in the order the fields appear in the 'groups' request parameter. The default sorting within groups is by count descending.  A groupOrder type of count will sort based on then number of documents in a group while a groupType of term will sort by the groupBy term.
  * Input:
      * groupRequest - Group request
        * indices - list of indices to search
        * query - lucene query
        * scoreField - field used to compute a total score for each group
        * groups - List of groups (field name and sort order) 
  * Returns:
    * 200 - Group response
    
### `GET /api/v1/search/findOne`
  * Description: Returns latest document for a guid and sensor
  * Input:
      * getRequest - Get request
        * guid - message UUID
        * sensorType - Sensor Type
      * Example: Return `bro` document with UUID of `000-000-0000`
```
{
  "guid" : "000-000-0000",
  "sensorType" : "bro"
}
```
  * Returns:
    * 200 - Document representing the output
    * 404 - Document with UUID and sensor type not found
    
### `GET /api/v1/search/column/metadata`
  * Description: Get column metadata for each index in the list of indicies
  * Input:
      * indices - Indices
  * Returns:
    * 200 - Column Metadata
    
### `GET /api/v1/search/column/metadata/common`
  * Description: Get metadata for columns shared by the list of indices
  * Input:
      * indices - Indices
  * Returns:
    * 200 - Common Column Metadata

### `GET /api/v1/sensor/enrichment/config`
  * Description: Retrieves all SensorEnrichmentConfigs from Zookeeper
  * Returns:
    * 200 - Returns all SensorEnrichmentConfigs

### `GET /api/v1/sensor/enrichment/config/list/available/enrichments`
  * Description: Lists the available enrichments
  * Returns:
    * 200 - Returns a list of available enrichments

### `GET /api/v1/sensor/enrichment/config/list/available/threat/triage/aggregators`
  * Description: Lists the available threat triage aggregators
  * Returns:
    * 200 - Returns a list of available threat triage aggregators

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

### `GET /api/v1/storm/supervisors`
  * Description: Retrieves the status of all Storm Supervisors
  * Returns:
    * 200 - Returns a list of the status of all Storm Supervisors 

### `PATCH /api/v1/update/patch`
  * Description: Update a document with a patch
  * Input:
    * request - Patch Request
      * guid - The Patch UUID
      * sensorType - The sensor type
      * patch - An array of [RFC 6902](https://tools.ietf.org/html/rfc6902) patches.
    * Example adding a field called `project` with value `metron` to the `bro` message with UUID of `000-000-0000` :
  ```
  {
     "guid" : "000-000-0000",
     "sensorType" : "bro",
     "patch" : [
      {
                "op": "add"
               , "path": "/project"
               , "value": "metron"
      }
              ]
   }
  ```
  * Returns:
    * 200 - nothing
    * 404 - document not found

### `PUT /api/v1/update/replace`
  * Description: Replace a document
  * Input:
    * request - Replacement request
      * guid - The Patch UUID
      * sensorType - The sensor type
      * replacement - A Map representing the replaced document
    * Example replacing a `bro` message with guid of `000-000-0000`
```
   {
     "guid" : "000-000-0000",
     "sensorType" : "bro",
     "replacement" : {
       "source:type": "bro",
       "guid" : "bro_index_2017.01.01.01:1",
       "ip_src_addr":"192.168.1.2",
       "ip_src_port": 8009,
       "timestamp":200,
       "rejected":false
      }
   }
```
  * Returns:
    * 200 - Current user

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

To run the application locally on the Quick Dev host (node1), follow the [Installation](#installation) instructions above.  Then set the METRON_SPRING_PROFILES_ACTIVE variable in `/etc/sysconfig/metron`:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
```

and start the application:
```
service metron-rest start
```

In a cluster with Kerberos enabled, update the security settings in `/etc/sysconfig/metron`.  Security is disabled by default in the `vagrant` Spring profile so that setting must be overriden with the METRON_SPRING_OPTIONS variable:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
METRON_JVMFLAGS="-Djava.security.auth.login.config=$METRON_HOME/client_jaas.conf"
METRON_SPRING_OPTIONS="--kerberos.enabled=true"
```

The metron-rest application will be available at http://node1:8082/swagger-ui.html#/.

## License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.
