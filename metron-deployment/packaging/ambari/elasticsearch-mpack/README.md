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

This provides a Management Pack (MPack) extension for [Apache Ambari](https://ambari.apache.org/) that simplifies the provisioning, management and monitoring of Elasticsearch and Kibana on clusters of any size.

This allows you to easily install Elasticsearch and Kibana using a simple, guided process.  This also allows you to monitor cluster health.

### Prerequisites

* Ambari 2.4.2+

### Quick Start

1. Build the Elasticsearch MPack. Execute the following command from the project's root directory.
    ```
    mvn clean package -Pmpack -DskipTests
    ```

1. This results in the Mpack being produced at the following location.
    ```
    metron-deployment/packaging/ambari/elasticsearch-mpack/target/elasticsearch_mpack-x.y.z.0.tar.gz
    ```

1. Copy the tarball to the host where Ambari Server is installed.

1. Ensure that Ambari Server is stopped.

1. Install the MPack.
    ```
    ambari-server install-mpack --mpack=elasticsearch_mpack-x.y.z.0.tar.gz --verbose
    ```

1. Elasticsearch and Kibana will now be available as an installable service within Ambari.

### Installation Notes

The MPack will make all Elasticsearch services available in Ambari in the same manner as any other services in a stack.  These can be installed using Ambari's user interface using "Add Services" or during an initial cluster install.

#### Kerberization

Elasticsearch does not provide free native Kerberos support.

#### Limitations

There are a few limitations that should be addressed to improve the Elasticsearch MPack installation.

* The MPack does not support upgrades.
