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
# Metron REST

This module provides a RESTful API for interacting with Metron.

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Configuration](#configuration)
* [Usage](#usage)
* [Security](#security)
* [API](#api)
* [Testing](#testing)

## Prerequisites

* A running Metron cluster
* A running real-time store, either Elasticsearch or Solr depending on which one is enabled
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
| ZOOKEEPER                             | Zookeeper quorum (ex. node1:2181,node2:2181)
| BROKERLIST                            | Kafka Broker list (ex. node1:6667,node2:6667)
| HDFS_URL                              | HDFS url or `fs.defaultFS` Hadoop setting (ex. hdfs://node1:8020)

### Optional - With Defaults
| Environment Variable                  | Description                                                                          | Required | Default
| ------------------------------------- | ------------------------------------------------------------------------------------ | -------- | -------
| METRON_LOG_DIR                        | Directory where the log file is written                                              | Optional | /var/log/metron/
| METRON_PID_FILE                       | File where the pid is written                                                        | Optional | /var/run/metron/
| METRON_REST_PORT                      | REST application port                                                                | Optional | 8082
| METRON_JDBC_CLIENT_PATH               | Path to JDBC client jar                                                              | Optional | H2 is bundled
| METRON_TEMP_GROK_PATH                 | Temporary directory used to test grok statements                                     | Optional | ./patterns/temp
| METRON_DEFAULT_GROK_PATH              | Defaults HDFS directory used to store grok statements                                | Optional | /apps/metron/patterns
| SECURITY_ENABLED                      | Enables Kerberos support                                                             | Optional | false
| METRON_USER_ROLE                      | Name of the role at the authentication provider that provides user access to Metron. | Optional | USER
| METRON_ADMIN_ROLE                     | Name of the role at the authentication provider that provides administrative access to Metron.| Optional | ADMIN

### Optional - Blank Defaults
| Environment Variable                  | Description                                                       | Required
| ------------------------------------- | ----------------------------------------------------------------- | --------
| METRON_JDBC_DRIVER                    | JDBC driver class                                                 | Optional
| METRON_JDBC_URL                       | JDBC url                                                          | Optional
| METRON_JDBC_USERNAME                  | JDBC username                                                     | Optional
| METRON_JDBC_PLATFORM                  | JDBC platform (one of h2, mysql, postgres, oracle)                | Optional
| METRON_JVMFLAGS                       | JVM flags added to the start command                              | Optional
| METRON_SPRING_PROFILES_ACTIVE         | Active Spring profiles (see [below](#spring-profiles))            | Optional
| METRON_SPRING_OPTIONS                 | Additional Spring input parameters                                | Optional
| METRON_PRINCIPAL_NAME                 | Kerberos principal for the metron user                            | Optional
| METRON_SERVICE_KEYTAB                 | Path to the Kerberos keytab for the metron user                   | Optional

These are set in the `/etc/default/metron` file.

## Usage

The REST application can be accessed with the Swagger UI at http://host:port/swagger-ui.html#/.  The default port is 8082.

### Logging

Logging for the REST application can be configured in Ambari.  Log levels can be changed at the root, package and class level:

1. Navigate to Services > Metron > Configs > REST and locate the `Metron Spring options` setting.

1. Logging configuration is exposed through Spring properties as explained [here](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html#howto-logging).

1. The root logging level defaults to ERROR but can be changed to INFO by adding `--logging.level.root=INFO` to the `Metron Spring options` setting.

1. The Metron REST logging level can be changed to INFO by adding `--logging.level.org.apache.metron.rest=INFO`.

1. HTTP request and response logging can be enabled by adding `--logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG --logging.level.org.apache.metron.rest.web.filter.ResponseLoggingFilter=DEBUG`.

### Spring Profiles

The REST application comes with a few [Spring Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html) to aid in testing and development.

| Profile                  | Description                                   |
| ------------------------ | --------------------------------------------- |
| test                     | adds test users `[user, user1, user2, admin]` to the database with password "`password`". sets variables to in-memory services, only used for integration testing |
| dev                      | adds test users `[user, user1, user2, admin]` to the database with password "`password`" |
| vagrant                  | sets configuration variables to match the Metron vagrant environment |
| docker                   | sets configuration variables to match the Metron docker environment |

Setting active profiles is done with the METRON_SPRING_PROFILES_ACTIVE variable.  For example, set this variable in `/etc/default/metron` to configure the REST application for the Vagrant environment and add a test user:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
```

## Security

* [Kerberos](#kerberos)
* [LDAP Authentication](#ldap-authentication)
* [JDBC Authentication](#jdbc-authentication)

### Kerberos

Metron REST can be configured for a cluster with Kerberos enabled.  A client JAAS file is required for Kafka and Zookeeper and a Kerberos keytab for the metron user principal is required for all other services.  Configure these settings in the `/etc/default/metron` file:
```
SECURITY_ENABLED=true
METRON_JVMFLAGS="-Djava.security.auth.login.config=$METRON_HOME/client_jaas.conf"
METRON_PRINCIPAL_NAME="metron@EXAMPLE.COM"
METRON_SERVICE_KEYTAB="/etc/security/keytabs/metron.keytab"
```

### LDAP Authentication

Metron REST can be configured to use LDAP for authentication and roles. Use the following steps to enable LDAP.

1. In Ambari, go to Metron > Config > Security > Roles

    * Set "User Role Name" to the name of the role at the authentication provider that provides user level access to Metron.

    * Set "Admin Role Name" to the name of the role at the authentication provider that provides administrative access to Metron.

1. In Ambari, go to Metron > Config > Security > LDAP

    * Turn on LDAP using the toggle.

    * Set "LDAP URL" to your LDAP instance. For example, `ldap://<host>:<port>`.

    * Set "Bind User" to the name of the bind user.  For example, `cn=admin,dc=apache,dc=org`.

    * Set the "Bind User Password"

    * Other fields may be required depending on your LDAP configuration.

1. Save the changes and restart the required services.

By default, configuration will default to matching Knox's Demo LDAP for convenience. This should only be used for development purposes. Manual instructions for setting up demo LDAP and finalizing configuration (e.g. setting up the user LDIF file) can be found in the [Development README](../../metron-deployment/development/README.md#knox-demo-ldap).

#### LDAPS

There is configuration to provide a path to a truststore with SSL certificates and provide a password. Users should import certificates as needed to appropriate truststores.  An example of doing this is:
```
keytool -import -alias <alias> -file <certificate> -keystore <keystore_file> -storepass <password>
```

#### Roles

Roles used by Metron are `ROLE_ADMIN` and `ROLE_USER`. Metron will use a property in a group containing the appropriate role to construct this.

Metron can be configured to map the roles defined in your authorization provider to the authorities used internally for access control.  This can be configured under Security > Roles in Ambari.

For example, our ldif file could create this group:
```
dn: cn=admin,ou=groups,dc=hadoop,dc=apache,dc=org
objectclass:top
objectclass: groupofnames
cn: admin
description:admin group
member: uid=admin,ou=people,dc=hadoop,dc=apache,dc=org
```

If we are using "cn" as our role attribute, Metron will give the "admin" user the role "ROLE_ADMIN".

Similarly, we could give a user "sam" ROLE_USER with the following group:
```
dn: cn=user,ou=groups,dc=hadoop,dc=apache,dc=org
objectclass:top
objectclass: groupofnames
cn: user
description: user group
member: uid=sam,ou=people,dc=hadoop,dc=apache,dc=org
```

### JDBC Authentication

The REST application persists data in a relational database and requires a dedicated database user and database (see https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html for more detail).  
Spring uses Hibernate as the default ORM framework but another framework is needed becaused Hibernate is not compatible with the Apache 2 license.  For this reason Metron uses [EclipseLink](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html#boot-features-embedded-database-support).  See the [Spring Data JPA - EclipseLink](https://github.com/spring-projects/spring-data-examples/tree/master/jpa/eclipselink) project for an example on how to configure EclipseLink in Spring.

The metron-rest module uses [Spring Security](http://projects.spring.io/spring-security/) for authentication and stores user credentials in the relational database configured above.  The required tables are created automatically the first time the application is started so that should be done first.  For example (continuing the MySQL example above), users can be added by connecting to MySQL and running:
```
use metronrest;
insert into users (username, password, enabled) values ('your_username','your_password',1);
insert into authorities (username, authority) values ('your_username', 'ROLE_USER');
```

### Development

The REST application comes with [embedded database support](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html#boot-features-embedded-database-support) for development purposes.

For example, edit these variables in `/etc/default/metron` before starting the application to configure H2:
```
METRON_JDBC_DRIVER="org.h2.Driver"
METRON_JDBC_URL="jdbc:h2:file:~/metrondb"
METRON_JDBC_USERNAME="root"
METRON_JDBC_PLATFORM="h2"
```

### Production

The REST application should be configured with a production-grade database outside of development.

#### Ambari Install

Installing with Ambari is recommended for production deployments.
Ambari handles setup, configuration, and management of the REST component.
This includes managing the PID file, directing logging, etc.

#### Manual Install

The following configures the application for MySQL:

1. Install MySQL if not already available (this example uses version 5.7, installation instructions can be found [here](https://dev.mysql.com/doc/refman/5.7/en/linux-installation-yum-repo.html))

1. Create a metron user and REST database and permission the user for that database:
    ```
    CREATE USER 'metron'@'node1' IDENTIFIED BY 'Myp@ssw0rd';
    CREATE DATABASE IF NOT EXISTS metronrest;
    GRANT ALL PRIVILEGES ON metronrest.* TO 'metron'@'node1';
    ```

1. Create the security tables as described in the [Spring Security Guide](https://docs.spring.io/spring-security/site/docs/5.0.4.RELEASE/reference/htmlsingle/#user-schema).

1. Install the MySQL JDBC client onto the REST application host and configurate the METRON_JDBC_CLIENT_PATH variable:
    ```
    cd $METRON_HOME/lib
    wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.41.tar.gz
    tar xf mysql-connector-java-5.1.41.tar.gz
    ```

1. Edit these variables in `/etc/default/metron` to configure the REST application for MySQL:
    ```
    METRON_JDBC_DRIVER="com.mysql.jdbc.Driver"
    METRON_JDBC_URL="jdbc:mysql://mysql_host:3306/metronrest"
    METRON_JDBC_USERNAME="metron"
    METRON_JDBC_PLATFORM="mysql"
    METRON_JDBC_CLIENT_PATH=$METRON_HOME/lib/mysql-connector-java-5.1.41/mysql-connector-java-5.1.41-bin.jar
    ```

1. Switch to the metron user
    ```
    sudo su - metron
    ```

1. Start the REST API. Adjust the password as necessary.
    ```
    set -o allexport;
    source /etc/default/metron;
    set +o allexport;
    export METRON_JDBC_PASSWORD='Myp@ssw0rd';
    $METRON_HOME/bin/metron-rest.sh
    unset METRON_JDBC_PASSWORD;
    ```


## Pcap Query

The REST application exposes endpoints for querying Pcap data.  For more information about filtering options see [Query Filter Utility](../../metron-platform/metron-pcap-backend#query-filter-utility).

There is an endpoint available that will return Pcap data in [PDML](https://wiki.wireshark.org/PDML) format.  [Wireshark](https://www.wireshark.org/) must be installed for this feature to work.
Installing wireshark in CentOS can be done with `yum -y install wireshark`.

The REST application uses a Java Process object to call out to the `pcap_to_pdml.sh` script.  This script is installed at `$METRON_HOME/bin/pcap_to_pdml.sh` by default.
Out of the box it is a simple wrapper around the tshark command to transform raw pcap data to PDML.  However it can be extended to do additional processing as long as the expected input/output is maintained.
REST will supply the script with raw pcap data through standard in and expects PDML data serialized as XML.

Pcap query jobs can be configured for submission to a YARN queue.  This setting is exposed as the Spring property `pcap.yarn.queue` and can be set in the PCAP tab under Metron service -> Configs in Ambari.  If configured, the REST application will set the `mapreduce.job.queuename` Hadoop property to that value.
It is highly recommended that a dedicated YARN queue be created and configured for Pcap queries to prevent a job from consuming too many cluster resources.  More information about setting up YARN queues can be found [here](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/CapacityScheduler.html#Setting_up_queues).

Pcap query results are stored in HDFS.  The location of query results when run through the REST app is determined by a couple factors.  The root of Pcap query results defaults to `/apps/metron/pcap/output` but can be changed with the
Spring property `pcap.final.output.path`.  Assuming the default Pcap query output directory, the path to a result page will follow this pattern:
```
/apps/metron/pcap/output/{username}/MAP_REDUCE/{job id}/page-{page number}.pcap
```
Over time Pcap query results will accumulate in HDFS.  Currently these results are not cleaned up automatically so cluster administrators should be aware of this and monitor them.  It is highly recommended that a process be put in place to
periodically delete files and directories under the Pcap query results root.

Users should also be mindful of date ranges used in queries so they don't produce result sets that are too large.  Currently there are no limits enforced on date ranges.

Queries can also be configured on a global level for setting the number of results per page via a Spring property `pcap.page.size`. This property can be set in the PCAP tab under Metron service -> Configs, in Ambari. By default, this value is set to 10 pcaps per page, but you may choose to set this value higher
based on observing frequenetly-run query result sizes. This setting works in conjunction with the property for setting finalizer threadpool size when optimizing query performance.

Pcap query jobs have a finalization routine that writes their results out to HDFS in pages. Depending on the size of your pcaps, the number or results typically returned, page sizing (described above), and available CPU cores for running
your REST application, your performance can be improved by adjusting the number of files that can be written to HDFS in parallel. To this end, there is a threadpool used for this finalization step that can be configured to use a specified
number of threads. This setting is exposed as the Spring property `pcap.finalizer.threadpool.size`. A default value of "1" is used if not specified by the user. Generally speaking, you should see a performance gain when this value is set
to anything higher than 1. A sizeable increase in performance can be achieved, especially for larger numbers of files of smaller size, by increasing the number of threads. It should be noted that this property is parsed as a String to allow
for more complex parallelism values. In addition to normal integer values, you can specify a multiple of the number of cores. If it's a string and ends with "C", then strip the C and treat it as an integral multiple of the number of cores.
If it's a string and does not end with a C, then treat it as a number in string form.

## API

Request and Response objects are JSON formatted.  The JSON schemas are available in the Swagger UI.

|            |
| ---------- |
| [ `POST /api/v1/alerts/ui/escalate`](#post-apiv1alertsuiescalate)|
| [ `GET /api/v1/alerts/ui/settings`](#get-apiv1alertsuisettings)|
| [ `GET /api/v1/alerts/ui/settings/all`](#get-apiv1alertsuisettingsall)|
| [ `DELETE /api/v1/alerts/ui/settings`](#delete-apiv1alertsuisettings)|
| [ `POST /api/v1/alerts/ui/settings`](#post-apiv1alertsuisettings)|
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
| [ `POST /api/v1/kafka/topic/{name}/produce`](#post-apiv1kafkatopicnameproduce)|
| [ `GET /api/v1/metaalert/searchByAlert`](#get-apiv1metaalertsearchbyalert)|
| [ `GET /api/v1/metaalert/create`](#get-apiv1metaalertcreate)|
| [ `GET /api/v1/metaalert/add/alert`](#get-apiv1metaalertaddalert)|
| [ `GET /api/v1/metaalert/remove/alert`](#get-apiv1metaalertremovealert)|
| [ `GET /api/v1/metaalert/update/status/{guid}/{status}`](#get-apiv1metaalertupdatestatusguidstatus)|
| [ `POST /api/v1/pcap/fixed`](#post-apiv1pcapfixed)|
| [ `POST /api/v1/pcap/query`](#post-apiv1pcapquery)|
| [ `GET /api/v1/pcap`](#get-apiv1pcap)|
| [ `GET /api/v1/pcap/{jobId}`](#get-apiv1pcapjobid)|
| [ `GET /api/v1/pcap/{jobId}/pdml`](#get-apiv1pcapjobidpdml)|
| [ `GET /api/v1/pcap/{jobId}/raw`](#get-apiv1pcapjobidraw)|
| [ `DELETE /api/v1/pcap/kill/{jobId}`](#delete-apiv1pcapkilljobid)|
| [ `GET /api/v1/pcap/{jobId}/config`](#get-apiv1pcapjobidconfig)|
| [ `GET /api/v1/search/search`](#get-apiv1searchsearch)|
| [ `POST /api/v1/search/search`](#post-apiv1searchsearch)|
| [ `POST /api/v1/search/group`](#post-apiv1searchgroup)|
| [ `GET /api/v1/search/findOne`](#get-apiv1searchfindone)|
| [ `GET /api/v1/search/column/metadata`](#get-apiv1searchcolumnmetadata)|
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
| [ `POST /api/v1/sensor/parser/group`](#post-apiv1sensorparsergroup)|
| [ `GET /api/v1/sensor/parser/group/{name}`](#get-apiv1sensorparsergroupname)|
| [ `GET /api/v1/sensor/parser/group`](#get-apiv1sensorparsergroup)|
| [ `DELETE /api/v1/sensor/parser/group/{name}`](#delete-apiv1sensorparsergroupname)|
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
| [ `GET /api/v1/storm/indexing/batch`](#get-apiv1stormindexingbatch)|
| [ `GET /api/v1/storm/indexing/batch/activate`](#get-apiv1stormindexingbatchactivate)|
| [ `GET /api/v1/storm/indexing/batch/deactivate`](#get-apiv1stormindexingbatchdeactivate)|
| [ `GET /api/v1/storm/indexing/batch/start`](#get-apiv1stormindexingbatchstart)|
| [ `GET /api/v1/storm/indexing/batch/stop`](#get-apiv1stormindexingbatchstop)|
| [ `GET /api/v1/storm/indexing/randomaccess`](#get-apiv1stormindexingrandomaccess)|
| [ `GET /api/v1/storm/indexing/randomaccess/activate`](#get-apiv1stormindexingrandomaccessactivate)|
| [ `GET /api/v1/storm/indexing/randomaccess/deactivate`](#get-apiv1stormindexingrandomaccessdeactivate)|
| [ `GET /api/v1/storm/indexing/randomaccess/start`](#get-apiv1stormindexingrandomaccessstart)|
| [ `GET /api/v1/storm/indexing/randomaccess/stop`](#get-apiv1stormindexingrandomaccessstop)|
| [ `GET /api/v1/storm/parser/activate/{name}`](#get-apiv1stormparseractivatename)|
| [ `GET /api/v1/storm/parser/deactivate/{name}`](#get-apiv1stormparserdeactivatename)|
| [ `GET /api/v1/storm/parser/start/{name}`](#get-apiv1stormparserstartname)|
| [ `GET /api/v1/storm/parser/stop/{name}`](#get-apiv1stormparserstopname)|
| [ `GET /api/v1/storm/{name}`](#get-apiv1stormname)|
| [ `GET /api/v1/storm/supervisors`](#get-apiv1stormsupervisors)|
| [ `PATCH /api/v1/update/patch`](#patch-apiv1updatepatch)|
| [ `POST /api/v1/update/add/comment`](#put-apiv1updateaddcomment)|
| [ `POST /api/v1/update/remove/comment`](#put-apiv1updateremovecomment)|
| [ `GET /api/v1/user`](#get-apiv1user)|

### `POST /api/v1/alerts/ui/escalate`
  * Description: Escalates a list of alerts by producing it to the Kafka escalate topic
  * Input:
    * alerts - The alerts to be escalated
  * Returns:
    * 200 - Alerts were escalated

### `GET /api/v1/alerts/ui/settings`
  * Description: Retrieves the current user's settings
  * Returns:
    * 200 - User settings
    * 404 - he current user does not have settings

### `GET /api/v1/alerts/ui/settings/all`
  * Description: Retrieves all users' settings.  Only users that are part of the "ROLE_ADMIN" role are allowed to get all user settings.
  * Returns:
    * 200 - List of all user settings
    * 403 - The current user does not have permission to get all user settings

### `DELETE /api/v1/alerts/ui/settings`
  * Description: Deletes a user's settings.  Only users that are part of the "ROLE_ADMIN" role are allowed to delete user settings.
  * Input:
    * user - The user whose settings will be deleted
  * Returns:
    * 200 - User settings were deleted
    * 403 - The current user does not have permission to delete user settings
    * 404 - User settings could not be found

### `POST /api/v1/alerts/ui/settings`
  * Description: Creates or updates the current user's settings
  * Input:
    * alertsUIUserSettings - The user settings to be saved
  * Returns:
    * 200 - User settings updated. Returns saved settings.
    * 201 - User settings created. Returns saved settings.

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

### `POST /api/v1/kafka/topic/{name}/produce`
  * Description: Produces a message to a Kafka topic
  * Input:
    * name - Kafka topic name
    * message - message to be published
  * Returns:
    * 200 - Published
    * 404 - Kafka topic is missing

### `POST /api/v1/metaalert/searchByAlert`
  * Description: Get all meta alerts that contain an alert.
  * Input:
    * guid - GUID of the alert
  * Returns:
    * 200 - Search results

### `POST /api/v1/metaalert/create`
  * Description: Creates a new meta alert from a list of existing alerts.  The meta alert status will initially be set to 'ACTIVE' and summary statistics will be computed from the list of alerts.  A list of groups included in the request are also added to the meta alert.
  * Input:
    * request - Meta alert create request which includes a list of alert get requests and a list of custom groups used to annotate a meta alert.
  * Returns:
    * 200 - The GUID of the new meta alert

### `POST /api/v1/metaalert/add/alert`
  * Description: Adds an alert to an existing meta alert.  An alert will not be added if it is already contained in a meta alert.
  * Input:
    * request - Meta alert add request which includes a meta alert GUID and list of alert get requests
  * Returns:
    * 200 - Returns 'true' if the alert was added and 'false' if the meta alert did not change.

### `POST /api/v1/metaalert/remove/alert`
  * Description: Removes an alert from an existing meta alert.  If the alert to be removed is not in a meta alert, 'false' will be returned.
  * Input:
    * request - Meta alert remove request which includes a meta alert GUID and list of alert get requests
  * Returns:
    * 200 - Returns 'true' if the alert was removed and 'false' if the meta alert did not change.

### `POST /api/v1/metaalert/update/status/{guid}/{status}`
  * Description: Updates the status of a meta alert to either 'ACTIVE' or 'INACTIVE'.
  * Input:
    * guid - Meta alert GUID
    * status - Meta alert status with a value of either 'ACTIVE' or 'INACTIVE'
  * Returns:
    * 200 - Returns 'true' if the status changed and 'false' if it did not.

### `POST /api/v1/pcap/fixed`
  * Description: Executes a Fixed Filter Pcap Query.
  * Input:
    * fixedPcapRequest - A Fixed Pcap Request which includes fixed filter fields like ip source address and protocol
  * Returns:
    * 200 - Returns a job status with job ID.

### `POST /api/v1/pcap/query`
  * Description: Executes a Query Filter Pcap Query.
  * Input:
    * queryPcapRequest - A Query Pcap Request which includes Stellar query field
  * Returns:
    * 200 - Returns a job status with job ID.

### `GET /api/v1/pcap`
  * Description: Gets a list of job statuses for Pcap query jobs that match the requested state.
  * Input:
    * state - Job state
  * Returns:
    * 200 - Returns a list of job statuses for jobs that match the requested state.  

### `GET /api/v1/pcap/{jobId}`
  * Description: Gets job status for Pcap query job.
  * Input:
    * jobId - Job ID of submitted job
  * Returns:
    * 200 - Returns a job status for the Job ID.
    * 404 - Job is missing.

### `GET /api/v1/pcap/{jobId}/pdml`
  * Description: Gets Pcap Results for a page in PDML format.
  * Input:
    * jobId - Job ID of submitted job
    * page - Page number
  * Returns:
    * 200 - Returns PDML in json format.
    * 404 - Job or page is missing.

### `GET /api/v1/pcap/{jobId}/raw`
  * Description: Download Pcap Results for a page.
  * Input:
    * jobId - Job ID of submitted job
    * page - Page number
  * Returns:
    * 200 - Returns Pcap as a file download.
    * 404 - Job or page is missing.

### `DELETE /api/v1/pcap/kill/{jobId}`
  * Description: Kills running job.
  * Input:
    * jobId - Job ID of submitted job
  * Returns:
    * 200 - Kills passed job.

### `GET /api/v1/pcap/{jobId}/config`
  * Description: Gets job configuration for Pcap query job.
  * Input:
    * jobId - Job ID of submitted job
  * Returns:
    * 200 - Returns a map of job properties for the Job ID.
    * 404 - Job is missing.

### `POST /api/v1/search/search`
  * Description: Searches the indexing store. GUIDs must be quoted to ensure correct results.
  * Input:
      * searchRequest - Search request
  * Returns:
    * 200 - Search response

### `POST /api/v1/search/group`
  * Description: Searches the indexing store and returns field groups. GUIDs must be quoted to ensure correct results. Groups are hierarchical and nested in the order the fields appear in the 'groups' request parameter. The default sorting within groups is by count descending.  A groupOrder type of count will sort based on then number of documents in a group while a groupType of term will sort by the groupBy term.
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
  * Description: Get index column metadata for a list of sensor types with duplicates removed.  Column names and types for each sensor are retrieved from the most recent index.  Columns that exist in multiple indices with different types will default to type 'other'.
  * Input:
      * sensorTypes - Sensor Types
  * Returns:
    * 200 - Column Metadata

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

### `POST /api/v1/sensor/parser/config/{name}`
  * Description: Updates or creates a SensorParserConfig in Zookeeper
  * Input:
    * sensorParserConfig - SensorParserConfig
    * name - SensorEnrichmentConfig name
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

### `POST /api/v1/sensor/parser/group`
  * Description: Updates or creates a SensorParserGroup in Zookeeper
  * Input:
    * sensorParserGroup - SensorParserGroup
  * Returns:
    * 200 - SensorParserGroup updated. Returns saved SensorParserGroup
    * 201 - SensorParserGroup created. Returns saved SensorParserGroup

### `GET /api/v1/sensor/parser/group/{name}`
  * Description: Retrieves a SensorParserGroup from Zookeeper
  * Input:
    * name - SensorParserGroup name
  * Returns:
    * 200 - Returns SensorParserGroup
    * 404 - SensorParserGroup is missing

### `GET /api/v1/sensor/parser/group`
  * Description: Retrieves all SensorParserGroups from Zookeeper
  * Returns:
    * 200 - Returns all SensorParserGroups

### `DELETE /api/v1/sensor/parser/group/{name}`
  * Description: Deletes a SensorParserGroup from Zookeeper
  * Input:
    * name - SensorParserGroup name
  * Returns:
    * 200 - SensorParserGroup was deleted
    * 404 - SensorParserGroup is missing

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

### `GET /api/v1/storm/indexing/batch`
  * Description: Retrieves the status of the Storm batch indexing topology
  * Returns:
    * 200 - Returns topology status information
    * 404 - Topology is missing

### `GET /api/v1/storm/indexing/batch/activate`
  * Description: Activates a Storm batch indexing topology
  * Returns:
    * 200 - Returns activate response message

### `GET /api/v1/storm/indexing/batch/deactivate`
  * Description: Deactivates a Storm batch indexing topology
  * Returns:
    * 200 - Returns deactivate response message

### `GET /api/v1/storm/indexing/batch/start`
  * Description: Starts a Storm batch indexing topology
  * Returns:
    * 200 - Returns start response message

### `GET /api/v1/storm/indexing/batch/stop`
  * Description: Stops a Storm batch indexing topology
  * Input:
    * stopNow - Stop the topology immediately
  * Returns:
    * 200 - Returns stop response message

### `GET /api/v1/storm/indexing/randomaccess`
  * Description: Retrieves the status of the Storm randomaccess indexing topology
  * Returns:
    * 200 - Returns topology status information
    * 404 - Topology is missing

### `GET /api/v1/storm/indexing/randomaccess/activate`
  * Description: Activates a Storm randomaccess indexing topology
  * Returns:
    * 200 - Returns activate response message

### `GET /api/v1/storm/indexing/randomaccess/deactivate`
  * Description: Deactivates a Storm randomaccess indexing topology
  * Returns:
    * 200 - Returns deactivate response message

### `GET /api/v1/storm/indexing/randomaccess/start`
  * Description: Starts a Storm randomaccess indexing topology
  * Returns:
    * 200 - Returns start response message

### `GET /api/v1/storm/indexing/randomaccess/stop`
  * Description: Stops a Storm randomaccess indexing topology
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
    * 200 - Nothing
    * 404 - Document not found

### `POST /api/v1/update/add/comment`
  * Description: Add a comment to an alert
  * Input:
    * request - Comment add request
  * Returns:
    * 200 - Returns the complete alert document with comments added.

### `POST /api/v1/update/remove/comment`
  * Description: Remove a comment from an alert
  * Input:
    * request - Comment remove request
  * Returns:
    * 200 - Returns the complete alert document with comments removed.

### `GET /api/v1/user`
  * Description: Retrieves the current user
  * Returns:
    * 200 - Current user

## Testing

Profiles are includes for both the metron-docker and Full Dev environments.

### metron-docker

Start the [metron-docker](../../metron-docker) environment.  Build the metron-rest module and start it with the Spring Boot Maven plugin:
```
mvn clean package
mvn spring-boot:run -Drun.profiles=docker,dev
```

The metron-rest application will be available at http://localhost:8080/swagger-ui.html#/.

### Full Dev

Start the [development environment](../../metron-deployment/development/centos6).  Build the metron-rest module and start it with the Spring Boot Maven plugin:
```
mvn clean package
mvn spring-boot:run -Drun.profiles=vagrant,dev
```

The metron-rest application will be available at http://localhost:8080/swagger-ui.html#/.

To run the application locally on the Full Dev host (node1), follow the [Installation](#installation) instructions above.  Then set the METRON_SPRING_PROFILES_ACTIVE variable in `/etc/default/metron`:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
```

and start the application:
```
service metron-rest start
```

In a cluster with Kerberos enabled, update the security settings in `/etc/default/metron`.  Security is disabled by default in the `vagrant` Spring profile so that setting must be overriden with the METRON_SPRING_OPTIONS variable:
```
METRON_SPRING_PROFILES_ACTIVE="vagrant,dev"
METRON_JVMFLAGS="-Djava.security.auth.login.config=$METRON_HOME/client_jaas.conf"
METRON_SPRING_OPTIONS="--kerberos.enabled=true"
```

The metron-rest application will be available at http://node1:8082/swagger-ui.html#/.

## License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.
