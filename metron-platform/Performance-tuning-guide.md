# Metron Performance Tunining Guide

## Overview

This document provides guidance from our experiences tuning the Apache Metron Storm topologies for maximum performance. You'll find
suggestions for optimum configurations under a 1 gbps load along with some guidance around the tooling we used to monitor and assess
our throughput.

In the simplest terms, Metron is a streaming architecture created on top of Kafka and three main types of Storm topologies: parsers,
enrichment, and indexing. Each parser has it's own topology and there is also a highly performant, specialized spout-only topology
for streaming PCAP data to HDFS. We found that the architecture can be tuned almost exclusively through using a few primary Storm and
Kafka parameters along with a few Metron-specific options. You can think of the data flow as being similar to water flowing through a
pipe, and the majority of these options assist in tweaking the various pipe widths in the system.

## General Suggestions

Note that there is currently no method for specifying the number of tasks from the number of executors in Flux topologies (enrichment,
 indexing). By default, the number of tasks will equal the number of executors. Logically, setting the number of tasks equal to the number
of executors is sensible. Storm enforces # executors <= # tasks. The reason you might set the number of tasks higher than the number of
executors is for future performance tuning and rebalancing without the need to bring down your topologies. The number of tasks is fixed
at topology startup time whereas the number of executors can be increased up to a maximum value equal to the number of tasks.

We found that the default values for poll.timeout.ms, offset.commit.period.ms, and max.uncommitted.offsets worked well in nearly all cases.
As a general rule, it was optimal to set spout parallelism equal to the number of partitions used in your Kafka topic. Any greater
parallelism will leave you with idle consumers since Kafka limits the max number of consumers to the number of partitions. This is
important because Kafka has certain ordering guarantees for message delivery per partition that would not be possible if more than
one consumer in a given consumer group were able to read from that partition.

## Tooling

Before we get to the actual tooling used to monitor performance, it helps to describe what we might actually want to monitor and potential
pain points. Prior to switching over to the new Storm Kafka client, which leverages the new Kafka consumer API under the hood, offsets
were stored in Zookeeper. While the broker hosts are still stored in Zookeeper, this is no longer true for the offsets which are now
stored in Kafka itself. This is a configurable option, and you may switch back to Zookeeper if you choose, but Metron is currently using
the new defaults. This is useful to know as you're investigating both correctness as well as throughput performance.

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

We can also monitor our Storm topologies by using the Storm UI - see http://www.malinga.me/reading-and-understanding-the-storm-ui-storm-ui-explained/

And lastly, you can leverage some GUI tooling to make creating and modifying your Kafka topics a bit easier -
see https://github.com/yahoo/kafka-manager

## General Knobs and Levers

Kafka
    - # partitions
Storm
    Kafka
        - polling frequency and timeouts
    - # workers
    - ackers
    - max spout pending
    - spout parallelism
    - bolt parallelism
    - # executors
Metron
    - bolt cache size - handles how many messages can be cached. This cache is used while waiting for all parts of the message to be rejoined.

## Topologies

### Parsers

The parsers and PCAP use a builder utility, as opposed to enrichments and indexing, which use Flux.

We set the number of partitions for our inbound Kafka topics to 48.

```
$ cat ~metron/.storm/storm-bro.config

{
    ...
    "topology.max.spout.pending" : 2000
    ...
}
```

These are the spout recommended defaults from Storm and are currently the defaults provided in the Kafka spout itself.
In fact, if you find the recommended defaults work fine for you, then this file might not be necessary at all.
```
$ cat ~/.storm/spout-bro.config
{
    ...
    "spout.pollTimeoutMs" : 200,
    "spout.maxUncommittedOffsets" : 10000000,
    "spout.offsetCommitPeriodMs" : 30000
}
```

We ran our bro parser topology with the following options

```
/usr/metron/0.4.0/bin/start_parser_topology.sh -k $BROKERLIST -z $ZOOKEEPER -s bro -ksp SASL_PLAINTEXT
    -ot enrichments
    -e ~metron/.storm/storm-bro.config \
    -esc ~/.storm/spout-bro.config \
    -sp 24 \
    -snt 24 \
    -nw 1 \
    -pnt 24 \
    -pp 24 \
```

From the usage docs, here are the options we've used. The full reference can be found here - https://github.com/apache/metron/blob/master/metron-platform/metron-parsers/README.md
```
-e,--extra_topology_options <JSON_FILE>        Extra options in the form
                                               of a JSON file with a map
                                               for content.
-esc,--extra_kafka_spout_config <JSON_FILE>    Extra spout config options
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
-sp,--spout_p <SPOUT_PARALLELISM_HINT>         Spout Parallelism Hint
-snt,--spout_num_tasks <NUM_TASKS>             Spout Num Tasks
-nw,--num_workers <NUM_WORKERS>                Number of Workers
-pnt,--parser_num_tasks <NUM_TASKS>            Parser Num Tasks
-pp,--parser_p <PARALLELISM_HINT>              Parser Parallelism Hint
```

### Enrichment

Kafka - partitions setup
    bro topic set to 48 partitions (referenced in the parser settings above)
    indexing topic set to 48 partitions

Here are the settings we changed for bro in Flux. Note that the main Metron-specific option we've changed to accomodate the desired rate
of data throughput is max cache size in the join bolts. More information on Flux can be found here - http://storm.apache.org/releases/1.0.1/flux.html

general storm settings
```
topology.workers: 8
topology.acker.executors: 48
topology.max.spout.pending: 2000
```

Spout and Bolt Settings
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

### Indexing (HDFS)

Here are the batch size settings for the bro index

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

Below are the settings we used for the indexing topology

General storm settings
```
topology.workers: 4
topology.acker.executors: 24
topology.max.spout.pending: 2000
```

Spout and Bolt Settings
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

### PCAP

PCAP is a specialized topology that is a Spout-only topology. Both Kafka topic consumption and HDFS writing is done within a spout to
avoid the additional network hop required if using an additional bolt.

General Storm topology properties
```
topology.workers=16
topology.ackers.executors: 0
```

Spout and Bolt properties
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

* http://storm.apache.org/releases/1.0.1/flux.html
* https://stackoverflow.com/questions/17257448/what-is-the-task-in-storm-parallelism
* http://storm.apache.org/releases/current/Understanding-the-parallelism-of-a-Storm-topology.html
* http://www.malinga.me/reading-and-understanding-the-storm-ui-storm-ui-explained/

