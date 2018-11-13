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

# Parser Chaining

Aggregating many different types sensors into a single data source (e.g.
syslog) and ingesting that aggregate sensor into Metron is a common pattern.  It 
is not obvious precisely how to manage these types of aggregate sensors 
as they require two-pass parsing.  This document will walk through an
example of supporting this kind of multi-pass ingest.

Multi-pass parser involves the following requirements:
* The enveloping parser (e.g. the aggregation format such as syslog or
  plain CSV) may contain metadata which should be ingested along with the data.
* The enveloping sensor contains many different sensor types

# High Level Solution

![High Level Approach](../../../use-cases/parser_chaining/message_routing_high_level.svg)

At a high level, we continue to maintain the architectural invariant of
a 1-1 relationship between logical sensors and storm topologies.
Eventually this relationship may become more complex, but at the moment
the approach is to construct a routing parser which will have two
responsibilities:
* Parse the envelope (e.g. syslog data) and extract any metadata fields
  from the envelope to pass along
* Route the unfolded data to the appropriate kafka topic associated with
  the enveloped sensor data

Because the data emitted from the routing parser is just like any data
emitted from any other parser, in that it is a JSON blob like any
data emitted from any parser, we will need to adjust the downstream
parsers to extract the enveloped data from the JSON blob and treat it as
the data to parse.

# Aggregated Parsers with Parser Chaining
Chained parsers can be run as aggregated parsers. These parsers continue to use the sensor specific Kafka topics, and do not do internal routing to the appropriate sensor.

Say, there were three sensors (`bro`, `snort` and `yaf`). Instead of creating a topology per sensor, all 3 can be run in a single aggregated parser. It is also possible to aggregate a subset of these parsers (e.g. run `bro` as it's own topology, and aggregate the other 2).

The step to start an aggregated parsers then becomes
```
$METRON_HOME/bin/start_parser_topology.sh -k $BROKERLIST -z $ZOOKEEPER -s bro,snort,yaf
```

which will result in a single storm topology named `bro__snort__yaf` to run.

Aggregated parsers can be specified using the Ambari Metron config as well under Services -> Metron -> Configs -> 'Parsers' tab -> 'Metron Parsers' field. The grouping is configured by enclosing the desired parsers in double quotes.

Some examples of specifying aggregated parsers are as follows:
* "bro,snort,yaf" --> Will start a single topology named `bro__snort__yaf`
* "ciscopixA,ciscopixB",yaf,"squid,ciscopixC" --> Will start three topologies viz. `ciscopixA__ciscopixB`, `yaf` and `squid__ciscopixC`

# Architecting a Parser Chaining Solution in Metron

Currently the approach to fulfill this requirement involves a couple
knobs in the Parser infrastructure for Metron.

Consider the case, for instance,
where we have many different TYPES of messages wrapped inside of syslog.
As an architectural abstraction, we would want to have the following
properties:
* separate the concerns of parsing the individual types of messages from
  each other
* separate the concerns of parsing the individual types of messages from
  parsing the envelope

## Data Dependent Parser Writing

Parsers allow users to configure the topic which the kafka producer uses
in a couple of ways (from the parser config in an individual parser):
* `kafka.topic` - Specify the topic in the config.  This can be updated by updating the config, but it is data independent (e.g. not dependent on the data in a message).  
* `kafka.topicField` - Specify the topic as the value of a particular field.  If unpopulated, then the message is dropped.  This is inherrently data dependent.

The `kafka.topicField` parameter allows for data dependent topic
selection and this inherrently enables the routing capabilities
necessary for handling enveloped data. 

## Flexibly Interpreting Data

### Aside: The Role of Metadata in Metron

Before we continue, let's briefly talk about metadata.  We have exposed
the ability to pass along metadata and interact with metadata in a
decoupled way from the actual parser logic (i.e. the GrokParser should
not have to consider how to interpret metadata).

There are three choices about manipulating metadata in Metron:
* Should you merge metadata into the downstream message?
* If you do, should you use a key prefix to set it off from the message
  by default?

This enables users to specify metadata independent of the data that is
persisted downstream and can inform the operations of enrichment and the
profiler.

### Interpretation

Now that we have an approach which enables the routing of the data, the
remaining question is how to decouple _parsing_ data from _interpreting_
data and metadata.  By default, Metron operates like so:
* The kafka record key (as a JSON Map) is considered metadata
* The kafka record value is considered data

Beyond that, we presume defaults for this default strategy around
handling metadata.  In particular, by default we do not merge metadata
and use a `metron.metadata` prefix for all metadata.

In order to enable chained parser WITH metadata, we allow the following
to be specified via strategy in the parser config:
* How to extract the data from the kafka record
* How to extract the metadata from the kafka record
* The default operations for merging
* The prefix for the metadata key

The available strategies, specified by the `rawMessageStrategy`
configuration is either`ENVELOPE` or `DEFAULT`.

Specifically, to enable parsing enveloped data (i.e. data in a field of a JSON
blob with the other fields being metadata), one can specify the strategy
and configuration of that strategy in the parser config.  One must
specify the `rawMessageStrategy` as `ENVELOPE` in the parser and the
`rawMessageStrategyConfig` to indicate the field which contains the
data.

Together with routing, we have the complete solution to chain parsers which can:
* parse the envelope
* route the parsed data to specific parsers
* have the specific parsers interpret the data via the `rawMessageStrategy` whereby they pull the data out from JSON Map that they receive

Together this enables a directed acyclic graph of parsers to handle single or multi-layer parsing.

### Example
For a complete example, look at the [parser chaining use-case](../../../use-cases/parser_chaining), however for a simple example the following should suffice.

If I want to configure a CSV parser to parse data which has 3 columns `f1`, `f2` and `f3` and is
held in a field called `payload` inside of a JSON Map, I can do so like
this:
```
{
  "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
  ,"sensorTopic" : "my_topic"
  ,"rawMessageStrategy" : "ENVELOPE"
  ,"rawMessageStrategyConfig" : {
      "messageField" : "payload",
      "metadataPrefix" : ""
  }
  , "parserConfig": {
     "columns" : { "f1": 0,
                 , "f2": 1,
                 , "f3": 2
                 } 
   }
}
```

This would parse the following message:
```
{
  "meta_f1" : "val1",
  "payload" : "foo,bar,grok",
  "original_string" : "2019 Jul, 01: val1 foo,bar,grok",
  "timestamp" : 10000
}
```
into
```
{
  "meta_f1" : "val1",
  "f1" : "foo",
  "f2" : "bar",
  "f3" : "grok",
  "original_string" : "2019 Jul, 01: val1 foo,bar,grok",
  "timestamp" : 10002
}
```

Note a couple of things here:
* The metadata field `meta_f1` is not prefixed here because we configured the strategy with `metadataPrefix` as empty string.
* The `timestamp` is not inherited from the metadata
* The `original_string` is inherited from the metadata
