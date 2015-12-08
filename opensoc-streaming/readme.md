#Current Build

The latest build of OpenSOC-Streaming is 0.3BETA.  We are still in the process of merging/porting additional
features from our production code base into this open source release.  This release will be followed by
a number of additional beta releases until the port is complete.  We will also work on getting additional 
documentation and user/developer guides to the community as soon as we can.  At this time we offer no support
for the beta software, but will try to respond to requests as promptly as we can.

# OpenSOC-Streaming

Extensible set of Storm topologies and topology attributes for streaming, enriching, indexing, and storing telemetry in Hadoop.  General information on OpenSOC is available at www.getopensoc.com

For OpenSOC FAQ please read the following wiki entry:  https://github.com/OpenSOC/opensoc-streaming/wiki/OpenSOC-FAQ


# Usage Instructions

## Message Parser Bolt

Bolt for parsing telemetry messages into a JSON format

```
TelemetryParserBolt parser_bolt = new TelemetryParserBolt()
				.withMessageParser(new BasicSourcefireParser())
				.withOutputFieldName(topology_name);
```
				
###Parameters:

MesageParser: parsers a raw message to JSON. Parsers listed below are available
- BasicSourcefireParser: will parse a Sourcefire message to JSON
- BasicBroParser: will parse a Bro message to JSON

OutputFieldName: name of the output field emitted by the bolt

## Telemetry Indexing Bolt

Bolt for indexing JSON telemetry messages in ElasticSearch or Solr

```
TelemetryIndexingBolt indexing_bolt = new TelemetryIndexingBolt()
				.withIndexIP(ElasticSearchIP).withIndexPort(elasticSearchPort)
				.withClusterName(ElasticSearchClusterName)
				.withIndexName(ElasticSearchIndexName)
				.withDocumentName(ElasticSearchDocumentName).withBulk(bulk)
				.withOutputFieldName(topology_name)
				.withIndexAdapter(new ESBaseBulkAdapter());
```

###Parameters:

IndexAdapter: adapter and strategy for indexing.  Adapters listed below are available
- ESBaseBulkAdapter: adapter for bulk loading telemetry into a single index in ElasticSearch
- ESBulkRotatingAdapter: adapter for bulk loading telemetry into Elastic search, rotating once per hour, and applying a single alias to all rotated indexes
- SolrAdapter (stubbed out, on roadmap)

OutputFieldName: name of the output field emitted by the bolt

IndexIP: IP of ElasticSearch/Solr

IndexPort: Port of ElasticSearch/Solr

ClusterName: ClusterName of ElasticSearch/Solr

IndexName: IndexName of ElasticSearch/Solr

DocumentName: DocumentName of ElasticSearch/Solr

Bulk: number of documents to bulk load into ElasticSearch/Solr.  If no value is passed, default is 10

## Enrichment Bolt

This bolt is for enriching telemetry messages with additional metadata from external data sources.  At the time of the release the data sources supported are GeoIP (MaxMind GeoLite), WhoisDomain, Collective Intelligence Framework (CIF), and Lancope. In order to use the bolt the data sources have to be setup and data has to be bulk-loaded into them.  The information on bulk-loading data sources and making them interoperable with the enrichment bolt is provided in the following wiki entries:

- GeoIP:  https://github.com/OpenSOC/opensoc-streaming/wiki/Setting-up-GeoLite-Data
- WhoisDomain: https://github.com/OpenSOC/opensoc-streaming/wiki/Setting-up-Whois-Data
- CIF Feeds: https://github.com/OpenSOC/opensoc-streaming/wiki/Setting-up-CIF-Data
- Lancope Metadata: https://github.com/OpenSOC/opensoc-streaming/wiki/Setting-up-Lancope-data
 
```
Map<String, Pattern> patterns = new HashMap<String, Pattern>();
		patterns.put("originator_ip_regex", Pattern.compile("ip_src_addr\":\"(.*?)\""));
		patterns.put("responder_ip_regex", Pattern.compile("ip_dst_addr\":\"(.*?)\""));

GeoMysqlAdapter geo_adapter = new GeoMysqlAdapter("IP", 0, "test", "test");

GenericEnrichmentBolt geo_enrichment = new GenericEnrichmentBolt()
				.withEnrichmentTag(geo_enrichment_tag)
				.withOutputFieldName(topology_name).withAdapter(geo_adapter)
				.withMaxTimeRetain(MAX_TIME_RETAIN)
				.withMaxCacheSize(MAX_CACHE_SIZE).withPatterns(patterns);
```

###Parameters:

GeoAdapter: adapter for the MaxMind GeoLite dataset.  Adapters listed below are available
- GeoMysqlAdapter: pulls geoIP data from MqSQL database
- GeoPosgreSQLAdapter: pulls geoIP data from Posgress database (on road map, not yet available)

WhoisAdapter: adapter for whois database.  Adapters listed below are available
- WhoisHBaseAdapter: adapter for HBase

CIFAdapter: Hortonworks to document

LancopeAdapter: Hortonworks to document

originator_ip_regex: regex to extract the source ip form message

responder_ip_regex: regex to extract dest ip from message
The single bolt is currently undergoing testing and will be uploaded shortly

geo_enrichment_tag: JSON field indicating how to tag the original message with the enrichment... {original_message:some_message, {geo_enrichment_tag:{from:xxx},{to:xxx}}}

MAX_TIME_RETAIN: this bolt utilizes in-memory cache. this variable (in minutes) indicates now long to retain each entry in the cache

MAX_CACHE_SIZE: this value defines the maximum size of the cache after which entries are evicted from cache

OutputFieldName: name of the output field emitted by the bolt


## Internal Test Spout

We provide a capability to test a topology with messages stored in a file and packaged in a jar that is sent to storm.  This functionality is exposed through a special spout that is able to replay test messages into a topology.

```
GenericInternalTestSpout test_spout = new GenericInternalTestSpout()
				.withFilename("sourcefire_enriched").withRepeating(false)
				.withMilisecondDelay(100);
```

###Parameters

Filename: name of a file in a jar you want to replay

Repeating: do you want to repeatedly play messages or stop after all the messages in the file have been read

WithMilisecondDelay: the amount of the delay (sleep) between replayed messages
