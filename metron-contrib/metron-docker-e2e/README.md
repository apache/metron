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

Metron Docker E2E is a [Docker Compose](https://docs.docker.com/compose/overview/) application that serves as a backend to integration tests.

Metron Docker includes these images that have been customized for Metron:

  - Kafka
  - Zookeeper
  - Elasticsearch
  - Metron REST
  - Metron UIs

Setup
-----

Install [Docker for Mac](https://docs.docker.com/docker-for-mac/) or [Docker for Windows](https://docs.docker.com/docker-for-windows/).  The following versions have been tested:

  - Docker version 17.12.0-ce
  - docker-machine version 0.13.0
  - docker-compose version 1.18.0

Build Metron from the top level directory with:
```
$ cd $METRON_HOME
$ mvn clean install -DskipTests
```

Create a Docker machine:
```
$ export METRON_DOCKER_E2E_HOME=$METRON_HOME/metron-contrib/metron-docker-e2e
$ cd $METRON_DOCKER_E2E_HOME
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
$ cd $METRON_DOCKER_E2E_HOME/compose/
```

The Metron Docker environment lifecycle is controlled by the [docker-compose](https://docs.docker.com/compose/reference/overview/) command.  The service names can be found in the docker-compose.yml file.  For example, to build and start the environment run this command:
```
$ eval "$(docker-machine env metron-machine)"
$ docker-compose up -d
```

After all services have started list the containers and ensure their status is 'Up':
```
$ docker-compose ps
         Name                       Command               State                       Ports                     
----------------------------------------------------------------------------------------------------------------
metron_elasticsearch_1   /bin/bash bin/es-docker          Up      0.0.0.0:9210->9200/tcp, 0.0.0.0:9310->9300/tcp
metron_kafka_1           start-kafka.sh                   Up      0.0.0.0:9092->9092/tcp                        
metron_metron-rest_1     /bin/sh -c ./bin/start.sh        Up      0.0.0.0:8082->8082/tcp                        
metron_metron-ui_1       /bin/sh -c ./bin/start.sh        Up      0.0.0.0:4201->4201/tcp                        
metron_zookeeper_1       /docker-entrypoint.sh zkSe ...   Up      0.0.0.0:2181->2181/tcp, 2888/tcp, 3888/tcp    
```

Various services are exposed through http on the Docker host.  Get the host ip from the URL property:
```
$ docker-machine ls
NAME             ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER    ERRORS
metron-machine   *        virtualbox   Running   tcp://192.168.99.100:2376           v1.12.5
```

The various integration tests can now be run against this environment.

TODO: document how to set docker machine ip address for e2e tests
