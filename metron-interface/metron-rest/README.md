# Metron REST and Configuration UI

This UI exposes and aids in sensor configuration.

# Prerequisites

* A running Metron cluster.
* A running instance of MySQL 
* Java 8 installed

# Installation

1. Unpack RPM.  This will create this directory structure: 

* bin
    * start.sh
* lib
    * metron-rest-<version>.jar

2. Create a MySQL user for the Config UI (http://dev.mysql.com/doc/refman/5.7/en/adding-users.html).

3. Create a Config UI database in MySQL with this command:

```
CREATE DATABASE IF NOT EXISTS metronrest
```
 
4. Create an "application.yml" file with the contents of src/main/resource/application-docker.yml

5. Update the configuration file in step 4:

* substitute the appropriate Metron service urls (Kafka, Zookeeper, Storm, etc) in properties containing ${docker.host.address}
* update the "spring.datasource.username" and "spring.datasource.password" properties using credentials from step 2

6. Start the UI with this command:

```
./bin/start.sh path_to_config_file_from_step_4
```

# Usage

The exposed REST endpoints can be accessed at http://host:port/swagger-ui.html#/.  The default port is 8080 but can be changed in application.yml by setting "server.port" to the desired port.

# License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.