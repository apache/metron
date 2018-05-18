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
# Contents

* [Stellar Language](#stellar-language)
* [Global Configuration](#global-configuration)
* [Validation Framework](#validation-framework)
* [Management Utility](#management-utility)
* [Topology Errors](topology-errors)
* [Performance Logging](#performance-logging)
* [Metron Debugging](#metron-debugging)

# Stellar Language

For a variety of components (threat intelligence triage and field
transformations) we have the need to do simple computation and
transformation using the data from messages as variables.
For those purposes, there exists a simple, scaled down DSL
created to do simple computation and transformation.

The query language supports the following:
* Referencing fields in the enriched JSON
* String literals are quoted with either `'` or `"`, and
support escaping for `'`, `"`, `\t`, `\r`, `\n`, and backslash
* Simple boolean operations: `and`, `not`, `or`
  * Boolean expressions are short-circuited (e.g. `true or FUNC()` would never execute `FUNC`)
* Simple arithmetic operations: `*`, `/`, `+`, `-` on real numbers or integers
* Simple comparison operations `<`, `>`, `<=`, `>=`
* Simple equality comparison operations `==`, `!=`
* if/then/else comparisons (i.e. `if var1 < 10 then 'less than 10' else '10 or more'`)
* Determining whether a field exists (via `exists`)
* An `in` operator that works like the `in` in Python
* The ability to have parenthesis to make order of operations explicit
* User defined functions, including Lambda expressions

For documentation of Stellar, please see the [Stellar README](../../metron-stellar/stellar-common/README.md).

# Global Configuration

The format of the global enrichment is a JSON String to Object map.  This is intended for
configuration which is non sensor specific configuration.

This configuration is stored in zookeeper, but looks something like

```json
{
  "es.clustername": "metron",
  "es.ip": "node1",
  "es.port": "9300",
  "es.date.format": "yyyy.MM.dd.HH",
  "parser.error.topic": "indexing",
  "fieldValidations" : [
              {
                "input" : [ "ip_src_addr", "ip_dst_addr" ],
                "validation" : "IP",
                "config" : {
                    "type" : "IPV4"
                           }
              }
                       ]
}
```

Various parts of our stack uses the global config are documented throughout the Metron documentation,
but a convenient index is provided here:

| Property Name                                                                                                       | Subsystem     | Type       | Ambari Property            |
|---------------------------------------------------------------------------------------------------------------------|---------------|------------|----------------------------|
| [`es.clustername`](../metron-elasticsearch#esclustername)                                                           | Indexing      | String     | `es_cluster_name`          |
| [`es.ip`](../metron-elasticsearch#esip)                                                                             | Indexing      | String     | `es_hosts`                 |
| [`es.port`](../metron-elasticsearch#esport)                                                                         | Indexing      | String     | `es_port`                  |
| [`es.date.format`](../metron-elasticsearch#esdateformat)                                                            | Indexing      | String     | `es_date_format`           |
| [`fieldValidations`](#validation-framework)                                                                         | Parsing       | Object     | N/A                        |
| [`parser.error.topic`](../metron-parsers#parsererrortopic)                                                          | Parsing       | String     | N/A                        |
| [`stellar.function.paths`](../../metron-stellar/stellar-common#stellarfunctionpaths)                                | Stellar       | CSV String | N/A                        |
| [`stellar.function.resolver.includes`](../../metron-stellar/stellar-common#stellarfunctionresolverincludesexcludes) | Stellar       | CSV String | N/A                        |
| [`stellar.function.resolver.excludes`](../../metron-stellar/stellar-common#stellarfunctionresolverincludesexcludes) | Stellar       | CSV String | N/A                        |
| [`profiler.period.duration`](../../metron-analytics/metron-profiler#profilerperiodduration)                         | Profiler      | Integer    | `profiler_period_duration` |
| [`profiler.period.duration.units`](../../metron-analytics/metron-profiler#profilerperioddurationunits)              | Profiler      | String     | `profiler_period_units`    |
| [`update.hbase.table`](../metron-indexing#updatehbasetable)                                                         | REST/Indexing | String     | `update_hbase_table`       |
| [`update.hbase.cf`](../metron-indexing#updatehbasecf)                                                               | REST/Indexing | String     | `update_hbase_cf`          |
| [`geo.hdfs.file`](../metron-enrichment#geohdfsfile)                                                                 | Enrichment    | String     | `geo_hdfs_file`            |
| [`source.type.field`](../../metron-interface/metron-alerts#sourcetypefield)                                         | UI            | String     |  N/A                       |

## Note Configs in Ambari
If a field is managed via ambari, you should change the field via
ambari.  Otherwise, upon service restarts, you may find your update
overwritten.

# Validation Framework

Inside of the global configuration, there is a validation framework in
place that enables the validation that messages coming from all parsers
are valid.  This is done in the form of validation plugins where
assertions about fields or whole messages can be made.

The format for this is a `fieldValidations` field inside of global
config.  This is associated with an array of field validation objects
structured like so:
* `input` : An array of input fields or a single field.  If this is omitted, then the whole messages is passed to the validator.
* `config` : A String to Object map for validation configuration.  This is optional if the validation function requires no configuration.
* `validation` : The validation function to be used.  This is one of
   * `STELLAR` : Execute a Stellar Language statement.  Expects the query string in the `condition` field of the config.
   * `IP` : Validates that the input fields are an IP address.  By default, if no configuration is set, it assumes `IPV4`, but you can specify the type by passing in the config by passing in `type` with either `IPV6` or `IPV4` or by passing in a list [`IPV4`,`IPV6`] in which case the input(s) will be validated against both.
   * `DOMAIN` : Validates that the fields are all domains.
   * `EMAIL` : Validates that the fields are all email addresses
   * `URL` : Validates that the fields are all URLs
   * `DATE` : Validates that the fields are a date.  Expects `format` in the config.
   * `INTEGER` : Validates that the fields are an integer.  String representation of an integer is allowed.
   * `REGEX_MATCH` : Validates that the fields match a regex.  Expects `pattern` in the config.
   * `NOT_EMPTY` : Validates that the fields exist and are not empty (after trimming.)


# Management Utility

Configurations should be stored on disk in the following structure starting at `$BASE_DIR`:
* global.json : The global config
* `sensors` : The subdirectory containing sensor enrichment configuration JSON (e.g. `snort.json`, `bro.json`)

By default, this directory as deployed by the ansible infrastructure is at `$METRON_HOME/config/zookeeper`

While the configs are stored on disk, they must be loaded into Zookeeper to be used.  To this end, there is a
utility program to assist in this called `$METRON_HOME/bin/zk_load_config.sh`

This has the following options:

```
 -c,--config_type <CONFIG_TYPE>            The configuration type: GLOBAL,
                                           PARSER, ENRICHMENT, INDEXING,
                                           PROFILER
 -f,--force                                Force operation
 -h,--help                                 Generate Help screen
 -i,--input_dir <DIR>                      The input directory containing
                                           the configuration files named
                                           like "$source.json"
 -m,--mode <MODE>                          The mode of operation: DUMP,
                                           PULL, PUSH, PATCH
 -n,--config_name <CONFIG_NAME>            The configuration name: bro,
                                           yaf, snort, squid, etc.
 -o,--output_dir <DIR>                     The output directory which will
                                           store the JSON configuration
                                           from Zookeeper
 -pk,--patch_key <PATCH_KEY>               The key to modify
 -pm,--patch_mode <PATCH_MODE>             One of: ADD, REMOVE - relevant
                                           only for key/value patches,
                                           i.e. when a patch file is not
                                           used.
 -pf,--patch_file <PATCH_FILE>             Path to the patch file.
 -pv,--patch_value <PATCH_VALUE>           Value to use in the patch.
 -z,--zk_quorum <host:port,[host:port]*>   Zookeeper Quorum URL
                                           (zk1:port,zk2:port,...)
```

Usage examples:

* To dump the existing configs from zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m DUMP`
* To dump the existing GLOBAL configs from zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m DUMP -c GLOBAL`
* To push the configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper`
* To push only the GLOBAL configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper -c GLOBAL`
* To push only the PARSER configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper -c PARSER`
* To push only the PARSER 'bro' configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper -c PARSER -n bro`
* To pull all configs from zookeeper to the singlenode vagrant machine disk: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PULL -o $METRON_HOME/config/zookeeper -f`

## Patching mechanism

The configuration management utility leverages a JSON patching library that conforms to [RFC-6902 spec](https://tools.ietf.org/html/rfc6902). We're using the zjsonpatch library implementation from here - https://github.com/flipkart-incubator/zjsonpatch.
There are a couple options for leveraging patching. You can choose to patch the Zookeeper config via patch file:

`$METRON_HOME/bin/zk_load_configs.sh -z $ZOOKEEPER -m PATCH -c GLOBAL -pf /tmp/mypatch.txt`

or key/value pair:

`$METRON_HOME/bin/zk_load_configs.sh -z $ZOOKEEPER -m PATCH -c GLOBAL -pm ADD -pk foo -pv \"\"bar\"\"`

The options exposed via patch file are the full range of options from RFC-6902:
  - ADD
  - REMOVE
  - REPLACE
  - MOVE
  - COPY
  - TEST

whereas with key/value patching, we only current expose ADD and REMOVE. Note that ADD will function as a REPLACE when the key already exists.

### Patch File

Let's say we want to add a complex JSON object to our configuration with a patch file. e.g.
```
"foo" : {
    "bar" : {
      "baz" : [ "bazval1", "bazval2" ]
    }
  }
```

We would write a patch file "/tmp/mypatch.txt" with contents:
```
[
    {
        "op": "add",
        "path": "/foo",
        "value": { "bar" : { "baz" : [ "bazval1", "bazval2" ] } }
    }
]
```

And submit via zk_load_configs as follows:
```
 $METRON_HOME/bin/zk_load_configs.sh -z $ZOOKEEPER -m PATCH -c GLOBAL -pf /tmp/mypatch.txt
```

### Patch Key/Value

Now let's try the same without using a patch file, instead using the patch_key and patch_value options right from the command line utility. This would like like the following.

```
$METRON_HOME/bin/zk_load_configs.sh -z $ZOOKEEPER -m PATCH -c GLOBAL -pm ADD -pk "/foo" -pv "{ \"bar\" : { \"baz\" : [ \"bazval1\", \"bazval2\" ] } }"
```

### Applying Multiple Patches

Applying multiple patches is also pretty straightforward. You can achieve this in a single command using patch files, or simply execute multiple commands in sequence using the patch_key/value approach.

Let's say we wanted to add the following to our global config:
```
"apache" : "metron",
"is" : "the best",
"streaming" : "analytics platform"
```

and remove the /foo key from the previous example.

Create a patch file /tmp/mypatch.txt with four separate patch operations.
```
[
    {
        "op": "remove",
        "path": "/foo"
    },
    {
        "op": "add",
        "path": "/apache",
        "value": "metron"
    },
    {
        "op": "add",
        "path": "/is",
        "value": "the best"
    },
    {
        "op": "add",
        "path": "/streaming",
        "value": "analytics platform"
    }
]
```

Now submit again and you should see a Global config with the "foo" key removed and three new keys added.
```
 $METRON_HOME/bin/zk_load_configs.sh -z $ZOOKEEPER -m PATCH -c GLOBAL -pf /tmp/mypatch.txt
```

### Notes On Patching

For any given patch key, the last/leaf node in the key's parent *must* exist, otherwise an exception will be thrown. For example, if you want to add the following:
```
"foo": {
    "bar": "baz"
}
```

It is not sufficient to use /foo/bar as a key if foo does not already exist. You would either need to incrementally build the JSON and make this a two step process
```
[
    {
        "op": "add",
        "path": "/foo",
        "value": { }
    },
    {
        "op": "add",
        "path": "/foo/bar",
        "value": "baz"
    }
]
```

Or provide the value as a complete JSON object.
```
[
    {
        "op": "add",
        "path": "/foo",
        "value": { "bar" : "baz" }
    }
]
```

The REMOVE operation is idempotent. Running the remove command on the same key multiple
times will not fail once the key has been removed.

# Topology Errors

Errors generated in Metron topologies are transformed into JSON format and follow this structure:

```
{
  "exception": "java.lang.IllegalStateException: Unable to parse Message: ...",
  "failed_sensor_type": "bro",
  "stack": "java.lang.IllegalStateException: Unable to parse Message: ...",
  "hostname": "node1",
  "source:type": "error",
  "raw_message": "{\"http\": {\"ts\":1488809627.000000.31915,\"uid\":\"C9JpSd2vFAWo3mXKz1\", ...",
  "error_hash": "f7baf053f2d3c801a01d196f40f3468e87eea81788b2567423030100865c5061",
  "error_type": "parser_error",
  "message": "Unable to parse Message: {\"http\": {\"ts\":1488809627.000000.31915,\"uid\":\"C9JpSd2vFAWo3mXKz1\", ...",
  "timestamp": 1488809630698
}
```

Each topology can be configured to send error messages to a specific Kafka topic.  The parser topologies retrieve this setting from the the `parser.error.topic` setting in the global config:
```
{
  "es.clustername": "metron",
  "es.ip": "node1",
  "es.port": "9300",
  "es.date.format": "yyyy.MM.dd.HH",
  "parser.error.topic": "indexing"
}
```

Error topics for enrichment and threat intel errors are passed into the enrichment topology as flux properties named `enrichment.error.topic` and `threat.intel.error.topic`.  These properties can be found in `$METRON_HOME/config/enrichment.properties`.

The error topic for indexing errors is passed into the indexing topology as a flux property named `index.error.topic`.  This property can be found in either `$METRON_HOME/config/elasticsearch.properties` or `$METRON_HOME/config/solr.properties` depending on the search engine selected.

By default all error messages are sent to the `indexing` topic so that they are indexed and archived, just like other messages.  The indexing config for error messages can be found at `$METRON_HOME/config/zookeeper/indexing/error.json`.

# Performance Logging

The PerformanceLogger class provides functionality that enables developers to debug performance issues. Basic usage looks like the following:
```
// create a simple inner performance class to use for logger instantiation
public static class Perf {}
// instantiation
PerformnanceLogger perfLog = new PerformanceLogger(() -> getConfigurations().getGlobalConfig(), Perf.class.getName());
// marking a start time
perfLog.mark("mark1");
// ...do some high performance stuff...
// log the elapsed time
perfLog.log("mark1", "My high performance stuff is very performant");
// log no additional message, just the basics
perfLog.log("mark1");
```

The logger maintains a Map<String, Long> of named markers that correspond to start times. Calling mark() performs a put on the underlying timing store. Output includes the mark name, elapsed time in nanoseconds, as well as any custom messaging you provide. A sample log would look like the following:
```
[DEBUG] markName=execute,time(ns)=121411,message=key=7a8dbe44-4cb9-4db2-9d04-7632f543b56c, elapsed time to run execute
```

__Configuration__

The first argument to the logger is a java.util.function.Supplier<Map<String, Object>>. The offers flexibility in being able to provide multiple configuration "suppliers" depending on your individual usage requirements. The example above,
taken from org.apache.metron.enrichment.bolt.GenericEnrichmentBolt, leverages the global config to dymanically provide configuration from Zookeeper. Any updates to the global config via Zookeeper are reflected live at runtime. Currently,
the PerformanceLogger supports the following options:

|Property Name                              |Type               |Valid Values   |
|-------------------------------------------|-------------------|---------------|
|performance.logging.percent.records        |Integer            |0-100          |


__Other Usage Details__

You can also provide your own format String and provide arguments that will be used when formatting that String. This code avoids expensive String concatenation by only formatting when debugging is enabled. For more complex arguments, e.g. JSON serialization, we expose an isDebugEnabled() method.

```
// log with format String and single argument
perfLog.log("join-message", "key={}, elapsed time to join messages", key);

// check if debugging is enabled for the performance logger to avoid more expensive operations
if (perfLog.isDebugEnabled()) {
    perfLog.log("join-message", "key={}, elapsed time to join messages, message={}", key, rawMessage.toJSONString());
}
```

__Side Effects__

Calling the mark() method multiple times simply resets the start time to the current nano time. Calling log() with a non-existent mark name will log 0 ns elapsed time with a warning indicating that log has been invoked for a mark name that does not exist.
The class is not thread-safe and makes no attempt at keeping multiple threads from modifying the same markers.

# Metron Debugging

A Python script is provided for gathering information useful in debugging your Metron cluster. Run from the node that has Metron installed on it. All options listed below are required.

_Note:_ Be aware that no anonymization/scrubbing is performed on the captured configuration details.

```
# $METRON_HOME/bin/cluster_info.py -h
Usage: cluster_info.py [options]

Options:
  -h, --help            show this help message and exit
  -a HOST:PORT, --ambari-host=HOST:PORT
                        Connect to Ambari via the supplied host:port
  -c NAME, --cluster-name=NAME
                        Name of cluster in Ambari to retrieve info for
  -o DIRECTORY, --out-dir=DIRECTORY
                        Write debugging data to specified root directory
  -s HOST:PORT, --storm-host=HOST:PORT
                        Connect to Storm via the supplied host:port
  -b HOST1:PORT,HOST2:PORT, --broker_list=HOST1:PORT,HOST2:PORT
                        Connect to Kafka via the supplied comma-delimited
                        host:port list
  -z HOST1:PORT,HOST2:PORT, --zookeeper_quorum=HOST1:PORT,HOST2:PORT
                        Connect to Zookeeper via the supplied comma-delimited
                        host:port quorum list
  -m DIRECTORY, --metron_home=DIRECTORY
                        Metron home directory
  -p DIRECTORY, --hdp_home=DIRECTORY
                        HDP home directory
```
