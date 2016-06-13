#Query Language

For a variety of components (currently only threat intelligence triage) we have the need to determine if a condition is true of the JSON documents being enriched.  For those purposes, there exists a simple DSL created to define those conditions.

The query language supports the following:
* Referencing fields in the enriched JSON
* Simple boolean operations: and, not, or
* Determining whether a field exists (via `exists`)
* The ability to have parenthesis to make order of operations explicit
* A fixed set of functions which take strings and return boolean.  Currently:
    * `IN_SUBNET(ip, cidr1, cidr2, ...)`
    * `IS_EMPTY(str)`
    * `STARTS_WITH(str, prefix)`
    * `ENDS_WITH(str, suffix)`
    * `REGEXP_MATCH(str, pattern)`
* A fixed set of string to string transformation functions:
    * `TO_LOWER`
    * `TO_UPPER`
    * `TRIM`
    * `IS_IP` : Validates that the input fields are an IP address.  By default, if no second arg is set, it assumes `IPV4`, but you can specify the type by passing in either `IPV6` or `IPV4` to the second argument.
   * `IS_DOMAIN` 
   * `IS_EMAIL`
   * `IS_URL`
   * `IS_DATE`
   * `IS_INTEGER`


Example query:

`IN_SUBNET( ip, '192.168.0.0/24') or ip in [ '10.0.0.1', '10.0.0.2' ] or exists(is_local)`

This evaluates to true precisely when one of the following is true:
* The value of the `ip` field is in the `192.168.0.0/24` subnet
* The value of the `ip` field is `10.0.0.1` or `10.0.0.2`
* The field `is_local` exists

#Enrichment Configuration

The configuration for the `enrichment` topology, the topology primarily
responsible for enrichment and threat intelligence enrichment, is
defined by JSON documents stored in zookeeper.

There are two types of configurations at the moment, `global` and
`sensor` specific.  

##Global Configuration
The format of the global enrichment is a JSON String to Object map.  This is intended for
configuration which is non sensor specific configuration.

This configuration is stored in zookeeper, but looks something like

```json
{
  "es.clustername": "metron",
  "es.ip": "node1",
  "es.port": "9300",
  "es.date.format": "yyyy.MM.dd.HH",
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

###Validation Framework

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
   * `MQL` : Execute a Query Language statement.  Expects the query string in the `condition` field of the config.
   * `IP` : Validates that the input fields are an IP address.  By default, if no configuration is set, it assumes `IPV4`, but you can specify the type by passing in the config by passing in `type` with either `IPV6` or `IPV4`.
   * `DOMAIN` : Validates that the fields are all domains.
   * `EMAIL` : Validates that the fields are all email addresses
   * `URL` : Validates that the fields are all URLs
   * `DATE` : Validates that the fields are a date.  Expects `format` in the config.
   * `INTEGER` : Validates that the fields are an integer.  String representation of an integer is allowed.
   * `REGEX_MATCH` : Validates that the fields match a regex.  Expects `pattern` in the config.
   * `NOT_EMPTY` : Validates that the fields exist and are not empty (after trimming.)

##Sensor Enrichment Configuration

The sensor specific configuration is intended to configure the
individual enrichments and threat intelligence enrichments for a given
sensor type (e.g. `snort`).

Just like the global config, the format is a JSON stored in zookeeper.
The configuration is a complex JSON object with the following top level fields:

* `index` : The name of the sensor
* `batchSize` : The size of the batch that is written to the indices at once.
* `enrichment` : A complex JSON object representing the configuration of the enrichments
* `threatIntel` : A complex JSON object representing the configuration of the threat intelligence enrichments

###The `enrichment` Configuration 


| Field            | Description                                                                                                                                                                                                                   | Example                                                          |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. | `"fieldToTypeMap" : { "ip_src_addr" : [ "asset_enrichment" ] }`  |
| `fieldMap`       | The map of enrichment bolts names to fields in the JSON messages.,Each field is sent to the enrichment referenced in the key.                                                                                                 | `"fieldMap": {"hbaseEnrichment": ["ip_src_addr","ip_dst_addr"]}` |
| `config`         | The general configuration for the enrichment                                                                                                                                                                                  | `"config": {"typeToColumnFamily": { "asset_enrichment","cf" } }` |

The `config` map is intended to house enrichment specific configuration.
For instance, for the `hbaseEnrichment`, the mappings between the
enrichment types to the column families is specified.

###The `threatIntel` Configuration 

| Field            | Description                                                                                                                                                                                                                                   | Example                                                                  |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase threat intel enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. | `"fieldToTypeMap" : { "ip_src_addr" : [ "malicious_ips" ] }`             |
| `fieldMap`       | The map of threat intel enrichment bolts names to fields in the JSON messages. Each field is sent to the threat intel enrichment bolt referenced in the key.                                                                              | `"fieldMap": {"hbaseThreatIntel": ["ip_src_addr","ip_dst_addr"]}`        |
| `triageConfig`   | The configuration of the threat triage scorer.  In the situation where a threat is detected, a score is assigned to the message and embedded in the indexed message.                                                                    | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `config`         | The general configuration for the Threat Intel                                                                                                                                                                                                | `"config": {"typeToColumnFamily": { "malicious_ips","cf" } }`            |

The `config` map is intended to house threat intel specific configuration.
For instance, for the `hbaseThreatIntel` threat intel adapter, the mappings between the
enrichment types to the column families is specified.

The `triageConfig` field is also a complex field and it bears some description:

| Field            | Description                                                                                                                                                | Example                                                                  |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `riskLevelRules` | The mapping of Metron Query Language (see above) queries to a score.                                                                                       | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `aggregator`     | An aggregation function that takes all non-zero scores representing the matching queries from `riskLevelRules` and aggregates them into a single score. | `"MAX"`                                                                  |

The supported aggregation functions are:
* `MAX` : The max of all of the associated values for matching queries
* `MIN` : The min of all of the associated values for matching queries
* `MEAN` : The mean of all of the associated values for matching queries
* `POSITIVE_MEAN` : The mean of the positive associated values for the matching queries.

###Example

An example configuration for the YAF sensor is as follows:
```json
{
  "index": "yaf",
  "batchSize": 5,
  "enrichment": {
    "fieldMap": {
      "geo": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "host": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "hbaseEnrichment": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    }
  ,"fieldToTypeMap": {
      "ip_src_addr": [
        "playful_classification"
      ],
      "ip_dst_addr": [
        "playful_classification"
      ]
    }
  },
  "threatIntel": {
    "fieldMap": {
      "hbaseThreatIntel": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    },
    "fieldToTypeMap": {
      "ip_src_addr": [
        "malicious_ip"
      ],
      "ip_dst_addr": [
        "malicious_ip"
      ]
    },
    "triageConfig" : {
      "riskLevelRules" : {
        "ip_src_addr == '10.0.2.3' or ip_dst_addr == '10.0.2.3'" : 10
      },
      "aggregator" : "MAX"
    }
  }
}
```


##Management Utility

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
