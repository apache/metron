#Enrichment

## Introduction

The `enrichment` topology is a topology dedicated to taking the data
from the parsing topologies that have been normalized into the Metron
data format (e.g. a JSON Map structure with `original_message` and
`timestamp`) and 
* Enriching messages with external data from data stores (e.g. hbase) by
  adding new fields based on existing fields in the messages.
* Marking messages as threats based on data in external data stores
* Marking threat alerts with a numeric triage level based on a set of
  Stellar rules.

## Enrichment Configuration

The configuration for the `enrichment` topology, the topology primarily
responsible for enrichment and threat intelligence enrichment, is
defined by JSON documents stored in zookeeper.

There are two types of configurations at the moment, `global` and
`sensor` specific.  

## Global Configuration 

See the "[Global Configuration](../metron-common)" section.

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
| `fieldMap`       | The map of enrichment bolts names to configuration handlers which know how to split the message up.  The simplest of which is just a list of fields.  More complex examples would be the stellar enrichment which provides stellar statements.  Each field is sent to the enrichment referenced in the key.                                                                                                 | `"fieldMap": {"hbaseEnrichment": ["ip_src_addr","ip_dst_addr"]}` |
| `config`         | The general configuration for the enrichment                                                                                                                                                                                  | `"config": {"typeToColumnFamily": { "asset_enrichment" : "cf" } }` |

The `config` map is intended to house enrichment specific configuration.
For instance, for the `hbaseEnrichment`, the mappings between the
enrichment types to the column families is specified.

The `fieldMap`contents are of interest because they contain the routing and configuration information for the enrichments.  When we say 'routing', we mean how the messages get split up and sent to the enrichment adapter bolts.  The simplest, by far, is just providing a simple list as in
```
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
```
For the `geo`, `host` and `hbaseEnrichment`, this is sufficient.  However, more complex enrichments may contain their own configuration.  Currently, the `stellar` enrichment requires a more complex configuration, such as:
```
    "fieldMap": {
       ...
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      }
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
        }
      }
    }
```

Whereas the simpler enrichments just need a set of fields explicitly stated so they can be separated from the message and sent to the enrichment adapter bolt for enrichment and ultimately joined back in the join bolt, the stellar enrichment has its set of required fields implicitly stated through usage.  For instance, if your stellar statement references a field, it should be included and if not, then it should not be included.  We did not want to require users to make explicit the implicit.

The other way in which the stellar enrichment is somewhat more complex is in how the statements are executed.  In the general purpose case for a list of fields, those fields are used to create a message to send to the enrichment adapter bolt and that bolt's worker will handle the fields one by one in serial for a given message.  For stellar enrichment, we wanted to have a more complex design so that users could specify the groups of stellar statements sent to the same worker in the same message (and thus executed sequentially).  Consider the following configuration:
```
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      "bar" : TO_LOWER(source.type)"
                      }
         ,"text" : {
                   "ALL_CAPS" : "TO_UPPER(source.type)"
                   }
        }
      }
    }
```
We have a group called `numeric` whose stellar statements will be executed sequentially.  In parallel to that, we have the group of stellar statements under the group `text` executing.  The intent here is to allow you to not force higher latency operations to be done sequentially.

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

| Field            | Description                                                                                                                                             | Example                                                                  |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `riskLevelRules` | The mapping of Stellar (see above) queries to a score.                                                                                                  | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
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



##Module Description

This module enables enrichment of message metafields with additional information from various enrichment sources.  Currently there is only a limited number of enrichments available, but this is an extensible framework that can be extended with additional enrichments.  Enrichments currently available are geo, whois, hosts, and CIF.

##Message Format

Enrichment bolts are designed to go after the parser bolts.  Parser bolts will parse the telemetry, taking it from its native format and producing a standard JSON that would look like so:

```json
{
"message":
{"ip_src_addr": xxxx,
"ip_dst_addr": xxxx,
"ip_src_port": xxxx,
"ip_dst_port": xxxx,
"protocol": xxxx,
"additional-field 1": xxx,
}

}
```

A single enrichment bolt would enrich the message and produce a JSON enrichment and attach it to the message.  Enrichments are stackable so multiple enrichments can be attached sequentially after a single parser bolt.  Stacked enrichments would produce messages under the "enrichment" tag and attach it to the message like so:

```json
{
"message":
{"ip_src_addr": xxxx,
"ip_dst_addr": xxxx,
"ip_src_port": xxxx,
"ip_dst_port": xxxx,
"protocol": xxxx,
"additional-field 1": xxxx,
},
"enrichment" : {"geo": xxxx, "whois": xxxx, "hosts": xxxxx, "CIF": "xxxxx"}

}
```

##Enrichment Sources

Each enrichment has to have an anrichment source which can serve as a lookup table for enriching relevant message fields.  In order to minimize the use of additional platforms and tools we primarily try to rely on HBase as much as possible to store the enrichment information for lookup by key.  In order to use Hbase we have to pre-process the enrichment feeds for bulk-loading into HBase with specific key format optimized for retrieval as well as utilize caches within the enrichment bolts to be able to provide enrichments real-time.  Our wiki contains information on how to setup the environment, pre-process feeds, and plug in the enrichment sources.

##Enrichment Bolt

The enrichment bolt is designed to be extensible to be re-used for all kinds of enrichment processes.  The bolt signature for declaration in a storm topology is as follows:



```
GenericEnrichmentBolt geo_enrichment = new GenericEnrichmentBolt()
.withEnrichmentTag(
config.getString("bolt.enrichment.geo.enrichment_tag"))
.withAdapter(geo_adapter)
.withMaxTimeRetain(
config.getInt("bolt.enrichment.geo.MAX_TIME_RETAIN_MINUTES"))
.withMaxCacheSize(
config.getInt("bolt.enrichment.geo.MAX_CACHE_SIZE_OBJECTS_NUM"))
.withKeys(geo_keys).withMetricConfiguration(config);

```

EnrichmentTag - Name of the enrichment (geo, whois, hosts, etc)
Keys - Keys which this enrichment is able to enrich (hosts field for hosts enrichment, source_ip, dest_ip, for geo enrichment, etc)
MaxTimeToRetain & MaxCacheSize - define the caching policy of the enrichment bolt
Adapter - which adapter to use with the enrichment bolt instance

###Geo Adapter
Geo adapter is able to do geo enrichment on hosts and destination IPs.  The open source verison of the geo adapter uses the free Geo feeds from MaxMind.  The format of these feeds does not easily lend itself to a no-sql DB so this adapter is designed to work with mySql.  But it is extensible enough to be made work with a variety of other back ends.

The signature of a geo adapter is as follows;

```
GeoMysqlAdapter geo_adapter = new GeoMysqlAdapter(
config.getString("mysql.ip"), config.getInt("mysql.port"),
config.getString("mysql.username"),
config.getString("mysql.password"),
config.getString("bolt.enrichment.geo.adapter.table"));

```

###Hosts Adapter
The hosts adapter is designed to enrich message format with the static host information that can be read from a standard text file.  This adapter is intended for use with a network crawling script that can identify all customer assets and place them in a text file.  For example, this script would identify all workstations, printers, appliantces, etc.  Then if any of these assets are seen in the telemetry messages flowing through the adapter this enrichment would fire and the relevant known information about a host would be attached.  We are currently working on porting this adapter to work with HBase, but this work is not ready yet.  The known hosts file is located under the /etc/whitelists config directory of Metron.

The signature of the hosts adapter is as follows:

```
Map<String, JSONObject> known_hosts = SettingsLoader
.loadKnownHosts(hosts_path);

HostFromPropertiesFileAdapter host_adapter = new HostFromPropertiesFileAdapter(
known_hosts);

```
* The source and dest ips refer to the name of the message JSON key where the host information is located

###Whois Adapter
Whois adapter enriches the host name with additional whois information obtained from our proprietary Cisco feed.  The enricher itself is provided in this open source distribution, but the feed is not.  You have to have your own feed in order to use it.  Alternatively, you can contact us for providing you with this feed, but we would have to charge you a fee (we can't distribute it for free). The implemetation of the whois enrichment we provide works with HBase

The signature of the whois adapter is as follows:

```

EnrichmentAdapter whois_adapter = new WhoisHBaseAdapter(
config.getString("bolt.enrichment.whois.hbase.table.name"),
config.getString("kafka.zk.list"),
config.getString("kafka.zk.port"));
```

###CIF Adapter
CIF adapter is designed to take in CIF feeds and cross-reference them against every message processed by Storm.  If there is a hit then the relevant information is attached to the message.  

The signature of the CIF adapter is as follows:

```
CIFHbaseAdapter = new CIFHbaseAdapter(config
.getString("kafka.zk.list"), config
.getString("kafka.zk.port"), config
.getString("bolt.enrichment.cif.tablename")))
```

##Stacking Enrichments
Enrichments can be stacked.  By default each enrichment bolt listens on the "message" stream.  In order to create and stack enrichment bolts create a new bolt and instantiate the appropariate adapter.  You can look at our sample topologies to see how enrichments can be stacked
