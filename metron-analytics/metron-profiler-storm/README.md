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
# Metron Profiler for Storm

This project allows profiles to be executed using [Apache Storm](https://storm.apache.org). This is a port of the Profiler to Storm that builds low-latency profiles over streaming data sets.

* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [Installation](#installation)
* [Configuring the Profiler](#configuring-the-profiler)
* [Implementation](#implementation)

## Introduction

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior.

This is achieved by summarizing the streaming telemetry data consumed by Metron over sliding windows. A summary statistic is applied to the data received within a given window.  Collecting this summary across many windows results in a time series that is useful for analysis.

Any field contained within a message can be used to generate a profile.  A profile can even be produced by combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language. A user only need configure the desired profiles and ensure that the Profiler topology is running.

For an introduction to the Profiler, see the [Profiler README](../metron-profiler-common/README.md).

## Getting Started

This section will describe the steps required to get your first "Hello, World!"" profile running.  This assumes that you have a successful Profiler [Installation](#installation) and have it running.  You can deploy profiles in two different ways.

* [Deploying Profiles with the Stellar Shell](#deploying-profiles-with-the-stellar-shell)
* [Deploying Profiles from the Command Line](#deploying-profiles-from-the-command-line)

### Deploying Profiles with the Stellar Shell

Continuing the previous running example, at this point, you have seen how your profile behaves against real, live telemetry in a controlled execution environment.  The next step is to deploy your profile to the live, actively running Profiler topology.

1.  Start the Stellar Shell with the `-z` command line argument so that a connection to Zookeeper is established.  This is required when  deploying a new profile definition as shown in the steps below.
    ```
    [root@node1 ~]# source /etc/default/metron
    [root@node1 ~]# $METRON_HOME/bin/stellar -z $ZOOKEEPER
    Stellar, Go!
    [Stellar]>>>
    [Stellar]>>> %functions CONFIG CONFIG_GET, CONFIG_PUT
    ```

1. If you haven't already, define your profile.
	```
	[Stellar]>>> conf := SHELL_EDIT()
	[Stellar]>>> conf
	{
	  "profiles": [
	    {
	      "profile": "hello-world",
	      "onlyif":  "exists(ip_src_addr)",
	      "foreach": "ip_src_addr",
	      "init":    { "count": "0" },
	      "update":  { "count": "count + 1" },
	      "result":  "count"
	    }
	  ]
	}
	```

1. Check what is already deployed.  

	Pushing a new profile configuration is destructive.  It will overwrite any existing configuration.  Check what you have out there.  Manually merge the existing configuration with your new profile definition.

	```
	[Stellar]>>> existing := CONFIG_GET("PROFILER")
	```

1. Deploy your profile.  This will push the configuration to to the live, actively running Profiler topology.  This will overwrite any existing profile definitions.
	```
	[Stellar]>>> CONFIG_PUT("PROFILER", conf)
	```

### Deploying Profiles from the Command Line

1. Create the profile definition in a file located at `$METRON_HOME/config/zookeeper/profiler.json`.  This file will likely not exist, if you have never created Profiles before.

    The following example will create a profile that simply counts the number of messages per `ip_src_addr`.
    ```
    {
      "profiles": [
        {
          "profile": "hello-world",
          "onlyif":  "exists(ip_src_addr)",
          "foreach": "ip_src_addr",
          "init":    { "count": "0" },
          "update":  { "count": "count + 1" },
          "result":  "count"
        }
      ]
    }
    ```

1. Upload the profile definition to Zookeeper.

    ```
    $ source /etc/default/metron
    $ cd $METRON_HOME
    $ bin/zk_load_configs.sh -m PUSH -i config/zookeeper/ -z $ZOOKEEPER
    ```

    You can validate this by reading back the Metron configuration from Zookeeper using the same script. The result should look-like the following.
    ```
    $ bin/zk_load_configs.sh -m DUMP -z $ZOOKEEPER
    ...
    PROFILER Config: profiler
    {
      "profiles": [
        {
          "profile": "hello-world",
          "onlyif":  "exists(ip_src_addr)",
          "foreach": "ip_src_addr",
          "init":    { "count": "0" },
          "update":  { "count": "count + 1" },
          "result":  "count"
        }
      ]
    }
    ```

1. Ensure that test messages are being sent to the Profiler's input topic in Kafka.  The Profiler will consume messages from the input topic defined in the Profiler's configuration (see [Configuring the Profiler](#configuring-the-profiler)).  By default this is the `indexing` topic.

1. Check the HBase table to validate that the Profiler is writing the profile.  Remember that the Profiler is flushing the profile every 15 minutes.  You will need to wait at least this long to start seeing profile data in HBase.
    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> count 'profiler'
    ```

1. Use the [Profiler Client](../metron-profiler-client) to read the profile data.  The following `PROFILE_GET` command will read the data written by the `hello-world` profile. This assumes that `10.0.0.1` is one of the values for `ip_src_addr` contained within the telemetry consumed by the Profiler.

    ```
    $ source /etc/default/metron
    $ bin/stellar -z $ZOOKEEPER
    [Stellar]>>> PROFILE_GET( "hello-world", "10.0.0.1", PROFILE_FIXED(30, "MINUTES"))
    [451, 448]
    ```

    This result indicates that over the past 30 minutes, the Profiler stored two values related to the source IP address "10.0.0.1".  In the first 15 minute period, the IP `10.0.0.1` was seen in 451 telemetry messages.  In the second 15 minute period, the same IP was seen in 448 telemetry messages.

    It is assumed that the `PROFILE_GET` client is correctly configured to match the Profile configuration before using it to read that Profile.  More information on configuring and using the Profiler client can be found [here](../metron-profiler-client).  

## Installation

The Profiler can be installed with either of these two methods.

 * [Ambari Installation](#ambari-installation)
 * [Manual Installation](#manual-installation)

### Ambari Installation

The Metron Profiler is installed automatically when installing Metron using the Ambari MPack.  You can skip the [Installation](#installation) section and move ahead to [Creating Profiles](#creating-profiles) should this be the case.

### Manual Installation

This section will describe the steps necessary to manually install the Profiler on an RPM-based Linux distribution.  This assumes that core Metron has already been installed and validated.  If you installed Metron using the [Ambari MPack](#ambari-mpack), then the Profiler has already been installed and you can skip this section.

1. Build the Metron RPMs (see Building the [RPMs](../../metron-deployment#rpms)).  

    You may have already built the Metron RPMs when core Metron was installed.

    ```
    $ find metron-deployment/ -name "metron-profiler*.rpm"
    metron-deployment//packaging/docker/rpm-docker/RPMS/noarch/metron-profiler-0.4.1-201707131420.noarch.rpm
    ```

1. Copy the Profiler RPM to the installation host.  

    The installation host must be the same host on which core Metron was installed.  Depending on how you installed Metron, the Profiler RPM might have already been copied to this host with the other Metron RPMs.

    ```
    [root@node1 ~]# find /localrepo/  -name "metron-profiler*.rpm"
    /localrepo/metron-profiler-0.4.1-201707112313.noarch.rpm
    ```

1. Install the RPM.

    ```
    [root@node1 ~]# rpm -ivh metron-profiler-*.noarch.rpm
    Preparing...                ########################################### [100%]
       1:metron-profiler        ########################################### [100%]
    ```

    ```
    [root@node1 ~]# rpm -ql metron-profiler
    /usr/metron
    /usr/metron/0.4.2
    /usr/metron/0.4.2/bin
    /usr/metron/0.4.2/bin/start_profiler_topology.sh
    /usr/metron/0.4.2/config
    /usr/metron/0.4.2/config/profiler.properties
    /usr/metron/0.4.2/flux
    /usr/metron/0.4.2/flux/profiler
    /usr/metron/0.4.2/flux/profiler/remote.yaml
    /usr/metron/0.4.2/lib
    /usr/metron/0.4.2/lib/metron-profiler-0.4.2-uber.jar
    ```

1. Edit the configuration file located at `$METRON_HOME/config/profiler.properties`.  
    ```
    kafka.zk=node1:2181
    kafka.broker=node1:6667
    ```
    * Change `kafka.zk` to refer to Zookeeper in your environment.  
    * Change `kafka.broker` to refer to a Kafka Broker in your environment.

1. Create a table within HBase that will store the profile data. By default, the table is named `profiler` with a column family `P`.  The table name and column family must match the Profiler's configuration (see [Configuring the Profiler](#configuring-the-profiler)).  

    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> create 'profiler', 'P'
    ```

1. Start the Profiler topology.
    ```
    $ cd $METRON_HOME
    $ bin/start_profiler_topology.sh
    ```

At this point the Profiler is running and consuming telemetry messages.  We have not defined any profiles yet, so it is not doing anything very useful.  The next section walks you through the steps to create your very first "Hello, World!" profile.

## Configuring the Profiler

The Profiler runs as an independent Storm topology.  The configuration for the Profiler topology is stored in local filesystem at `$METRON_HOME/config/profiler.properties`. After changing these values, the Profiler topology must be restarted for the changes to take effect.

| Setting                                                                       | Description
|---                                                                            |---
| [`profiler.input.topic`](#profilerinputtopic)                                 | The name of the input Kafka topic.
| [`profiler.output.topic`](#profileroutputtopic)                               | The name of the output Kafka topic.
| [`profiler.period.duration`](#profilerperiodduration)                         | The duration of each profile period.  
| [`profiler.period.duration.units`](#profilerperioddurationunits)              | The units used to specify the [`profiler.period.duration`](#profilerperiodduration).
| [`profiler.window.duration`](#profilerwindowduration)                         | The duration of each profile window.
| [`profiler.window.duration.units`](#profilerpwindowdurationunits)             | The units used to specify the [`profiler.window.duration`](#profilerwindowduration).
| [`profiler.window.lag`](#profilerwindowlag)                                   | The maximum time lag for timestamps.
| [`profiler.window.lag.units`](#profilerpwindowlagunits)                       | The units used to specify the [`profiler.window.lag`](#profilerwindowlag).
| [`profiler.workers`](#profilerworkers)                                        | The number of worker processes for the topology.
| [`profiler.executors`](#profilerexecutors)                                    | The number of executors to spawn per component.
| [`profiler.ttl`](#profilerttl)                                                | If a message has not been applied to a Profile in this period of time, the Profile will be forgotten and its resources will be cleaned up.
| [`profiler.ttl.units`](#profilerttlunits)                                     | The units used to specify the `profiler.ttl`.
| [`profiler.hbase.salt.divisor`](#profilerhbasesaltdivisor)                    | A salt is prepended to the row key to help prevent hot-spotting.
| [`profiler.hbase.table`](#profilerhbasetable)                                 | The name of the HBase table that profiles are written to.
| [`profiler.hbase.column.family`](#profilerhbasecolumnfamily)                  | The column family used to store profiles.
| [`profiler.hbase.batch`](#profilerhbasebatch)                                 | The number of puts that are written to HBase in a single batch.
| [`profiler.hbase.flush.interval.seconds`](#profilerhbaseflushintervalseconds) | The maximum number of seconds between batch writes to HBase.
| [`topology.kryo.register`](#topologykryoregister)                             | Storm will use Kryo serialization for these classes.
| [`profiler.writer.batchSize`](#profilerwriterbatchsize)                       | The number of records to batch when writing to Kakfa.
| [`profiler.writer.batchTimeout`](#profilerwriterbatchtimeout)                 | The timeout in ms for batching when writing to Kakfa.


### `profiler.input.topic`

*Default*: indexing

The name of the Kafka topic from which to consume data.  By default, the Profiler consumes data from the `indexing` topic so that it has access to fully enriched telemetry.

### `profiler.output.topic`

*Default*: enrichments

The name of the Kafka topic to which profile data is written.  This property is only applicable to profiles that define  the [`result` `triage` field](#result).  This allows Profile data to be selectively triaged like any other source of telemetry in Metron.

### `profiler.period.duration`

*Default*: 15

The duration of each profile period.  This value should be defined along with [`profiler.period.duration.units`](#profilerperioddurationunits).

*Important*: To read a profile using the [Profiler Client](metron-analytics/metron-profiler-client), the Profiler Client's `profiler.client.period.duration` property must match this value.  Otherwise, the Profiler Client will be unable to read the profile data.  

### `profiler.period.duration.units`

*Default*: MINUTES

The units used to specify the `profiler.period.duration`.  This value should be defined along with [`profiler.period.duration`](#profilerperiodduration).

*Important*: To read a profile using the Profiler Client, the Profiler Client's `profiler.client.period.duration.units` property must match this value.  Otherwise, the [Profiler Client](metron-analytics/metron-profiler-client) will be unable to read the profile data.

### `profiler.window.duration`

*Default*: 30

The duration of each profile window.  Telemetry that arrives within a slice of time is processed within a single window.  

Many windows of telemetry will be processed during a single profile period.  This does not change the output of the Profiler, it only changes how the Profiler processes data. The window defines how much data the Profiler processes in a single pass.

This value should be defined along with [`profiler.window.duration.units`](#profilerwindowdurationunits).

This value must be less than the period duration as defined by [`profiler.period.duration`](#profilerperiodduration) and [`profiler.period.duration.units`](#profilerperioddurationunits).

### `profiler.window.duration.units`

*Default*: SECONDS

The units used to specify the `profiler.window.duration`.  This value should be defined along with [`profiler.window.duration`](#profilerwindowduration).

### `profiler.window.lag`

*Default*: 1

The maximum time lag for timestamps. Timestamps cannot arrive out-of-order by more than this amount. This value should be defined along with [`profiler.window.lag.units`](#profilerwindowlagunits).

### `profiler.window.lag.units`

*Default*: SECONDS

The units used to specify the `profiler.window.lag`.  This value should be defined along with [`profiler.window.lag`](#profilerwindowlag).

### `profiler.workers`

*Default*: 1

The number of worker processes to create for the Profiler topology.  This property is useful for performance tuning the Profiler.

### `profiler.executors`

*Default*: 0

The number of executors to spawn per component for the Profiler topology.  This property is useful for performance tuning the Profiler.

### `profiler.ttl`

*Default*: 30

 If a message has not been applied to a Profile in this period of time, the Profile will be terminated and its resources will be cleaned up. This value should be defined along with [`profiler.ttl.units`](#profilerttlunits).

 This time-to-live does not affect the persisted Profile data in HBase.  It only affects the state stored in memory during the execution of the latest profile period.  This state will be deleted if the time-to-live is exceeded.

### `profiler.ttl.units`

*Default*: MINUTES

The units used to specify the [`profiler.ttl`](#profilerttl).

### `profiler.hbase.salt.divisor`

*Default*: 1000

A salt is prepended to the row key to help prevent hotspotting.  This constant is used to generate the salt.  This constant should be roughly equal to the number of nodes in the Hbase cluster to ensure even distribution of data.

### `profiler.hbase.table`

*Default*: profiler

The name of the HBase table that profile data is written to.  The Profiler expects that the table exists and is writable.  It will not create the table.

### `profiler.hbase.column.family`

*Default*: P

The column family used to store profile data in HBase.

### `profiler.hbase.batch`

*Default*: 10

The number of puts that are written to HBase in a single batch.

### `profiler.hbase.flush.interval.seconds`

*Default*: 30

The maximum number of seconds between batch writes to HBase.

### `topology.kryo.register`

*Default*:
```
[ org.apache.metron.profiler.ProfileMeasurement, \
  org.apache.metron.profiler.ProfilePeriod, \
  org.apache.metron.common.configuration.profiler.ProfileResult, \
  org.apache.metron.common.configuration.profiler.ProfileResultExpressions, \
  org.apache.metron.common.configuration.profiler.ProfileTriageExpressions, \
  org.apache.metron.common.configuration.profiler.ProfilerConfig, \
  org.apache.metron.common.configuration.profiler.ProfileConfig, \
  org.json.simple.JSONObject, \
  java.util.LinkedHashMap, \
  org.apache.metron.statistics.OnlineStatisticsProvider ]
```               

Storm will use Kryo serialization for these classes. Kryo serialization is more performant than Java serialization, in most cases.  

For these classes, Storm will uses Kryo's `FieldSerializer` as defined in the [Storm Serialization docs](http://storm.apache.org/releases/1.1.2/Serialization.html).  For all other classes not in this list, Storm defaults to using Java serialization which is slower and not recommended for a production topology.

This value should only need altered if you have defined a profile that results in a non-primitive, user-defined type that is not in this list.  If the class is not defined in this list, Java serialization will be used and the class must adhere to Java's serialization requirements.  

The performance of the entire Profiler topology can be negatively impacted if any profile produces results that undergo Java serialization.
