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

# Enrichment Performance

This guide defines a set of benchmarks used to measure the performance of the Enrichment topology.  The guide also provides detailed steps on how to execute those benchmarks along with advice for tuning the Unified Enrichment topology.

* [Benchmarks](#benchmarks)
* [Benchmark Execution](#benchmark-execution)
* [Performance Tuning](#performance-tuning)
* [Benchmark Results](#benchmark-results)

## Benchmarks

The following section describes a set of enrichments that will be used to benchmark the performance of the Enrichment topology.

* [Geo IP Enrichment](#geo-ip-enrichment)
* [HBase Enrichment](#hbase-enrichment)
* [Stellar Enrichment](#stellar-enrichment)

### Geo IP Enrichment

This benchmark measures the performance of executing a Geo IP enrichment.  Given a valid IP address the enrichment will append detailed location information for that IP.  The location information is sourced from an external Geo IP data source like [Maxmind](https://github.com/maxmind/GeoIP2-java).

#### Configuration

Adding the following Stellar expression to the Enrichment topology configuration will define a Geo IP enrichment.
```
geo := GEO_GET(ip_dst_addr)
```

After the enrichment process completes, the  telemetry message will contain a set of fields with location information for the given IP address.
```
{
   "ip_dst_addr":"151.101.129.140",
   ...
   "geo.city":"San Francisco",
   "geo.country":"US",
   "geo.dmaCode":"807",
   "geo.latitude":"37.7697",
   "geo.location_point":"37.7697,-122.3933",
   "geo.locID":"5391959",
   "geo.longitude":"-122.3933",
   "geo.postalCode":"94107",
 }
```

### HBase Enrichment

This benchmark measures the performance of executing an enrichment that retrieves data from an external HBase table. This type of enrichment is useful for enriching telemetry from an Asset Database or other source of relatively static data.

#### Configuration

Adding the following Stellar expression to the Enrichment topology configuration will define an Hbase enrichment.  This looks up the 'ip_dst_addr' within an HBase table 'top-1m' and returns a hostname.
```
top1m := ENRICHMENT_GET('top-1m', ip_dst_addr, 'top-1m', 't')
```

After the telemetry has been enriched, it will contain the host and IP elements that were retrieved from the HBase table.
```
{
	"ip_dst_addr":"151.101.2.166",
	...
	"top1m.host":"earther.com",
	"top1m.ip":"151.101.2.166"
}
```

### Stellar Enrichment

This benchmark measures the performance of executing a basic Stellar expression.  In this benchmark, the enrichment is purely a computational task that has no dependence on an external system like a database.  

#### Configuration

Adding the following Stellar expression to the Enrichment topology configuration will define a basic Stellar enrichment.  The following returns true if the IP is in the given subnet and false otherwise.
```
local := IN_SUBNET(ip_dst_addr, '192.168.0.0/24')
```

After the telemetry has been enriched, it will contain a field with a boolean value indicating whether the IP was within the given subnet.
```
{
	"ip_dst_addr":"151.101.2.166",
	...
	"local":false
}
```

## Benchmark Execution

This section describes the steps necessary to execute the performance benchmarks for the Enrichment topology.

* [Prepare Enrichment Data](#prepare-enrichment-data)
* [Load HBase with Enrichment Data](#load-hbase-with-enrichment-data)
* [Configure the Enrichments](#configure-the-enrichments)
* [Create Input Telemetry](#create-input-telemetry)
* [Cluster Setup](#cluster-setup)
* [Monitoring](#monitoring)

### Prepare Enrichment Data

The Alexa Top 1 Million was used as a data source for these benchmarks.

1. Download the [Alexa Top 1 Million](http://s3.amazonaws.com/alexa-static/top-1m.csv.zip) or another similar data set with a variety of valid hostnames.

2. For each hostname, query DNS to retrieve an associated IP address.  

	A script like the following can be used for this.  There is no need to do this for all 1 million entries in the data set. Doing this for around 10,000 records is sufficient.
        
	```python
	import dns.resolver
	import csv
	#
	resolver = dns.resolver.Resolver()
	resolver.nameservers = ['8.8.8.8', '8.8.4.4']
	#
	with open('top-1m.csv', 'r') as infile:
	  with open('top-1m-with-ip.csv', 'w') as outfile:
	    #
	    reader = csv.reader(infile, delimiter=',')
	    writer = csv.writer(outfile, delimiter=',')
	    for row in reader:
	      #
	      host = row[1]
	      try:
	        response = resolver.query(host, "A")
	        for record in response:
	          ip = record
	          writer.writerow([host, ip])
	          print "host={}, ip={}".format(host, ip)
	        #
	      except:
	        pass
	```

3. The resulting data set contains an IP to hostname mapping.
	```bash
	$ head top-1m-with-ip.csv
	google.com,172.217.9.46
	youtube.com,172.217.4.78
	facebook.com,157.240.18.35
	baidu.com,220.181.57.216
	baidu.com,111.13.101.208
	baidu.com,123.125.114.144
	wikipedia.org,208.80.154.224
	yahoo.com,98.139.180.180
	yahoo.com,206.190.39.42
	reddit.com,151.101.1.140
	```

### Load HBase with Enrichment Data

1. Create an HBase table for this data.  

	Ensure that the table is evenly distributed across the HBase nodes.  This can be done by pre-splitting the table or splitting the data after loading it.  

	```
	create 'top-1m', 't', {SPLITS => ['2','4','6','8','a','c','e']}
	```

1. Create a configuration file called `extractor.json`.  This defines how the data will be loaded into the HBase table.

	```bash
	> cat extractor.json
	{
	    "config": {
	        "columns": {
	            "host" : 0,
	            "ip": 1
	        },
	        "indicator_column": "ip",
	        "type": "top-1m",
	        "separator": ","
	    },
	    "extractor": "CSV"
	}
	```

1. Use the `flatfile_loader.sh` to load the data into the HBase table.
	```
	$METRON_HOME/bin/flatfile_loader.sh \
		-e extractor.json \
		-t top-1m \
		-c t \
		-i top-1m-with-ip.csv
	```

### Configure the Enrichments

1. Define the Enrichments using the REPL.

	```
	> $METRON_HOME/bin/stellar -z $ZOOKEEPER
	Stellar, Go!
	[Stellar]>>> conf
	{
	  "enrichment": {
	    "fieldMap": {
	     "stellar" : {
	       "config" : {
	         "geo" : "GEO_GET(ip_dst_addr)",
	         "top1m" : "ENRICHMENT_GET('top-1m', ip_dst_addr, 'top-1m', 't')",
	         "local" : "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')"
	       }
	     }
	    },
	    "fieldToTypeMap": {
	    }
	  },
	  "threatIntel": {
	  }
	}
	[Stellar]>>> CONFIG_PUT("ENRICHMENT", conf, "asa")
	```

### Create Input Telemetry

1.  Create a template file that defines what your input telemetry will look-like.

	```bash
	> cat asa.template
	{"ciscotag": "ASA-1-123123", "source.type": "asa", "ip_dst_addr": "$DST_ADDR", "original_string": "<134>Feb 22 17:04:43 AHOSTNAME %ASA-1-123123: Built inbound ICMP connection for faddr 192.168.11.8/50244 gaddr 192.168.1.236/0 laddr 192.168.1.1/161", "ip_src_addr": "192.168.1.35", "syslog_facility": "local1", "action": "built", "syslog_host": "AHOSTNAME", "timestamp": "$METRON_TS", "protocol": "icmp", "guid": "$METRON_GUID", "syslog_severity": "info"}
	```

2.  Use the template file along with the enrichment data to create input telemetry with varying IP addresses.

	```bash
	for i in $(head top-1m-with-ip.csv | awk -F, '{print $2}');do
		cat asa.template | sed "s/\$DST_ADDR/$i/";
	done > asa.input.template
	```

3. Use the `load_tool.sh` script to push messages onto the input topic `enrichments` and monitor the output topic `indexing`.  See more information in the Performance [README.md](metron-contrib/metron-performance/README.md).

	If the topology is keeping up, obviously the events per second produced on the input topic should roughly match the output topic.

### Cluster Setup

#### Isolation

The Enrichment topology depends on an environment with at least two and often three components that work together; Storm, Kafka, and HBase.  When any of two of these are run on the same node, it can be difficult to identify which of them is becoming a bottleneck.  This can cause poor and highly volatile performance as each steals resources from the other.  

It is highly recommended that each of these systems be fully isolated from the others.  Storm should be run on nodes that are completely isolated from Kafka and HBase.

### Monitoring

1. The `load_test.sh` script will report the throughput for the input and output topics.  

	* The input throughput should roughly match the output throughput if the topology is able to handle a given load.

	* Not only are the raw throughput numbers important, but also the consistency of what is reported over time.  If the reported throughput is sporadic, then further tuning may be required.

1. The Storm UI is obviously an important source of information.  The bolt capacity, complete latency, and any reported errors are all important to monitor

1. The load reported by the OS is also an important metric to monitor.  

	* The load metric should be monitored to ensure that each node is being pushed sufficiently, but not too much.

	* The load should be evenly distributed across each node.  If the load is uneven, this may indicate a problem.

	A simple script like the following is sufficient for the task.

	```
	for host in $(cat cluster.txt); do
	  echo $host;
	  ssh root@$host 'uptime';
	done
	```

1. Monitoring the Kafka offset lags indicates how far behind a consumer may be.  This can be very useful to determine if the topology is keeping up.

	```
	${KAFKA_HOME}/bin/kafka-consumer-groups.sh \
	    --command-config=/tmp/consumergroup.config \
	    --describe \
	    --group enrichments \
	    --bootstrap-server $BROKERLIST \
	    --new-consumer
	```

1. A tool like [Kafka Manager](https://github.com/yahoo/kafka-manager) is also very useful for monitoring the input and output topics during test execution.

## Performance Tuning

The approach to tuning the topology will look something like the following.  More detailed tuning information is available next to each named parameter

* Start the tuning process with a single worker.  After tuning the bolts within a single worker, scale out with additional worker processes.

* Initially set the thread pool size to 1.  Increase this value slowly only after tuning the other parameters first.  Consider that each worker has its own thread pool and the total size of this thread pool should be far less than the total number of cores available in the cluster.

* Initially set each bolt parallelism hint to the number of partitions on the input Kafka topic.  Monitor bolt capacity and increase the parallelism hint for any bolt whose capacity is close to or exceeds 1.  

* If the topology is not able to keep-up with a given input, then increasing the parallelism is the primary means to scale up.

* Parallelism units can be used for determining how to distribute processing tasks across the topology.  The sum of parallelism can be close to, but should not far exceed this value.

	 (number of worker nodes in cluster * number cores per worker node) - (number of acker tasks)

* The throughput that the topology is able to sustain should be relatively consistent.  If the throughput fluctuates greatly, increase back pressure using [`topology.max.spout.pending`](#topologymaxspoutpending).

### Parameters

The following parameters are useful for tuning the "Unified" Enrichment topology.  

WARNING: Some of the parameter names have been reused from the "Split/Join" topology so the name may not be appropriate. This will be corrected in the future.

* [`enrichment.workers`](#enrichmentworkers)
* [`enrichment.acker.executors`](#enrichmentackerexecutors)
* [`topology.worker.childopts`](#topologyworkerchildopts)
* [`topology.max.spout.pending`](#topologymaxspoutpending)
* [`kafka.spout.parallelism`](#kafkaspoutparallelism)
* [`enrichment.join.parallelism`](#enrichmentjoinparallelism)
* [`threat.intel.join.parallelism`](#threatinteljoinparallelism)
* [`kafka.writer.parallelism`](#kafkawriterparallelism)
* [`enrichment.join.cache.size`](#enrichmentjoincachesize)
* [`threat.intel.join.cache.size`](#threatinteljoincachesize)
* [`metron.threadpool.size`](#metronthreadpoolsize)
* [`metron.threadpool.type`](#metronthreadpooltype)

#### `enrichment.workers`

The number of worker processes for the enrichment topology.

* Start by tuning only a single worker.  Maximize throughput for that worker, then increase the number of workers.

* The throughput should scale relatively linearly as workers are added.  This reaches a limit as the number of workers running on a single node saturate the resources available.  When this happens, adding workers, but on additional nodes should allow further scaling.

* Increase parallelism before attempting to increase the number of workers.

#### `enrichment.acker.executors`

The number of ackers within the topology.

* This should most often be equal to the number of workers defined in `enrichment.workers`.

* Within the Storm UI, click the "Show System Stats" button.  This will display a bolt named `__acker`.  If the capacity of this bolt is too high, then increase the number of ackers.

#### `topology.worker.childopts`

This parameter accepts arguments that will be passed to the JVM created for each Storm worker.  This allows for control over the heap size, garbage collection, and any other JVM-specific parameter.

* Start with a 2G heap and increase as needed.  Running with 8G was found to be beneficial, but will vary depending on caching needs.

    `-Xms8g -Xmx8g`

* The Garbage First Garbage Collector (G1GC) is recommended along with a cap on the amount of time spent in garbage collection.  This is intended to help address small object allocation issues due to our extensive use of caches.

    `-XX:+UseG1GC -XX:MaxGCPauseMillis=100`

* If the caches in use are very large (as defined by either [`enrichment.join.cache.size`](#enrichmentjoincachesize) or [`threat.intel.join.cache.size`](#threatinteljoincachesize)) and performance is poor, turning on garbage collection logging might be helpful.

#### `topology.max.spout.pending`

This limits the number of unacked tuples that the spout can introduce into the topology.

* Decreasing this value will increase back pressure and allow the topology to consume messages at a pace that is maintainable.

* If the spout throws 'Commit Failed Exceptions' then the topology is not keeping up.  Decreasing this value is one way to ensure that messages can be processed before they time out.

* If the topology's throughput is unsteady and inconsistent, decrease this value.  This should help the topology consume messages at a maintainable pace.

* If the bolt capacity is low, the topology can handle additional load.  Increase this value so that more tuples are introduced into the topology which should increase the bolt capacity.

#### `kafka.spout.parallelism`

The parallelism of the Kafka spout within the topology.  Defines the maximum number of executors for each worker dedicated to running the spout.

* The spout parallelism should most often be set to the number of partitions of the input Kafka topic.

* If the enrichment bolt capacity is low, increasing the parallelism of the spout can introduce additional load on the topology.

####  `enrichment.join.parallelism`

The parallelism hint for the enrichment bolt.  Defines the maximum number of executors within each worker dedicated to running the enrichment bolt.

WARNING: The property name does not match its current usage in the Unified topology.  This property name may change in the near future as it has been reused from the Split-Join topology.  

* If the capacity of the enrichment bolt is high, increasing the parallelism will introduce additional executors to bring the bolt capacity down.

* If the throughput of the topology is too low, increase this value.  This allows additional tuples to be enriched in parallel.

* Increasing parallelism on the enrichment bolt will at some point put pressure on the downstream threat intel and output bolts.  As this value is increased, monitor the capacity of the downstream bolts to ensure that they do not become a bottleneck.

#### `threat.intel.join.parallelism`

The parallelism hint for the threat intel bolt.  Defines the maximum number of executors within each worker dedicated to running the threat intel bolt.

WARNING: The property name does not match its current usage in the Unified topology.  This property name may change in the near future as it has been reused from the Split-Join topology.  

* If the capacity of the threat intel bolt is high, increasing the parallelism will introduce additional executors to bring the bolt capacity down.

* If the throughput of the topology is too low, increase this value.  This allows additional tuples to be enriched in parallel.

* Increasing parallelism on this bolt will at some point put pressure on the downstream output bolt.  As this value is increased, monitor the capacity of the output bolt to ensure that it does not become a bottleneck.

#### `kafka.writer.parallelism`

The parallelism hint for the output bolt which writes to the output Kafka topic.  Defines the maximum number of executors within each worker dedicated to running the output bolt.

* If the capacity of the output bolt is high, increasing the parallelism will introduce additional executors to bring the bolt capacity down.

#### `enrichment.join.cache.size`

The Enrichment bolt maintains a cache so that if the same enrichment occurs repetitively, the value can be retrieved from the cache instead of it being recomputed.  

There is a great deal of repetition in network telemetry, which leads to a great deal of repetition for the enrichments that operate on that telemetry.  Having a highly performant cache is one of the most critical factors driving performance.

WARNING: The property name does not match its current usage in the Unified topology.  This property name may change in the near future as it has been reused from the Split-Join topology.  

* Increase the size of the cache to improve the rate of cache hits.

* Increasing the size of the cache may require that you increase the worker heap size using `topology.worker.childopts'.  

#### `threat.intel.join.cache.size`

The Threat Intel bolt maintains a cache so that if the same enrichment occurs repetitively, the value can be retrieved from the cache instead of it being recomputed.  

There is a great deal of repetition in network telemetry, which leads to a great deal of repetition for the enrichments that operate on that telemetry.  Having a highly performant cache is one of the most critical factors driving performance.

WARNING: The property name does not match its current usage in the Unified topology.  This property name may change in the near future as it has been reused from the Split-Join topology.  

* Increase the size of the cache to improve the rate of cache hits.

* Increasing the size of the cache may require that you increase the worker heap size using `topology.worker.childopts'.  

#### `metron.threadpool.size`

This value defines the number of threads maintained within a pool to execute each enrichment.  This value can either be a fixed number or it can be a multiple of the number of cores (5C = 5 times the number of cores).

The enrichment bolt maintains a static thread pool that is used to execute each enrichment.  This thread pool is shared by all of the executors running within the same worker.  

WARNING: This value must be manually defined within the flux file at `$METRON_HOME/flux/enrichment/remote-unified.yaml`.  This value cannot be altered within Ambari at this time.

* Start with a thread pool size of 1.  Adjust this value after tuning all other parameters first.  Only increase this value if testing shows performance improvements in your environment given your workload.  

* If the thread pool size is too large this will cause the work to be shuffled amongst multiple CPU cores, which significantly decreases performance.  Using a smaller thread pool helps pin work to a single core.

* If the thread pool size is too small this can negatively impact IO-intensive workloads.  Increasing the thread pool size, helps when using IO-intensive workloads with a significant cache miss rate.   A thread pool size of 3-5 can help in these cases.

* Most workloads will make significant use of the cache and so 1-2 threads will most likely be optimal.

* The bolt uses a static thread pool.  To scale out, but keep the work mostly pinned to a CPU core, add more Storm workers while keeping the thread pool size low.

* If a larger thread pool increases load on the system, but decreases the throughput, then it is likely that the system is thrashing.  In this case the thread pool size should be decreased.

#### `metron.threadpool.type`

The enrichment bolt maintains a static thread pool that is used to execute each enrichment.  This thread pool is shared by all of the executors running within the same worker.  

Defines the type of thread pool used.  This value can be either "FIXED" or "WORK_STEALING".

Currently, this value must be manually defined within the flux file at `$METRON_HOME/flux/enrichment/remote-unified.yaml`.  This value cannot be altered within Ambari.

### Benchmark Results

This section describes one execution of these benchmarks to help provide an understanding of what reasonably tuned parameters might look-like.  

These parameters and the throughput reported are highly dependent on the workload and resources available. The throughput is what was achievable given a reasonable amount of tuning on a small, dedicated cluster.  The throughput is largely dependent on the enrichments performed and the distribution of data within the incoming telemetry.

The Enrichment topology has been show to scale relatively linearly.  Adding more resources allows for more complex enrichments, across more diverse data sets, at higher volumes.  The throughput that one might see in production largely depends on how much hardware can be committed to the task.  

#### Environment

* Apache Metron 0.5.0 (pre-release) March, 2018
	* This included [a patch to the underlying caching mechanism](https://github.com/apache/metron/pull/947) that greatly improves performance.

* Cisco UCS nodes
 	* 32 core, 64-bit CPU (Intel(R) Xeon(R) CPU E5-2630 v3 @ 2.40GHz)
	* 256 GB RAM
	* x2 10G NIC bonded
	* x4 6TB 7200 RPM disks

* Storm Supervisors are isolated and running on a dedicated set of 3 nodes.

* Kafka Brokers are isolated and running on a separate, dedicated set of 3 nodes.

#### Results

* These benchmarks executed all 3 enrichments simultaneously; the [Geo IP Enrichment](#geo-ip-enrichment), [Stellar Enrichment](#stellar-enrichment) and the [HBase Enrichment](#hbase-enrichment).

* The data used to drive the benchmark includes 10,000 unique IP addresses.  The telemetry was populated with IP addresses such that 10% of these IPs were chosen 80% of the time.  This bias was designed to mimic the typical distribution seen in real-world telemetry.

* The Unified Enrichment topology was able to sustain 308,000 events per second on a small, dedicated 3 node cluster.

* The values used to achieve these results with the Unified Enrichment topology follows.  You should not attempt to use these parameters in your topology directly.  These are specific to the environment and workload and should only be used as a guideline.
	```
	enrichment.workers=9
	enrichment.acker.executors=9
	enrichment.join.cache.size=100000
  	threat.intel.join.cache.size=100000
	kafka.spout.parallelism=27
	enrichment.join.parallelism=54
	threat.intel.join.parallelism=9
	kafka.writer.parallelism=27
	topology.worker.childopts=-XX:+UseG1GC -Xms8g -Xmx8g -XX:MaxGCPauseMillis=100
	topology.max.spout.pending=3000
	metron.threadpool.size=1
	metron.threadpool.type=FIXED
	```
