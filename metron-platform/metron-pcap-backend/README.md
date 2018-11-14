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
# Metron PCAP Backend

The purpose of the Metron PCAP backend is to create a storm topology
capable of rapidly ingesting raw packet capture data directly into HDFS
from Kafka.

* [Sensors](#the-sensors-feeding-kafka)
* [PCAP Topology](#the-pcap-topology)
* [HDFS Files](#the-files-on-hdfs)
* [Configuration](#configuration)
* [Starting the Topology](#starting-the-topology)
* [Utilities](#utilities)
  * [Inspector Utility](#inspector-utility)
  * [Query Filter Utility](#query-filter-utility)
* [Performance Tuning](#performance-tuning)

## The Sensors Feeding Kafka

This component must be fed by fast packet capture components upstream
via Kafka.  The two supported components shipped with Metron are as follows:
* The pycapa [tool](../../metron-sensors/pycapa) aimed at low-volume packet capture
* The [DPDK](http://dpdk.org/) based [tool](../../metron-sensors/fastcapa) aimed at high-volume packet capture

Both of these sensors feed kafka raw packet data directly into Kafka.
The format of the record structure that this component expects is the
following:
* A key which is the byte representation of a 64-bit `unsigned long` representing a time-unit since the unix epoch
* A value which is the raw packet data without header (either global pcap header or packet header)

## The PCAP Topology

The structure of the topology is extremely simple.  In fact, it is a spout-only
topology.  The `Storm Kafka` spout is used but extended to allow a
callback to be used rather than having a separate bolt. 

The following happens as part of this spout for each packet:
* A custom `Scheme` is used which attaches the appropriate headers to the packet (both global and packet headers) using the timestamp in the key and the raw packet data in the value.
* A callback is called which appends the packet data to a sequence file in HDFS.

## The Files on HDFS

The sequence files on HDFS fit the following pattern: `$BASE_PATH/pcap_$TOPIC_$TS_$PARTITION_$UUID`

where
* `BASE_PATH` is the base path to where pcap data is stored in HDFS
* `TOPIC` is the kafka topic
* `TS` is the timestamp, in nanoseconds since the unix epoch
* `PARTITION` is the kafka partition
* `UUID` the UUID for the storm worker

These files contain a set of packet data with headers on them in
sequence files.

## Configuration

The configuration properties for PCAP sensor is managed via Ambari at Services -> Metron -> Config -> PCAP tab.
Note that changes to PCAP sensor config properties via Ambari requires restarting the Metron PCAP service.

The configuration file for the Flux topology is located at
`$METRON_HOME/config/pcap.properties` and the possible options
are as follows:
* `spout.kafka.topic.pcap` : The kafka topic to listen to
* `storm.auto.credentials` : The kerberos ticket renewal.  If running on a kerberized cluster, this should be `['org.apache.storm.security.auth.kerberos.AutoTGT']`
* `kafka.security.protocol` : The security protocol to use for kafka.  This should be `PLAINTEXT` for a non-kerberized cluster and probably `SASL_PLAINTEXT` for a kerberized cluster.
* `kafka.zk` : The comma separated zookeeper quorum (i.e.  host:2181,host2:2181)
* `kafka.pcap.start` : One of `EARLIEST`, `LATEST`, `UNCOMMITTED_EARLIEST`, `UNCOMMITTED_LATEST` representing where to start listening on the queue. 
* `kafka.pcap.numPackets` : The number of packets to keep in one file.
* `kafka.pcap.maxTimeMS` : The number of packets to keep in one file in terms of duration (in milliseconds).  For instance, you may only want to keep an hour's worth of packets in a given file.
* `kafka.pcap.ts_scheme` : One of `FROM_KEY` or `FROM_VALUE`.  You really only want `FROM_KEY` as that fits the current tooling.  `FROM_VALUE` assumes that fully headerized packets are coming in on the value, which is legacy.
* `kafka.pcap.out` : The directory in HDFS to store the packet capture data
* `kafka.pcap.ts_granularity` : The granularity of timing used in the timestamps.  One of `MILLISECONDS`, `MICROSECONDS`, or `NANOSECONDS` representing milliseconds, microseconds or nanoseconds since the unix epoch (respectively).

## Starting the Topology

To assist in starting the topology, a utility script which takes no
arguments has been created to make this very simple.  Simply, execute
`$METRON_HOME/bin/start_pcap_topology.sh`.

## Utilities

### Inspector Utility
In order to ensure that data can be read back out, a utility,
`$METRON_HOME/bin/pcap_inspector.sh` has been
created to read portions of the sequence files.
 
```
usage: PcapInspector
 -h,--help               Generate Help screen
 -i,--input <SEQ_FILE>   Input sequence file on HDFS
 -n,--num_packets <N>    Number of packets to dump
```

### Query Filter Utility
This tool exposes the two methods for filtering PCAP data via a command line tool:
- fixed
- query (via Stellar)

The tool is executed via 
```
${metron_home}/bin/pcap_query.sh [fixed|query]
```

#### Usage
```
usage: Fixed filter options
 -bop,--base_output_path <arg>   Query result output path. Default is
                                 '/tmp'
 -bp,--base_path <arg>           Base PCAP data path. Default is
                                 '/apps/metron/pcap'
 -da,--ip_dst_addr <arg>         Destination IP address
 -df,--date_format <arg>         Date format to use for parsing start_time
                                 and end_time. Default is to use time in
                                 millis since the epoch.
 -dp,--ip_dst_port <arg>         Destination port
 -pf,--packet_filter <arg>       Packet filter regex
 -et,--end_time <arg>            Packet end time range. Default is current
                                 system time.
 -nr,--num_reducers <arg>        The number of reducers to use.  Default
                                 is 10.
 -h,--help                       Display help
 -ps,--print_status              Print the status of the job as it runs
 -ir,--include_reverse           Indicates if filter should check swapped
                                 src/dest addresses and IPs
 -p,--protocol <arg>             IP Protocol
 -sa,--ip_src_addr <arg>         Source IP address
 -sp,--ip_src_port <arg>         Source port
 -st,--start_time <arg>          (required) Packet start time range.
 -yq,--yarn_queue <arg>          Yarn queue this job will be submitted to
```

```
usage: Query filter options
 -bop,--base_output_path <arg>   Query result output path. Default is
                                 '/tmp'
 -bp,--base_path <arg>           Base PCAP data path. Default is
                                 '/apps/metron/pcap'
 -df,--date_format <arg>         Date format to use for parsing start_time
                                 and end_time. Default is to use time in
                                 millis since the epoch.
 -et,--end_time <arg>            Packet end time range. Default is current
                                 system time.
 -nr,--num_reducers <arg>        The number of reducers to use.  Default
                                 is 10.
 -h,--help                       Display help
 -ps,--print_status              Print the status of the job as it runs
 -q,--query <arg>                Query string to use as a filter
 -st,--start_time <arg>          (required) Packet start time range.
 -yq,--yarn_queue <arg>          Yarn queue this job will be submitted to
```

The Query filter's `--query` argument specifies the Stellar expression to
execute on each packet.  To interact with the packet, a few variables are exposed:
* `packet` : The packet data (a `byte[]`)
* `ip_src_addr` : The source address for the packet (a `String`)
* `ip_src_port` : The source port for the packet (an `Integer`)
* `ip_dst_addr` : The destination address for the packet (a `String`)
* `ip_dst_port` : The destination port for the packet (an `Integer`)

#### Binary Regex

Filtering can be done both by the packet header as well as via a binary regular expression
which can be run on the packet payload itself.  This filter can be specified via:
* The `-pf` or `--packet_filter` options for the fixed query filter
* The `BYTEARRAY_MATCHER(pattern, data)` Stellar function.
The first argument is the regex pattern and the second argument is the data.
The packet data will be exposed via the`packet` variable in Stellar.

The format of this regular expression is described [here](https://github.com/nishihatapalmer/byteseek/blob/master/sequencesyntax.md).

## Performance Tuning
The PCAP topology is extremely lightweight and functions as a Spout-only topology. In order to tune the topology, users currently must specify a combination of
properties in pcap.properties as well as configuration in the pcap remote.yaml flux file itself. Tuning the number of partitions in your Kafka topic
will have a dramatic impact on performance as well. We ran data into Kafka at 1.1 Gbps and our tests resulted in configuring 128 partitions for our kakfa topic
along with the following settings in pcap.properties and remote.yaml (unrelated properties for performance have been removed):

### pcap.properties file
```
spout.kafka.topic.pcap=pcap
storm.topology.workers=16
kafka.spout.parallelism=128
kafka.pcap.numPackets=1000000000
kafka.pcap.maxTimeMS=0
hdfs.replication=1
hdfs.sync.every=10000
```
You'll notice that the number of kakfa partitions equals the spout parallelism, and this is no coincidence. The ordering guarantees for a partition in Kafka enforces that you may have no more
consumers than 1 per topic. Any additional parallelism will leave you with dormant threads consuming resources but performing no additional work. For our cluster with 4 Storm Supervisors, we found 16 workers to
provide optimal throughput as well. We were largely IO bound rather than CPU bound with the incoming PCAP data.

### remote.yaml
In the flux file, we introduced the following configuration:

```
name: "pcap"
config:
    topology.workers: ${storm.topology.workers}
    topology.worker.childopts: ${topology.worker.childopts}
    topology.auto-credentials: ${storm.auto.credentials}
    topology.ackers.executors: 0
components:

  # Any kafka props for the producer go here.
  - id: "kafkaProps"
    className: "java.util.HashMap"
    configMethods:
      -   name: "put"
          args:
            - "value.deserializer"
            - "org.apache.kafka.common.serialization.ByteArrayDeserializer"
      -   name: "put"
          args:
            - "key.deserializer"
            - "org.apache.kafka.common.serialization.ByteArrayDeserializer"
      -   name: "put"
          args:
            - "group.id"
            - "pcap"
      -   name: "put"
          args:
            - "security.protocol"
            - "${kafka.security.protocol}"
      -   name: "put"
          args:
            - "poll.timeout.ms"
            - 100
      -   name: "put"
          args:
            - "offset.commit.period.ms"
            - 30000
      -   name: "put"
          args:
            - "session.timeout.ms"
            - 30000
      -   name: "put"
          args:
            - "max.uncommitted.offsets"
            - 200000000
      -   name: "put"
          args:
            - "max.poll.interval.ms"
            - 10
      -   name: "put"
          args:
            - "max.poll.records"
            - 200000
      -   name: "put"
          args:
            - "receive.buffer.bytes"
            - 431072
      -   name: "put"
          args:
            - "max.partition.fetch.bytes"
            - 8097152

  - id: "hdfsProps"
    className: "java.util.HashMap"
    configMethods:
      -   name: "put"
          args:
            - "io.file.buffer.size"
            - 1000000
      -   name: "put"
          args:
            - "dfs.blocksize"
            - 1073741824

  - id: "kafkaConfig"
    className: "org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder"
    constructorArgs:
      - ref: "kafkaProps"
      # topic name
      - "${spout.kafka.topic.pcap}"
      - "${kafka.zk}"
    configMethods:
      -   name: "setFirstPollOffsetStrategy"
          args:
            # One of EARLIEST, LATEST, UNCOMMITTED_EARLIEST, UNCOMMITTED_LATEST
            - ${kafka.pcap.start}

  - id: "writerConfig"
    className: "org.apache.metron.spout.pcap.HDFSWriterConfig"
    configMethods:
      -   name: "withOutputPath"
          args:
            - "${kafka.pcap.out}"
      -   name: "withNumPackets"
          args:
            - ${kafka.pcap.numPackets}
      -   name: "withMaxTimeMS"
          args:
            - ${kafka.pcap.maxTimeMS}
      -   name: "withZookeeperQuorum"
          args:
            - "${kafka.zk}"
      -   name: "withSyncEvery"
          args:
            - ${hdfs.sync.every}
      -   name: "withReplicationFactor"
          args:
            - ${hdfs.replication}
      -   name: "withHDFSConfig"
          args:
              - ref: "hdfsProps"
      -   name: "withDeserializer"
          args:
            - "${kafka.pcap.ts_scheme}"
            - "${kafka.pcap.ts_granularity}"
spouts:
  - id: "kafkaSpout"
    className: "org.apache.metron.spout.pcap.KafkaToHDFSSpout"
    parallelism: ${kafka.spout.parallelism}
    constructorArgs:
      - ref: "kafkaConfig"
      - ref: "writerConfig"

```

#### Flux Changes Introduced

##### Topology Configuration

The only change here is `topology.ackers.executors: 0`, which disables Storm tuple acking for maximum throughput.

##### Kafka configuration

```
poll.timeout.ms
offset.commit.period.ms
session.timeout.ms
max.uncommitted.offsets
max.poll.interval.ms
max.poll.records
receive.buffer.bytes
max.partition.fetch.bytes
```

##### Writer Configuration

This is a combination of settings for the HDFSWriter (see pcap.properties values above) as well as HDFS.

__HDFS config__

Component config HashMap with the following properties:
```
io.file.buffer.size
dfs.blocksize
```

__Writer config__

References the HDFS props component specified above.
```
 -   name: "withHDFSConfig"
     args:
       - ref: "hdfsProps"
```

