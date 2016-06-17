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
          , "transformation" : "REMOVE"
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
          , "transformation" : "REMOVE"
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

* `MTL` : This transformation executes a set of transformations expressed as [Metron Transformation Language](../metron-common) statements.

Consider the following sensor parser config to add three new fields to a
message:
* `utc_timestamp` : The unix epoch timestamp based on the `timestamp` field, a `dc` field which is the data center the message comes from and a `dc2tz` map mapping data centers to timezones
* `url_host` : The host associated with the url in the `url` field
* `url_protocol` : The protocol associated with the url in the `url` field

```
{
...
    "fieldTransformations" : [
          {
           "transformation" : "MTL"
          ,"output" : [ "utc_timestamp", "url_host", "url_protocol" ]
          ,"config" : {
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd
HH:mm:ss', MAP_GET(dc, dc2tz, 'UTC') )"
           ,"url_host" : "URL_TO_HOST(url)"
           ,"url_protocol" : "URL_TO_PROTOCOL(url)"
                      }
          }
                      ]
   ,"parserConfig" : {
      "dc2tz" : {
                "nyc" : "EST"
               ,"la" : "PST"
               ,"london" : "UTC"
                }
    }
}
```

Note that the `dc2tz` map is in the parser config, so it is accessible
in the functions.

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

#Starting the Parser Topology

Starting a particular parser topology on a running Metron deployment is
as easy as running the `start_parser_topology.sh` script located in
`$METRON_HOME/bin`.  This utility will allow you to configure and start
the running topology assuming that the sensor specific parser configuration
exists within zookeeper.

The usage for `start_parser_topology.sh` is as follows:

```
usage: start_parser_topology.sh
 -e,--extra_topology_options <JSON_FILE>        Extra options in the form
                                                of a JSON file with a map
                                                for content.
 -esc,--extra_kafka_spout_config <JSON_FILE>    Extra spout config options
                                                in the form of a JSON file
                                                with a map for content.
                                                Possible keys are:
                                                retryDelayMaxMs,retryDelay
                                                Multiplier,retryInitialDel
                                                ayMs,stateUpdateIntervalMs
                                                ,bufferSizeBytes,fetchMaxW
                                                ait,fetchSizeBytes,maxOffs
                                                etBehind,metricsTimeBucket
                                                SizeInSecs,socketTimeoutMs
 -ewnt,--error_writer_num_tasks <NUM_TASKS>     Error Writer Num Tasks
 -ewp,--error_writer_p <PARALLELISM_HINT>       Error Writer Parallelism
                                                Hint
 -h,--help                                      This screen
 -iwnt,--invalid_writer_num_tasks <NUM_TASKS>   Invalid Writer Num Tasks
 -iwp,--invalid_writer_p <PARALLELISM_HINT>     Invalid Message Writer
                                                Parallelism Hint
 -k,--kafka <BROKER_URL>                        Kafka Broker URL
 -mt,--message_timeout <TIMEOUT_IN_SECS>        Message Timeout in Seconds
 -mtp,--max_task_parallelism <MAX_TASK>         Max task parallelism
 -na,--num_ackers <NUM_ACKERS>                  Number of Ackers
 -nw,--num_workers <NUM_WORKERS>                Number of Workers
 -pnt,--parser_num_tasks <NUM_TASKS>            Parser Num Tasks
 -pp,--parser_p <PARALLELISM_HINT>              Parser Parallelism Hint
 -s,--sensor <SENSOR_TYPE>                      Sensor Type
 -snt,--spout_num_tasks <NUM_TASKS>             Spout Num Tasks
 -sp,--spout_p <SPOUT_PARALLELISM_HINT>         Spout Parallelism Hint
 -t,--test <TEST>                               Run in Test Mode
 -z,--zk <ZK_QUORUM>                            Zookeeper Quroum URL
                                                (zk1:2181,zk2:2181,...
```

# The `--extra_kafka_spout_config` Option
These options are intended to configure the Storm Kafka Spout more completely.  These options can be
specified in a JSON file containing a map associating the kafka spout configuration parameter to a value.
The range of values possible to configure are:
* retryDelayMaxMs
* retryDelayMultiplier
* retryInitialDelayMs
* stateUpdateIntervalMs
* bufferSizeBytes
* fetchMaxWait
* fetchSizeBytes
* maxOffsetBehind
* metricsTimeBucketSizeInSecs
* socketTimeoutMs

These are described in some detail [here](https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.3.4/bk_storm-user-guide/content/storm-kafka-api-ref.html).

For instance, creating a JSON file which will set the `bufferSizeBytes` to 2MB and `retryDelayMaxMs` to 2000 would look like
```
{
  "bufferSizeBytes" : 2000000,
  "retryDelayMaxMs" : 2000
}
```

This would be loaded by passing the file as argument to `--extra_kafka_spout_config`

# The `--extra_topology_options` Option

These options are intended to be Storm configuration options and will live in
a JSON file which will be loaded into the Storm config.  For instance, if you wanted to set a storm property on
the config called `topology.ticks.tuple.freq.secs` to 1000 and `storm.local.dir` to `/opt/my/path`
you could create a file called `custom_config.json` containing 
```
{ 
  "topology.ticks.tuple.freq.secs" : 1000,
  "storm.local.dir" : "/opt/my/path"
}
```
and pass `--extra_topology_options custom_config.json` to `start_parser_topology.sh`.
