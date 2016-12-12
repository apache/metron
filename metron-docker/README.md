# Metron Docker

Metron Docker is a [Docker Compose](https://docs.docker.com/compose/overview/) application that is intended for development and integration testing of Metron.  Use this instead of Vagrant when:
  
  - You want an environment that can be built and spun up quickly
  - You need to frequently rebuild and restart services
  - You only need to test, troubleshoot or develop against a subset of services
  
Metron Docker includes these images that have been customized for Metron:

  - Kafka (with Zookeeper)
  - HBase
  - Storm (with all topologies deployed)
  - MySQL
  - Elasticsearch
  - Kibana

Setup
-----

Install Docker from https://docs.docker.com/docker-for-mac/.  The following versions have been tested:

  - Docker version 1.12.0
  - docker-machine version 0.8.0
  - docker-compose version 1.8.0
  
External Metron binaries must be deployed to the various Dockerfile contexts.  Run this script to do that:
```
./install-metron.sh -b
```

If your Metron project has already been compiled and packaged with Maven, you can skip that step by removing the -b option:
```
./install-metron.sh
```

You are welcome to use an existing docker-machine but we prefer a machine with more resources.  You can create one of those by running this:
```
./create-docker-machine.sh
```

This will create a docker-machine called "metron-machine".  Anytime you want to run Docker commands against this machine, make sure you run this first to set the Docker environment variables:
```
eval "$(docker-machine env metron-machine)"
```

Usage
-----

The Metron Docker environment lifecycle is controlled by the [docker-compose](https://docs.docker.com/compose/reference/overview/) command.  These service names can be found in the docker-compose.yml file.  For example, to build the environment run this command:
```
docker-compose up -d
```

If a new parser is added to metron-parsers, run these commands to redeploy the parsers to the Storm image:
```
docker-compose down
./install-metron -b
docker-compose build storm
docker-compose up -d
```

If there is a problem with Kafka, run this command to connect and explore the Kafka container:
```
docker-compose exec kafkazk bash
```

A tool for producing test data in Kafka is included with the Kafka/Zookeeper image.  It loops through lines in a test data file and outputs them to Kafka at the desired frequency.  Create a test data file in `./kafkazk/data/` and rebuild the Kafka/Zookeeper image:
```
printf 'first test data\nsecond test data\nthird test data\n' > ./kafkazk/data/TestData.txt
docker-compose down
docker-compose build kafkazk
docker-compose up -d
```

This will deploy the test data file to the Kafka/Zookeeper container.  Now that data can be streamed to a Kafka topic:
```
docker-compose exec kafkazk ./bin/produce-data.sh
Usage:  produce-data.sh data_path topic [message_delay_in_seconds]

# Stream data in TestData.txt to the 'test' Kafka topic at a frequency of 5 seconds (default is 1 second)
docker-compose exec kafkazk ./bin/produce-data.sh /data/TestData.txt test 5 
```

The Kafka/Zookeeper image comes with sample Bro and Squid data:
```
# Stream Bro test data every 1 second
docker-compose exec kafkazk ./bin/produce-data.sh /data/BroExampleOutput.txt bro

# Stream Squid test data every 0.1 seconds
docker-compose exec kafkazk ./bin/produce-data.sh /data/SquidExampleOutput.txt squid 0.1
```