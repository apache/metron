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
# Parsers

## Introduction
Metron's parsers can be run in Storm topologies, complete with their own set of configuration options (e.g. parallelism). A script is provided to deploy a parser as a Storm topologoy.

## Parser Configuration

* `spoutParallelism` : The kafka spout parallelism (default to `1`).  This can be overridden on the command line, and if there are multiple sensors should be in a comma separated list in the same order as the sensors.
* `spoutNumTasks` : The number of tasks for the spout (default to `1`). This can be overridden on the command line, and if there are multiple sensors should be in a comma separated list in the same order as the sensors.
* `parserParallelism` : The parser bolt parallelism (default to `1`). If there are multiple sensors, the last one's configuration will be used. This can be overridden on the command line.
* `parserNumTasks` : The number of tasks for the parser bolt (default to `1`). If there are multiple sensors, the last one's configuration will be used. This can be overridden on the command line.
* `errorWriterParallelism` : The error writer bolt parallelism (default to `1`). This can be overridden on the command line.
* `errorWriterNumTasks` : The number of tasks for the error writer bolt (default to `1`). This can be overridden on the command line.
* `numWorkers` : The number of workers to use in the topology (default is the storm default of `1`).
* `numAckers` : The number of acker executors to use in the topology (default is the storm default of `1`).
* `spoutConfig` : A map representing a custom spout config (this is a map). If there are multiple sensors, the configs will be merged with the last specified taking precedence. This can be overridden on the command line.
* `stormConfig` : The storm config to use (this is a map).  This can be overridden on the command line.  If both are specified, they are merged with CLI properties taking precedence.

# Starting the Parser Topology

Starting a particular parser topology on a running Metron deployment is
as easy as running the `start_parser_topology.sh` script located in
`$METRON_HOME/bin`.  This utility will allow you to configure and start
the running topology assuming that the sensor specific parser configuration
exists within zookeeper.

The usage for `start_parser_topology.sh` is as follows:

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
 -ewnt,--error_writer_num_tasks <NUM_TASKS>            Error Writer Num Tasks
 -ewp,--error_writer_p <PARALLELISM_HINT>              Error Writer Parallelism
                                                       Hint
 -h,--help                                             This screen
 -iwnt,--invalid_writer_num_tasks <NUM_TASKS>          Invalid Writer Num Tasks
 -iwp,--invalid_writer_p <PARALLELISM_HINT>            Invalid Message Writer Parallelism Hint
 -k,--kafka <BROKER_URL>                               Kafka Broker URL
 -ksp,--kafka_security_protocol <SECURITY_PROTOCOL>    Kafka Security Protocol
 -mt,--message_timeout <TIMEOUT_IN_SECS>               Message Timeout in Seconds
 -mtp,--max_task_parallelism <MAX_TASK>                Max task parallelism
 -na,--num_ackers <NUM_ACKERS>                         Number of Ackers
 -nw,--num_workers <NUM_WORKERS>                       Number of Workers
 -ot,--output_topic <KAFKA_TOPIC>                      Output Kafka Topic
 -pnt,--parser_num_tasks <NUM_TASKS>                   Parser Num Tasks
 -pp,--parser_p <PARALLELISM_HINT>                     Parser Parallelism Hint
 -s,--sensor <SENSOR_TYPE>                             Sensor Type
 -snt,--spout_num_tasks <NUM_TASKS>                    Spout Num Tasks
 -sp,--spout_p <SPOUT_PARALLELISM_HINT>                Spout Parallelism Hint
 -t,--test <TEST>                                      Run in Test Mode
 -z,--zk <ZK_QUORUM>                                   Zookeeper Quroum URL
                                                       (zk1:2181,zk2:2181,...
```

## The `--extra_kafka_spout_config` Option
These options are intended to configure the Storm Kafka Spout more completely.  These options can be
specified in a JSON file containing a map associating the kafka spout configuration parameter to a value.
The range of values possible to configure are:
* `spout.pollTimeoutMs` -  Specifies the time, in milliseconds, spent waiting in poll if data is not available. Default is 2s
* `spout.firstPollOffsetStrategy` - Sets the offset used by the Kafka spout in the first poll to Kafka broker upon process start.  One of
  * `EARLIEST`
  * `LATEST`
  * `UNCOMMITTED_EARLIEST` - Last uncommitted and if offsets aren't found, defaults to earliest. NOTE: This is the default.
  * `UNCOMMITTED_LATEST` - Last uncommitted and if offsets aren't found, defaults to latest.
* `spout.offsetCommitPeriodMs` - Specifies the period, in milliseconds, the offset commit task is periodically called. Default is 15s.
* `spout.maxUncommittedOffsets` - Defines the max number of polled offsets (records) that can be pending commit, before another poll can take place. Once this limit is reached, no more offsets (records) can be polled until the next successful commit(s) sets the number of pending offsets bellow the threshold. The default is 10,000,000. 
* `spout.maxRetries` -  Defines the max number of retrials in case of tuple failure. The default is to retry forever, which means that no new records are committed until the previous polled records have been acked. This guarantees at once delivery of all the previously polled records.  By specifying a finite value for maxRetries, the user decides to sacrifice guarantee of delivery for the previous polled records in favor of processing more records.
* Any of the configs in the Consumer API for [Kafka 0.10.x](http://kafka.apache.org/0100/documentation.html#newconsumerconfigs)

For instance, creating a JSON file which will set the offsets to `UNCOMMITTED_EARLIEST`
```
{
  "spout.firstPollOffsetStrategy" : "UNCOMMITTED_EARLIEST"
}
```

This would be loaded by passing the file as argument to `--extra_kafka_spout_config`

## The `--extra_topology_options` Option

These options are intended to be Storm configuration options and will live in
a JSON file which will be loaded into the Storm config.  For instance, if you wanted to set a storm property on
the config called `topology.ticks.tuple.freq.secs` to 1000 and `storm.local.dir` to `/opt/my/path`
you could create a file called `custom_config.json` containing 
```
{ 
  "topology.ticks.tuple.freq.secs" : 1000,
  "storm.local.dir" : "/opt/my/path"
}
```
and pass `--extra_topology_options custom_config.json` to `start_parser_topology.sh`.

## Parser Topology
The enrichment topology as started by the `$METRON_HOME/bin/start_parser_topology.sh` 
script uses a default of one executor per bolt.  In a real production system, this should 
be customized by modifying the arguments sent to this utility.
* Topology Wide
  * `--num_workers` : The number of workers for the topology
  * `--num_ackers` : The number of ackers for the topology
* The Kafka Spout
  * `--spout_num_tasks` : The number of tasks for the spout
  * `--spout_p` : The parallelism hint for the spout
  * Ensure that the spout has enough parallelism so that it can dedicate a worker per partition in your kafka topic.
* The Parser Bolt
  * `--parser_num_tasks` : The number of tasks for the parser bolt
  * `--parser_p` : The parallelism hint for the spout
  * This is bolt that gets the most processing, so ensure that it is configured with sufficient parallelism to match your throughput expectations.
* The Error Message Writer Bolt
  * `--error_writer_num_tasks` : The number of tasks for the error writer bolt
  * `--error_writer_p` : The parallelism hint for the error writer bolt
 
Finally, if workers and executors are new to you, the following might be of use to you:
* [Understanding the Parallelism of a Storm Topology](http://www.michael-noll.com/blog/2012/10/16/understanding-the-parallelism-of-a-storm-topology/)

## Parser Aggregation
For performance reasons, multiple sensors can be aggregated into a single Storm topology. When this is done, there will be multiple Kafka spouts, but only a single parser bolt which will handle delegating to the correct parser as needed. There are some constraints around this, in particular regarding some configuration. Additionally, all sensors must flow to the same error topic. The Kafka topic is retrieved from the input Tuple itself.

A worked example of this can be found in the [Parser Chaining use case](../../../use-cases/parser_chaining/README.md#aggregated-parsers-with-parser-chaining).
