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
# Metron Profiler

* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [Profiles](#profiles)
* [Examples](#examples)

## Introduction

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior.

This is achieved by summarizing the telemetry data consumed by Metron over tumbling windows. A summary statistic is applied to the data received within a given window.  Collecting these values across many windows result in a time series that is useful for analysis.

Any field contained within a message can be used to generate a profile.  A profile can even be produced by combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language.

There are three separate ports of the Profiler that share this common code base.
* The [Storm Profiler](../metron-profiler-storm/README.md) builds low-latency profiles over streaming data sets.
* The [Spark Profiler](../metron-profiler-spark/README.md) backfills profiles using archived telemetry.
* The [REPL Profiler](../metron-profiler-repl/README.md) allows profiles to be tested and debugged within the Stellar REPL.

## Getting Started

1. [Create a profile](../metron-profiler-repl/README.md#getting-started) using the Stellar REPL. Validate your profile using mock data, then apply real, live data.

1. [Backfill your profile](../metron-profiler-spark/README.md#getting-started) using archived telemetry to see how your profile behaves over time.

1. [Deploy your profile](../metron-profiler-storm/README.md#getting-started) to Storm to maintain a low-latency profile over a streaming data set.

1. [Retrieve your profile data](../metron-profiler-client/README.md) using the Stellar API so that you can build enrichments, alert on abnormalities.

1. Explore more ways to create [profiles](#more-examples).

## Profiles

Let's start with a simple example. The following profile maintains a count of the number of telemetry messages for each IP source address.  A counter is initialized to 0, then incremented each time a message is received for a given IP source address.  At regular intervals the count is flushed and stored. Over time this results in a time series describing the amount of telemetry received for each IP source address.

```
{
  "profiles": [
    {
      "profile": "hello-world",
      "foreach": "ip_src_addr",
      "init": {
        "count": "0"
      },
      "update": {
        "count": "count + 1"
      },
      "result": "count"
    }
  ]
}
```

A profile definition contains two fields; only one of which is required.

```
{
    "profiles": [
        { "profile": "one", ... },
        { "profile": "two", ... }
    ],
    "timestampField": "timestamp"
}
```

| Name                              |               | Description
|---                                |---            |---
| [profiles](#profiles)             | Required      | A list of zero or more Profile definitions.
| [timestampField](#timestampfield) | Optional      | Indicates whether processing time or event time should be used. By default, processing time is enabled.


#### `profiles`

*Required*

A list of zero or more Profile definitions.

#### `timestampField`

*Optional*

Indicates whether processing time or event time is used. By default, processing time is enabled.

##### Processing Time

By default, no `timestampField` is defined.  In this case, the Profiler uses system time when generating profiles.  This means that the profiles are generated based on when the data has been processed by the Profiler.  This is also known as 'processing time'.

This is the simplest mode of operation, but has some draw backs.  If the Profiler is consuming live data and all is well, the processing and event times will likely remain similar and consistent. If processing time diverges from event time, then the Profiler will generate skewed profiles.

There are a few scenarios that might cause skewed profiles when using processing time.  For example when a system has undergone a scheduled maintenance window and is restarted, a high volume of messages will need to be processed by the Profiler. The output of the Profiler might indicate an increase in activity during this time, although no change in activity actually occurred on the target network. The same situation could occur if an upstream system which provides telemetry undergoes an outage.  

[Event Time](#event-time) can be used to mitigate these problems.

##### Event Time

Alternatively, a `timestampField` can be defined.  This must be the name of a field contained within the telemetry processed by the Profiler.  The Profiler will extract and use the timestamp contained within this field.

* If a message does not contain this field, it will be dropped.

* The field must contain a timestamp in epoch milliseconds expressed as either a numeric or string. Otherwise, the message will be dropped.

* The Profiler will use the same field across all telemetry sources and for all profiles.

* Be aware of clock skew across telemetry sources.  If your profile is processing telemetry from multiple sources where the clock differs significantly, the Profiler may assume that some of those messages are late and will be ignored.  Adjusting the [`profiler.window.duration`](#profilerwindowduration) and [`profiler.window.lag`](#profilerwindowlag) can help accommodate skewed clocks.

### Profiles

A profile definition requires a JSON-formatted set of elements, many of which can contain Stellar code.  The specification contains the following elements.  (For the impatient, skip ahead to the [Examples](#examples).)

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

#### `profile`

*Required*

A unique name identifying the profile.  The field is treated as a string.

#### `foreach`

*Required*

A separate profile is maintained 'for each' of these.  This is effectively the entity that the profile is describing.  The field is expected to contain a Stellar expression whose result is the entity name.  

For example, if `ip_src_addr` then a separate profile would be maintained for each unique IP source address in the data; 10.0.0.1, 10.0.0.2, etc.

#### `onlyif`

*Optional*

An expression that determines if a message should be applied to the profile.  A Stellar expression that returns a Boolean is expected.  A message is only applied to a profile if this expression is true. This allows a profile to filter the messages that get applied to it.

#### `groupBy`

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

#### `init`

*Optional*

One or more expressions executed at the start of a window period.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can contain zero or more variable:expression pairs. At the start of each window period, each expression is executed once and stored in the given variable. Note that constant init values such as "0" must be in quotes regardless of their type, as the init value must be a string to be executed by Stellar.

```
"init": {
  "var1": "0",
  "var2": "1"
}
```

#### `update`

*Required*

One or more expressions executed when a message is applied to the profile.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can include 0 or more variables/expressions. When each message is applied to the profile, the expression is executed and stored in a variable with the given name.

```
"update": {
  "var1": "var1 + 1",
  "var2": "var2 + 1"
}
```

#### `result`

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

#### `expires`

*Optional*

A numeric value that defines how many days the profile data is retained.  After this time, the data expires and is no longer accessible.  If no value is defined, the data does not expire.

The REPL can be a powerful tool for developing profiles. Read all about [Developing Profiles](../metron-profiler-client/#developing_profiles).

## Examples

The following examples are intended to highlight the functionality provided by the Profiler. Try out these examples easily in the Stellar REPL as described in the [Getting Started](#getting-started) section.

### Example 1

This example captures the ratio of DNS traffic to HTTP traffic for each host. The following configuration would be used to generate this profile.

```
{
  "profiles": [
    {
      "profile": "dns-to-http-by-source",
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
 * Named ‘dns-to-http-by-source’
 * That for each IP source address
 * Only if the 'protocol' field equals 'HTTP' or 'DNS'
 * Accumulates the number of DNS requests
 * Accumulates the number of HTTP requests
 * Returns the ratio of these as the result

### Example 2

This example captures the average of the `length` field for HTTP traffic. The following profile could be used.

```
{
  "profiles": [
    {
      "profile": "avg-http-length",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "update": { "s": "STATS_ADD(s, length)" },
      "result": "STATS_MEAN(s)"
    }
  ]
}
```

This creates a profile...
 * Named ‘avg-http-length’
 * That for each IP source address
 * Only if the 'protocol' field is 'HTTP'
 * Captures the `length` field
 * Calculates the average as the result

It is important to note that the Profiler can persist any serializable Object, not just numeric values. Instead of storing the actual mean, the profile could store a statistical sketch of the lengths.  This summary can then be used at a later time to calculate the mean, min, max, percentiles, or any other sensible metric.  This provides a much greater degree of flexibility. The following Stellar REPL session shows how you might do this.

1. Retrieve the last 30 minutes of profile measurements for a specific host.
    ```
    $ source /etc/default/metron
    $ bin/stellar -z $ZOOKEEPER

    [Stellar]>>> stats := PROFILE_GET( "example4", "10.0.0.1", PROFILE_FIXED(30, "MINUTES"))
    [org.apache.metron.common.math.stats.OnlineStatisticsProvider@79fe4ab9, ...]
    ```

1. Calculate different summary metrics using the same profile data.
    ```
    [Stellar]>>> aStat := GET_FIRST(stats)
    org.apache.metron.common.math.stats.OnlineStatisticsProvider@79fe4ab9

    [Stellar]>>> STATS_MEAN(aStat)
    15979.0625

    [Stellar]>>> STATS_PERCENTILE(aStat, 90)
    30310.958
    ```

1. Merge all of the profile measurements over the past 30 minutes into a single sketch and calculate the 90th percentile.
    ```
    [Stellar]>>> merged := STATS_MERGE( stats)

    [Stellar]>>> STATS_PERCENTILE(merged, 90)
    29810.992
    ```


More information on accessing profile data can be found in the [Profiler Client](../metron-profiler-client/README.md).

More information on using the [`STATS_*` functions](../metron-statistics/README.md).


### Example 3

This profile captures the vertex degree of a host. If you view network communication as a directed graph, the in and out degree of each host can distinguish behaviors. Anomalies can serve as an indicator of compromise.  For example, you might find clients normally have an out-degree >> in-degree, whereas a server might be the opposite.

```
{
  "profiles": [
    {
      "profile": "in-degrees",
      "onlyif": "source.type == 'yaf'",
      "foreach": "ip_dst_addr",
      "update": { "in": "HLLP_ADD(in, ip_src_addr)" },
      "result": "HLLP_CARDINALITY(in)"
    },
    {
      "profile": "out-degrees",
      "onlyif": "source.type == 'yaf'",
      "foreach": "ip_src_addr",
      "update": { "out": "HLLP_ADD(out, ip_dst_addr)" },
      "result": "HLLP_CARDINALITY(out)"
    }
  ]
}
```

This creates a profile...
 * Named ‘in-degrees’
 * That for each IP destination address
 * Captures the IP source address
 * Then calculates the cardinality; the number of unique IPs this host has interacted with

The second profile calculates the out-degree.
