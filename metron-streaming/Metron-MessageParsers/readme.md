#Metron-Parsers

##Module Description

This module provides a list of parsers that can be used with the Metron framework.  There are two types of parsers.  First type is a Java parser.  This kind of parser is optimized for speed and performance and is built for use with higher velicity topologies.  These parsers are not easily modifiable and in order to make changes to them the entire topology need to be recompiled.  The second type of parser provided with the system is a Grok parser.  This type of parser is primarily designed for lower-velocity topologies or for quickly standing up a parser for a new telemetry before a permanent Java parser can be written for it.

##Message Format

All Metron messages follow a specific format in order to ingest a message.  If a message does not conform to this format it will be dropped and put onto an error queue for further examination.  The message must be of a JSON format and must have a JSON tag message like so:

```
{"message" : message content}

```

Where appropriate there is also a standardization around the 5-tuple JSON fields.  This is done so the topology correlation engine further down stream can correlate messages from different topologies by these fields.  We are currently working on expanding the message standardization beyond these fields, but this feature is not yet availabe.  The standard field names are as follows:

* ip_src_addr: layer 3 source IP
* ip_dst_addr: layer 3 dest IP
* ip_src_port: layer 4 source port
* ip_dst_port: layer 4 dest port
* protocol: layer 4 protocol
* timestamp (epoch)
* original_string: A human friendly string representation of the message

The timestamp and original_string fields are madatory. The remaining standard fields are optional.  If any of the optional fields are not applicable then the field should be left out of the JSON.

So putting it all together a typical Metron message with all 5-tuple fields present would look like the following:

```json
{
"message": 
{"ip_src_addr": xxxx, 
"ip_dst_addr": xxxx, 
"ip_src_port": xxxx, 
"ip_dst_port": xxxx, 
"protocol": xxxx, 
"original_string": xxx,
"additional-field 1": xxx,
}

}
```

##Parser Bolt

The Metron parser bolt is a standard bolt, which can be extended with multiple Java and Grok parser adapter for parsing different topology messages.  The bolt signature for declaration in a storm topology is as follows:

```
AbstractParserBolt parser_bolt = new TelemetryParserBolt()
.withMessageParser(parser)
.withMessageFilter(new GenericMessageFilter())
.withMetricConfig(config);

```

Metric Config - optional argument for exporting custom metrics to graphite.  If set to null no metrics will be exported.  If set, then a list of metrics defined in the metrics.conf file of each topology will define will metrics are exported and how often.

Message Filter - a filter defining which messages can be dropped.  This feature is only present in the Java paerer adapters

Message Parser - defines the parser adapter to be used for a topology

##Parser Adapters

Parser adapters are loaded dynamically in each Metron topology.  They are defined in topology.conf in the configuration item bolt.parser.adapter

###Java Parser Adapters
Java parser adapters are indended for higher-velocity topologies and are not easily changed or extended.  As the adoption of Metron continues we plan on extending our library of Java adapters to process more log formats.  As of this moment the Java adapters included with Metron are:

* com.apache.metron.parsing.parsers.BasicIseParser : Parse ISE messages
* com.apache.metron.parsing.parsers.BasicBroParser : Parse Bro messages
* com.apache.metron.parsing.parsers.BasicSourcefireParser : Parse Sourcefire messages
* com.apache.metron.parsing.parsers.BasicLancopeParser : Parse Lancope messages

###Grok Parser Adapters
Grok parser adapters are designed primarly for someone who is not a Java coder for quickly standing up a parser adapter for lower velocity topologies.  Grok relies on Regex for message parsing, which is much slower than purpose-built Java parsers, but is more extensible.  Grok parsers are defined via a config file and the topplogy does not need to be recombiled in order to make changes to them.  An example of a Grok perser is:

* com.apache.metron.parsing.parsers.GrokSourcefireParser

For more information on the Grok project please refer to the following link:

https://github.com/thekrakken/java-grok
