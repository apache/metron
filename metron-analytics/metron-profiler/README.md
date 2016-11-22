# Metron Profiler

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior. 

This is achieved by summarizing the streaming telemetry data consumed by Metron over sliding windows. A summary statistic is applied to the data received within a given window.  Collecting this summary across many windows results in a time series that is useful for analysis.
 
Any field contained within a message can be used to generate a profile.  A profile can even be produced by combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language. A user only need configure the desired profiles and ensure that the Profiler topology is running.

* [Getting Started](#getting-started)
* [Creating Profiles](#creating-profiles)
* [Configuring the Profiler](#configuring-the-profiler)
* [Examples](#examples)
* [Implementation](#implementation)

## Getting Started

This section will describe the steps required to get your first profile running.

1. Stand-up a Metron environment.  For this example, we will use the 'Quick Dev' environment.  Follow the instructions included with [Quick Dev](../../metron-deployment/vagrant/quick-dev-platform) or build your own.

1. Create a table within HBase that will store the profile data. The table name and column family must match the [Profiler's configuration](#configuring-the-profiler).
    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> create 'profiler', 'P'
    ```
    
1. Define the profile in a file located at `$METRON_HOME/config/zookeeper/profiler.json`.  The following example JSON will create a profile that simply counts the number of messages per `ip_src_addr`, during each sampling interval.
    ```
    {
      "profiles": [
        {
          "profile": "test",
          "foreach": "ip_src_addr",
          "init":    { "count": 0 },
          "update":  { "count": "count + 1" },
          "result":  "count"
        }
      ]
    }
    ```

1. Upload the profile definition to Zookeeper.
    ```
    $ cd /usr/metron/0.3.0/
    $ bin/zk_load_configs.sh -m PUSH -i config/zookeeper/ -z node1:2181
    ```

1. Start the Profiler topology.
    ```
    $ bin/start_profiler_topology.sh
    ```

1. Ensure that test messages are being sent to the Profiler's input topic in Kafka.  The Profiler will consume messages from the `inputTopic` defined in the [Profiler's configuration](#configuring-the-profiler).

1. Check the HBase table to validate that the Profiler is writing the profile.  Remember that the Profiler is flushing the profile every 15 minutes.  You will need to wait at least this long to start seeing profile data in HBase.
    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> count 'profiler'
    ``` 

1. Use the Profiler Client to read the profile data.  The below example `PROFILE_GET` command will read data written by the sample profile given above, if 10.0.0.1 is one of the input values for `ip_src_addr`.  More information on using the client can be found [here](../metron-profiler-client).
    ```
    $ bin/stellar -z node1:2181
    
    [Stellar]>>> PROFILE_GET( "test", "10.0.0.1", 30, "MINUTES")
    [451, 448]
    ```

## Creating Profiles

The Profiler configuration requires a JSON-formatted set of elements, many of which can contain Stellar code.  The configuration contains the following elements.  For the impatient, skip ahead to the [Examples](#examples).

| Name 	                |               | Description 	
|---	                |---	        |---
| [profile](#profile)   | Required   	| Unique name identifying the profile. 
| [foreach](#foreach)   | Required  	| A separate profile is maintained "for each" of these. 
| [onlyif](#onlyif)  	| Optional  	| Boolean expression that determines if a message should be applied to the profile.
| [groupBy](#groupby)   | Optional      | One or more Stellar expressions used to group the profile measurements when persisted.
| [init](#init)  	    | Optional  	| One or more expressions executed at the start of a window period.
| [update](#update)  	| Required  	| One or more expressions executed when a message is applied to the profile.
| [result](#result)   	| Required  	| A Stellar expression that is executed when the window period expires.
| [expires](#expires)   | Optional      | Profile data is purged after this period of time, specified in milliseconds.

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

One or more Stellar expressions used to group the profile measurements when persisted. This is intended to sort the Profile data to allow for a contiguous scan when accessing subsets of the data. 

The 'groupBy' expressions can refer to any field within a `org.apache.metron.profiler.ProfileMeasurement`.  A common use case would be grouping by day of week.  This allows a contiguous scan to access all profile data for Mondays only.  Using the following definition would achieve this. 

```
"groupBy": [ "DAY_OF_WEEK()" ] 
```

### `init`

*Optional*

One or more expressions executed at the start of a window period.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can contain 0 or more variables/expressions. At the start of each window period the expression is executed once and stored in a variable with the given name. 

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

A Stellar expression that is executed when the window period expires.  The expression is expected to summarize the messages that were applied to the profile over the window period.  The expression must result in a numeric value such as a Double, Long, Float, Short, or Integer.  	   

### `expires`

*Optional*

A numeric value that defines how many days the profile data is retained.  After this time, the data expires and is no longer accessible.  If no value is defined, the data does not expire.

## Configuring the Profiler

The Profiler runs as an independent Storm topology.  The configuration for the Profiler topology is stored in Zookeeper at  `/metron/topology/profiler`.  These properties also exist in the the default installation of Metron at `$METRON_HOME/config/zookeeper/profiler.json`. The values can be changed on disk and then uploaded to Zookeeper using `$METRON_HOME/bin/zk_load_configs.sh`.

| Setting   | Description   |
|---        |---            |
| profiler.workers | The number of worker processes to create for the topology.   |
| profiler.executors | The number of executors to spawn per component.  |
| profiler.input.topic | The name of the Kafka topic from which to consume data.  |
| profiler.period.duration | The duration of each profile period.  This value should be defined along with `profiler.period.duration.units`.  |
| profiler.period.duration.units | The units used to specify the `profiler.period.duration`. |
| profiler.ttl | If a message has not been applied to a Profile in this period of time, the Profile will be forgotten and its resources will be cleaned up. This value should be defined along with `profiler.ttl.units`. |
| profiler.ttl.units | The units used to specify the `profiler.ttl`. |
| profiler.hbase.salt.divisor  |  A salt is prepended to the row key to help prevent hotspotting.  This constant is used to generate the salt.  Ideally, this constant should be roughly equal to the number of nodes in the Hbase cluster.  |
| profiler.hbase.table | The name of the HBase table that profiles are written to.  |
| profiler.hbase.column.family | The column family used to store profiles. |
| profiler.hbase.batch | The number of puts that are written in a single batch.  |
| profiler.hbase.flush.interval.seconds | The maximum number of seconds between batch writes to HBase. |


After altering the configuration, start the Profiler.

```
$ /usr/metron/0.3.0/start_profiler_topology.sh
```

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

Instead of storing the mean of the length, the profile could store a more generic summary of the length.  This summary can then be used at a later time to calculate the mean, min, max, percentiles, or any other sensible metric.  This provides a much greater degree of flexibility.
 
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

Retrieve the last 30 minutes of profile measurements for a specific host.
```
$ bin/stellar -z node1:2181

[Stellar]>>> stats := PROFILE_GET( "example4", "10.0.0.1", 30, "MINUTES")
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

