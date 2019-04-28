<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 -->

# Writer

## Introduction
The writer module provides some utilties for writing to outside components from within Storm.  This includes managing bulk writing.  An implemention is included for writing to HDFS in this module. Other writers can be found in their own modules.

## Bulk Message Writing
Most external components encourage messages to be written in batches for performance reasons.  The writer module includes an abstraction for doing this in an efficient manner.  This abstraction provides the following features:
* A high-level `BulkWriterComponent` class that manages a per-sensor cache of batched messages and flushes when appropriate
* An extension point for determining when a batch should be flushed
* An extention point for handling a bulk message write response after a batch has been flushed

### Flush Policies
Flushing behavior is controlled by a collection of `FlushPolicy` objects.  They are responsible for 2 things:

1. Determining when a batch should be flushed
2. Handling a `BulkWriterResponse` after a batch is flushed and messages are written with a `BulkMessageWriter`

The `FlushPolicy` interface defines methods for handling these responsiblities: 

- `boolean shouldFlush(String sensorType, WriterConfiguration configurations, List<BulkMessage<MESSAGE_T>> messages)`
- `void onFlush(String sensorType, BulkWriterResponse response)`

There are 2 `FlushPolicy` implementations included by default:  

- The `BatchSizePolicy` will flush a batch whenever the batch size reaches a configured value.  This configuration value is represented by the `batchSize` property in either the parser, enrichment or indexing configuration (whichever is appropriate in the current context).
- The `BatchTimeoutPolicy` will flush a batch whenever the batch timeout has elapsed.  This configuration value is represented by the `batchTimeout` property in either the parser, enrichment or indexing configuration (whichever is appropriate in the current context).  A `maxBatchTimeout` is set at creation time and serves as the ceiling for a batch timeout.  In Storm topologies, this value is set to 1/2 the tuple timeout setting to ensure messages are always flushed before their tuples timeout.  After a batch is flushed, the batch timer is reset for that sensor type.

For example, a configuration that sets the `batchSize` and `batchTimeout` in a parser topology will look like:
```
{
  "parserClassName": "org.apache.metron.parsers.bro.BasicBroParser",
  "sensorTopic": "bro",
  "parserConfig": {
    "batchSize": 5
    "batchTimeout": 2
  }
}
```
Similarly for the enrichment topology (configured in the [Global Configuration](../metron-common#global-configuration)):
```
{
  "enrichment.writer.batchSize": "5",
  "enrichment.writer.batchTimeout": "2",
  ...
}
```
And finally for the indexing topology:
```
{
  "elasticsearch": {
    "index": "bro",
    "batchSize": 5,
    "batchTimeout": 2,
    "enabled": true
  },
  ...
}
```


Additional policies can be added as needed.  For example, an `AckTuplesPolicy` is added in the Storm bolts to handle acking tuples after a batch is flushed.

### Bulk Writing Workflow
The `BulkWriterComponent` class collects messages in separate sensor-specific caches.  This class is instantiated and supplied to classes that need to write messages to external components.  A collection of default `FlushPolicy` implementations
are created by default with the option of passing in additional `FlushPolicy` objects as needed.

Batching and writing messages follows this process:

1. A single message is passed to the `BulkWriterComponent.write` method and stored in the appropriate cache based on the sensor type.  A `BulkMessageWriter` is also supplied to do the actual writing when messages are flushed.
2. The collection of `FlushPolicy` implementations are checked and a batch is flushed whenever the first `FlushPolicy.shouldFlush` returns true.
3. If no policies signal a flush, then nothing happens.  If a policy does signal a flush, the batch of messages for that sensor are written with the supplied `BulkMessageWriter`.  Each `FlushPolicy.onFlush` method is then called with the `BulkWriterResponse`.  
4. If a sensor type has been disabled, it's batch is flushed immediately (`FlushPolicy.shouldFlush` is not checked). 
5. A `BulkWriterComponent.flushAll` method is available that immediately calls the `FlushPolicy.shouldFlush` methods for each sensor type in the cache.  This should be called periodically by the class containing `BulkWriterComponent` to ensure messages
are not left sitting in the cache.  For example, the Storm bolts call this whenever a tick tuple is received.

### Logging
Logging can be enabled for the classes described in this section to provide insight into how messages are being batched and flushed.  This can be an important tool when performance tuning.
Setting the log level to `DEBUG` on the `org.apache.metron.writer` package will produce detailed information about when batches are flushed, which sensor a flushed batch corresponds to, which policy caused the flush, and how long it took to write the batch.

### Performance Tuning
The primary purpose of the Bulk Message Writing abstraction is to enable efficient writing to external components.  This section provides recommendations and strategies for doing that.  It is assumed that most Metron installations will include a variety of sensors with different volumes and velocities.  Different sensors will likely need to be tuned differently.

#### Set batches sizes higher for high volume sensors
High volume sensors should be identified and configured with higher batch sizes (1000+ is recommended).  Use logging to verify these batches are in fact filling up.  The number of actual message written should match the batch size.  Keep in mind that large batch sizes also require more memory to hold messages.  Streaming engines like Storm limit how many messages can be processed at a time (the `topology.max.spout.pending` setting).

#### Set batch timeouts lower for low volume sensors
Low volume sensors make take longer to fill up a batch, especially if the batch size is set higher.  This can be undesirable because messages may stay cached for longer than necessary, consuming memory and increasing latency for that sensor type.

#### Be careful with threads
Each thread (executor in Storm) maintains it's own message cache.  Allocating too many threads will cause messages to be spread too thin across separate caches and batches won't fill up completely.  This should be balanced with having enough threads to take advantage of any parallel write capability offered by the endpoint that's being written to.

#### Watch for high write times
Use logging to evaluate write timing.  Unusually high write times can indicate that an endpoint is not configured correctly or undersized.

## Kafka Writer
We have an implementation of a writer which will write batches of
messages to Kafka.  An interesting aspect of this writer is that it can
be configured to allow users to specify a message field which contains
the topic for the message.

The configuration for this writer is held in the individual Sensor
Configurations:
* [Enrichment](../metron-enrichment/README.md#sensor-enrichment-configuration) under the `config` element
* [Parsers](../metron-parsers-common/README.md#parser-configuration) in the `parserConfig` element
* Profiler - Unsupported currently

In each of these, the kafka writer can be configured via a map which has
the following elements:
* `kafka.brokerUrl` : The broker URL
* `kafka.keySerializer` : The key serializer (defaults to `StringSerializer`)
* `kafka.valueSerializer` : The key serializer (defaults to `StringSerializer`)
* `kafka.zkQuorum` : The zookeeper quorum
* `kafka.requiredAcks` : Whether to require acks.
* `kafka.topic` : The topic to write to
* `kafka.topicField` : The field to pull the topic from.  If this is specified, then the producer will use this.  If it is unspecified, then it will default to the `kafka.topic` property.  If neither are specified, then an error will occur.
* `kafka.producerConfigs` : A map of kafka producer configs for advanced customization.
 

## HDFS Writer
The HDFS writer included here expands on what Storm has in several ways. There's customization in syncing to HDFS, rotation policy, etc. In addition, the writer allows for users to define output paths based on the fields in the provided JSON message.  This can be defined using Stellar.

To manage the output path, a base path argument is provided by the Flux file, with the FileNameFormat as follows
```
    -   id: "fileNameFormat"
        className: "org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat"
        configMethods:
            -   name: "withPrefix"
                args:
                    - "enrichment-"
            -   name: "withExtension"
                args:
                  - ".json"
            -   name: "withPath"
                args:
                    - "/apps/metron/"
```
This means that all output will land in `/apps/metron/`.  With no further adjustment, it will be `/apps/metron/<sensor>/`.
However, by modifying the sensor's JSON config, it is possible to provide additional pathing based on the the message itself.

The output format of a file will be `{prefix}{componentId}-{taskId}-{rotationNum}-{timestamp}{extension}`. Notably, because of the way
file rotations are handled by the HdfsWriter, `rotationNum` will always be 0, but RotationActions still get executed normally.

E.g.
```
{
  "index": "bro",
  "batchSize": 5,
  "outputPathFunction": "FORMAT('uid-%s', uid)"
}
```
will land data in `/apps/metron/uid-<uid>/`.

For example, if the data contains uid's 1, 3, and 5, there will be 3 output folders in HDFS:
```
/apps/metron/uid-1/
/apps/metron/uid-3/
/apps/metron/uid-5/
```

The Stellar function must return a String, but is not limited to FORMAT functions. Other functions, such as `TO_LOWER`, `TO_UPPER`, etc. are all available for use. Typically, it's preferable to do nontrivial transformations as part of enrichment and simply reference the output here.

If no Stellar function is provided, it will default to putting the sensor in a folder, as above.

A caveat is that the writer will only allow a certain number of files to be created at once.  HdfsWriter has a function `withMaxOpenFiles` allowing this to be set.  The default is 500.  This can be set in Flux:
```
    -   id: "hdfsWriter"
        className: "org.apache.metron.writer.hdfs.HdfsWriter"
        configMethods:
            -   name: "withFileNameFormat"
                args:
                    - ref: "fileNameFormat"
            -   name: "withRotationPolicy"
                args:
                    - ref: "hdfsRotationPolicy"
            -   name: "withMaxOpenFiles"
                args: 500
```

