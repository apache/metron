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

1. Build the Metron MPack.
    ```
    cd metron-deployment
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

1. Metron swill now be available as an installable service within Ambari.  

### Installation Notes

The MPack will make all Metron services available in Ambari in the same manner as any other services in a stack.  These can be installed using Ambari's user interface using "Add Services" or during an initial cluster install.

#### Co-Location

1. The Parsers, Enrichment, Indexing, and Profiler masters should be colocated on a host with a Kafka Broker.  This is necessary so that the correct Kafka topics can be created.

1. The Enrichment and Profiler masters should be colocated on a host with an HBase client.  This is necessary so that the Enrichment, Threat Intel, and Profile tables can be created.

This colocation is currently not enforced by Ambari and should be managed by either a Service or Stack advisor as an enhancement.

#### Kerberization

The MPack allows Metron to be automatically kerberized in two different ways.  
* Metron can be installed on a non-kerberized cluster and then the entire cluster can be kerberized using Ambari.  
* Alternatively, Metron can be installed on top of an already kerberized cluster.  

Using the MPack is preferred, but instructions for manually Kerberizing a cluster with Metron can be found at [../../Kerberos-manual-setup.md](Kerberos-manual-setup.md).

#### Zeppelin Import

A custom action is available in Ambari to import Zeppelin dashboards. See the [metron-indexing documentation](../metron-platform/metron-indexing) for more information.

#### Offline Installation

There is only one point during installation that reaches out to the internet.  That is necessary to retrieve the GeoIP database.

The RPMs DO NOT reach out to the internet (because there is currently no hosting for them).  They look on the local filesystem in `/localrepo`.

#### Limitations

There are a few limitations that should be addressed to improve the Metron MPack installation.

* There is no external hosting for Metron packages (either RPMs or DEBs).  These have to be built locally and installed on each host in a repository located at `/localrepo`.

* Colocation of services should be enforced by Ambari.  

* Several configuration parameters used when installing Metron could retrieved from Ambari rather than requiring user input.  

* The MPack does not support upgrades.
