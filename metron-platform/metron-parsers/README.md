#Parsers

Parsers are pluggable components which are used to transform raw data
(textual or raw bytes) into JSON messages suitable for downstream
enrichment and indexing.  

There are two types of parsers:
*  A parser written in Java which conforms to the `MessageParser` interface.  This kind of parser is optimized for speed and performance and
is built for use with higher velocity topologies.  These parsers are not easily modifiable and in order to make changes to them the entire topology need to be recompiled.  
* A Grok parser.  This type of parser is primarily designed for lower-velocity topologies or for quickly standing up a parser for a new telemetry before a permanent Java parser can be written for it.

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

##Parser Configuration

The configuration for the various parser topologies is defined by JSON
documents stored in zookeeper.

The document is structured in the following way

* `parserClassName` : The fully qualified classname for the parser to be used.
* `sensorTopic` : The kafka topic to send the parsed messages to.
* `parserConfig` : A JSON Map representing the parser implementation specific configuration.
* `fieldTransformations` : An array of complex objects representing the transformations to be done on the message generated from the parser before writing out to the kafka topic.

The `fieldTransformations` is a complex object which defines a
transformation which can be done to a message.  This transformation can 
* Modify existing fields to a message
* Add new fields given the values of existing fields of a message
* Remove existing fields of a message

###`fieldTransformation` configuration

The format of a `fieldTransformation` is as follows:
* `input` : An array of fields or a single field representing the input.  This is optional; if unspecified, then the whole message is passed as input.
* `output` : The outputs to produce from the transformation.  If unspecified, it is assumed to be the same as inputs.
* `transformation` : The fully qualified classname of the transformation to be used.  This is either a class which implements `FieldTransformation` or a member of the `FieldTransformations` enum.
* `config` : A String to Object map of transformation specific configuration.
 
The currently implemented fieldTransformations are:
* `REMOVE` : This transformation removes the specified input fields.  If you want a conditional removal, you can pass a Metron Query Language statement to define the conditions under which you want to remove the fields. 

Consider the following simple configuration which will remove `field1`
unconditionally:
```
{
...
    "fieldTransformations" : [
          {
            "input" : "field1"
          , "mapping" : "REMOVE"
          }
                      ]
}
```

Consider the following simple sensor parser configuration which will remove `field1`
whenever `field2` exists and whose corresponding equal to 'foo':
```
{
...
  "fieldTransformations" : [
          {
            "input" : "field1"
          , "mapping" : "REMOVE"
          , "config" : {
              "condition" : "exists(field2) and field2 == 'foo'"
                       }
          }
                      ]
}
```

* `IP_PROTOCOL` : This transformation maps IANA protocol numbers to consistent string representations.

Consider the following sensor parser config to map the `protocol` field
to a textual representation of the protocol:
```
{
...
    "fieldTransformations" : [
          {
            "input" : "protocol"
          , "transformation" : "IP_PROTOCOL"
          }
                      ]
}
```

This transformation would transform `{ "protocol" : 6, "source.type" : "bro", ... }` 
into `{ "protocol" : "TCP", "source.type" : "bro", ...}`

###An Example Configuration for a Sensor
Consider the following example configuration for the `yaf` sensor:

```
{
  "parserClassName":"org.apache.metron.parsers.GrokParser",
  "sensorTopic":"yaf",
  "fieldTransformations" : [
                    {
                      "input" : "protocol"
                     ,"transformation": "IP_PROTOCOL"
                    }
                    ],
  "parserConfig":
  {
    "grokPath":"/patterns/yaf",
    "patternLabel":"YAF_DELIMITED",
    "timestampField":"start_time",
    "timeFields": ["start_time", "end_time"],
    "dateFormat":"yyyy-MM-dd HH:mm:ss.S"
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

* org.apache.metron.parsers.ise.BasicIseParser : Parse ISE messages
* org.apache.metron.parsers.bro.BasicBroParser : Parse Bro messages
* org.apache.metron.parsers.sourcefire.BasicSourcefireParser : Parse Sourcefire messages
* org.apache.metron.parsers.lancope.BasicLancopeParser : Parse Lancope messages

###Grok Parser Adapters
Grok parser adapters are designed primarly for someone who is not a Java coder for quickly standing up a parser adapter for lower velocity topologies.  Grok relies on Regex for message parsing, which is much slower than purpose-built Java parsers, but is more extensible.  Grok parsers are defined via a config file and the topplogy does not need to be recombiled in order to make changes to them.  An example of a Grok perser is:

* org.apache.metron.parsers.GrokParser

For more information on the Grok project please refer to the following link:

https://github.com/thekrakken/java-grok
