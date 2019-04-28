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
# Enrichment

## Introduction

This module holds code for enrichments running in a Storm topology.

## Enrichment Architecture

![Unified Architecture](unified_enrichment_arch.svg)

### Unified Enrichment Topology

The unified enrichment topology uses data parallelism as opposed to the deprecated
split/join topology's task parallelism. This architecture uses a worker pool to fully
enrich any message within a worker.  This results in
* Fewer bolts in the topology
* Each bolt fully operates on a message.
* Fewer network hops

This architecture is fully backwards compatible with the old split-join
topology; the only difference is how the enrichment will operate on each
message (in one bolt where the split/join is done in a threadpool as opposed
to split across multiple bolts).

#### Configuring It

There are two parameters which you might want to tune in this topology.
Both of them are topology configuration adjustable in the flux file
`$METRON_HOME/config/flux/enrichment/remote-unified.yaml`:
* `metron.threadpool.size` : The size of the threadpool.  This can take a number or a multiple of the number of cores (e.g. `5C` to 5 times the number of cores).  The default is `2C`.
* `metron.threadpool.type` : The type of threadpool. (note: descriptions taken from [here](https://zeroturnaround.com/rebellabs/fixedthreadpool-cachedthreadpool-or-forkjoinpool-picking-correct-java-executors-for-background-tasks/)).
   * `FIXED` is a fixed threadpool of size `n`. `n` threads will process tasks at the time, when the pool is saturated, new tasks will get added to a queue without a limit on size. Good for CPU intensive tasks.  This is the default.
   * `WORK_STEALING` is a work stealing threadpool.  This will create and shut down threads dynamically to accommodate the required parallelism level. It also tries to reduce the contention on the task queue, so can be really good in heavily loaded environments. Also good when your tasks create more tasks for the executor, like recursive tasks.

In order to configure the parallelism for the enrichment bolt and threat
intel bolt, the configurations will be taken from the respective join bolt
parallelism.  When proper ambari support for this is added, we will add
its own property.

### Split-Join Enrichment Topology

The now-deprecated split/join topology is also available and performs enrichments in parallel.
This poses some issues in terms of ease of tuning and reasoning about performance.

![Architecture](enrichment_arch.png)

#### Using It

In order to use the older, deprecated topology, you will need to
* Edit `$METRON_HOME/bin/start_enrichment_topology.sh` and adjust it to use `remote-splitjoin.yaml` instead of `remote-unified.yaml`
* Restart the enrichment topology.

## Enrichment Configuration

The configuration for the `enrichment` topology, the topology primarily
responsible for enrichment and threat intelligence enrichment, is
defined by JSON documents stored in zookeeper.

See [Enrichment Configuration](../metron-enrichment-common#enrichment-configuration)

# Example Enrichment via Stellar

Let's walk through doing a simple enrichment using Stellar on your cluster using the Squid topology.

## Install Prerequisites
Now let's install some prerequisites:
* Squid client via `yum install squid`
* ES Head plugin via `/usr/share/elasticsearch/bin/plugin install mobz/elasticsearch-head`

Start Squid via `service squid start`

## Adjust Enrichment Configurations for Squid to Call Stellar
Let's adjust the configurations for the Squid topology to annotate the messages using some Stellar functions.

* Edit the squid enrichment configuration at `$METRON_HOME/config/zookeeper/enrichments/squid.json` (this file will not exist, so create a new one) to add some new fields based on stellar queries:

```
{
  "enrichment" : {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      }
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
        }
      }
     }
  },
  "threatIntel" : {
    "fieldMap":{
     "stellar" : {
        "config" : {
          "bar" : "TO_UPPER(source.type)"
        }
      }
    },
    "triageConfig" : {
    }
  }
}
```
We have added the following fields as part of the enrichment phase of the enrichment topology:
* `foo` ==  2
* `ALL_CAPS` == SQUID

We have added the following as part of the threat intel:
* ` bar` == SQUID

Please note that foo and ALL_CAPS will be applied in separate workers due to them being in separate groups.

* Upload new configs via `$METRON_HOME/bin/zk_load_configs.sh --mode PUSH -i $METRON_HOME/config/zookeeper -z node1:2181`
* Make the Squid topic in kafka via `/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper node1:2181 --create --topic squid --partitions 1 --replication-factor 1`

## Start Topologies and Send Data
Now we need to start the topologies and send some data:
* Start the squid topology via `$METRON_HOME/bin/start_parser_topology.sh -k node1:6667 -z node1:2181 -s squid`
* Generate some data via the squid client:
  * `squidclient http://yahoo.com`
  * `squidclient http://cnn.com`
* Send the data to kafka via `cat /var/log/squid/access.log | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list node1:6667 --topic squid`
* Browse the data in elasticsearch via the ES Head plugin @ [http://node1:9200/_plugin/head/](http://node1:9200/_plugin/head/) and verify that in the squid index you have two documents
* Ensure that the documents have new fields `foo`, `bar` and `ALL_CAPS` with values as described above.

Note that we could have used any Stellar statements here, including calling out to HBase via `ENRICHMENT_GET` and `ENRICHMENT_EXISTS` or even calling a machine learning model via [Model as a Service](../../../metron-analytics/metron-maas-service).
