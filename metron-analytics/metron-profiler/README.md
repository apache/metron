# Metron Profiler

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior.

This is achieved by summarizing the streaming telemetry data consumed by Metron over sliding windows. A summary statistic is applied to the data received within a given window.  Collecting this summary across many windows results in a time series that is useful for analysis.

Any field contained within a message can be used to generate a profile.  A profile can even be produced by combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language. A user only need configure the desired profiles and ensure that the Profiler topology is running.

* [Installation](#installation)
* [Getting Started](#getting-started)
* [Creating Profiles](#creating-profiles)
* [Configuring the Profiler](#configuring-the-profiler)
* [Examples](#examples)
* [Implementation](#implementation)

## Installation

Follow these instructions to install the Profiler.  This assumes that core Metron has already been installed and validated.  

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
    /localrepo/metron-profiler-0.4.0-201707112313.noarch.rpm
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
    /usr/metron/0.4.1
    /usr/metron/0.4.1/bin
    /usr/metron/0.4.1/bin/start_profiler_topology.sh
    /usr/metron/0.4.1/config
    /usr/metron/0.4.1/config/profiler.properties
    /usr/metron/0.4.1/flux
    /usr/metron/0.4.1/flux/profiler
    /usr/metron/0.4.1/flux/profiler/remote.yaml
    /usr/metron/0.4.1/lib
    /usr/metron/0.4.1/lib/metron-profiler-0.4.0-uber.jar
    ```

1. Create a table within HBase that will store the profile data. By default, the table is named `profiler` with a column family `P`.  The table name and column family must match the Profiler's configuration (see [Configuring the Profiler](#configuring-the-profiler)).  

    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> create 'profiler', 'P'
    ```

1. Edit the configuration file located at `$METRON_HOME/config/profiler.properties`.  
    ```
    kafka.zk=node1:2181
    kafka.broker=node1:6667
    ```
    Change `kafka.zk` to refer to Zookeeper in your environment.  
    Change `kafka.broker` to refer to a Kafka Broker in your environment.

1. Start the Profiler topology.
    ```
    $ cd $METRON_HOME
    $ bin/start_profiler_topology.sh
    ```

At this point the Profiler is running and consuming telemetry messages.  We have not defined any profiles yet, so it is not doing anything very useful.  The next section walks you through the steps to create your very first "Hello, World!" profile.

## Getting Started

This section will describe the steps required to get your first "Hello, World!"" profile running.  This assumes that you have a successful Profiler [Installation](#installation) and have it running.

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

1. Upload the profile definition to Zookeeper.  Change `node1:2181` to refer the actual Zookeeper host in your environment.

    ```
    $ cd $METRON_HOME
    $ bin/zk_load_configs.sh -m PUSH -i config/zookeeper/ -z node1:2181
    ```

    You can validate this by reading back the Metron configuration from Zookeeper using the same script. The result should look-like the following.
    ```
    $ bin/zk_load_configs.sh -m DUMP -z node1:2181
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
    $ bin/stellar -z node1:2181
    [Stellar]>>> PROFILE_GET( "hello-world", "10.0.0.1", PROFILE_FIXED(30, "MINUTES"))
    [451, 448]
    ```

    This result indicates that over the past 30 minutes, the Profiler stored two values related to the source IP address "10.0.0.1".  In the first 15 minute period, the IP `10.0.0.1` was seen in 451 telemetry messages.  In the second 15 minute period, the same IP was seen in 448 telemetry messages.

    It is assumed that the `PROFILE_GET` client is correctly configured to match the Profile configuration before using it to read that Profile.  More information on configuring and using the Profiler client can be found [here](../metron-profiler-client).  


## Creating Profiles

The Profiler specification requires a JSON-formatted set of elements, many of which can contain Stellar code.  The specification contains the following elements.  (For the impatient, skip ahead to the [Examples](#examples).)
The specification for the Profiler topology is stored in Zookeeper at  `/metron/topology/profiler`.  These properties also exist in the local filesystem at `$METRON_HOME/config/zookeeper/profiler.json`.
The values can be changed on disk and then uploaded to Zookeeper using `$METRON_HOME/bin/zk_load_configs.sh`.

| Name                          |               | Description
|---                            |---            |---
| [profile](#profile)           | Required      | Unique name identifying the profile.
| [foreach](#foreach)           | Required      | A separate profile is maintained "for each" of these.
| [onlyif](#onlyif)             | Optional      | Boolean expression that determines if a message should be applied to the profile.
| [groupBy](#groupby)           | Optional      | One or more Stellar expressions used to group the profile measurements when persisted.
| [init](#init)                 | Optional      | One or more expressions executed at the start of a window period.
| [update](#update)             | Required      | One or more expressions executed when a message is applied to the profile.
| [result](#result)             | Required      | Stellar expressions that are executed when the window period expires.
| [expires](#expires)           | Optional      | Profile data is purged after this period of time, specified in days.

### `profile`

*Required*

A unique name identifying the profile.  The field is treated as a string.

### `foreach`

*Required*

A separate profile is maintained 'for each' of these.  This is effectively the entity that the profile is describing.  The field is expected to contain a Stellar expression whose result is the entity name.  

For example, if `ip_src_addr` then a separate profile would be maintained for each unique IP source address in the data; 10.0.0.1, 10.0.0.2, etc.

### `onlyif`

*Optional*

An expression that determines if a message should be applied to the profile.  A Stellar expression that returns a Boolean is expected.  A message is only applied to a profile if this expression is true. This allows a profile to filter the messages that get applied to it.

### `groupBy`

*Optional*

One or more Stellar expressions used to group the profile measurements when persisted. This can be used to sort the Profile data to allow for a contiguous scan when accessing subsets of the data.  This is also one way to deal with calendar effects.  For example, where activity on a weekday can be very different from a weekend.

A common use case would be grouping by day of week.  This allows a contiguous scan to access all profile data for Mondays only.  Using the following definition would achieve this.

```
"groupBy": [ "DAY_OF_WEEK(start)" ]
```

The expression can reference any of these variables.
* Any variable defined by the profile in its `init` or `update` expressions.
* `profile` The name of the profile.
* `entity` The name of the entity being profiled.
* `start` The start time of the profile period in epoch milliseconds.
* `end` The end time of the profile period in epoch milliseconds.
* `duration` The duration of the profile period in milliseconds.
* `result` The result of executing the `result` expression.

### `init`

*Optional*

One or more expressions executed at the start of a window period.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can contain zero or more variable:expression pairs. At the start of each window period, each expression is executed once and stored in the given variable. Note that constant init values such as "0" must be in quotes regardless of their type, as the init value must be a string to be executed by Stellar.

```
"init": {
  "var1": "0",
  "var2": "1"
}
```

### `update`

*Required*

One or more expressions executed when a message is applied to the profile.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can include 0 or more variables/expressions. When each message is applied to the profile, the expression is executed and stored in a variable with the given name.

```
"update": {
  "var1": "var1 + 1",
  "var2": "var2 + 1"
}
```

### `result`

*Required*

Stellar expressions that are executed when the window period expires.  The expressions are expected to summarize the messages that were applied to the profile over the window period.  In the most basic form a single result is persisted for later retrieval.
```
"result": "var1 + var2"
```

For more advanced use cases, a profile can generate two types of results.  A profile can define one or both of these result types at the same time.
* `profile`:  A required expression that defines a value that is persisted for later retrieval.
* `triage`: An optional expression that defines values that are accessible within the Threat Triage process.

**profile**

A required Stellar expression that results in a value that is persisted in the profile store for later retrieval.  The expression can result in any object that is Kryo serializable.  These values can be retrieved for later use with the [Profiler Client](../metron-profiler-client).
```
"result": {
    "profile": "2 + 2"
}
```

An alternative, simplified form is also acceptable.
```
"result": "2 + 2"
```

**triage**

An optional map of one or more Stellar expressions. The value of each expression is made available to the Threat Triage process under the given name.  Each expression must result in a either a primitive type, like an integer, long, or short, or a String.  All other types will result in an error.

In the following example, three values, the minimum, the maximum and the mean are appended to a message.  This message is consumed by Metron, like other sources of telemetry, and each of these values are accessible from within the Threat Triage process using the given field names; `min`, `max`, and `mean`.
```
"result": {
    "triage": {
        "min": "STATS_MIN(stats)",
        "max": "STATS_MAX(stats)",
        "mean": "STATS_MEAN(stats)"
    }
}
```

### `expires`

*Optional*

A numeric value that defines how many days the profile data is retained.  After this time, the data expires and is no longer accessible.  If no value is defined, the data does not expire.

The REPL can be a powerful for developing profiles. Read all about [Developing Profiles](../metron-profiler-client/#developing_profiles).

## Configuring the Profiler

The Profiler runs as an independent Storm topology.  The configuration for the Profiler topology is stored in local filesystem at `$METRON_HOME/config/profiler.properties`.
The values can be changed on disk and then the Profiler topology must be restarted.


| Setting                                                                       | Description
|---                                                                            |---
| [`profiler.input.topic`](#profilerinputtopic)                                 | The name of the Kafka topic from which to consume data.
| [`profiler.output.topic`](#profileroutputtopic)                               | The name of the Kafka topic to which profile data is written.  Only used with profiles that define the [`triage` result field](#result).
| [`profiler.period.duration`](#profilerperiodduration)                         | The duration of each profile period.  
| [`profiler.period.duration.units`](#profilerperioddurationunits)              | The units used to specify the [`profiler.period.duration`](#profilerperiodduration).  
| [`profiler.workers`](#profilerworkers)                                        | The number of worker processes for the topology.
| [`profiler.executors`](#profilerexecutors)                                    | The number of executors to spawn per component.
| [`profiler.ttl`](#profilerttl)                                                | If a message has not been applied to a Profile in this period of time, the Profile will be forgotten and its resources will be cleaned up.
| [`profiler.ttl.units`](#profilerttlunits)                                     | The units used to specify the `profiler.ttl`.
| [`profiler.hbase.salt.divisor`](#profilerhbasesaltdivisor)                    | A salt is prepended to the row key to help prevent hotspotting.
| [`profiler.hbase.table`](#profilerhbasetable)                                 | The name of the HBase table that profiles are written to.
| [`profiler.hbase.column.family`](#profilerhbasecolumnfamily)                  | The column family used to store profiles.
| [`profiler.hbase.batch`](#profilerhbasebatch)                                 | The number of puts that are written to HBase in a single batch.
| [`profiler.hbase.flush.interval.seconds`](#profilerhbaseflushintervalseconds) | The maximum number of seconds between batch writes to HBase.

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

## Examples

The following examples are intended to highlight the functionality provided by the Profiler. Each shows the configuration that would be required to generate the profile.  

These examples assume a fictitious input message stream that looks something like the following.

```
{
  "ip_src_addr": "10.0.0.1",
  "protocol": "HTTPS",
  "length": "10",
  "bytes_in": "234"
},
{
  "ip_src_addr": "10.0.0.2",
  "protocol": "HTTP",
  "length": "20",
  "bytes_in": "390"
},
{
  "ip_src_addr": "10.0.0.3",
  "protocol": "DNS",
  "length": "30",
  "bytes_in": "560"
}
```


### Example 1

The total number of bytes of HTTP data for each host. The following configuration would be used to generate this profile.

```
{
  "profiles": [
    {
      "profile": "example1",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "init": {
        "total_bytes": 0.0
      },
      "update": {
        "total_bytes": "total_bytes + bytes_in"
      },
      "result": "total_bytes",
      "expires": 30
    }
  ]
}
```

This creates a profile...
 * Named ‘example1’
 * That for each IP source address
 * Only if the 'protocol' field equals 'HTTP'
 * Initializes a counter ‘total_bytes’ to zero
 * Adds to ‘total_bytes’ the value of the message's ‘bytes_in’ field
 * Returns ‘total_bytes’ as the result
 * The profile data will expire in 30 days

### Example 2

The ratio of DNS traffic to HTTP traffic for each host. The following configuration would be used to generate this profile.

```
{
  "profiles": [
    {
      "profile": "example2",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'DNS' or protocol == 'HTTP'",
      "init": {
        "num_dns": 1.0,
        "num_http": 1.0
      },
      "update": {
        "num_dns": "num_dns + (if protocol == 'DNS' then 1 else 0)",
        "num_http": "num_http + (if protocol == 'HTTP' then 1 else 0)"
      },
      "result": "num_dns / num_http"
    }
  ]
}
```

This creates a profile...
 * Named ‘example2’
 * That for each IP source address
 * Only if the 'protocol' field equals 'HTTP' or 'DNS'
 * Accumulates the number of DNS requests
 * Accumulates the number of HTTP requests
 * Returns the ratio of these as the result

### Example 3

The average of the `length` field of HTTP traffic. The following configuration would be used to generate this profile.

```
{
  "profiles": [
    {
      "profile": "example3",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "update": { "s": "STATS_ADD(s, length)" },
      "result": "STATS_MEAN(s)"
    }
  ]
}
```

This creates a profile...
 * Named ‘example3’
 * That for each IP source address
 * Only if the 'protocol' field is 'HTTP'
 * Adds the `length` field from each message
 * Calculates the average as the result

### Example 4

It is important to note that the Profiler can persist any serializable Object, not just numeric values.  An alternative to the previous example could take advantage of this.  

Instead of storing the mean of the lengths, the profile could store a statistical summarization of the lengths.  This summary can then be used at a later time to calculate the mean, min, max, percentiles, or any other sensible metric.  This provides a much greater degree of flexibility.

```
{
  "profiles": [
    {
      "profile": "example4",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "update": { "s": "STATS_ADD(s, length)" },
      "result": "s"
    }
  ]
}
```

The following Stellar REPL session shows how you might use this summary to calculate different metrics with the same underlying profile data.
It is assumed that the PROFILE_GET client is configured as described [here](../metron-profiler-client).

Retrieve the last 30 minutes of profile measurements for a specific host.
```
$ bin/stellar -z node1:2181

[Stellar]>>> stats := PROFILE_GET( "example4", "10.0.0.1", PROFILE_FIXED(30, "MINUTES"))
[Stellar]>>> stats
[org.apache.metron.common.math.stats.OnlineStatisticsProvider@79fe4ab9, ...]
```

Calculate different metrics with the same profile data.
```
[Stellar]>>> STATS_MEAN( GET_FIRST( stats))
15979.0625

[Stellar]>>> STATS_PERCENTILE( GET_FIRST(stats), 90)
30310.958
```

Merge all of the profile measurements over the past 30 minutes into a single summary and calculate the 90th percentile.
```
[Stellar]>>> merged := STATS_MERGE( stats)
[Stellar]>>> STATS_PERCENTILE(merged, 90)
29810.992
```

More information on accessing profile data can be found in the [Profiler Client](../metron-profiler-client).

More information on using the [`STATS_*` functions in Stellar can be found here](../../metron-platform/metron-common).

## Implementation

## Key Classes

* `ProfileMeasurement` - Represents a single data point within a Profile.  A Profile is effectively a time series.  To this end a Profile is composed of many ProfileMeasurement values which in aggregate form a time series.  

* `ProfilePeriod` - The Profiler captures one `ProfileMeasurement` each `ProfilePeriod`.  A `ProfilePeriod` will occur at fixed, deterministic points in time.  This allows for efficient retrieval of profile data.

* `RowKeyBuilder` - Builds row keys that can be used to read or write profile data to HBase.

* `ColumnBuilder` - Defines the columns of data stored with a profile measurement.

* `ProfileHBaseMapper` - Defines for the `HBaseBolt` how profile measurements are stored in HBase.  This class leverages a `RowKeyBuilder` and `ColumnBuilder`.

## Storm Topology

The Profiler is implemented as a Storm topology using the following bolts and spouts.

* `KafkaSpout` - A spout that consumes messages from a single Kafka topic.  In most cases, the Profiler topology will consume messages from the `indexing` topic.  This topic contains fully enriched messages that are ready to be indexed.  This ensures that profiles can take advantage of all the available data elements.

* `ProfileSplitterBolt` - The bolt responsible for filtering incoming messages and directing each to the one or more downstream bolts that are responsible for building a profile.  Each message may be needed by 0, 1 or even many profiles.  Each emitted tuple contains the 'resolved' entity name, the profile definition, and the input message.

* `ProfileBuilderBolt` - This bolt maintains all of the state required to build a profile.  When the window period expires, the data is summarized as a `ProfileMeasurement`, all state is flushed, and the `ProfileMeasurement` is emitted.  Each instance of this bolt is responsible for maintaining the state for a single Profile-Entity pair.

* `HBaseBolt` - A bolt that is responsible for writing to HBase.  Most profiles will be flushed every 15 minutes or so.  If each `ProfileBuilderBolt` were responsible for writing to HBase itself, there would be little to no opportunity to optimize these writes.  By aggregating the writes from multiple Profile-Entity pairs these writes can be batched, for example.
