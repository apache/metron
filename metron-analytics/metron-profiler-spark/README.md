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
# Metron Profiler for Spark

This project allows profiles to be executed using [Apache Spark](https://spark.apache.org). This is a port of the Profiler to Spark that allows you to backfill profiles using archived telemetry.

* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [Installation](#installation)
* [Running the Profiler](#running-the-profiler)
* [Configuring the Profiler](#configuring-the-profiler)

## Introduction

Using the [Streaming Profiler](../metron-profiler-storm/README.md) in [Apache Storm](http://storm.apache.org) allows you to create profiles based on the stream of telemetry being captured, enriched, triaged, and indexed by Metron. This does not allow you to create a profile based on telemetry that was captured in the past.  

There are many cases where you might want to produce a profile from telemetry in the past.  This is referred to as profile seeding or backfilling.

* As a Security Data Scientist, I want to understand the historical behaviors and trends of a profile so that I can determine if the profile has predictive value for model building.

* As a Security Platform Engineer, I want to generate a profile using archived telemetry when I deploy a new model to production so that models depending on that profile can function on day 1.

The Batch Profiler running in [Apache Spark](https://spark.apache.org) allows you to seed a profile using archived telemetry.

The portion of a profile produced by the Batch Profiler should be indistinguishable from the portion created by the Streaming Profiler.  Consumers of the profile should not care how the profile was generated.  Using the Streaming Profiler together with the Batch Profiler allows you to create a complete profile over a wide range of time.

For an introduction to the Profiler, see the [Profiler README](../metron-profiler-common/README.md).

## Getting Started

1. Create a profile definition by editing `$METRON_HOME/config/zookeeper/profiler.json` as follows.  

    ```
    cat $METRON_HOME/config/zookeeper/profiler.json
    {
      "profiles": [
        {
          "profile": "hello-world",
          "foreach": "'global'",
          "init":    { "count": "0" },
          "update":  { "count": "count + 1" },
          "result":  "count"
        }
      ],
      "timestampField": "timestamp"
    }
    ```

1. Ensure that you have archived telemetry available for the Batch Profiler to consume.  By default, Metron will store this in HDFS at `/apps/metron/indexing/indexed/*/*`.

    ```
    hdfs dfs -cat /apps/metron/indexing/indexed/*/* | wc -l
    ```

1. Copy the `hbase-site.xml` file from `/etc/hbase/conf` to `/etc/spark2/conf`. It is advised to create a symlink to avoid the duplication of file, also to keep consistency between files while config updates.

   ```
    ln -s /etc/hbase/conf/hbase-site.xml /etc/spark2/conf/hbase-site.xml
   ```

1. Review the Batch Profiler's properties located at `$METRON_HOME/config/batch-profiler.properties`.  See [Configuring the Profiler](#configuring-the-profiler) for more information on these properties.

1. You may want to edit the log4j properties that sits in your config directory in `${SPARK_HOME}` or create one.  It may be helpful to turn on `DEBUG` logging for the Profiler by adding the following line.

	  ```
	  log4j.logger.org.apache.metron.profiler.spark=DEBUG
	  ```

1. Run the Batch Profiler.

    ```
    source /etc/default/metron
    cd $METRON_HOME
    $METRON_HOME/bin/start_batch_profiler.sh
    ```

1. Query for the profile data using the [Profiler Client](../metron-profiler-client/README.md).

## Installation

The Batch Profiler package is installed automatically when installing Metron using the Ambari MPack.  See the following notes when installing the Batch Profiler without the Ambari MPack.

### Prerequisites

The Batch Profiler requires Spark version 2.3.0+.

#### Build the RPM

1. Build Metron.
    ```
    mvn clean package -DskipTests -T2C
    ```

1. Build the RPMs.
    ```
    cd metron-deployment/
    mvn clean package -Pbuild-rpms
    ```

1. Retrieve the package.
    ```
    find ./ -name "metron-profiler-spark*.rpm"
    ```

#### Build the DEB

1. Build Metron.
    ```
    mvn clean package -DskipTests -T2C
    ```

1. Build the DEBs.
    ```
    cd metron-deployment/
    mvn clean package -Pbuild-debs
    ```

1. Retrieve the package.
    ```
    find ./ -name "metron-profiler-spark*.deb"
    ```

## Running the Profiler

* [Usage](#usage)
* [Advanced Usage](#advanced-usage)
* [Spark Execution](#spark-execution)
* [Kerberos](#kerberos)
* [Input Formats](#input-formats)

### Usage

A script located at `$METRON_HOME/bin/start_batch_profiler.sh` has been provided to simplify running the Batch Profiler.  This script makes the following assumptions.

  * The script builds the profiles defined in `$METRON_HOME/config/zookeeper/profiler.json`.

  * The properties defined in `$METRON_HOME/config/batch-profiler.properties` are passed to both the Profiler and Spark.  You can define both Spark and Profiler properties in this same file.

  * The script assumes that Spark is installed at `/usr/hdp/current/spark2-client`.  This can be overridden if you define an environment variable called `SPARK_HOME` prior to executing the script.

### Advanced Usage

The Batch Profiler may also be started using `spark-submit` as follows.  See the Spark Documentation for more information about [`spark-submit`](https://spark.apache.org/docs/latest/submitting-applications.html#launching-applications-with-spark-submit).

```
${SPARK_HOME}/bin/spark-submit \
    --class org.apache.metron.profiler.spark.cli.BatchProfilerCLI \
    --properties-file ${SPARK_PROPS_FILE} \
    ${METRON_HOME}/lib/metron-profiler-spark-*.jar \
    --config ${PROFILER_PROPS_FILE} \
    --profiles ${PROFILES_FILE}
```

The Batch Profiler accepts the following arguments when run from the command line as shown above.  All arguments following the Profiler jar are passed to the Profiler.  All argument preceeding the Profiler jar are passed to Spark.

| Argument                              | Description
|---                                    |---
| [`-p`, `--profiles`](#--profiles)     | Path to the profile definitions.
| [`-c`, `--config`](#--config)         | Path to the profiler properties file.
| [`-g`, `--globals`](#--globals)       | Path to the Stellar global config file.
| [`-r`, `--reader`](#--reader)         | Path to properties for the DataFrameReader.
| `-h`, `--help`                        | Print the help text.

#### `--profiles`

The path to a file containing the profile definition in JSON.

#### `--config`

The path to a file containing key-value properties for the Profiler. This file would contain the properties described under [Configuring the Profiler](#configuring-the-profiler).

#### `--globals`

The path to a file containing key-value properties that define the global properties. This can be used to customize how certain Stellar functions behave during execution.

#### `--reader`

The path to a file containing key-value properties that are passed to the DataFrameReader when reading the input telemetry. This allows additional customization for how the input telemetry is read.

### Spark Execution

Spark supports a number of different [cluster managers](https://spark.apache.org/docs/latest/cluster-overview.html#cluster-manager-types).  The underlying cluster manager is transparent to the Profiler.  To run the Profiler on a particular cluster manager, it is just a matter of setting the appropriate options as defined in the Spark documentation.

#### Local Mode

By default, the Batch Profiler instructs Spark to run in local mode.  This will run all of the Spark execution components within a single JVM.  This mode is only useful for testing with a limited set of data.

`$METRON_HOME/config/batch-profiler.properties`
```
spark.master=local
```

#### Spark on YARN

To run the Profiler using [Spark on YARN](https://spark.apache.org/docs/latest/running-on-yarn.html#running-spark-on-yarn), at a minimum edit the value of `spark.master` as shown. In many cases it also makes sense to set the YARN [deploy mode](https://spark.apache.org/docs/latest/running-on-yarn.html#launching-spark-on-yarn) to `cluster`.

`$METRON_HOME/config/batch-profiler.properties`
```
spark.master=yarn
spark.submit.deployMode=cluster
```

See the Spark documentation for information on how to further control the execution of Spark on YARN.  Any of [these properties](http://spark.apache.org/docs/latest/running-on-yarn.html#spark-properties) can be added to the Profiler properties file.

The following command can be useful to review the logs generated when the Profiler is executed on YARN.
```
yarn logs -applicationId <application-id>
```

### Kerberos

See the Spark documentation for information on running the Batch Profiler in a [secure, kerberized cluster](https://spark.apache.org/docs/latest/running-on-yarn.html#running-in-a-secure-cluster).

### Input Formats

The Profiler can consume archived telemetry stored in a variety of input formats.  By default, it is configured to consume the text/json that Metron archives in HDFS. This is often not the best format for archiving telemetry.  If you choose a different format, you should be able to configure the Profiler to consume it by doing the following.

1. Edit [`profiler.batch.input.format`](#profilerbatchinputformat) and [`profiler.batch.input.path`](#profilerbatchinputpath) as needed.  For example, to read ORC you might do the following.

  `$METRON_HOME/config/batch-profiler.properties`
  ```
  profiler.batch.input.format=org.apache.spark.sql.execution.datasources.orc
  profiler.batch.input.path=hdfs://localhost:9000/apps/metron/indexing/orc/\*/\*
  ```

1. If additional options are required for your input format, then use the [`--reader`](#--reader) command-line argument when launching the Batch Profiler as [described here](#advanced-usage).

#### Common Formats

The following examples highlight the configuration values needed to read telemetry stored in common formats.  These values should be defined in the Profiler properties (see [`--config`](#--config)).

##### JSON
```
profiler.batch.input.reader=json
profiler.batch.input.path=/path/to/json/
```

##### [Apache ORC](https://orc.apache.org/)
```
profiler.batch.input.reader=orc
profiler.batch.input.path=/path/to/orc/
```

##### [Apache Parquet](http://parquet.apache.org/)
```
profiler.batch.input.reader=parquet
profiler.batch.input.path=/path/to/parquet/
```


## Configuring the Profiler

By default, the configuration for the Batch Profiler is stored in the local filesystem at `$METRON_HOME/config/batch-profiler.properties`.

You can store both settings for the Profiler along with settings for Spark in this same file.  Spark will only read settings that start with `spark.`.

| Setting                                                                       | Description
|---                                                                            |---
| [`profiler.batch.input.path`](#profilerbatchinputpath)                        | The path to the input data read by the Batch Profiler.
| [`profiler.batch.input.reader`](#profilerbatchinputreader)                    | The telemetry reader used to read the input data.
| [`profiler.batch.input.format`](#profilerbatchinputformat)                    | The format of the input data read by the Batch Profiler.
| [`profiler.batch.input.begin`](#profilerbatchinputend)                        | Only messages with a timestamp after this will be profiled.
| [`profiler.batch.input.end`](#profilerbatchinputbegin)                        | Only messages with a timestamp before this will be profiled.
| [`profiler.period.duration`](#profilerperiodduration)                         | The duration of each profile period.  
| [`profiler.period.duration.units`](#profilerperioddurationunits)              | The units used to specify the [`profiler.period.duration`](#profilerperiodduration).
| [`profiler.hbase.salt.divisor`](#profilerhbasesaltdivisor)                    | A salt is prepended to the row key to help prevent hot-spotting.
| [`profiler.hbase.table`](#profilerhbasetable)                                 | The name of the HBase table that profiles are written to.
| [`profiler.hbase.column.family`](#profilerhbasecolumnfamily)                  | The column family used to store profiles.

### `profiler.batch.input.path`

*Default*: hdfs://localhost:9000/apps/metron/indexing/indexed/\*/\*

The path to the input data read by the Batch Profiler.

### `profiler.batch.input.reader`

*Default*: json

Defines how the input data is treated when read.  The value is not case sensitive so `JSON` and `json` are equivalent.

 * `json`: Read text/json formatted telemetry
 * `orc`: Read [Apache ORC](https://orc.apache.org/) formatted telemetry
 * `parquet`: Read [Apache Parquet](http://parquet.apache.org/) formatted telemetry
 * `text` Consumes input data stored as raw text. Should be defined along with [`profiler.batch.input.format`](#profilerbatchinputformat). Only use if the input format is not directly supported like `json`.
 * `columnar` Consumes input data stored in columnar formats. Should be defined along with [`profiler.batch.input.format`](#profilerbatchinputformat).  Only use if the input format is not directly supported like `json`.

See [Common Formats](#common-formats) for further information.

### `profiler.batch.input.format`

*Default*: text

The format of the input data read by the Batch Profiler. This is optional and not required in most cases. For example, this property is not required when [`profiler.batch.input.reader`](#profilerbatchinputreader)  is `json`, `orc`, or `parquet`.

### `profiler.batch.input.begin`

*Default*: undefined; no time constraint

Only messages with a timestamp equal to or after this will be profiled. The Profiler will only profiles messages with a timestamp in [`profiler.batch.input.begin`, `profiler.batch.input.end`] inclusive.

By default, no time constraint is defined. The value is expected to follow the [ISO-8601 instant format](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT); 2011-12-03T10:15:30Z.


### `profiler.batch.input.end`

*Default*: undefined; no time constraint

Only messages with a timestamp before or equal to this will be profiled. The Profiler will only profiles messages with a timestamp in [`profiler.batch.input.begin`, `profiler.batch.input.end`] inclusive.

By default, no time constraint is defined. The value is expected to follow the [ISO-8601 instant format](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT); 2011-12-03T10:15:30Z.

### `profiler.period.duration`

*Default*: 15

The duration of each profile period.  This value should be defined along with [`profiler.period.duration.units`](#profilerperioddurationunits).

*Important*: To read a profile using the [Profiler Client](metron-analytics/metron-profiler-client), the Profiler Client's `profiler.client.period.duration` property must match this value.  Otherwise, the Profiler Client will be unable to read the profile data.  

### `profiler.period.duration.units`

*Default*: MINUTES

The units used to specify the `profiler.period.duration`.  This value should be defined along with [`profiler.period.duration`](#profilerperiodduration).

*Important*: To read a profile using the Profiler Client, the Profiler Client's `profiler.client.period.duration.units` property must match this value.  Otherwise, the [Profiler Client](metron-analytics/metron-profiler-client) will be unable to read the profile data.

### `profiler.hbase.salt.divisor`

*Default*: 1000

A salt is prepended to the row key to help prevent hotspotting.  This constant is used to generate the salt.  This constant should be roughly equal to the number of nodes in the Hbase cluster to ensure even distribution of data.

### `profiler.hbase.table`

*Default*: profiler

The name of the HBase table that profile data is written to.  The Profiler expects that the table exists and is writable.  It will not create the table.

### `profiler.hbase.column.family`

*Default*: P

The column family used to store profile data in HBase.
