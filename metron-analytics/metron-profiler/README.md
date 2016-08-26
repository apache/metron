# Metron Profiler

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity on a network.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior. 

This is achieved by summarizing the streaming telemetry data consumed by Metron over sliding windows. A summary statistic is applied to the data received within a given window.  Collecting this summary across many windows results in a time series that is useful for analysis.
 
## Usage

Any field contained within a message can be used to generate a profile.  A profile can even be produced from combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language. A user only need configure the desired profiles in Zookeeper and ensure that the Profiler topology is running.

### Configuration

The Profiler configuration requires a JSON-formatted set of elements, many of which can contain Stellar code.  The configuration contains the following elements.

| Name 	    |            	| Description 	|
|---	    |---	        |---	        |
| profile  	| Required   	| A unique name identifying the profile.  The field is treated as a string. |
| foreach  	| Required  	| A separate profile is maintained *for each* of these.  This is effectively the entity that the profile is describing.  The field is expected to contain a Stellar expression whose result is the entity name.  For example, if `ip_src_addr` then a separate profile would be maintained for each unique IP source address in the data; 10.0.0.1, 10.0.0.2, etc. | 
| onlyif  	| Optional  	| An expression that determines if a message should be applied to the profile.  A Stellar expression is expected that when executed returns a boolean.  A message is only applied to a profile if this condition is true. This allows a profile to filter the messages that it receives. |
| init  	| Optional  	| A set of expressions that is executed at the start of a window period.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can contain 0 or more variables/expressions. At the start of each window period the expression is executed once and stored in a variable with the given name. |
| update  	| Required  	| A set of expressions that is executed when a message is applied to the profile.  A map is expected where the key is the variable name and the value is a Stellar expression.  The map can include 0 or more variables/expressions.  	    |
| result  	| Required  	| A Stellar expression that is executed when the window period expires.  The expression is expected to in some way summarize the messages that were applied to the profile over the window period.  The expression must result in a numeric value such as a Double, Long, Float, Short, or Integer.  	    |

### Examples

Examples of the types of profiles that can be built include the following.  Each shows the configuration that would be required to produce the profile.  These examples assume a fictitious input messages that looks something like the following.

```
{
  "ip_src_addr": "10.0.0.1",
  "protocol": "HTTPS",
  "length": "10",
  "bytes_in": "234"
},
{
  "ip_src_addr": "10.0.0.2",
  "protocol": "HTTP",
  "length": "20",
  "bytes_in": "390"
},
{
  "ip_src_addr": "10.0.0.3",
  "protocol": "DNS",
  "length": "30",
  "bytes_in": "560"
}
```


#### Example 1

The total number of bytes of HTTP data for each host. The following configuration would be used to generate this profile.

```
{
  "inputTopic": "indexing",
  "profiles": [
    {
      "profile": "example1",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "init": {
        "total_bytes": 0.0
      },
      "update": {
        "total_bytes": "total_bytes + bytes_in"
      },
      "result": "total_bytes"
    }
  ]
}
```

This creates a profile...
 * Named ‘example1’
 * That for each IP source address
 * Only if the 'protocol' field equals 'HTTP'
 * Initializes a counter ‘total_bytes’ to zero
 * Adds to ‘total_bytes’ the value of the message's ‘bytes_in’ field
 * Returns ‘total_bytes’ as the result

#### Example 2

The ratio of DNS traffic to HTTP traffic for each host. The following configuration would be used to generate this profile.

```
{
  "inputTopic": "indexing",
  "profiles": [
    {
      "profile": "example2",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'DNS' or protocol == 'HTTP'",
      "init": {
        "num_dns": 1.0,
        "num_http": 1.0
      },
      "update": {
        "num_dns": "num_dns + (if protocol == 'DNS' then 1 else 0)",
        "num_http": "num_http + (if protocol == 'HTTP' then 1 else 0)"
      },
      "result": "num_dns / num_http"
    }
  ]
}
```

This creates a profile...
 * Named ‘example2’
 * That for each IP source address
 * Only if the 'protocol' field equals 'HTTP' or 'DNS'
 * Accumulates the number of DNS requests 
 * Accumulates the number of HTTP requests
 * Returns the ratio of these as the result

#### Example 3

The average of the `length` field of HTTP traffic. The following configuration would be used to generate this profile.

```
{
  "inputTopic": "indexing",
  "profiles": [
    {
      "profile": "example3",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "update": { "s": "STATS_ADD(s, length)" },
      "result": "STATS_MEAN(s)"
    }
  ]
}
```

This creates a profile...
 * Named ‘example3’
 * That for each IP source address
 * Only if the 'protocol' field is 'HTTP'
 * Adds the `length` field from each message
 * Calculates the average as the result

### Topology Configuration

The Profiler topology also accepts the following configuration settings.

| Setting   | Description   |
|---        |---            |
| profiler.workers | The number of worker processes to create for the topology.   |
| profiler.executors | The number of executors to spawn per component.  |
| profiler.input.topic | The name of the Kafka topic from which to consume data.  |
| profiler.flush.interval.seconds | The duration of a profile's sliding window before it is flushed. |
| profiler.hbase.salt.divisor  |  A salt is prepended to the row key to help prevent hotspotting.  This constant is used to generate the salt.  Ideally, this constant should be roughly equal to the number of nodes in the Hbase cluster.  |
| profiler.hbase.table | The name of the HBase table that profiles are written to.  |
| profiler.hbase.batch | The number of puts that are written in a single batch.  |
| profiler.hbase.flush.interval.seconds | The maximum number of seconds between batch writes to HBase. |

## Getting Started

This section will describe the steps required to get your first profile running.

1. Launch the 'Quick Dev' environment.
    ```
    $ cd metron-deployment/vagrant/quick-dev-platform/
    $ ./run.sh
    ```

2. After the environment has been deployed, then login to the host.
    ```
    $ vagrant ssh
    $ sudo su -
    $ cd /usr/metron/0.2.0BETA/
    ```

3. Create a table within HBase that will store the profile data. The table name and column family must match the Profiler topology configuration stored at `/usr/metron/0.2.0BETA/config/profiler.properties`.
    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> create 'profiler', 'P'
    ```

4. Shorten the flush intervals to more immediately see results.  Edit the Profiler topology properties located at `/usr/metron/0.2.0BETA/config/profiler.properties`.  Alter the following two properties.
    ```
    profiler.flush.interval.seconds=30
    profiler.hbase.flush.interval.seconds=30
    ```

5. Create the Profiler definition in a file located at `/usr/metron/0.2.0BETA/config/zookeeper/profiler.json`.  The following JSON will create a profile that simply counts the number of messages.
    ```
    {
      "inputTopic": "indexing",
      "profiles": [
        {
          "profile": "test",
          "foreach": "ip_src_addr",
          "onlyif":  "true",
          "init":    { "sum": 0 },
          "update":  { "sum": "sum + 1" },
          "result":  "sum"
        }
      ]
    }
    ```

6. Upload the Profiler definition to Zookeeper.
    ```
    $ bin/zk_load_configs.sh -m PUSH -i config/zookeeper/ -z node1:2181
    ```

7. Start the Profiler topology.
    ```
    bin/start_profiler_topology.sh
    ```

8. Ensure that test messages are being sent to the Profiler's input topic in Kafka.  The Profiler will consume messages from the `inputTopic` in the Profiler definition.

9. Check the HBase table to validate that the Profiler is working. 
    ```
    $ /usr/hdp/current/hbase-client/bin/hbase shell
    hbase(main):001:0> count 'profiler'
    ``` 

## Implementation

## Topology

The Profiler is implemented as a Storm topology using the following bolts and spouts.

### KafkaSpout

A spout that consumes messages from a single Kafka topic.  In most cases, the Profiler topology will consume messages from the `indexing` topic.  This topic contains fully enriched messages that are ready to be indexed.  This ensures that profiles can take advantage of all the available data elements.

### ProfileSplitterBolt
 
The bolt responsible for filtering incoming messages and directing each to the one or more downstream bolts that are responsible for building a Profile.  Each message may be needed by 0, 1 or even many Profiles.  Each emitted tuple contains the 'resolved' entity name, the profile definition, and the input message.

### ProfileBuilderBolt

This bolt maintains all of the state required to build a Profile.  When the window period expires, the data is summarized as a ProfileMeasurement, all state is flushed, and the ProfileMeasurement is emitted.  Each instance of this bolt is responsible for maintaining the state for a single Profile-Entity pair.

### HBaseBolt

A bolt that is responsible for writing to HBase.  Most profiles will be flushed every 15 minutes or so.  If each ProfileBuilderBolt were responsible for writing to HBase itself, there would be little to no opportunity to optimize these writes.  By aggregating the writes from multiple Profile-Entity pairs these writes can be batched, for example.

