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
# Indexing

## Introduction

The `indexing` topology is a topology dedicated to taking the data
from the enrichment topology that have been enriched and storing the data in one or more supported indices
* HDFS as rolled text files, one JSON blob per line
* Elasticsearch
* Solr

By default, this topology writes out to both HDFS and one of
Elasticsearch and Solr.

## Minimal Assumptions for Message Structure

If a message is missing the `source.type` field, the message tuple will be failed and not written
with an appropriate error indicated in the Storm UI and logs.

## Indexing Architecture

![Architecture](indexing_arch.png)

The indexing topology is extremely simple.  Data is ingested into kafka
and sent to 
* An indexing bolt configured to write to either elasticsearch or Solr
* An indexing bolt configured to write to HDFS under `/apps/metron/enrichment/indexed`

By default, errors during indexing are sent back into the `indexing` kafka queue so that they can be indexed and archived.

## Indexing Topology
The `indexing` topology as started by the `$METRON_HOME/bin/start_elasticsearch_topology.sh` 
or `$METRON_HOME/bin/start_solr_topology.sh`
script uses a default of one executor per bolt.  In a real production system, this should 
be customized by modifying the flux file in
`$METRON_HOME/flux/indexing/remote.yaml`. 
* Add a `parallelism` field to the bolts to give Storm a parallelism
  hint for the various components.  Give bolts which appear to be bottlenecks (e.g. the indexing bolt) a larger hint.
* Add a `parallelism` field to the kafka spout which matches the number of partitions for the enrichment kafka queue.
* Adjust the number of workers for the topology by adjusting the 
  `topology.workers` field for the topology. 

Finally, if workers and executors are new to you or you don't know where
to modify the flux file, the following might be of use to you:
* [Understanding the Parallelism of a Storm Topology](http://www.michael-noll.com/blog/2012/10/16/understanding-the-parallelism-of-a-storm-topology/)
* [Flux Docs](http://storm.apache.org/releases/current/flux.html)

### Rest endpoints
There are rest endpoints available to perform operations like start, stop, activate, deactivate on the `indexing` topologies.


|            |
| ---------- |
| [ `GET /api/v1/storm/indexing/batch`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingbatch)|
| [ `GET /api/v1/storm/indexing/batch/activate`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingbatchactivate)|
| [ `GET /api/v1/storm/indexing/batch/deactivate`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingbatchdeactivate)|
| [ `GET /api/v1/storm/indexing/batch/start`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingbatchstart)|
| [ `GET /api/v1/storm/indexing/batch/stop`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingbatchstop)|
| [ `GET /api/v1/storm/indexing/randomaccess`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingrandomaccess)|
| [ `GET /api/v1/storm/indexing/randomaccess/activate`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingrandomaccessactivate)|
| [ `GET /api/v1/storm/indexing/randomaccess/deactivate`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingrandomaccessdeactivate)|
| [ `GET /api/v1/storm/indexing/randomaccess/start`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingrandomaccessstart)|
| [ `GET /api/v1/storm/indexing/randomaccess/stop`](../../../metron-interface/metron-rest/README.md#get-apiv1stormindexingrandomaccessstop)|
