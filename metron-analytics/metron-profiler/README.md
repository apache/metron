# Metron Profiler

The Profiler is a feature extraction mechanism that can generate a profile describing the behavior of an entity on a network.  An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior. 

This is achieved by summarizing the streaming telemetry data consumed by Metron over sliding windows. A summary statistic is applied to the data received within a given window.  Collecting this summary across many windows results in a time series that is useful for analysis.
 
## Usage

Any field contained within a message can be used to generate a profile.  A profile can even be produced from combining fields that originate in different data sources.  A user has considerable power to transform the data used in a profile by leveraging the Stellar language. A user only need configure the desired profiles in Zookeeper and ensure that the Profiler topology is running.

### Configuration

The Profiler configuration requires a JSON-formatted set of elements, many of which can contain Stellar code.  The configuration contains the following elements.

* `profile` A unique name identifying the profile.
* `foreach` A separate profile is maintained for each of these.  This is effectively the entity that the profile is describing.  For example, if `ip_src_addr` then a separate profile would be maintained for each unique IP source address.
* `onlyif` A message is only applied to a profile if this condition is true. This allows the incoming messages to be filtered.
* `init` A set of variables and Stellar code that describes how they are initialized at the beginning of each window period.
* `update` A set of variables along with Stellar code that describes how those variables are updated by each message.
* `result` Stellar code that is executed at the end of a window period.  This field must result in a Long that becomes part of the profile.

### Examples

Examples of the types of profiles that can be collected include the following.  Each shows the configuration that would be required to produce the profile.

### Example 1

The total number of bytes received for each host. The following configuration would be used to generate this profile.

```
{ "profiler": [
  {
    "profile": "sum_bytes_in",
    "foreach": "ip_src_addr",
    "onlyif":  "EXISTS(is_local)",
    “init”:    { “sum”: 0 },
    "update":  { "sum": "sum + bytes_in" },
    "result":  "sum"
  }
]}
```

This creates a profile...
 * Named ‘sum_bytes_in’
 * That for each IP source address
 * Only if it is on the local network
 * Initializes a counter ‘sum’ to 0
 * Updates ‘sum’ by adding the value of ‘bytes_in’ from each message
 * After the window expires, ‘sum’ becomes the result

### Example 2

The ratio of DNS traffic to HTTP traffic for each host. The following configuration would be used to generate this profile.

```
{ "profiler": [
  {
    "profile": "ratio_dns_to_http",
    "foreach": "ip_src_addr",
    "onlyif": "protocol == 'DNS' or protocol == ‘HTTP’
    “init”: {
      “num_dns”: 0,
      “num_http”: 0
    },
    "update": {
      "num_dns": "num_dns + IF_THEN_ELSE(protocol == ‘DNS’, 1, 0)",
      "num_http": "num_http + IF_THEN_ELSE(protocol == ‘HTTP’, 1, 0)"
    },
    "result": "num_dns / num_http"
  }
]}
```

This creates a profile...
 * Named ‘ratio_dns_to_http’
 * That for each IP source address
 * Only if the message is either DNS or HTTP
 * Accumulates the number of DNS requests 
 * Accumulates the number of HTTP requests
 * After the window expires, the ratio of these is the result

### Example 3

The average response body length of HTTP traffic. The following configuration would be used to generate this profile.

```
{ "profiler": [
    {
      "profile": "http_mean_resp_body_len",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      “init”: {
        “sum”: 0,
        “cnt”: 0
      },
      "update": {
        "sum": "sum + resp_body_len",
        "cnt": "cnt + 1"
      },
      "result": "sum / cnt",
    }
 ]}
```

This creates a profile...
 * Named ‘http_mean_resp_body_len’
 * That for each IP source address
 * That is either HTTP or HTTPS
 * Accumulates the sum of response body length
 * Accumulates the count of messages
 * After the window period expires, the average is calculated

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
    hbase(main):001:0> create 'profiler1', 'cfProfile'
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
          "update":  { "sum": "ADD(sum,1)" },
          "result":  "TO_LONG(sum)"
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
    hbase(main):001:0> count 'profiler1'
    ``` 

## Design

The Profiler is implemented as a Storm topology using the following bolts and spouts.

### KafkaSpout

A spout that consumes messages from a single Kafka topic.  In most cases, the Profiler topology will consume messages from the `indexing` topic.  This topic contains fully enriched messages that are ready to be indexed.  This ensures that profiles can take advantage of all the available data elements.

### ProfileSplitterBolt
 
The bolt responsible for filtering incoming messages and directing each to the one or more downstream bolts that are responsible for building a Profile.  Each message may be needed by 0, 1 or even many Profiles.  Each emitted tuple contains the 'resolved' entity name, the profile definition, and the input message.

### ProfileBuilderBolt

This bolt maintains all of the state required to build a Profile.  When the window period expires, the data is summarized as a ProfileMeasurement, all state is flushed, and the ProfileMeasurement is emitted.  Each instance of this bolt is responsible for maintaining the state for a single Profile-Entity pair.

### HBaseBolt

A bolt that is responsible for writing to HBase.  Most profiles will be flushed every 15 minutes or so.  If each ProfileBuilderBolt were responsible for writing to HBase itself, there would be little to no opportunity to optimize these writes.  By aggregating the writes from multiple Profile-Entity pairs these writes can be batched, for example.
