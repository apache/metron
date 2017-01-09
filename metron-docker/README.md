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

The Metron Docker environment lifecycle is controlled by the [docker-compose](https://docs.docker.com/compose/reference/overview/) command.  For example, to build the environment run this command:
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

These service names can be found in the docker-compose.yml file.