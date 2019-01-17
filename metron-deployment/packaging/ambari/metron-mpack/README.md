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

#### Kibana Dashboards

The dashboards installed by the Kibana custom action are managed by two JSON files:
* metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/dashboard/kibana.template
* metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/dashboard/dashboard-bulkload.json

The first file, `kibana.template`, is an Elasticsearch template that specifies the proper mapping types for the Kibana index. This configuration is necessary due to a bug
in the default dynamic mappings provided by Elasticsearch for long types versus integer that are incompatible with Kibana \[1\]. The second file, `dashboard-bulkload.json`,
contains all of the dashboard metadata necessary to create the Metron dashboard. It is an Elasticsearch bulk-insert formatted file \[2\] that contains a series
of documents necessary for setting up the dashboard in Elasticsearch. The main features installed are index patterns, searches, and a variety of visualizations
that are used in the Metron dashboard.

Deploying the existing dashboard is easy. Once the MPack is installed, run the Metron service's action "Load Template" to install dashboards.  This will no longer overwrite
the .kibana in Elasticsearch. The bulk load is configured to fail inserts for existing documents. If you want to _completely_ reload the dashboard, you would need to delete
the .kibana index and reload again from Ambari.

1. [https://github.com/elastic/kibana/issues/9888#issuecomment-298096954](https://github.com/elastic/kibana/issues/9888#issuecomment-298096954)
2. [https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-bulk.html](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-bulk.html)

##### Modifying Pre-Installed Dashboards

You can modify dashboards in Kibana and bring those changes into the core MPack distribution by performing the following steps:

1. Export the .kibana index from ES
2. Convert the data into the ES bulk load format
3. Replace the dashboard-bulkload.json file in the Metron MPack.

You can export the .kibana index using a tool like [https://github.com/taskrabbit/elasticsearch-dump](https://github.com/taskrabbit/elasticsearch-dump). The important
feature is to have one document per line. Here's an exmaple export using elasticsearch-dump

```
elasticdump \
  --input=http://node1:9200/.kibana \
  --output=~/dashboard-data.json \
  --type=data
```

Once you've exported the data, you can now format it as a bulk load ES file by running the import/export tool located in
metron-platform/metron-elasticsearch/src/main/java/org/apache/metron/elasticsearch/bulk/ElasticsearchImportExport.java. This tool can be run from full-dev
as follows

```
java -cp $METRON_HOME/lib/metron-elasticsearch-0.4.2-uber.jar org.apache.metron.elasticsearch.bulk.ElasticsearchImportExport \
  ~/dashboard-data.json \
  ~/dashboard-bulkload.json
```

Locate the "create" command for setting the default index by searching for "5.6.14". Change "create" to "index" so that it modifies the existing value. It should look similar to line 1 below.

```
{ "index" : { "_id": "5.6.14", "_type": "config" } }
{"defaultIndex":"AV-S2e81hKs1cXXnFMqN"}
```

Now copy this file to the Kibana MPack, overwriting the existing bulk load file. That should be everything needed to backup the dashboard.

**Note**: the dashboard Python Pickle binary file is deprecated and no longer used for backing up and restoring Kibana dashboards. The tooling is still provided as of this
version but is expected to be removed in the future. A section describing the deprecated backup process remains below.

##### Deprecated Dashboard Install/Backup Instructions

The dashboards installed by the Kibana custom action are managed by the dashboard.p file.  This file is created by exporting existing dashboards from a running Kibana instance.

To create a new version of the file, make any necessary changes to Kibana (e.g. on full-dev), and export with the appropriate script.

**Script Options**
```
[elasticsearch_host]        ES host
[elasticsearch_port]        ES port number
[input_output_filename]     Filename used for reading or writing out pickle file
[-s]                        Flag to indicate that the .kibana index should be saved locally. Not including this flag will overwrite the .kibana
                            index completely with the contents of 'input_output_filename'. Careful with this.
```

**Saving a Backup**
```
python packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/dashboard/dashboardindex.py \
$ES_HOST 9200 \
~/dashboard.p -s
```

**Restoring From a Backup**
```
python packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/dashboard/dashboardindex.py \
$ES_HOST 9200 \
~/dashboard.p
```

**Note**: This method of writing the Kibana dashboard to Elasticsearch will overwrite the entire .kibana index. Be sure to first backup the index first using either the new JSON
method described above, or writing out the dashboard.p pickle file using the old method (passing -s option to dashboardindex.py) described here.

#### Zeppelin Import

A custom action is available in Ambari to import Zeppelin dashboards. See the [metron-indexing documentation](../../../../metron-platform/metron-indexing) for more information.

#### Offline Installation

Retrieval of the GeoIP and ASN databases (both from MaxMind) is the only point during installation that reaches out to the internet. For an offline installation, the URL for the databases can be manually set to a local path on the file system such as  `file:///home/root/geoip/GeoLite2-City.tar.gz`.

The properties for configuration are `geoip_url` and `asn_url` in the `Enrichment` section.

The RPMs DO NOT reach out to the internet (because there is currently no hosting for them).  They look on the local filesystem in `/localrepo`.

#### Limitations

There are a few limitations that should be addressed to improve the Metron MPack installation.

* There is no external hosting for Metron packages (either RPMs or DEBs).  These have to be built locally and installed on each host in a repository located at `/localrepo`.

* Several configuration parameters used when installing Metron could retrieved from Ambari rather than requiring user input.  

* The MPack does not support upgrades.
