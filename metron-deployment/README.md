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
# Overview
This set of playbooks can be used to deploy an Ambari-managed Hadoop cluster containing Metron services using Ansible. These playbooks target RHEL/CentOS 6.x operating
systems.

Installation consists of -
- Building Metron tarballs, RPMs and the Ambari MPack
- Deploying Ambari
- Leveraging Ambari to install:
  * The required Hadoop Components
  * Core Metron (Parsing, Enrichment, Indexing)
  * Elasticsearch
  * Kibana
- Starting All Services

## Prerequisites
The following tools are required to run these scripts:

- [Maven](https://maven.apache.org/)
- [Git](https://git-scm.com/)
- [Ansible](http://www.ansible.com/) (2.0.0.2 or 2.2.2.0)
- [Docker](https://www.docker.com/) (Docker for Mac on OSX)

These scripts depend on two files for configuration:

- hosts - declares which Ansible roles will be run on which hosts
- group_vars/all - various configuration settings needed to install Metron

For production use, it is recommended that Metron be installed on an existing cluster managed by Ambari as described in the Installing Management Pack section below.
## Ambari
The Ambari playbook will install a Hadoop cluster including the Metron Services (Parsing, Enrichment, Indexing). Ambari will also install Elasticsearch and Kibana.

Currently, the playbooks supports building a local development cluster running on one node or deploying to a 10 node cluster on AWS EC2.

## Vagrant
There is a development environment based on Vagrant that is referred to as "Full Dev".  This installs the entire Ambari/Metron stack. This is useful in testing out changes to the installation procedure.

### Prerequsities
- Install [Vagrant](https://www.vagrantup.com/) (5.0.16+)
- Install the Hostmanager plugin for vagrant - Run `vagrant plugin install vagrant-hostmanager` on the machine where Vagrant is
installed

### Full-Dev
Navigate to `metron/metron-deployment/vagrant/full-dev-platform` and run `vagrant up`.

## Ambari Management Pack
An Ambari Management Pack can be built in order to make the Metron service available on top of an existing stack, rather than needing a direct stack update.

This will set up
- Metron Parsers
- Enrichment
- Indexing
- GeoIP data
- Optional Elasticsearch
- Optional Kibana

### Prerequisites
- A cluster managed by Ambari 2.4.2+
- Metron RPMs available on the cluster in the /localrepo directory.  See [RPMs](#rpms) for further information.
- [Node.js](https://nodejs.org/en/download/package-manager/) repository installed on the Management UI host

### Building Management Pack
From `metron-deployment` run
```
mvn clean package
```

A tar.gz that can be used with Ambari can be found at `metron-deployment/packaging/ambari/metron-mpack/target/`

### Installing Management Pack
Before installing the mpack, update Storm's topology.classpath in Ambari to include '/etc/hbase/conf:/etc/hadoop/conf'. Restart Storm service.

Place the mpack's tar.gz onto the node running Ambari Server. From the command line on this node, run
```
ambari-server install-mpack --mpack=<mpack_location> --verbose
```

This will make the services available in Ambari in the same manner as any services in a stack, e.g. through Add Services or during cluster install.
The Indexing / Parsers/ Enrichment masters should be colocated with a Kafka Broker (to create topics) and HBase client (to create the enrichment and theatintel tables).
This colocation is currently not enforced by Ambari, and should be managed by either a Service or Stack advisor as an enhancement.

Several configuration parameters will need to be filled in, and should be pretty self explanatory (primarily a couple of Elasticsearch configs, and the Storm REST URL).  Examples are provided in the descriptions on Ambari.
Notably, the URL for the GeoIP database that is preloaded (and is prefilled by default) can be set to use a `file:///` location

After installation, a custom action is available in Ambari (where stop / start services are) to install Elasticsearch templates.  Similar to this, a custom Kibana action to Load Template is available.

Another custom action is available in Ambari to import Zeppelin dashboards. See the [metron-indexing documentation](../metron-platform/metron-indexing)

#### Offline installation
Currently there is only one point that would reach out to the internet during an install.  This is the URL for the GeoIP database information.

The RPMs DO NOT reach out to the internet (because there is currently no hosting for them).  They look on the local filesystem in `/localrepo`.

### Current Limitations
There are a set of limitations that should be addressed based to improve the current state of the mpacks.

- There is currently no hosting for RPMs remotely.  They will have to be built locally.
- Colocation of appropriate services should be enforced by Ambari.  See [#Installing Management Pack] for more details.
- Storm's topology.classpath is not updated with the Metron service install and needs to be updated separately.
- Several configuration parameters used when installing the Metron service could (and should) be grabbed from Ambari.  Install will require them to be manually entered.
- Need to handle upgrading Metron

## RPMs
RPMs can be built to install the components in metron-platform. These RPMs are built in a Docker container and placed into `target`.

Components in the RPMs:
- metron-common
- metron-data-management
- metron-elasticsearch
- metron-enrichment
- metron-parsers
- metron-pcap
- metron-solr
- stellar-common

### Prerequisites
- Docker.  The image detailed in: `metron-deployment/packaging/docker/rpm-docker/README.md` will automatically be built (or rebuilt if necessary).
- Artifacts for metron-platform have been produced.  E.g. `mvn clean package -DskipTests` in `metron-platform`

The artifacts are required because there is a dependency on modules not expressed via Maven (we grab the resulting assemblies, but don't need the jars).  These are
- metron-common
- metron-data-management
- metron-elasticsearch
- metron-enrichment
- metron-indexing
- metron-parsers
- metron-pcap-backend
- metron-solr
- metron-profiler
- metron-config

### Building RPMs
```
cd metron-deployment
mvn clean package -Pbuild-rpms
```

The output RPM files will land in `target/RPMS/noarch`.  They can be installed with the standard
```
rpm -i <package>
```

## Kibana Dashboards

The dashboards installed by the Kibana custom action are managed by the dashboard.p file.  This file is created by exporting existing dashboards from a running Kibana instance.

To create a new version of the file, make any necessary changes to Kibana (e.g. on full-dev), and export with the appropriate script.

```
python packaging/ambari/metron-mpack/src/main/resources/common-services/KIBANA/4.5.1/package/scripts/dashboard/dashboardindex.py \
$ES_HOST 9200 \
packaging/ambari/metron-mpack/src/main/resources/common-services/KIBANA/4.5.1/package/scripts/dashboard/dashboard.p -s
```

Build the Ambari Mpack to get the dashboard updated appropriately.

Once the MPack is installed, run the Kibana service's action "Load Template" to install dashboards.  This will completely overwrite the .kibana in Elasticsearch, so use with caution.

## Kerberos
The MPack can allow Metron to be installed and then Kerberized, or installed on top of an already Kerberized cluster.  This is done through Ambari's standard Kerberization setup.

### Caveats
* For nodes using a Metron client and a local repo, the repo must exist on all nodes (e.g via createrepo). This repo can be empty; only the main Metron services need the RPMs.
* A Metron client must be installed on each supervisor node in a secured cluster.  This is to ensure that the Metron keytab and client_jaas.conf get distributed in order to allow reading and writing from Kafka.
  * When Metron is already installed on the cluster, this should be done before Kerberizing.
  * When addding Metron to an already Kerberized cluster, ensure that all supervisor nodes receive a Metron client.
* Storm (and Metron) must be restarted after Metron is installed on an already Kerberized cluster.  Several Storm configs get updated, and Metron will be unable to write to Kafka without a restart.
  * Kerberizing a cluster with an existing Metron already has restarts of all services during Kerberization, so it's unneeded.

Instructions for setup on Full Dev can be found at [Kerberos-ambari-setup.md](Kerberos-ambari-setup.md).  These instructions reference the manual install instructions.

### Kerberos Without an MPack
Using the MPack is preferred, but instructions for Kerberizing manually can be found at [Kerberos-manual-setup.md](Kerberos-manual-setup.md). These instructions are reference by the Ambari Kerberos install instructions and include commands for setting up a KDC.

## TODO
- Support Ubuntu deployments
