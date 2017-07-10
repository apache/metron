

# Contents

* [Stellar Language](#stellar-language)
* [Global Configuration](#global-configuration)
* [Validation Framework](#validation-framework)
* [Management Utility](#management-utility)
* [Topology Errors](topology-errors)

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
  "parser.error.topic": "indexing"
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
 -f,--force                                Force operation
 -h,--help                                 Generate Help screen
 -i,--input_dir <DIR>                      The input directory containing
                                           the configuration files named
                                           like "$source.json"
 -m,--mode <MODE>                          The mode of operation: DUMP,
                                           PULL, PUSH
 -o,--output_dir <DIR>                     The output directory which will
                                           store the JSON configuration
                                           from Zookeeper
 -z,--zk_quorum <host:port,[host:port]*>   Zookeeper Quorum URL
                                           (zk1:port,zk2:port,...)
```

Usage examples:

* To dump the existing configs from zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m DUMP`
* To push the configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper`
* To pull the configs from zookeeper to the singlenode vagrant machine disk: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PULL -o $METRON_HOME/config/zookeeper -f`

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
