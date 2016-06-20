# Metron PCAP Backend

The purpose of the Metron PCAP backend is to create a storm topology
capable of ingesting rapidly raw packet capture data directly into HDFS
from Kafka.

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

The configuration file for the Flux topology is located at
`$METRON_HOME/config/etc/env/pcap.properties` and the possible options
are as follows:
* `spout.kafka.topic.pcap` : The kafka topic to listen to
* `kafka.zk` : The comma separated zookeeper quorum (i.e.  host:2181,host2:2181)
* `kafka.pcap.start` : One of `START`, `END`, `WHERE_I_LEFT_OFF` representing where to start listening on the queue. 
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
- query (Metron Stellar)

The tool is executed via ```${metron_home}/bin/pcap_query.sh [fixed|query]```

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
 -et,--end_time <arg>            Packet end time range. Default is current
                                 system time.
 -h,--help                       Display help
 -ir,--include_reverse           Indicates if filter should check swapped
                                 src/dest addresses and IPs
 -p,--protocol <arg>             IP Protocol
 -sa,--ip_src_addr <arg>         Source IP address
 -sp,--ip_src_port <arg>         Source port
 -st,--start_time <arg>          (required) Packet start time range.
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
 -h,--help                       Display help
 -q,--query <arg>                Query string to use as a filter
 -st,--start_time <arg>          (required) Packet start time range.
```
