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
# Metron Performance Tuning Guide

- [Overview](#overview)
- [General Tuning Suggestions](#general-tuning-suggestions)
- [Component Tuning Levers](#component-tuning-levers)
- [Use Case Specific Tuning Suggestions](#use-case-specific-tuning-suggestions)
- [Debugging](#debugging)
- [Issues](#issues)
- [Reference](#reference)

## Overview

This document provides guidance from our experiences tuning the Apache Metron Storm topologies for maximum performance. You'll find
suggestions for optimum configurations under a 1 gbps load along with some guidance around the tooling we used to monitor and assess
our throughput.

In the simplest terms, Metron is a streaming architecture created on top of Kafka and three main types of Storm topologies: parsers,
enrichment, and indexing. Each parser has it's own topology and there is also a highly performant, specialized spout-only topology
for streaming PCAP data to HDFS. We found that the architecture can be tuned almost exclusively through using a few primary Storm and
Kafka parameters along with a few Metron-specific options. You can think of the data flow as being similar to water flowing through a
pipe, and the majority of these options assist in tweaking the various pipe widths in the system.

## General Tuning Suggestions

### Storm Executors vs. Tasks

Note that there is currently no method for specifying the number of tasks from the number of executors in Flux topologies (enrichment,
 indexing). By default, the number of tasks will equal the number of executors. Logically, setting the number of tasks equal to the number
of executors is sensible. Storm enforces num executors <= num tasks. The reason you might set the number of tasks higher than the number of
executors is for future performance tuning and rebalancing without the need to bring down your topologies. The number of tasks is fixed
at topology startup time whereas the number of executors can be increased up to a maximum value equal to the number of tasks.

### Kafka Spout Configuration

When configuring Storm Kafka spouts, we found that the default values for

- `poll.timeout.ms`
- `offset.commit.period.ms`
- `max.uncommitted.offsets `

worked well in nearly all cases. As a general rule, it was optimal to set spout parallelism equal to the number of partitions used in your Kafka topic. Any greater
parallelism will leave you with idle consumers since Kafka limits the max number of consumers to the number of partitions. This is
important because Kafka has certain ordering guarantees for message delivery per partition that would not be possible if more than
one consumer in a given consumer group were able to read from that partition.

## Sensor Topology Tuning Suggestions

If you are using stellar field transformations in your sensors, by default, stellar expressions
are not cached.  Sensors that use stellar field transformations by see a performance
boost by turning on caching via setting the `cacheConfig`
[property](metron-parsers-common#parser_configuration).
This is beneficial if your transformations:

* Are complex (e.g. `ENRICHMENT_GET` calls or other high latency calls)
* All Yield the same results for the same inputs ( caching is either off or applied to all transformations)
  * If any of your transformations are non-deterministic, caching should not be used as it will result in the likelihood of incorrect results being returned.


## Component Tuning Levers

### High Level Overview

There are a number of levers that can be set while tuning a Metron cluster. The main services to interact with for performance tuning are: Kafka, Storm, HDFS, and indexing (Elasticsearch or Solr). For each service, here is a high level breakdown of the major knobs and levers that can be modified while tuning your cluster.

- Kafka
    - Number partitions
- Storm
    - Kafka spout
        - Polling frequency
        - Polling timeouts
        - Offset commit period
        - Max uncommitted offsets
    - Number workers (OS processes)
    - Number executors (threads in a process)
    - Number ackers
    - Max spout pending
    - Spout and bolt parallelism
- HDFS
    - Replication factor

### Kafka Tuning

The main lever you're going to work with when tuning Kafka throughput will be the number of partitions. A handy method for deciding how many partitions to use
is to first calculate the throughput for a single producer (p) and a single consumer (c), and then use that with the desired throughput (t) to roughly estimate the number
of partitions to use. You would want at least max(t/p, t/c) partitions to attain the desired throughput. See https://www.confluent.io/blog/how-to-choose-the-number-of-topicspartitions-in-a-kafka-cluster/
for more details.

### Storm Tuning

#### Overview

There are quite a few options you will be confronted with when tuning your Storm topologies and this is largely trial and error. As a general rule of thumb,
we recommend starting with the defaults and smaller numbers in terms of parallelism while iteratively working up until the desired performance is achieved.
You will find the offset lag tool indispensable while verifying your settings.

We won't go into a full discussion about Storm's architecture - see references section for more info - but there are some general rules of thumb that should be
followed. It's first important to understand the ways you can impact parallelism in a Storm topology.

- num tasks
- num executors (parallelism hint)
- num workers

Tasks are instances of a given spout or bolt, executors are threads in a process, and workers are jvm processes. You'll want the number of tasks as a multiple of the number of executors,
the number of executors as multiple of the number of workers, and the number of workers as a multiple of the number of machines. The main reason for this approach is
 that it will give a uniform distribution of work to each machine and jvm process. More often than not, your number of tasks will be equal to the number of executors, which
 is the default in Storm. Flux does not actually provide a way to independently set number of tasks, so for enrichments and indexing, which use Flux, num tasks will always equal
 num executors.

You can change the number of workers via the Storm property `topology.workers`

__Other Storm Settings__

```
topology.max.spout.pending
```
This is the maximum number of tuples that can be in flight (ie, not yet acked) at any given time within your topology. You set this as a form of backpressure to ensure
you don't flood your topology.


```
topology.ackers.executors
```

This specifies how many threads should be dedicated to tuple acking. We found that setting this equal to the number of partitions in your inbound Kafka topic worked well.

__spout-config.json__

```
{
    ...
    "spout.pollTimeoutMs" : 200,
    "spout.maxUncommittedOffsets" : 10000000,
    "spout.offsetCommitPeriodMs" : 30000
}
```

Above is a snippet for configuring parsers. These are the spout recommended defaults from Storm and are currently the defaults provided in the Kafka spout itself. In fact, if you find the recommended defaults work fine for you,
then you can omit these settings altogether.

#### Where to Find Tuning Properties

**Important:** The parser topologies are deployed via a builder pattern that takes parameters from the CLI as set via Ambari. The enrichment and indexing topologies are configured
using a Storm Flux file, a configuration properties file, and Ambari. Here is a setting materialization summary for each of the topology types:

- Parsers
	- Management UI -> parser json config and CLI -> Storm
- Enrichment
	- Ambari UI -> properties file -> Flux -> Storm
- Indexing
	- Ambari UI -> properties file -> Flux -> Storm

**Parsers**

This is a mapping of the various performance tuning properties for parsers and how they are materialized.

See more detail on starting parsers [here](https://github.com/apache/metron/blob/master/metron-platform/metron-parsing/metron-parsers-common/README.md#starting-the-parser-topology)

| Category                    | Management UI Property Name                | JSON Config File Property Name     | CLI Option                                                                                     | Storm Property Name             |  Notes                                                                        |
|-----------------------------|--------------------------------------------|------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------|-------------------------------------------------------------------------------|
| Storm topology config       | Num Workers                                | n/a                                | -nw,--num_workers <NUM_WORKERS>                                                                | topology.workers                |                                                                               |
|                             | Num Ackers                                 | n/a                                | -na,--num_ackers <NUM_ACKERS>                                                                  | topology.acker.executors        |                                                                               |
|                             | Storm Config                               | topology.max.spout.pending         | -e,--extra_topology_options <JSON_FILE>, e.g. { "topology.max.spout.pending" : NUM }           | topology.max.spout.pending      | Put property in JSON format in a file named `storm-<MY_PARSER>-config.json`   |
| Kafka spout                 | Spout Parallelism                          | n/a                                | -sp,--spout_p <SPOUT_PARALLELISM_HINT>                                                         | n/a                             |                                                                               |
|                             | Spout Num Tasks                            | n/a                                | -snt,--spout_num_tasks <NUM_TASKS>                                                             | n/a                             |                                                                               |
|                             | Spout Config                               | spout.pollTimeoutMs                | -esc,--extra_kafka_spout_config <JSON_FILE>, e.g. { "spout.pollTimeoutMs" : 200 }              | n/a                             | Put property in JSON format in a file named `spout-<MY_PARSER>-config.json`   |
|                             | Spout Config                               | spout.maxUncommittedOffsets        | -esc,--extra_kafka_spout_config <JSON_FILE>, e.g. { "spout.maxUncommittedOffsets" : 10000000 } | n/a                             | Put property in JSON format in a file named `spout-<MY_PARSER>-config.json`   |
|                             | Spout Config                               | spout.offsetCommitPeriodMs         | -esc,--extra_kafka_spout_config <JSON_FILE>, e.g. { "spout.offsetCommitPeriodMs" : 30000 }     | n/a                             | Put property in JSON format in a file named `spout-<MY_PARSER>-config.json`   |
| Parser bolt                 | Parser Num Tasks                           | n/a                                | -pnt,--parser_num_tasks <NUM_TASKS>                                                            | n/a                             |                                                                               |
|                             | Parser Parallelism                         | n/a                                | -pp,--parser_p <PARALLELISM_HINT>                                                              | n/a                             |                                                                               |
|                             | Parser Parallelism                         | n/a                                | -pp,--parser_p <PARALLELISM_HINT>                                                              | n/a                             |                                                                               |

**Enrichment**

__Note__ These recommendations are based on the deprecated split-join enrichment topology. See [Enrichment Performance](metron-enrichment/Performance.md) for tuning recommendations for the new default unified enrichment topology.

This is a mapping of the various performance tuning properties for enrichments and how they are materialized.

Flux file found here - $METRON_HOME/flux/enrichment/remote-splitjoin.yaml

_Note 1:_ Changes to Flux file properties that are managed by Ambari will render Ambari unable to further manage the property.

_Note 2:_ Many of these settings will be irrelevant in the alternate non-split-join topology

| Category                    | Ambari Property Name                       | enrichment.properties property                         | Flux Property                                          | Flux Section Location               | Storm Property Name             | Notes                                  |
|-----------------------------|--------------------------------------------|--------------------------------------------------------|--------------------------------------------------------|-------------------------------------|---------------------------------|----------------------------------------|
| Storm topology config       | enrichment_workers                         | enrichment.workers                                     | topology.workers                                       | line 18, config                     | topology.workers                |                                        |
|                             | enrichment_acker_executors                 | enrichment.acker.executors                             | topology.acker.executors                               | line 18, config                     | topology.acker.executors        |                                        |
|                             | enrichment_topology_max_spout_pending      | topology.max.spout.pending                             | topology.max.spout.pending                             | line 18, config                     | topology.max.spout.pending      |                                        |
| Kafka spout                 | enrichment_kafka_spout_parallelism         | kafka.spout.parallelism                                | parallelism                                            | line 245, id: kafkaSpout            | n/a                             |                                        |
|                             | n/a                                        | session.timeout.ms                                     | session.timeout.ms                                     | line 201, id: kafkaProps            | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | enable.auto.commit                                     | enable.auto.commit                                     | line 201, id: kafkaProps            | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setPollTimeoutMs                                       | line 230, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setMaxUncommittedOffsets                               | line 230, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setOffsetCommitPeriodMs                                | line 230, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
| Enrichment splitter         | enrichment_split_parallelism               | enrichment.split.parallelism                           | parallelism                                            | line 253, id: enrichmentSplitBolt   | n/a                             |                                        |
| Enrichment joiner           | enrichment_join_parallelism                | enrichment.join.parallelism                            | parallelism                                            | line 316, id: enrichmentJoinBolt    | n/a                             |                                        |
| Threat intel splitter       | threat_intel_split_parallelism             | threat.intel.split.parallelism                         | parallelism                                            | line 338, id: threatIntelSplitBolt  | n/a                             |                                        |
| Threat intel joiner         | threat_intel_join_parallelism              | threat.intel.join.parallelism                          | parallelism                                            | line 376, id: threatIntelJoinBolt   | n/a                             |                                        |
| Output bolt                 | kafka_writer_parallelism                   | kafka.writer.parallelism                               | parallelism                                            | line 397, id: outputBolt            | n/a                             |                                        |

When adding Kafka spout properties, there are 3 ways you'll do this.

1. Ambari: If they are properties managed by Ambari (noted in the table under 'Ambari Property Name'), look for the setting in Ambari.

1. Flux -> kafkaProps: add a new key/value to the kafkaProps section HashMap on line 201. For example, if you want to set the Kafka Spout consumer's session.timeout.ms to 30 seconds, you would add the following:

    ```
           -   name: "put"
               args:
                   - "session.timeout.ms"
                   - 30000
    ```

1. Flux -> kafkaConfig: add a new setter to the kafkaConfig section on line 230. For example, if you want to set the Kafka Spout consumer's poll timeout to 200 milliseconds, you would add the following under `configMethods`:

    ```
             -   name: "setPollTimeoutMs"
                 args:
                     - 200
    ```

**Indexing (Batch)**

This is a mapping of the various performance tuning properties for indexing and how they are materialized.

Flux file can be found here - $METRON_HOME/flux/indexing/batch/remote.yaml.

Note: Changes to Flux file properties that are managed by Ambari will render Ambari unable to further manage the property.

| Category                    | Ambari Property Name                       | hdfs.properties property                               | Flux Property                                          | Flux Section Location               | Storm Property Name             | Notes                                  |
|-----------------------------|--------------------------------------------|--------------------------------------------------------|--------------------------------------------------------|-------------------------------------|---------------------------------|----------------------------------------|
| Storm topology config       | enrichment_workers                         | enrichment.workers                                     | topology.workers                                       | line 19, config                     | topology.workers                |                                        |
|                             | enrichment_acker_executors                 | enrichment.acker.executors                             | topology.acker.executors                               | line 19, config                     | topology.acker.executors        |                                        |
|                             | enrichment_topology_max_spout_pending      | topology.max.spout.pending                             | topology.max.spout.pending                             | line 19, config                     | topology.max.spout.pending      |                                        |
| Kafka spout                 | batch_indexing_kafka_spout_parallelism     | kafka.spout.parallelism                                | parallelism                                            | line 123, id: kafkaSpout            | n/a                             |                                        |
|                             | n/a                                        | session.timeout.ms                                     | session.timeout.ms                                     | line 80, id: kafkaProps             | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | enable.auto.commit                                     | enable.auto.commit                                     | line 80, id: kafkaProps             | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setPollTimeoutMs                                       | line 108, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setMaxUncommittedOffsets                               | line 108, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
|                             | n/a                                        | n/a                                                    | setOffsetCommitPeriodMs                                | line 108, id: kafkaConfig           | n/a                             | Kafka consumer client property         |
| Output bolt                 | hdfs_writer_parallelism                    | hdfs.writer.parallelism                                | parallelism                                            | line 133, id: hdfsIndexingBolt      | n/a                             |                                        |
|                             | n/a                                        | n/a                                                    | hdfsSyncPolicy (see notes below)                       | line 47, id: hdfsWriter             | n/a                             | See notes below about adding this prop |
|                             | bolt_hdfs_rotation_policy_units            | bolt.hdfs.rotation.policy.units                        | constructorArgs                                        | line 41, id: hdfsRotationPolicy     | n/a                             |                                        |
|                             | bolt_hdfs_rotation_policy_count            | bolt.hdfs.rotation.policy.count                        | constructorArgs                                        | line 41, id: hdfsRotationPolicy     | n/a                             |                                        |

_Note_: HDFS sync policy is not currently managed via Ambari. You will need to modify the Flux file directly to accommodate this setting. e.g.

Add a new setter to the hdfsWriter around line 56. Lines 53-55 provided for context.

```
 53             -   name: "withRotationPolicy"
 54                 args:
 55                     - ref: "hdfsRotationPolicy
 56             -   name: "withSyncPolicy"
 57                 args:
 58                     - ref: "hdfsSyncPolicy
```

Add an hdfsSyncPolicy after the hdfsRotationPolicy that appears on line 41. e.g.

```
 41     -   id: "hdfsRotationPolicy"
...
 45           - "${bolt.hdfs.rotation.policy.units}"
 46
 47     -   id: "hdfsSyncPolicy"
 48         className: "org.apache.storm.hdfs.bolt.sync.CountSyncPolicy"
 49         constructorArgs:
 50           -  100000
```

## Use Case Specific Tuning Suggestions

The below discussion outlines a specific tuning exercise we went through for driving 1 Gbps of traffic through a Metron cluster running with 4 Kafka brokers and 4
Storm Supervisors.

General machine specs

- 10 Gb network cards
- 256 GB memory
- 12 disks
- 32 cores

### Performance Monitoring Tools

Before we get to tuning our cluster, it helps to describe what we might actually want to monitor as well as any potential
pain points. Prior to switching over to the new Storm Kafka client, which leverages the new Kafka consumer API under the hood, offsets
were stored in Zookeeper. While the broker hosts are still stored in Zookeeper, this is no longer true for the offsets which are now
stored in Kafka itself. This is a configurable option, and you may switch back to Zookeeper if you choose, but Metron is currently using
the new defaults. With this in mind, there are some useful tools that come with Storm and Kafka that we can use to monitor our topologies.

#### Tooling

Kafka

- consumer group offset lag viewer
- There is a GUI tool to make creating, modifying, and generally managing your Kafka topics a bit easier - see https://github.com/yahoo/kafka-manager
- console consumer - useful for quickly verifying topic contents

Storm

- Storm UI - http://www.malinga.me/reading-and-understanding-the-storm-ui-storm-ui-explained/

#### Example - Viewing Kafka Offset Lags

First we need to setup some environment variables
```
export BROKERLIST=<your broker comma-delimated list of host:ports>
export ZOOKEEPER=<your zookeeper comma-delimated list of host:ports>
export KAFKA_HOME=<kafka home dir>
export METRON_HOME=<your metron home>
export HDP_HOME=<your HDP home>
```

If you have Kerberos enabled, setup the security protocol
```
$ cat /tmp/consumergroup.config
security.protocol=SASL_PLAINTEXT
```

Now run the following command for a running topology's consumer group. In this example we are using enrichments.
```
${KAFKA_HOME}/bin/kafka-consumer-groups.sh \
    --command-config=/tmp/consumergroup.config \
    --describe \
    --group enrichments \
    --bootstrap-server $BROKERLIST \
    --new-consumer
```

This will return a table with the following output depicting offsets for all partitions and consumers associated with the specified
consumer group:

```
GROUP                          TOPIC              PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             OWNER
enrichments                    enrichments        9          29746066        29746067        1               consumer-2_/xxx.xxx.xxx.xxx
enrichments                    enrichments        3          29754325        29754326        1               consumer-1_/xxx.xxx.xxx.xxx
enrichments                    enrichments        43         29754331        29754332        1               consumer-6_/xxx.xxx.xxx.xxx
...
```

_Note_: You won't see any output until a topology is actually running because the consumer groups only exist while consumers in the
spouts are up and running.

The primary column we're concerned with paying attention to is the LAG column, which is the current delta calculation between the
current and end offset for the partition. This tells us how close we are to keeping up with incoming data. And, as we found through
multiple trials, whether there are any problems with specific consumers getting stuck.

Taking this one step further, it's probably more useful if we can watch the offsets and lags change over time. In order to do this
we'll add a "watch" command and set the refresh rate to 10 seconds.

```
watch -n 10 -d ${KAFKA_HOME}/bin/kafka-consumer-groups.sh \
    --command-config=/tmp/consumergroup.config \
    --describe \
    --group enrichments \
    --bootstrap-server $BROKERLIST \
    --new-consumer
```

Every 10 seconds the command will re-run and the screen will be refreshed with new information. The most useful bit is that the
watch command will highlight the differences from the current output and the last output screens.

### Parser Tuning

We'll be using the bro sensor in this example. Note that the parsers and PCAP use a builder utility, as opposed to enrichments and indexing, which use Flux.

We started with a single partition for the inbound Kafka topics and eventually worked our way up to 48. And We're using the following pending value, as shown below.
The default is 'null' which would result in no limit.

__storm-bro.config__

```
{
    ...
    "topology.max.spout.pending" : 2000
    ...
}
```

And the following default spout settings. Again, this can be ommitted entirely since we are using the defaults.

__spout-bro.config__

```
{
    ...
    "spout.pollTimeoutMs" : 200,
    "spout.maxUncommittedOffsets" : 10000000,
    "spout.offsetCommitPeriodMs" : 30000
}
```

And we ran our bro parser topology with the following options. We did not need to fully match the number of Kafka partitions with our parallelism in this case,
though you could certainly do so if necessary. Notice that we only needed 1 worker.

```
$METRON_HOME/bin/start_parser_topology.sh \
    -e ~metron/.storm/storm-bro.config \
    -esc ~/.storm/spout-bro.config \
    -k $BROKERLIST \
    -ksp SASL_PLAINTEXT \
    -nw 1 \
    -ot enrichments \
    -pnt 24 \
    -pp 24 \
    -s bro \
    -snt 24 \
    -sp 24 \
    -z $ZOOKEEPER \
```

From the usage docs, here are the options we've used. The full reference can be found [here](../metron-platform/metron-parsing/metron-parsers-common/README.md#Starting_the_Parser_Topology).

```
usage: start_parser_topology.sh
 -e,--extra_topology_options <JSON_FILE>               Extra options in the form
                                                       of a JSON file with a map
                                                       for content.
 -esc,--extra_kafka_spout_config <JSON_FILE>           Extra spout config options
                                                       in the form of a JSON file
                                                       with a map for content.
                                                       Possible keys are:
                                                       retryDelayMaxMs,retryDelay
                                                       Multiplier,retryInitialDel
                                                       ayMs,stateUpdateIntervalMs
                                                       ,bufferSizeBytes,fetchMaxW
                                                       ait,fetchSizeBytes,maxOffs
                                                       etBehind,metricsTimeBucket
                                                       SizeInSecs,socketTimeoutMs
 -k,--kafka <BROKER_URL>                               Kafka Broker URL
 -ksp,--kafka_security_protocol <SECURITY_PROTOCOL>    Kafka Security Protocol
 -nw,--num_workers <NUM_WORKERS>                       Number of Workers
 -ot,--output_topic <KAFKA_TOPIC>                      Output Kafka Topic
 -pnt,--parser_num_tasks <NUM_TASKS>                   Parser Num Tasks
 -pp,--parser_p <PARALLELISM_HINT>                     Parser Parallelism Hint
 -s,--sensor <SENSOR_TYPE>                             Sensor Type
 -snt,--spout_num_tasks <NUM_TASKS>                    Spout Num Tasks
 -sp,--spout_p <SPOUT_PARALLELISM_HINT>                Spout Parallelism Hint
 -z,--zk <ZK_QUORUM>                                   Zookeeper Quroum URL
                                                       (zk1:2181,zk2:2181,...
```

### Enrichment Tuning

__Note__ These tuning suggestions are based on the deprecated split-join topology.

We landed on the same number of partitions for enrichemnt and indexing as we did for bro - 48.

For configuring Storm, there is a flux file and properties file that we modified. Here are the settings we changed for bro in Flux.
Note that the main Metron-specific option we've changed to accomodate the desired rate of data throughput is max cache size in the join bolts.
More information on Flux can be found here - http://storm.apache.org/releases/1.0.1/flux.html

__General storm settings__

```
topology.workers: 8
topology.acker.executors: 48
topology.max.spout.pending: 2000
```

__Spout and Bolt Settings__

```
kafkaSpout
    parallelism=48
    session.timeout.ms=29999
    enable.auto.commit=false
    setPollTimeoutMs=200
    setMaxUncommittedOffsets=10000000
    setOffsetCommitPeriodMs=30000
enrichmentSplitBolt
    parallelism=4
enrichmentJoinBolt
    parallelism=8
    withMaxCacheSize=200000
    withMaxTimeRetain=10
threatIntelSplitBolt
    parallelism=4
threatIntelJoinBolt
    parallelism=4
    withMaxCacheSize=200000
    withMaxTimeRetain=10
outputBolt
    parallelism=48
```

### Indexing (HDFS) Tuning

There are 48 partitions set for the indexing partition, per the enrichment exercise above.

These are the batch size settings for the bro index

```
cat ${METRON_HOME}/config/zookeeper/indexing/bro.json
{
  "hdfs" : {
    "index": "bro",
    "batchSize": 50,
    "enabled" : true
  }...
}
```

And here are the settings we used for the indexing topology

__General storm settings__

```
topology.workers: 4
topology.acker.executors: 24
topology.max.spout.pending: 2000
```

__Spout and Bolt Settings__

```
hdfsSyncPolicy
    org.apache.storm.hdfs.bolt.sync.CountSyncPolicy
    constructor arg=100000
hdfsRotationPolicy
    bolt.hdfs.rotation.policy.units=DAYS
    bolt.hdfs.rotation.policy.count=1
kafkaSpout
    parallelism: 24
    session.timeout.ms=29999
    enable.auto.commit=false
    setPollTimeoutMs=200
    setMaxUncommittedOffsets=10000000
    setOffsetCommitPeriodMs=30000
hdfsIndexingBolt
    parallelism: 24
```

### PCAP Tuning

PCAP is a specialized topology that is a Spout-only topology. Both Kafka topic consumption and HDFS writing is done within a spout to
avoid the additional network hop required if using an additional bolt.

__General Storm topology properties__

```
topology.workers=16
topology.ackers.executors: 0
```

__Spout and Bolt properties__

```
kafkaSpout
    parallelism: 128
    poll.timeout.ms=100
    offset.commit.period.ms=30000
    session.timeout.ms=39000
    max.uncommitted.offsets=200000000
    max.poll.interval.ms=10
    max.poll.records=200000
    receive.buffer.bytes=431072
    max.partition.fetch.bytes=10000000
    enable.auto.commit=false
    setMaxUncommittedOffsets=20000000
    setOffsetCommitPeriodMs=30000

writerConfig
    withNumPackets=1265625
    withMaxTimeMS=0
    withReplicationFactor=1
    withSyncEvery=80000
    withHDFSConfig
        io.file.buffer.size=1000000
        dfs.blocksize=1073741824
```

## Debugging

Set the following env vars accordingly for your cluster. This is how we would configure it for the Metron full dev development environment.

```
export HDP_HOME=/usr/hdp/current
export KAFKA_HOME=$HDP_HOME/kafka-broker
export STORM_UI=http://node1:8744
export ELASTIC=http://node1:9200
export ZOOKEEPER=node1:2181
export METRON_VERSION=0.7.1
export METRON_HOME=/usr/metron/${METRON_VERSION}
```

Note that the output from Storm will be a flattened blob of JSON. In order to pretty print for readability, you can pipe it through a JSON formatter, e.g.

```
[some Storm curl command] | python -m json.tool
```

**Getting Storm Configuration Details**

Storm has a useful REST API you can use to get full details about your running topologies. This is generally more convenient and complete for troubleshooting performance problems than going to the Storm UI alone. See Storm's [REST API docs](http://storm.apache.org/releases/1.1.0/STORM-UI-REST-API.html) for more details.

```
# get Storm cluster summary info including version
curl -XGET ${STORM_UI}'/api/v1/cluster/summary'
```

```
# get overall Storm cluster configuration
curl -XGET ${STORM_UI}'/api/v1/cluster/configuration'
```

```
# get list of topologies and brief summary detail
curl -XGET ${STORM_UI}'/api/v1/topology/summary'
```

```
# get all topology runtime settings. Plugin the ID for your topology, which you can get from the topology summary command or from the Storm UI. Passing sys=1 will also return system stats.
curl -XGET ${STORM_UI}'/api/v1/topology/:id?sys=1​'
```

**Getting Kafka Configuration Details**

```
# Get list of Kafka topics
${HDP_HOME}/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list
```

```
# Get Kafka topic details - plugin the desired topic name in place of "enrichments"
${HDP_HOME}/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --topic enrichments --describe
```

**Getting Metron Topology Zookeeper Configuration**

```
# Provides a full listing of all Metron parser, enrichment, and indexing topology configuration
$METRON_HOME/bin/zk_load_configs.sh -m DUMP -z $ZOOKEEPER
```

## Issues

__Error__

```
org.apache.kafka.clients.consumer.CommitFailedException: Commit cannot be completed since the group has already rebalanced and assigned
the partitions to another member. This means that the time between subsequent calls to poll() was longer than the configured session.timeout.ms,
which typically implies that the poll loop is spending too much time message processing. You can address this either by increasing the
session timeout or by reducing the maximum size of batches returned in poll() with max.poll.records
```

__Suggestions__

This implies that the spout hasn't been given enough time between polls before committing the offsets. In other words, the amount of
time taken to process the messages is greater than the timeout window. In order to fix this, you can improve message throughput by
modifying the options outlined above, increasing the poll timeout, or both.

## Reference

* [Enrichment Performance](metron-enrichment/Performance.md)
* http://storm.apache.org/releases/1.1.0/flux.html
* https://stackoverflow.com/questions/17257448/what-is-the-task-in-storm-parallelism
* http://storm.apache.org/releases/current/Understanding-the-parallelism-of-a-Storm-topology.html
* http://www.malinga.me/reading-and-understanding-the-storm-ui-storm-ui-explained/
* https://www.confluent.io/blog/how-to-choose-the-number-of-topicspartitions-in-a-kafka-cluster/
* https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.1/bk_storm-component-guide/content/storm-kafkaspout-perf.html
* http://storm.apache.org/releases/1.1.0/STORM-UI-REST-API.html


