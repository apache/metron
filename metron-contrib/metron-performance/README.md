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
# Performance Utilities

This project creates some useful performance monitoring and measurement
utilities.

## `load-tool.sh`

The Load tool is intended to do the following:
* Generate a load at a specific events per second into kafka
  * The messages are taken from a template file, where there is a message template per line
  * The load can be biased (e.g. 80% of the load can be comprised of 20% of the templates)
* Monitor the kafka offsets for a topic to determine the events per second written
  * This could be the topic that you are generating load on
  * This could be another topic that represents the output of some topology (e.g. generate load on `enrichments` and monitor `indexing` to determine the throughput of the enrichment topology).

```
usage: Generator
 -bs,--sample_bias <BIAS_FILE>         The discrete distribution to bias
                                       the sampling. This is a CSV of 2
                                       columns.  The first column is the %
                                       of the templates and the 2nd column
                                       is the probability (0-100) that
                                       it's chosen.  For instance:
                                       20,80
                                       80,20
                                       implies that 20% of the templates
                                       will comprise 80% of the output and
                                       the remaining 80% of the templates
                                       will comprise 20% of the output.
 -cg,--consumer_group <GROUP_ID>       Consumer Group.  The default is
                                       load.group
 -e,--eps <EPS>                        The target events per second
 -h,--help                             Generate Help screen
 -k,--kafka_config <CONFIG_FILE>       The kafka config.  This is a file
                                       containing a JSON map with the
                                       kafka config.
 -l,--lookback <LOOKBACK>              When summarizing, how many
                                       monitoring periods should we
                                       summarize over?  If 0, then no
                                       summary.  Default: 5
 -md,--monitor_delta_ms <TIME_IN_MS>   The time (in ms) between monitoring
                                       output. Default is 10000
 -mt,--monitor_topic <TOPIC>           The kafka topic to monitor.
 -ot,--output_topic <TOPIC>            The kafka topic to write to
 -p,--threads <NUM_THREADS>            The number of threads to use when
                                       extracting data.  The default is
                                       the number of cores of your
                                       machine.
 -sd,--send_delta_ms <TIME_IN_MS>      The time (in ms) between sending a
                                       batch of messages. Default is 100
 -t,--template <TEMPLATE_FILE>         The template file to use for
                                       generation.  This should be a file
                                       with a template per line with
                                       $METRON_TS and $METRON_GUID in the
                                       spots for timestamp and guid, if
                                       you so desire them.
 -tl,--time_limit_ms <MS>              The total amount of time to run
                                       this in milliseconds.  By default,
                                       it never stops.
 -z,--zk_quorum <QUORUM>               zookeeper quorum

```

## Templates
Messages are drawn from a template file.  A template file has a message template per line.  
For instance, let's say we want to generate JSON maps with fields: `source.type`, `ip_src_addr` 
and `ip_dst_addr`.  We can generate a template file with a template like the following per line:
```
{ "source.type" : "asa", "ip_src_addr" : "127.0.0.1", "ip_dst_addr" : "191.168.1.1" }
```

When messages are generated, there are some special replacements that can be used: `$METRON_TS` and `$METRON_GUID`.
We can adjust our previous template to use these like so:
```
{ "source.type" : "asa", "ip_src_addr" : "127.0.0.1", "ip_dst_addr" : "191.168.1.1", "timestamp" : $METRON_TS, "guid" : "$METRON_GUID" }
```
One note about GUIDs generated.  We do not generate global UUIDs, they are unique only within the context of a given generator run.  

## Biased Sampling

This load tool can be configured to use biased sampling.  This is useful if, for instance, you are trying to model data which is not distributed
uniformly, like many types of network data.  Generating synthetic data with similar distribution to your regular data will enable the caches
to be exercised in the same way, for instance, and yield a more realistic scenario.

You specify the biases in a csv file with 2 columns:
* The first column represents the % of the templates
* The second column represents the % of the generated output. 

A simple example would be to generate samples based on Pareto's principle:
```
20,80
80,20
``` 
This would yield biases that mean the first 20% of the templates in the template file would comprise 80% of the output.

A more complex example might be:
```
20,80
20,5
50,1
10,14
``` 
This would would imply:
* The first 20% of the templates would comprise 80% of the output
* The next 20% of the templates would comprise 5% of the output
* The next 50% of the templates would comprise 1% of the output
* The next 10% of the templates would comprise 14% of the output.

## Use-cases for the Load Tool

### Measure Throughput of a Topology

One can use the load tool to monitor performance of a kafka-to-kafka topology.
For instance, we could monitor the throughput of the enrichment topology by monitoring the `enrichments` kafka topic:
```
$METRON_HOME/bin/load_tool.sh -mt enrichments -z $ZOOKEEPER
```

### Generate Synthetic Load and Measure Performance

One can use the load tool to generate synthetic load and monitor performance of a kafka-to-kafka topology.  For instance, we could
monitor the performance of the enrichment topology.  It is advised to start the enrichment topology against a new topic and write 
to a new topic so as to not pollute your downstream indices.  So, for instance we could create a kafka topic called 
`enrichments_load` by generating load on it.  We could also create a new  kafka topic called `indexing_load` and configure the enrichment
topology to output to it.  We would then generate load on `enrichments_load` and monitor `indexing_load`.
```
#Threadpool of size 5, you want somewhere between 5 and 10 depending on the throughput numbers you're trying to drive
#Messages drawn from ~/dummy.templates, which is a message template per line
#Generate at a rate of 1000 messages per second
$METRON_HOME/bin/load_tool.sh -p 5 -ot enrichments_load -mt indexing_load -t ~/dummy.templates -eps 1000 -z $ZOOKEEPER 
```

