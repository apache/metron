# Metron REST and Configuration UI

This UI exposes and aids in sensor configuration.

## Prerequisites

* A running Metron cluster
* A running instance of MySQL 
* Java 8 installed

## Installation

1. Download and Unpack RPM.  The directory structure will look like: 
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
 
1. Create an `application.yml` file with the contents of [application-docker.yml](src/main/resource/application-docker.yml).  Substitute the appropriate Metron service urls (Kafka, Zookeeper, Storm, etc) in properties containing `${docker.host.address}` and update the `spring.datasource.username` and `spring.datasource.password` properties using the MySQL credentials from step 4.

1. Start the UI with this command:
    ```
    ./bin/start.sh /path/to/application.yml
    ```

## Usage

The exposed REST endpoints can be accessed at http://host:port/swagger-ui.html#/.  The default port is 8080 but can be changed in application.yml by setting "server.port" to the desired port.

## License

This project depends on the Java Transaction API.  See https://java.net/projects/jta-spec/ for more details.