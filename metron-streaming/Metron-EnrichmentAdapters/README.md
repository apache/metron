#Metron-Enrichments

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
