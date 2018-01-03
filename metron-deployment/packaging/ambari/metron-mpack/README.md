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

This provides a Management Pack (MPack) extension for [Apache Ambari](https://ambari.apache.org/) that simplifies the provisioning, management and monitoring of Metron on clusters of any size.  

This allows you to easily install Metron using a simple, guided process.  This also allows you to monitor cluster health and even secure your cluster with kerberos.

### Prerequisites

* Ambari 2.4.2+

* Installable Metron packages (either RPMs or DEBs) located in a repository on each host at `/localrepo`.

* A [Node.js](https://nodejs.org/en/download/package-manager/) repository installed on the host running the Management and Alarm UI.

### Quick Start

1. Build the Metron MPack. Execute the following command from the project's root directory.
    ```
    mvn clean package -Pmpack -DskipTests
    ```

1. This results in the Mpack being produced at the following location.
    ```
    metron-deployment/packaging/ambari/metron-mpack/target/metron_mpack-x.y.z.0.tar.gz
    ```

1. Copy the tarball to the host where Ambari Server is installed.

1. Ensure that Ambari Server is stopped.

1. Install the MPack.
    ```
    ambari-server install-mpack --mpack=metron_mpack-x.y.z.0.tar.gz --verbose
    ```

1. Install the Metron packages (RPMs or DEBs) in a local repository on each host where a Metron component is installed.  By default, the repository is expected to exist at `/localrepo`.

    On hosts where only a Metron client is installed, the local repository must exist, but it does not need to contain Metron packages.  For example to create an empty repository for an RPM-based system, run the following commands.

    ```
    yum install createrepo
    mkdir /localrepo
    cd /localrepo
    createrepo
    ```

1. Metron will now be available as an installable service within Ambari.  

### Installation Notes

The MPack will make all Metron services available in Ambari in the same manner as any other services in a stack.  These can be installed using Ambari's user interface using "Add Services" or during an initial cluster install.

#### Kerberization

The MPack allows Metron to be automatically kerberized in two different ways.  
* Metron can be installed on a non-kerberized cluster and then the entire cluster can be kerberized using Ambari.  
* Alternatively, Metron can be installed on top of an already kerberized cluster.  

Using the MPack is preferred, but instructions for manually Kerberizing a cluster with Metron can be found at [Kerberos-manual-setup.md](../../Kerberos-manual-setup.md).

##### Metron Client

A "Metron Client" must be installed on each supervisor node in a kerberized cluster.  This client ensures that the Metron keytab and `client_jaas.conf` get distributed to each node in order to allow reading and writing from Kafka.		
* When Metron is already installed on the cluster, installation of the "Metron Client" should be done before Kerberizing.
* When adding Metron to an already Kerberized cluster, ensure that all supervisor nodes receive a Metron client.

##### Restarts

Storm (and the Metron topologies) must be restarted after Metron is installed on an already Kerberized cluster.  The restart triggers several Storm configurations to get updated and Metron will be unable to write to Kafka without a restart.		

Kerberizing a cluster with a pre-existing Metron, automatically restarts all services during Kerberization.  No additional manual restart is needed in this case.

#### Zeppelin Import

A custom action is available in Ambari to import Zeppelin dashboards. See the [metron-indexing documentation](../../../../metron-platform/metron-indexing) for more information.

#### Kibana Dashboards

The dashboards installed by the Kibana custom action are managed by the `dashboard.p` file.  This file is created by exporting existing dashboards from a running Kibana instance.		

To create a new version of the file, make any necessary changes to Kibana and run the following commands to export your changes.
  ```
  cd packaging/ambari/metron-mpack/src/main/resources/common-services/KIBANA/4.5.1/package/scripts/dashboard
  python dashboardindex.py $ES_HOST 9200 dashboard.p -s		
  ```

#### Offline Installation

Retrieval of the GeoIP database is the only point during installation that reaches out to the internet. For an offline installation, the URL for the GeoIP database can be manually set to a local path on the file system such as  `file:///home/root/geoip/GeoLite2-City.mmdb.gz`.

The RPMs DO NOT reach out to the internet (because there is currently no hosting for them).  They look on the local filesystem in `/localrepo`.

#### Limitations

There are a few limitations that should be addressed to improve the Metron MPack installation.

* There is no external hosting for Metron packages (either RPMs or DEBs).  These have to be built locally and installed on each host in a repository located at `/localrepo`.

* Several configuration parameters used when installing Metron could retrieved from Ambari rather than requiring user input.  

* The MPack does not support upgrades.
