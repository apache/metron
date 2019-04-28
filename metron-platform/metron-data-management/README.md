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
# Resource Data Management

## Table of Contents

* [Overview](#overview)
* [Simple HBase Enrichments/Threat Intelligence](#simple-hbase-enrichmentsthreat-intelligence)
* [Extractor Framework](#extractor-framework)
* [Enrichment Config](#enrichment-config)
* [Loading Utilities](#loading-utilities)
* [Pruning Data from Elasticsearch](#pruning-data-from-elasticsearch)

## Overview

This project is a collection of classes to assist with loading of
various enrichment and threat intelligence sources into Metron.

## Simple HBase Enrichments/Threat Intelligence

The vast majority of enrichments and threat intelligence processing tend
toward the following pattern:
* Take a field
* Look up the field in a key/value store
* If the key exists, then either it's a threat to be alerted or it should be enriched with the value associated with the key.

As such, we have created this capability as a default threat intel and enrichment adapter.  The basic primitive for simple enrichments and threat intelligence sources
is a complex key containing the following:
* Type : The type of threat intel or enrichment (e.g. malicious_ip)
* Indicator : The indicator in question
* Value : The value to associate with the type, indicator pair.  This is a JSON map.

At present, all of the dataloads utilities function by converting raw data
sources to this primitive key (type, indicator) and value to be placed in HBase.

In the case of threat intel, a hit on the threat intel table will result
in:
* The `is_alert` field being set to `true` in the index
* A field named `threatintels.hbaseThreatIntel.$field.$threatintel_type` is set to `alert` 
   * `$field` is the field in the original document that was a match (e.g. `src_ip_addr`) 
   * `$threatintel_type` is the type of threat intel imported (defined in the Extractor configuration below).

In the case of simple hbase enrichment, a hit on the enrichments table
will result in the following new field for each key in the value:`enrichments.hbaseEnrichment.$field.$enrichment_type.$key` 
* `$field` is the field in the original document that was a match (e.g.  `src_ip_addr`)
* `$enrichment_type` is the type of enrichment imported (defined in the Extractor configuration below).
* `$key` is a key in the JSON map associated with the row in HBase.

For instance, in the situation where we had the following very silly key/value in
HBase in the enrichment table:
* indicator: `127.0.0.1`
* type : `important_addresses`
* value: `{ "name" : "localhost", "location" : "home" }`

If we had a document whose `ip_src_addr` came through with a value of
`127.0.0.1`, we would have the following fields added to the indexed
document:
* `enrichments.hbaseEnrichment.ip_src_addr.important_addresses.name` : `localhost`
* `enrichments.hbaseEnrichment.ip_src_addr.important_addresses.location` : `home`

## Extractor Framework

For the purpose of ingesting data of a variety of formats, we have
created an Extractor framework which allows for common data formats to
be interpreted as enrichment or threat intelligence sources.  The
formats supported at present are:
* CSV (both threat intel and enrichment)
* STIX (threat intel only)
* Custom (pass your own class)

All of the current utilities take a JSON file to configure how to
interpret input data.  This JSON describes the type of data and the
schema if necessary for the data if it is not fixed (as in STIX, e.g.).

### CSV Extractor

Consider the following example configuration file which
describes how to process a CSV file.

```
{
  "config" : {
    "columns" : {
         "ip" : 0
        ,"source" : 2
    }
    ,"indicator_column" : "ip"
    ,"type" : "malicious_ip"
    ,"separator" : ","
  }
  ,"extractor" : "CSV"
}
```

In this example, we have instructed the extractor of the schema (i.e. the columns field), 
two columns at the first and third position.  We have indicated that the `ip` column is the indicator type
and that the enrichment type is named `malicious_ip`.  We have also indicated that the extractor to use is the CSV Extractor.
The other option is the STIX extractor or a fully qualified classname for your own extractor.

The meta column values will show up in the value in HBase because it is called out as a non-indicator column.  The key
for the value will be 'meta'.  For instance, given an input string of `123.45.123.12,something,the grapevine`, the following key, value
would be extracted:
* Indicator : `123.45.123.12`
* Type : `malicious_ip`
* Value : `{ "ip" : "123.45.123.12", "source" : "the grapevine" }`

### STIX Extractor

Consider the following config for importing STIX documents.  This is a threat intelligence interchange
format, so it is particularly relevant and attractive data to import for our purposes.  Because STIX is
a standard format, there is no need to specify the schema or how to interpret the documents.

We support the versions of Stix and Cybox supported by
[java-stix](https://github.com/STIXProject/java-stix/tree/v1.2.0.2):
* Stix - [1.2](https://github.com/STIXProject/schemas/blob/356cc4f6b06625465f0808388eb166807313b4e0/stix_core.xsd) and earlier
* Cybox - [2.1](https://github.com/CybOXProject/schemas/blob/97beb32c376a9223e91b52cb3e4c8d2af6baf786/cybox_core.xsd) and earlier

We support a subset of STIX messages for importation:

| STIX Type | Specific Type | Enrichment Type Name |
|-----------|---------------|----------------------|
| Address   | IPV_4_ADDR    | address:IPV_4_ADDR   |
| Address   | IPV_6_ADDR    | address:IPV_6_ADDR   |
| Address   | E_MAIL        | address:E_MAIL       |
| Address   | MAC           | address:MAC          |
| Domain    | FQDN          | domain:FQDN          |
| Hostname  |               | hostname             |
| URI       |               | uriobjecttype        |


NOTE: The enrichment type will be used as the type above.

Consider the following configuration for an Extractor

```
{
  "config" : {
    "stix_address_categories" : "IPV_4_ADDR"
  }
  ,"extractor" : "STIX"
}
```

In here, we're configuring the STIX extractor to load from a series of STIX files, however we only want to bring in IPv4
addresses from the set of all possible addresses.  Note that if no categories are specified for import, all are assumed.
Also, only address and domain types allow filtering via `stix_address_categories` and `stix_domain_categories` config
parameters.

### Common Extractor Properties

Users also have the ability to transform and filter enrichment and threat intel data using Stellar as it is loaded into HBase. This feature is available to all extractor types.

As an example, we will be providing a CSV list of top domains as an enrichment and filtering the value metadata, as well as the indicator column, with Stellar expressions.

```
{
  "config" : {
    "zk_quorum" : "node1:2181",
    "columns" : {
       "rank" : 0,
       "domain" : 1
    },
    "value_transform" : {
       "domain" : "DOMAIN_REMOVE_TLD(domain)"
    },
    "value_filter" : "LENGTH(domain) > 0",
    "indicator_column" : "domain",
    "indicator_transform" : {
       "indicator" : "DOMAIN_REMOVE_TLD(indicator)"
    },
    "indicator_filter" : "LENGTH(indicator) > 0",
    "type" : "top_domains",
    "separator" : ","
  },
  "extractor" : "CSV"
}
```

There are 2 property maps that work with full Stellar expressions, and 2 properties that will work with Stellar predicates.

| Property             | Description
|----------------------|---
| `value_transform`    | Transform fields defined in the "columns" mapping with Stellar transformations. New keys introduced in the transform will be added to the key metadata.
| `value_filter`       | Allows additional filtering with Stellar predicates based on results from the value transformations. In this example, records whose domain property is empty after removing the TLD will be omitted.
| `indicator_transform`| Transform the indicator column independent of the value transformations. You can refer to the original indicator value by using "indicator" as the variable name, as shown in the example above. In addition, if you prefer to piggyback your transformations, you can refer to the variable "domain", which will allow your indicator transforms to inherit transformations done to this value during the value transformations.
| `indicator_filter`   | Allows additional filtering with Stellar predicates based on results from the value transformations. In this example, records whose indicator value is empty after removing the TLD will be omitted.
| `state_init`         | Allows a state object to be initialized.  This is a string, so a single expression is created.  The output of this expression will be available as the `state` variable.  This is to be used with the `flatfile_summarizer.sh` rather than the loader.
| `state_update`       | Allows a state object to be updated.  This is a map, so you can have temporary variables here.  Note that you can reference the `state` variable from this.  This is to be used with the `flatfile_summarizer.sh` rather than the loader.
| `state_merge`        | Allows a list of states to be merged. This is a string, so a single expression.  There is a special field called `states` available, which is a list of the states (one per thread).  This is to be used with the `flatfile_summarizer.sh` rather than the loader.

top-list.csv
```
1,google.com
2,youtube.com
...
```

Running a file import with the above data and extractor configuration would result in the following 2 extracted data records:

| Indicator | Type        | Value                                 |
|-----------|-------------|---------------------------------------|
| google    | top_domains | { "rank" : "1", "domain" : "google" } |
| yahoo     | top_domains | { "rank" : "2", "domain" : "yahoo" }  |

Similar to the parser framework, providing a Zookeeper quorum via the zk_quorum property will enable Stellar to access properties that reside in the global config.
Expanding on our example above, if the global config looks as follows:
```
{
    "global_property" : "metron-ftw"
}
```

And we expand our value_tranform:
```
...
    "value_transform" : {
       "domain" : "DOMAIN_REMOVE_TLD(domain)",
       "a-new-prop" : "global_property"
    },
...
```

The resulting value data would look like the following:

| Indicator | Type        | Value                                                              |
|-----------|-------------|--------------------------------------------------------------------|
| google    | top_domains | { "rank" : "1", "domain" : "google", "a-new-prop" : "metron-ftw" } |
| yahoo     | top_domains | { "rank" : "2", "domain" : "yahoo", "a-new-prop" : "metron-ftw" }  |

## Enrichment Config

In order to automatically add new enrichment and threat intel types to existing, running enrichment topologies, you will
need to add new fields and new types to the zookeeper configuration.  A convenience parameter has been made to assist in this
when doing an import.  Namely, you can specify the enrichment configs and how they associate with the fields of the 
documents flowing through the enrichment topology.

Consider the following Enrichment Configuration JSON.  This one is for a threat intelligence type:

```
{
  "zkQuorum" : "localhost:2181"
 ,"sensorToFieldList" : {
    "bro" : {
           "type" : "THREAT_INTEL"
          ,"fieldToEnrichmentTypes" : {
             "ip_src_addr" : [ "malicious_ip" ]
            ,"ip_dst_addr" : [ "malicious_ip" ]
                                      }
           }
                        }
}
```

We have to specify the following:
* The zookeeper quorum which holds the cluster configuration
* The mapping between the fields in the enriched documents and the enrichment types.

This configuration allows the ingestion tools to update zookeeper post-ingestion so that the enrichment topology can take advantage
immediately of the new type.


## Loading Utilities

The two configurations above are used in the three separate ingestion tools:
* Taxii Loader
* Bulk load from HDFS via MapReduce
* Flat File ingestion

### Taxii Loader

The shell script `$METRON_HOME/bin/threatintel_taxii_load.sh` can be used to poll a Taxii server for STIX documents and ingest them into HBase.  
It is quite common for this Taxii server to be an aggregation server such as Soltra Edge.

In addition to the Enrichment and Extractor configs described above, this loader requires a configuration file describing the connection information
to the Taxii server.  An illustrative example of such a configuration file is:

```
{
   "endpoint" : "http://localhost:8282/taxii-discovery-service"
  ,"type" : "DISCOVER"
  ,"collection" : "guest.Abuse_ch"
  ,"table" : "threat_intel"
  ,"columnFamily" : "cf"
  ,"allowedIndicatorTypes" : [ "domainname:FQDN", "address:IPV_4_ADDR" ]
}
```

As you can see, we are specifying the following information:
* endpoint : The URL of the endpoint
* type : `POLL` or `DISCOVER` depending on the endpoint.
* collection : The Taxii collection to ingest
* table : The HBase table to import into
* columnFamily : The column family to import into
* allowedIndicatorTypes : an array of acceptable threat intel types (see the "Enrichment Type Name" column of the Stix table above for the possibilities).

The parameters for the utility are as follows:

| Short Code | Long Code                 | Is Required? | Description                                                                                                                                        |
|------------|---------------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| -h         |                           | No           | Generate the help screen/set of options                                                                                                            |
| -e         | --extractor_config        | Yes          | JSON Document describing the extractor for this input data source                                                                                  |
| -c         | --taxii_connection_config | Yes          | The JSON config file to configure the connection                                                                                                   |
| -p         | --time_between_polls      | No           | The time between polling the Taxii server in milliseconds. (default: 1 hour)                                                                       |
| -b         | --begin_time              | No           | Start time to poll the Taxii server (all data from that point will be gathered in the first pull).  The format for the date is yyyy-MM-dd HH:mm:ss |
| -l         | --log4j                   | No           | The Log4j Properties to load                                                                                                                       |
| -n         | --enrichment_config       | No           | The JSON document describing the enrichments to configure.  Unlike other loaders, this is run first if specified.                                  |



### Flatfile Loader

The shell script `$METRON_HOME/bin/flatfile_loader.sh` will read data from local disk, HDFS or URLs and load the enrichment or threat intel data into an HBase table.  
Note: This utility works for enrichment as well as threat intel due to the underlying infrastructure being the same.

One special thing to note here is that there is a special configuration
parameter to the Extractor config that is only considered during this
loader:
* inputFormat : This specifies how to consider the data.  The two implementations are `BY_LINE` and `WHOLE_FILE`.

The default is `BY_LINE`, which makes sense for a list of CSVs where
each line indicates a unit of information which can be imported.
However, if you are importing a set of STIX documents, then you want
each document to be considered as input to the Extractor.

The parameters for the utility are as follows:

| Short Code | Long Code           | Is Required? | Description                                                                                                                                                                         |
|------------|---------------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| -h         |                     | No           | Generate the help screen/set of options                                                                                                                                             |
| -q         | --quiet             | No           | Do not update progress                                                                                                                                                              |
| -e         | --extractor_config  | Yes          | JSON Document describing the extractor for this input data source                                                                                                                   |
| -m         | --import_mode       | No           | The Import mode to use: LOCAL, MR.  Default: LOCAL                                                                                                                                  |
| -t         | --hbase_table       | Yes          | The HBase table to import into                                                                                                                                                      |
| -c         | --hbase_cf          | Yes          | The HBase table column family to import into                                                                                                                                        |
| -i         | --input             | Yes          | The input data location on local disk.  If this is a file, then that file will be loaded.  If this is a directory, then the files will be loaded recursively under that directory.  |
| -l         | --log4j             | No           | The log4j properties file to load                                                                                                                                                   |
| -n         | --enrichment_config | No           | The JSON document describing the enrichments to configure.  Unlike other loaders, this is run first if specified.                                                                   |
| -p         | --threads           | No           | The number of threads to use when extracting data.  The default is the number of cores.                                                                                             |
| -b         | --batchSize         | No           | The batch size to use for HBase puts                                                                                                                                                |

### GeoLite2 Loader

The shell script `$METRON_HOME/bin/maxmind_enrichment_load.sh` will retrieve MaxMind GeoLite2 data and load data into HDFS, and update the configuration.
The script will retrieve both the City and ASN databases.

THIS SCRIPT WILL NOT UPDATE AMBARI'S GLOBAL.JSON, JUST THE ZK CONFIGS.  CHANGES WILL GO INTO EFFECT, BUT WILL NOT PERSIST PAST AN AMBARI RESTART UNTIL UPDATED THERE.

The parameters for the utility are as follows:

| Short Code | Long Code           | Is Required? | Description                                                                                         |
|------------|---------------------|--------------|-----------------------------------------------------------------------------------------------------|
| -h         |                     | No           | Generate the help screen/set of options                                                             |
| -g         | --geo_url           | No           | GeoIP URL - defaults to http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.tar.gz     |
| -r         | --remote_dir        | No           | HDFS directory to land formatted GeoIP City file - defaults to /apps/metron/geo/\<epoch millis\>/   |
| -ra        | --remote_asn_dir    | No           | HDFS directory to land formatted GeoIP ASN file - defaults to /apps/metron/asn/\<epoch millis\>/    |
| -re        | --retries           | No           | Number of GeoLite2 database download retries, after an initial failure.                             |
| -t         | --tmp_dir           | No           | Directory for landing the temporary GeoIP data - defaults to /tmp                                   |
| -z         | --zk_quorum         | Yes          | Zookeeper Quorum URL (zk1:port,zk2:port,...)                                                        |

### Flatfile Summarizer

The shell script `$METRON_HOME/bin/flatfile_summarizer.sh` will read data from local disk, HDFS or URLs and generate a summary object.
The object will be serialized and written to disk, either HDFS or local disk depending on the output mode specified.

It should be noted that this utility uses the same extractor config as the `flatfile_loader.sh`,
but as the output target is not a key value store (but rather a summary object), it is not necessary
to specify certain configs:
* `indicator`, `indicator_filter` and `indicator_transform` are not required, but will be executed if present.
As in the loader, there will be an indicator field available if you so specify it (by using `indicator` in the config).
* `type` is neither required nor used

Indeed, some new configs are expected:
* `state_init` : Executed once to initialize the state object (the object written out).
* `state_update`: Called once per message.  The fields available are the fields for the row as well as
  * `indicator` - the indicator value if you've specified it in the config
  * `state` - the current state.  Useful for adding to the state (e.g. `BLOOM_ADD(state, val)` where `val` is the name of a field).
* `state_merge` : If you are running this multi-threaded and your objects can be merged, this is the statement that will
merge the state objects created per thread.  There is a special field available to this config:
  * `states` - a list of the state objects

One special thing to note here is that there is a special configuration
parameter to the Extractor config that is only considered during this
loader:
* inputFormat : This specifies how to consider the data.  The two implementations are `BY_LINE` and `WHOLE_FILE`.

The default is `BY_LINE`, which makes sense for a list of CSVs where
each line indicates a unit of information which can be imported.
However, if you are importing a set of STIX documents, then you want
each document to be considered as input to the Extractor.

#### Example

Consider the possibility that you want to generate a bloom filter with all of the domains in a CSV structured similarly to
the Alexa top 1M domains, so the columns are:
* rank
* domain name

You want to generate a bloom filter with just the domains, not considering the TLD.
You would execute the following to:
* read data from `./top-1m.csv`
* write data to `./filter.ser`
* use 5 threads

```
$METRON_HOME/bin/flatfile_summarizer.sh -i ./top-1m.csv -o ./filter.ser -e ./extractor.json -p 5 -b 128
```

To configure this, `extractor.json` would look like:
```
{
  "config" : {
    "columns" : {
      "rank" : 0,
      "domain" : 1
    },
    "value_transform" : {
      "domain" : "DOMAIN_REMOVE_TLD(domain)"
    },
    "value_filter" : "LENGTH(domain) > 0",
    "state_init" : "BLOOM_INIT()",
    "state_update" : {
      "state" : "BLOOM_ADD(state, domain)"
    },
    "state_merge" : "BLOOM_MERGE(states)",
    "separator" : ","
  },
  "extractor" : "CSV"
}
```

#### Parameters

The parameters for the utility are as follows:

| Short Code | Long Code           | Is Required? | Description                                                                                                                                                                         |
|------------|---------------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| -h         |                     | No           | Generate the help screen/set of options                                                                                                                                             |
| -q         | --quiet             | No           | Do not update progress                                                                                                                                                              |
| -e         | --extractor_config  | Yes          | JSON Document describing the extractor for this input data source                                                                                                                   |
| -m         | --import_mode       | No           | The Import mode to use: LOCAL, MR.  Default: LOCAL                                                                                                                                  |
| -om        | --output_mode       | No           | The Output mode to use: LOCAL, HDFS.  Default: LOCAL                                                                                                                                  |
| -i         | --input             | Yes          | The input data location on local disk.  If this is a file, then that file will be loaded.  If this is a directory, then the files will be loaded recursively under that directory.  |
| -o         | --output            | Yes          | The output data location.    |
| -l         | --log4j             | No           | The log4j properties file to load                                                                                                                                                   |
| -p         | --threads           | No           | The number of threads to use when extracting data.  The default is the number of cores.                                                                                             |
| -b         | --batchSize         | No           | The batch size to use for HBase puts                                                                                                                                                |

## Pruning Data from Elasticsearch

**Note** - As of the Metron upgrade from Elasticsearch 2.3.3 to 5.6.2, the included Data Pruner is no longer supported. It is replaced in favor of the Curator utility
provided by Elasticsearch. The current Curator version is 5.4 as of this version of Metron and does not match exactly with ES and Kibana.

Elasticsearch provides tooling to prune index data through [Curator](https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/index.html).

Here is a sample invocation that you can configure through Cron to prune indexes based on timestamp in the index name.

```
/opt/elasticsearch-curator/curator_cli --host localhost delete_indices --filter_list '
    {
      "filtertype": "age",
      "source": "name",
      "timestring": "%Y.%m.%d",
      "unit": "days",
      "unit_count": 10,
      "direction": "older”
    }'
```

From the ES documentation:
> Using name as the source tells Curator to look for a timestring within the index or snapshot name, and convert that into an epoch timestamp (epoch implies UTC).

You can also provide multiple filters as an array of JSON objects to filter_list if you want finer-grained control over the indexes that will be pruned.
There is an implicit logical AND when chaining multiple filters.

```
--filter_list '[{"filtertype":"age","source":"creation_date","direction":"older","unit":"days","unit_count":13},{"filtertype":"pattern","kind":"prefix","value":"logstash"}]'
```

### Reference
* [https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/index.html](https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/index.html)
* [https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/filtertype_age.html](https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/filtertype_age.html)
* [https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/singleton-cli.html](https://www.elastic.co/guide/en/elasticsearch/client/curator/5.4/singleton-cli.html)
