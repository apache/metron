#Enrichment

## Introduction

The `enrichment` topology is a topology dedicated to taking the data
from the parsing topologies that have been normalized into the Metron
data format (e.g. a JSON Map structure with `original_message` and
`timestamp`) and 
* Enriching messages with external data from data stores (e.g. hbase) by
  adding new fields based on existing fields in the messages.
* Marking messages as threats based on data in external data stores
* Marking threat alerts with a numeric triage level based on a set of
  Stellar rules.

## Enrichment Architecture

![Architecture](enrichment_arch.png)

## Enrichment Configuration

The configuration for the `enrichment` topology, the topology primarily
responsible for enrichment and threat intelligence enrichment, is
defined by JSON documents stored in zookeeper.

There are two types of configurations at the moment, `global` and
`sensor` specific.  

## Global Configuration 

See the "[Global Configuration](../metron-common)" section.

##Sensor Enrichment Configuration

The sensor specific configuration is intended to configure the
individual enrichments and threat intelligence enrichments for a given
sensor type (e.g. `snort`).

Just like the global config, the format is a JSON stored in zookeeper.
The configuration is a complex JSON object with the following top level fields:

* `index` : The name of the sensor
* `batchSize` : The size of the batch that is written to the indices at once.
* `enrichment` : A complex JSON object representing the configuration of the enrichments
* `threatIntel` : A complex JSON object representing the configuration of the threat intelligence enrichments

###The `enrichment` Configuration 


| Field            | Description                                                                                                                                                                                                                   | Example                                                          |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. Note: applies to hbaseEnrichment only. | `"fieldToTypeMap" : { "ip_src_addr" : [ "asset_enrichment" ] }`  |
| `fieldMap`       | The map of enrichment bolts names to configuration handlers which know how to split the message up.  The simplest of which is just a list of fields.  More complex examples would be the stellar enrichment which provides stellar statements. Each field listed in the array arg is sent to the enrichment referenced in the key. Cardinality of fields to enrichments is many-to-many. | `"fieldMap": {"hbaseEnrichment": ["ip_src_addr","ip_dst_addr"]}` |
| `config`         | The general configuration for the enrichment                                                                                                                                                                                  | `"config": {"typeToColumnFamily": { "asset_enrichment" : "cf" } }` |

The `config` map is intended to house enrichment specific configuration.
For instance, for the `hbaseEnrichment`, the mappings between the
enrichment types to the column families is specified.

The `fieldMap`contents are of interest because they contain the routing and configuration information for the enrichments.  When we say 'routing', we mean how the messages get split up and sent to the enrichment adapter bolts.  The simplest, by far, is just providing a simple list as in
```
    "fieldMap": {
      "geo": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "host": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "hbaseEnrichment": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
      }
```
Based on this sample config, both ip_src_addr and ip_dst_addr will go to the `geo`, `host`, and `hbaseEnrichment` adapter bolts. For the `geo`, `host` and `hbaseEnrichment`, this is sufficient.  However, more complex enrichments may contain their own configuration.  Currently, the `stellar` enrichment requires a more complex configuration, such as:
```
    "fieldMap": {
       ...
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      }
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
        }
      }
    }
```

Whereas the simpler enrichments just need a set of fields explicitly stated so they can be separated from the message and sent to the enrichment adapter bolt for enrichment and ultimately joined back in the join bolt, the stellar enrichment has its set of required fields implicitly stated through usage.  For instance, if your stellar statement references a field, it should be included and if not, then it should not be included.  We did not want to require users to make explicit the implicit.

The other way in which the stellar enrichment is somewhat more complex is in how the statements are executed.  In the general purpose case for a list of fields, those fields are used to create a message to send to the enrichment adapter bolt and that bolt's worker will handle the fields one by one in serial for a given message.  For stellar enrichment, we wanted to have a more complex design so that users could specify the groups of stellar statements sent to the same worker in the same message (and thus executed sequentially).  Consider the following configuration:
```
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      "bar" : TO_LOWER(source.type)"
                      }
         ,"text" : {
                   "ALL_CAPS" : "TO_UPPER(source.type)"
                   }
        }
      }
    }
```
We have a group called `numeric` whose stellar statements will be executed sequentially.  In parallel to that, we have the group of stellar statements under the group `text` executing.  The intent here is to allow you to not force higher latency operations to be done sequentially. You can use any name for your groupings you like. Be aware that the configuration is a map and duplicate configuration keys' values are not combined, so the duplicate configuration value will be overwritten.

###The `threatIntel` Configuration 

| Field            | Description                                                                                                                                                                                                                                   | Example                                                                  |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase threat intel enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. Note: applies to hbaseThreatIntel only. | `"fieldToTypeMap" : { "ip_src_addr" : [ "malicious_ips" ] }`             |
| `fieldMap`       | The map of threat intel enrichment bolts names to fields in the JSON messages. Each field is sent to the threat intel enrichment bolt referenced in the key. Each field listed in the array arg is sent to the enrichment referenced in the key. Cardinality of fields to enrichments is many-to-many.                                                     | `"fieldMap": {"hbaseThreatIntel": ["ip_src_addr","ip_dst_addr"]}`        |
| `triageConfig`   | The configuration of the threat triage scorer.  In the situation where a threat is detected, a score is assigned to the message and embedded in the indexed message.                                                                    | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `config`         | The general configuration for the Threat Intel                                                                                                                                                                                                | `"config": {"typeToColumnFamily": { "malicious_ips","cf" } }`            |

The `config` map is intended to house threat intel specific configuration.
For instance, for the `hbaseThreatIntel` threat intel adapter, the mappings between the
enrichment types to the column families is specified.

The `triageConfig` field is also a complex field and it bears some description:

| Field            | Description                                                                                                                                             | Example                                                                  |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `riskLevelRules` | The mapping of Stellar (see above) queries to a score.                                                                                                  | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `aggregator`     | An aggregation function that takes all non-zero scores representing the matching queries from `riskLevelRules` and aggregates them into a single score. | `"MAX"`                                                                  |

The supported aggregation functions are:
* `MAX` : The max of all of the associated values for matching queries
* `MIN` : The min of all of the associated values for matching queries
* `MEAN` : The mean of all of the associated values for matching queries
* `POSITIVE_MEAN` : The mean of the positive associated values for the matching queries.

###Example Configuration

An example configuration for the YAF sensor is as follows:
```json
{
  "index": "yaf",
  "batchSize": 5,
  "enrichment": {
    "fieldMap": {
      "geo": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "host": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "hbaseEnrichment": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    }
  ,"fieldToTypeMap": {
      "ip_src_addr": [
        "playful_classification"
      ],
      "ip_dst_addr": [
        "playful_classification"
      ]
    }
  },
  "threatIntel": {
    "fieldMap": {
      "hbaseThreatIntel": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    },
    "fieldToTypeMap": {
      "ip_src_addr": [
        "malicious_ip"
      ],
      "ip_dst_addr": [
        "malicious_ip"
      ]
    },
    "triageConfig" : {
      "riskLevelRules" : {
        "ip_src_addr == '10.0.2.3' or ip_dst_addr == '10.0.2.3'" : 10
      },
      "aggregator" : "MAX"
    }
  }
}
```

ThreatIntel alert levels are emitted as a new field "threat.triage.level." So for the example above, an incoming message that trips the `ip_src_addr` rule will have a new field threat.triage.level=10.

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
  "index": "squid",
  "batchSize": 1,
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

Note that we could have used any Stellar statements here, including calling out to HBase via `ENRICHMENT_GET` and `ENRICHMENT_EXISTS` or even calling a machine learning model via [Model as a Service](../../metron-analytics/metron-maas-service).

# Notes on Performance Tuning

Default installed Metron is untuned for production deployment.  There
are a few knobs to tune to get the most out of your system.

## Kafka Queue
The `enrichments` kafka queue is a collection point from all of the
parser topologies.  As such, make sure that the number of partitions in
the kafka topic is sufficient to handle the throughput that you expect
from your parser topologies.

## Enrichment Topology
The enrichment topology as started by the `$METRON_HOME/bin/start_enrichment_topology.sh` 
script uses a default of one executor per bolt.  In a real production system, this should 
be customized by modifying the flux file in
`$METRON_HOME/flux/enrichment/remote.yaml`. 
* Add a `parallelism` field to the bolts to give Storm a parallelism hint for the various components.  Give bolts which appear to be bottlenecks (e.g. stellar enrichment bolt, hbase enrichment and threat intel bolts) a larger hint.
* Add a `parallelism` field to the kafka spout which matches the number of partitions for the enrichment kafka queue.
* Adjust the number of workers for the topology by adjusting the 
  `topology.workers` field for the topology. 

Finally, if workers and executors are new to you or you don't know where
to modify the flux file, the following might be of use to you:
* [Understanding the Parallelism of a Storm Topology](http://www.michael-noll.com/blog/2012/10/16/understanding-the-parallelism-of-a-storm-topology/)
* [Flux Docs](http://storm.apache.org/releases/current/flux.html)
