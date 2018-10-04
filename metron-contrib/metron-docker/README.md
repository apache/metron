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
# Metron Docker

Metron Docker is a [Docker Compose](https://docs.docker.com/compose/overview/) application that is intended only for development and integration testing of Metron.  These images can quickly spin-up the underlying components on which Apache Metron runs.

None of the core Metron components are setup or launched automatically with these Docker images.  You will need to manually setup and start the Metron components that you require.  You should not expect to see telemetry being parsed, enriched, or indexed.  If you are looking to try-out, experiment or demo Metron capabilities on a single node, then the [Vagrant-driven VM](../../metron-deployment/development/centos6) is what you need.  Use this instead of Vagrant when:

  - You want an environment that can be built and spun up quickly
  - You need to frequently rebuild and restart services
  - You only need to test, troubleshoot or develop against a subset of services

Metron Docker includes these images that have been customized for Metron:

  - Kafka (with Zookeeper)
  - HBase
  - Storm
  - Elasticsearch
  - Kibana
  - HDFS

Setup
-----

Install [Docker for Mac](https://docs.docker.com/docker-for-mac/) or [Docker for Windows](https://docs.docker.com/docker-for-windows/).  The following versions have been tested:

  - Docker version 1.12.0
  - docker-machine version 0.8.0
  - docker-compose version 1.8.0

Build Metron from the top level directory with:
```
$ cd $METRON_HOME
$ mvn clean install -DskipTests
```

You are welcome to use an existing Docker host but we prefer one with more resources.  You can create one of those with this script:
```
$ export METRON_DOCKER_HOME=$METRON_HOME/metron-contrib/metron-docker
$ cd $METRON_DOCKER_HOME
$ ./scripts/create-docker-machine.sh
```

This will create a host called "metron-machine".  Anytime you want to run Docker commands against this host, make sure you run this first to set the Docker environment variables:
```
$ eval "$(docker-machine env metron-machine)"
```

If you wish to use a local docker-engine install, please set an environment variable BROKER_IP_ADDR to the IP address of your host machine. This cannot be the loopback address.

Usage
-----

Navigate to the compose application root:
```
$ cd $METRON_DOCKER_HOME/compose/
```

The Metron Docker environment lifecycle is controlled by the [docker-compose](https://docs.docker.com/compose/reference/overview/) command.  The service names can be found in the docker-compose.yml file.  For example, to build and start the environment run this command:
```
$ eval "$(docker-machine env metron-machine)"
$ docker-compose up -d
```

After all services have started list the containers and ensure their status is 'Up':
```
$ docker ps --format 'table {{.Names}}\t{{.Status}}'
NAMES                    STATUS
metron_storm_1           Up 5 minutes
metron_hbase_1           Up 5 minutes
metron_kibana_1          Up 5 minutes
metron_kafkazk_1         Up 5 minutes
metron_elasticsearch_1   Up 5 minutes
```

Various services are exposed through http on the Docker host.  Get the host ip from the URL property:
```
$ docker-machine ls
NAME             ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER    ERRORS
metron-machine   *        virtualbox   Running   tcp://192.168.99.100:2376           v1.12.5
```

Then, assuming a host ip of `192.168.99.100`, the UIs and APIs are available at:

* Storm - http://192.168.99.100:8080/
* HBase - http://192.168.99.100:16010/
* Elasticsearch - http://192.168.99.100:9200/_plugin/head/
* Kibana - http://192.168.99.100:5601/
* HDFS (Namenode) - http://192.168.99.100:50070/

The Storm logs can be useful when troubleshooting topologies.  They can be found on the Storm container in `/usr/share/apache-storm/logs`.

When done using the machine, shut it down with:
```
$ docker-compose down
```

Examples
-----
* [Deploy a new parser class](#deploy-a-new-parser-class)
* [Connect to a container](#connect-to-a-container)
* [Create a sensor from sample data](create-a-sensor-from-sample-data)
* [Upload configs to Zookeeper](upload-configs-to-zookeeper)
* [Manage a topology](manage-a-topology)
* [Run sensor data end to end](run-sensor-data-end-to-end)


### Deploy a new parser class

After adding a new parser to metron-parsers-common, build Metron from the top level directory:
```
$ cd $METRON_HOME
$ mvn clean install -DskipTests
```

Then run these commands to redeploy the parsers to the Storm image:
```
$ cd $METRON_DOCKER_HOME/compose
$ docker-compose down
$ docker-compose build storm
$ docker-compose up -d
```

### Connect to a container

Suppose there is a problem with Kafka and the logs are needed for further investigation. Run this command to connect and explore the running Kafka container:
```
$ cd $METRON_DOCKER_HOME/compose
$ docker-compose exec kafkazk bash
```

### Create a sensor from sample data

A tool for producing test data in Kafka is included with the Kafka/Zookeeper image.  It loops through lines in a test data file and outputs them to Kafka at the desired frequency.  Create a test data file in `./kafkazk/data/` and rebuild the Kafka/Zookeeper image:
```
$ cd $METRON_DOCKER_HOME/compose
$ printf 'first test data\nsecond test data\nthird test data\n' > ./kafkazk/data/TestData.txt
$ docker-compose down
$ docker-compose build kafkazk
$ docker-compose up -d
```

This will deploy the test data file to the Kafka/Zookeeper container.  Now that data can be streamed to a Kafka topic:
```
$ docker-compose exec kafkazk ./bin/produce-data.sh
Usage:  produce-data.sh data_path topic [message_delay_in_seconds]

# Stream data in TestData.txt to the 'test' Kafka topic at a frequency of 5 seconds (default is 1 second)
$ docker-compose exec kafkazk ./bin/produce-data.sh /data/TestData.txt test 5
```

The Kafka/Zookeeper image comes with sample Bro and Squid data:
```
# Stream Bro test data every 1 second
$ docker-compose exec kafkazk ./bin/produce-data.sh /data/BroExampleOutput.txt bro

# Stream Squid test data every 0.1 seconds
$ docker-compose exec kafkazk ./bin/produce-data.sh /data/SquidExampleOutput.txt squid 0.1
```

### Upload configs to Zookeeper

Parser configs and a global config configured for this Docker environment are included with the Kafka/Zookeeper image.  Load them with:
```
$ docker-compose exec kafkazk bash
# $METRON_HOME/bin/zk_load_configs.sh -z localhost:2181 -m PUSH -i $METRON_HOME/config/zookeeper
# exit
```

Dump out the configs with:
```
$ docker-compose exec kafkazk bash
# $METRON_HOME/bin/zk_load_configs.sh -z localhost:2181 -m DUMP
# exit
```

### Manage a topology

The Storm image comes with a script to easily start parser topologies:
```
docker-compose exec storm ./bin/start_docker_parser_topology.sh sensor_name
```

The enrichment topology can be started with:
```
docker-compose exec storm ./bin/start_enrichment_topology.sh
```

The indexing topology can be started with:
```
docker-compose exec storm ./bin/start_elasticsearch_topology.sh
```

Topologies can be stopped using the Storm CLI.  For example, stop the enrichment topology with:
```
docker-compose exec storm storm kill enrichments -w 0
```

### Run sensor data end to end

First ensure configs were uploaded as described in the previous example. Then start a sensor and leave it running:
```
$ cd $METRON_DOCKER_HOME/compose
$ docker-compose exec kafkazk ./bin/produce-data.sh /data/BroExampleOutput.txt bro
```

Open a separate console session and verify the sensor is running by consuming a message from Kafka:
```
$ export METRON_DOCKER_HOME=$METRON_HOME/metron-contrib/metron-docker
$ cd $METRON_DOCKER_HOME/compose
$ docker-compose exec kafkazk ./bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic bro
```

A new message should be printed every second. Now kill the consumer and start the Bro parser topology:
```
$ docker-compose exec storm ./bin/start_docker_parser_topology.sh bro
```

Bro data should be flowing through the bro parser topology and into the Kafka enrichments topic.  The enrichments topic should be created automatically:
```
$ docker-compose exec kafkazk ./bin/kafka-topics.sh --zookeeper localhost:2181 --list
bro
enrichments
indexing
```

Verify parsed Bro data is in the Kafka enrichments topic:
```
docker-compose exec kafkazk ./bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic enrichments
```

Now start the enrichment topology:
```
docker-compose exec storm ./bin/start_enrichment_topology.sh
```

Parsed Bro data should be flowing through the enrichment topology and into the Kafka indexing topic.  Verify enriched Bro data is in the Kafka indexing topic:
```
docker-compose exec kafkazk ./bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic indexing
```

Now start the indexing topology:
```
docker-compose exec storm ./bin/start_elasticsearch_topology.sh
```

Enriched Bro data should now be present in the Elasticsearch container:
```
$ docker-machine ls
NAME             ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER    ERRORS
metron-machine   *        virtualbox   Running   tcp://192.168.99.100:2376           v1.12.5

$ curl -XGET http://192.168.99.100:9200/_cat/indices?v
health status index                   pri rep docs.count docs.deleted store.size pri.store.size
yellow open   .kibana                   1   1          1            0      3.1kb          3.1kb
yellow open   bro_index_2016.12.19.18   5   1        180            0      475kb          475kb
```
