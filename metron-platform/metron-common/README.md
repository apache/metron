#Stellar Language

For a variety of components (threat intelligence triage and field
transformations) we have the need to do simple computation and
transformation using the data from messages as variables.  
For those purposes, there exists a simple, scaled down DSL 
created to do simple computation and transformation.

The query language supports the following:
* Referencing fields in the enriched JSON
* Simple boolean operations: `and`, `not`, `or`
* Simple arithmetic operations: `*`, `/`, `+`, `-` on real numbers or integers
* Simple comparison operations `<`, `>`, `<=`, `>=`
* if/then/else comparisons (i.e. `if var1 < 10 then 'less than 10' else '10 or more'`)
* Determining whether a field exists (via `exists`)
* The ability to have parenthesis to make order of operations explicit
* User defined functions
<table>
<tr>
<th>Stellar Function</th><th>Description</th><th>Input</th><th>Returns</th>
</tr>
<tr><td>IS_DATE</td><td>Determines if a string passed is a date of a given format.</td><td><ul><li>date - The date in string form.</li><li>format - The format of the date.</li></ul></td><td>True if the date is of the specified format and false otherwise.</td></tr>
<tr><td>MONTH</td><td>The number representing the month.  The first month, January, has a value of 0.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The current month (0-based).</td></tr>
<tr><td>IS_DOMAIN</td><td>Tests if a string is a valid domain.  Domain names are evaluated according to the standards RFC1034 section 3, and RFC1123 section 2.1.</td><td><ul><li>address - The String to test</li></ul></td><td>True if the string is a valid domain and false otherwise.</td></tr>
<tr><td>TRIM</td><td>Trims whitespace from both sides of a string.</td><td><ul><li>input - String</li></ul></td><td>String</td></tr>
<tr><td>WEEK_OF_MONTH</td><td>The numbered week within the month.  The first week within the month has a value of 1.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The numbered week within the month.</td></tr>
<tr><td>JOIN</td><td>Joins the components of the list with the specified delimiter.</td><td><ul><li>list - List of Strings</li><li>delim - String delimiter</li></ul></td><td>String</td></tr>
<tr><td>MAP_GET</td><td>Gets the value associated with a key from a map</td><td><ul><li>key - The key</li><li>map - The map</li><li>default - Optionally the default value to return if the key is not in the map.</li></ul></td><td>The object associated with key in the map.  If there is no value associated, then default if specified and null if a default is not specified.</td></tr>
<tr><td>TO_INTEGER</td><td>Transforms the first argument to an integer</td><td><ul><li>input - Object of string or numeric type</li></ul></td><td>Integer</td></tr>
<tr><td>YEAR</td><td>The number representing the year. </td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The current year</td></tr>
<tr><td>WEEK_OF_YEAR</td><td>The numbered week within the year.  The first week in the year has a value of 1.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The numbered week within the year.</td></tr>
<tr><td>PROTOCOL_TO_NAME</td><td>Convert the IANA protocol number to the protocol name</td><td><ul><li>IANA Number</li></ul></td><td>The protocol name associated with the IANA number.</td></tr>
<tr><td>ENDS_WITH</td><td>Determines whether a string ends with a prefix</td><td><ul><li>string - The string to test</li><li>suffix - The proposed suffix</li></ul></td><td>True if the string ends with the specified suffix and false otherwise.</td></tr>
<tr><td>GET_FIRST</td><td>Returns the first element of the list</td><td><ul><li>input - List</li></ul></td><td>First element of the list</td></tr>
<tr><td>STATS_MAX</td><td>Calculates the max of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The max of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>DOMAIN_TO_TLD</td><td>Extract the top level domain from a domain</td><td><ul><li>domain - fully qualified domain name</li></ul></td><td>The TLD of the domain.  e.g. DOMAIN_TO_TLD('mail.yahoo.co.uk') yields 'co.uk'</td></tr>
<tr><td>TO_STRING</td><td>Transforms the first argument to a string</td><td><ul><li>input - Object</li></ul></td><td>String</td></tr>
<tr><td>DOMAIN_REMOVE_SUBDOMAINS</td><td>Remove subdomains from a domain.</td><td><ul><li>domain - fully qualified domain name</li></ul></td><td>The domain without the subdomains.  e.g. DOMAIN_REMOVE_SUBDOMAINS('mail.yahoo.com') yields 'yahoo.com'</td></tr>
<tr><td>STARTS_WITH</td><td>Determines whether a string starts with a prefix</td><td><ul><li>string - The string to test</li><li>prefix - The proposed prefix</li></ul></td><td>True if the string starts with the specified prefix and false otherwise.</td></tr>
<tr><td>BLOOM_MERGE</td><td>Returns a merged bloom filter</td><td><ul><li>bloomfilters - A list of bloom filters to merge</li></ul></td><td>Bloom Filter or null if the list is empty</td></tr>
<tr><td>STATS_KURTOSIS</td><td>Calculates the kurtosis of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The kurtosis of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>DOMAIN_REMOVE_TLD</td><td>Remove top level domain suffix from a domain.</td><td><ul><li>domain - fully qualified domain name</li></ul></td><td>The domain without the TLD.  e.g. DOMAIN_REMOVE_TLD('mail.yahoo.co.uk') yields 'mail.yahoo'</td></tr>
<tr><td>STATS_SUM_SQUARES</td><td>Calculates the sum of the squares of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The sum of the squares of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>STATS_INIT</td><td>Initialize a Statistics object</td><td><ul><li>window_size - The number of input data values to maintain in a rolling window in memory.  If equal to 0, then no rolling window is maintained. Using no rolling window is less memory intensive, but cannot calculate certain statistics like percentiles and kurtosis.</li></ul></td><td>A StatisticsProvider object</td></tr>
<tr><td>STATS_SD</td><td>Calculates the standard deviation of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The standard deviation of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>GET</td><td>Returns the i'th element of the list </td><td><ul><li>input - List</li><li>i - the index (0-based)</li></ul></td><td>First element of the list</td></tr>
<tr><td>STATS_COUNT</td><td>Calculates the count of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The count of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>IS_INTEGER</td><td>Determine if an object is an integer or not.</td><td><ul><li>x - An object which we wish to test is an integer</li></ul></td><td>True if the object can be converted to an integer and false otherwise.</td></tr>
<tr><td>DAY_OF_WEEK</td><td>The numbered day within the week.  The first day of the week, Sunday, has a value of 1.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The numbered day within the week.</td></tr>
<tr><td>IS_EMPTY</td><td>Returns true if string or collection is empty and false otherwise</td><td><ul><li>input - Object of string or collection type (e.g. list)</li></ul></td><td>Boolean</td></tr>
<tr><td>IS_EMAIL</td><td>Tests if a string is a valid email address</td><td><ul><li>address - The String to test</li></ul></td><td>True if the string is a valid email address and false otherwise.</td></tr>
<tr><td>MAP_EXISTS</td><td>Checks for existence of a key in a map.</td><td><ul><li>key - The key to check for existence</li><li>map - The map to check for existence of the key</li></ul></td><td>True if the key is found in the map and false otherwise.</td></tr>
<tr><td>DAY_OF_YEAR</td><td>The day number within the year.  The first day of the year has value of 1.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The day number within the year.</td></tr>
<tr><td>REGEXP_MATCH</td><td>Determines whether a regex matches a string</td><td><ul><li>string - The string to test</li><li>pattern - The proposed regex pattern</li></ul></td><td>True if the regex pattern matches the string and false otherwise.</td></tr>
<tr><td>TO_LOWER</td><td>Transforms the first argument to a lowercase string</td><td><ul><li>input - String</li></ul></td><td>String</td></tr>
<tr><td>STATS_SKEWNESS</td><td>Calculates the skewness of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The skewness of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>URL_TO_PORT</td><td>Extract the port from a URL.  If the port is not explicitly stated in the URL, then an implicit port is inferred based on the protocol.</td><td><ul><li>url - URL in String form</li></ul></td><td>The port used in the URL as an Integer.  e.g. URL_TO_PORT('http://www.yahoo.com/foo') would yield 80</td></tr>
<tr><td>DAY_OF_MONTH</td><td>The numbered day within the month.  The first day within the month has a value of 1.</td><td><ul><li>dateTime - The datetime as a long representing the milliseconds since unix epoch</li></ul></td><td>The numbered day within the month.</td></tr>
<tr><td>GET_LAST</td><td>Returns the last element of the list</td><td><ul><li>input - List</li></ul></td><td>Last element of the list</td></tr>
<tr><td>IN_SUBNET</td><td>Returns if an IP is within a subnet range.</td><td><ul><li>ip - the IP address in String form</li><li>cidr+ - one or more IP ranges specified in CIDR notation (e.g. 192.168.0.0/24)</li></ul></td><td>True if the IP address is within at least one of the network ranges and false otherwise</td></tr>
<tr><td>SPLIT</td><td>Splits the string by the delimiter.</td><td><ul><li>input - String to split</li><li>delim - String delimiter</li></ul></td><td>List of Strings</td></tr>
<tr><td>MAAS_MODEL_APPLY</td><td>Returns the output of a model deployed via model which is deployed at endpoint. NOTE: Results are cached at the client for 10 minutes.</td><td><ul><li>endpoint - a map containing name, version, url for the REST endpoint</li><li>function - the optional endpoint path, default is 'apply'</li><li>model_args - dictionary of arguments for the model (these become request params).</li></ul></td><td>The output of the model deployed as a REST endpoint in Map form.  Assumes REST endpoint returns a JSON Map.</td></tr>
<tr><td>STATS_POPULATION_VARIANCE</td><td>Calculates the population variance of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The population variance of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>STATS_VARIANCE</td><td>Calculates the variance of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The variance of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>STATS_ADD</td><td>Add one or more input values to those that are used to calculate the summary statistics.</td><td><ul><li>stats - The Stellar statistics object.  If null, then a new one is initialized.</li><li>value+ - one or more numbers to add </li></ul></td><td>A StatisticsProvider object</td></tr>
<tr><td>TO_UPPER</td><td>Transforms the first argument to an uppercase string</td><td><ul><li>input - String</li></ul></td><td>String</td></tr>
<tr><td>TO_EPOCH_TIMESTAMP</td><td>Returns the epoch timestamp of the dateTime given the format. If the format does not have a timestamp and you wish to assume a given timestamp, you may specify the timezone optionally.</td><td><ul><li>dateTime - DateTime in String format</li><li>format - DateTime format as a String</li><li>timezone - Optional timezone in String format</li></ul></td><td>Boolean</td></tr>
<tr><td>MAAS_GET_ENDPOINT</td><td>Inspects zookeeper and returns a map containing the name, version and url for the model referred to by the input params</td><td><ul><li>model_name - the name of the model</li><li>model_version - the optional version of the model.  If it is not specified, the most current version is used.</li></ul></td><td>A map containing the name, version, url for the REST endpoint (fields named name, version and url).  Note that the output of this function is suitable for input into the first argument of MAAS_MODEL_APPLY.</td></tr>
<tr><td>BLOOM_EXISTS</td><td>If the bloom filter contains the value</td><td><ul><li>bloom - The bloom filter</li><li>value - The value to check</li></ul></td><td>True if the filter might contain the value and false otherwise</td></tr>
<tr><td>BLOOM_INIT</td><td>Returns an empty bloom filter</td><td><ul><li>expectedInsertions - The expected insertions</li><li>falsePositiveRate - The false positive rate you are willing to tolerate</li></ul></td><td>Bloom Filter</td></tr>
<tr><td>STATS_QUADRATIC_MEAN</td><td>Calculates the quadratic mean of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The quadratic mean of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>BLOOM_ADD</td><td>Adds an element to the bloom filter passed in</td><td><ul><li>bloom - The bloom filter</li><li>value* - The values to add</li></ul></td><td>Bloom Filter</td></tr>
<tr><td>URL_TO_PATH</td><td>Extract the path from a URL.</td><td><ul><li>url - URL in String form</li></ul></td><td>The path from the URL as a String.  e.g. URL_TO_PATH('http://www.yahoo.com/foo') would yield 'foo'</td></tr>
<tr><td>STATS_GEOMETRIC_MEAN</td><td>Calculates the geometric mean of the values accumulated (or in the window if a window is used). See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The geometric mean of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>URL_TO_PROTOCOL</td><td>Extract the protocol from a URL.</td><td><ul><li>url - URL in String form</li></ul></td><td>The protocol from the URL as a String. e.g. URL_TO_PROTOCOL('http://www.yahoo.com/foo') would yield 'http'</td></tr>
<tr><td>STATS_MIN</td><td>Calculates the min of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The min of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>URL_TO_HOST</td><td>Extract the hostname from a URL.</td><td><ul><li>url - URL in String form</li></ul></td><td>The hostname from the URL as a String.  e.g. URL_TO_HOST('http://www.yahoo.com/foo') would yield 'www.yahoo.com'</td></tr>
<tr><td>STATS_SUM_LOGS</td><td>Calculates the sum of the (natural) log of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics </td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The sum of the (natural) log of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>IS_URL</td><td>Tests if a string is a valid URL</td><td><ul><li>url - The String to test</li></ul></td><td>True if the string is a valid URL and false otherwise.</td></tr>
<tr><td>STATS_SUM</td><td>Calculates the sum of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The sum of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>IS_IP</td><td>Determine if an string is an IP or not.</td><td><ul><li>ip - An object which we wish to test is an ip</li><li>type (optional) - one of IPV4 or IPV6.  The default is IPV4.</li></ul></td><td>True if the string is an IP and false otherwise.</td></tr>
<tr><td>STATS_MEAN</td><td>Calculates the mean of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li></ul></td><td>The mean of the values in the window or NaN if the statistics object is null.</td></tr>
<tr><td>STATS_MERGE</td><td>Merge statistic providers</td><td><ul><li>statisticsProviders - A list of statistics providers</li></ul></td><td>A StatisticsProvider object</td></tr>
<tr><td>STATS_PERCENTILE</td><td>Computes the p'th percentile of the values accumulated (or in the window if a window is used).</td><td><ul><li>stats - The Stellar statistics object.</li><li>p - a double where 0 <= p < 1 representing the percentile</li></ul></td><td>The p'th percentile of the data or NaN if the statistics object is null</td></tr>
<tr><td>TO_DOUBLE</td><td>Transforms the first argument to a double precision number</td><td><ul><li>input - Object of string or numeric type</li></ul></td><td>Double</td></tr>
</table>

The following is an example query (i.e. a function which returns a
boolean) which would be seen possibly in threat triage:

`IN_SUBNET( ip, '192.168.0.0/24') or ip in [ '10.0.0.1', '10.0.0.2' ] or exists(is_local)`

This evaluates to true precisely when one of the following is true:
* The value of the `ip` field is in the `192.168.0.0/24` subnet
* The value of the `ip` field is `10.0.0.1` or `10.0.0.2`
* The field `is_local` exists

The following is an example transformation which might be seen in a
field transformation:

`TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', MAP_GET(dc, dc2tz, 'UTC'))`

For a message with a `timestamp` and `dc` field, we want to set the
transform the timestamp to an epoch timestamp given a timezone which we
will lookup in a separate map, called `dc2tz`.

This will convert the timestamp field to an epoch timestamp based on the 
* Format `yyyy-MM-dd HH:mm:ss`
* The value in `dc2tz` associated with the value associated with field
  `dc`, defaulting to `UTC`


#Enrichment Configuration

The configuration for the `enrichment` topology, the topology primarily
responsible for enrichment and threat intelligence enrichment, is
defined by JSON documents stored in zookeeper.

There are two types of configurations at the moment, `global` and
`sensor` specific.  

##Global Configuration
The format of the global enrichment is a JSON String to Object map.  This is intended for
configuration which is non sensor specific configuration.

This configuration is stored in zookeeper, but looks something like

```json
{
  "es.clustername": "metron",
  "es.ip": "node1",
  "es.port": "9300",
  "es.date.format": "yyyy.MM.dd.HH",
  "fieldValidations" : [
              {
                "input" : [ "ip_src_addr", "ip_dst_addr" ],
                "validation" : "IP",
                "config" : {
                    "type" : "IPV4"
                           }
              } 
                       ]
}
```

###Validation Framework

Inside of the global configuration, there is a validation framework in
place that enables the validation that messages coming from all parsers
are valid.  This is done in the form of validation plugins where
assertions about fields or whole messages can be made. 

The format for this is a `fieldValidations` field inside of global
config.  This is associated with an array of field validation objects
structured like so:
* `input` : An array of input fields or a single field.  If this is omitted, then the whole messages is passed to the validator.
* `config` : A String to Object map for validation configuration.  This is optional if the validation function requires no configuration.
* `validation` : The validation function to be used.  This is one of
   * `STELLAR` : Execute a Stellar Language statement.  Expects the query string in the `condition` field of the config.
   * `IP` : Validates that the input fields are an IP address.  By default, if no configuration is set, it assumes `IPV4`, but you can specify the type by passing in the config by passing in `type` with either `IPV6` or `IPV4`.
   * `DOMAIN` : Validates that the fields are all domains.
   * `EMAIL` : Validates that the fields are all email addresses
   * `URL` : Validates that the fields are all URLs
   * `DATE` : Validates that the fields are a date.  Expects `format` in the config.
   * `INTEGER` : Validates that the fields are an integer.  String representation of an integer is allowed.
   * `REGEX_MATCH` : Validates that the fields match a regex.  Expects `pattern` in the config.
   * `NOT_EMPTY` : Validates that the fields exist and are not empty (after trimming.)

##Sensor Enrichment Configuration

The sensor specific configuration is intended to configure the
individual enrichments and threat intelligence enrichments for a given
sensor type (e.g. `snort`).

Just like the global config, the format is a JSON stored in zookeeper.
The configuration is a complex JSON object with the following top level fields:

* `index` : The name of the sensor
* `batchSize` : The size of the batch that is written to the indices at once.
* `enrichment` : A complex JSON object representing the configuration of the enrichments
* `threatIntel` : A complex JSON object representing the configuration of the threat intelligence enrichments

###The `enrichment` Configuration 


| Field            | Description                                                                                                                                                                                                                   | Example                                                          |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. | `"fieldToTypeMap" : { "ip_src_addr" : [ "asset_enrichment" ] }`  |
| `fieldMap`       | The map of enrichment bolts names to configuration handlers which know how to split the message up.  The simplest of which is just a list of fields.  More complex examples would be the stellar enrichment which provides stellar statements.  Each field is sent to the enrichment referenced in the key.                                                                                                 | `"fieldMap": {"hbaseEnrichment": ["ip_src_addr","ip_dst_addr"]}` |
| `config`         | The general configuration for the enrichment                                                                                                                                                                                  | `"config": {"typeToColumnFamily": { "asset_enrichment" : "cf" } }` |

The `config` map is intended to house enrichment specific configuration.
For instance, for the `hbaseEnrichment`, the mappings between the
enrichment types to the column families is specified.

The `fieldMap`contents are of interest because they contain the routing and configuration information for the enrichments.  When we say 'routing', we mean how the messages get split up and sent to the enrichment adapter bolts.  The simplest, by far, is just providing a simple list as in
```
    "fieldMap": {
      "geo": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "host": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "hbaseEnrichment": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
      }
```
For the `geo`, `host` and `hbaseEnrichment`, this is sufficient.  However, more complex enrichments may contain their own configuration.  Currently, the `stellar` enrichment requires a more complex configuration, such as:
```
    "fieldMap": {
       ...
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      }
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
        }
      }
    }
```

Whereas the simpler enrichments just need a set of fields explicitly stated so they can be separated from the message and sent to the enrichment adapter bolt for enrichment and ultimately joined back in the join bolt, the stellar enrichment has its set of required fields implicitly stated through usage.  For instance, if your stellar statement references a field, it should be included and if not, then it should not be included.  We did not want to require users to make explicit the implicit.

The other way in which the stellar enrichment is somewhat more complex is in how the statements are executed.  In the general purpose case for a list of fields, those fields are used to create a message to send to the enrichment adapter bolt and that bolt's worker will handle the fields one by one in serial for a given message.  For stellar enrichment, we wanted to have a more complex design so that users could specify the groups of stellar statements sent to the same worker in the same message (and thus executed sequentially).  Consider the following configuration:
```
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : {
                      "foo": "1 + 1"
                      "bar" : TO_LOWER(source.type)"
                      }
         ,"text" : {
                   "ALL_CAPS" : "TO_UPPER(source.type)"
                   }
        }
      }
    }
```
We have a group called `numeric` whose stellar statements will be executed sequentially.  In parallel to that, we have the group of stellar statements under the group `text` executing.  The intent here is to allow you to not force higher latency operations to be done sequentially.

###The `threatIntel` Configuration 

| Field            | Description                                                                                                                                                                                                                                   | Example                                                                  |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `fieldToTypeMap` | In the case of a simple HBase threat intel enrichment (i.e. a key/value lookup), the mapping between fields and the enrichment types associated with those fields must be known.  This enrichment type is used as part of the HBase key. | `"fieldToTypeMap" : { "ip_src_addr" : [ "malicious_ips" ] }`             |
| `fieldMap`       | The map of threat intel enrichment bolts names to fields in the JSON messages. Each field is sent to the threat intel enrichment bolt referenced in the key.                                                                              | `"fieldMap": {"hbaseThreatIntel": ["ip_src_addr","ip_dst_addr"]}`        |
| `triageConfig`   | The configuration of the threat triage scorer.  In the situation where a threat is detected, a score is assigned to the message and embedded in the indexed message.                                                                    | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `config`         | The general configuration for the Threat Intel                                                                                                                                                                                                | `"config": {"typeToColumnFamily": { "malicious_ips","cf" } }`            |

The `config` map is intended to house threat intel specific configuration.
For instance, for the `hbaseThreatIntel` threat intel adapter, the mappings between the
enrichment types to the column families is specified.

The `triageConfig` field is also a complex field and it bears some description:

| Field            | Description                                                                                                                                             | Example                                                                  |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `riskLevelRules` | The mapping of Stellar (see above) queries to a score.                                                                                                  | `"riskLevelRules" : { "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')" : 10 }` |
| `aggregator`     | An aggregation function that takes all non-zero scores representing the matching queries from `riskLevelRules` and aggregates them into a single score. | `"MAX"`                                                                  |

The supported aggregation functions are:
* `MAX` : The max of all of the associated values for matching queries
* `MIN` : The min of all of the associated values for matching queries
* `MEAN` : The mean of all of the associated values for matching queries
* `POSITIVE_MEAN` : The mean of the positive associated values for the matching queries.

###Example

An example configuration for the YAF sensor is as follows:
```json
{
  "index": "yaf",
  "batchSize": 5,
  "enrichment": {
    "fieldMap": {
      "geo": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "host": [
        "ip_src_addr",
        "ip_dst_addr"
      ],
      "hbaseEnrichment": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    }
  ,"fieldToTypeMap": {
      "ip_src_addr": [
        "playful_classification"
      ],
      "ip_dst_addr": [
        "playful_classification"
      ]
    }
  },
  "threatIntel": {
    "fieldMap": {
      "hbaseThreatIntel": [
        "ip_src_addr",
        "ip_dst_addr"
      ]
    },
    "fieldToTypeMap": {
      "ip_src_addr": [
        "malicious_ip"
      ],
      "ip_dst_addr": [
        "malicious_ip"
      ]
    },
    "triageConfig" : {
      "riskLevelRules" : {
        "ip_src_addr == '10.0.2.3' or ip_dst_addr == '10.0.2.3'" : 10
      },
      "aggregator" : "MAX"
    }
  }
}
```


##Management Utility

Configurations should be stored on disk in the following structure starting at `$BASE_DIR`:
* global.json : The global config
* `sensors` : The subdirectory containing sensor enrichment configuration JSON (e.g. `snort.json`, `bro.json`)

By default, this directory as deployed by the ansible infrastructure is at `$METRON_HOME/config/zookeeper`

While the configs are stored on disk, they must be loaded into Zookeeper to be used.  To this end, there is a
utility program to assist in this called `$METRON_HOME/bin/zk_load_config.sh`

This has the following options:

```
 -f,--force                                Force operation
 -h,--help                                 Generate Help screen
 -i,--input_dir <DIR>                      The input directory containing
                                           the configuration files named
                                           like "$source.json"
 -m,--mode <MODE>                          The mode of operation: DUMP,
                                           PULL, PUSH
 -o,--output_dir <DIR>                     The output directory which will
                                           store the JSON configuration
                                           from Zookeeper
 -z,--zk_quorum <host:port,[host:port]*>   Zookeeper Quorum URL
                                           (zk1:port,zk2:port,...)
```

Usage examples:

* To dump the existing configs from zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m DUMP`
* To push the configs into zookeeper on the singlenode vagrant machine: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PUSH -i $METRON_HOME/config/zookeeper`
* To pull the configs from zookeeper to the singlenode vagrant machine disk: `$METRON_HOME/bin/zk_load_configs.sh -z node1:2181 -m PULL -o $METRON_HOME/config/zookeeper -f`
