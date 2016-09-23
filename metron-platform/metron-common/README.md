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

The following functions are supported:

* `BLOOM_ADD`
  * Description: Adds an element to the bloom filter passed in
  * Input:
    * bloom - The bloom filter
    * value* - The values to add
  * Returns: Bloom Filter
* `BLOOM_EXISTS`
  * Description: If the bloom filter contains the value
  * Input:
    * bloom - The bloom filter
    * value - The value to check
  * Returns: True if the filter might contain the value and false otherwise
* `BLOOM_INIT`
  * Description: Returns an empty bloom filter
  * Input:
    * expectedInsertions - The expected insertions
    * falsePositiveRate - The false positive rate you are willing to tolerate
  * Returns: Bloom Filter
* `BLOOM_MERGE`
  * Description: Returns a merged bloom filter
  * Input:
    * bloomfilters - A list of bloom filters to merge
  * Returns: Bloom Filter or null if the list is empty
* `DAY_OF_MONTH`
  * Description: The numbered day within the month.  The first day within the month has a value of 1.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The numbered day within the month.
* `DAY_OF_WEEK`
  * Description: The numbered day within the week.  The first day of the week, Sunday, has a value of 1.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The numbered day within the week.
* `DAY_OF_YEAR`
  * Description: The day number within the year.  The first day of the year has value of 1.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The day number within the year.
* `DOMAIN_REMOVE_SUBDOMAINS`
  * Description: Remove subdomains from a domain.
  * Input:
    * domain - fully qualified domain name
  * Returns: The domain without the subdomains.  e.g. DOMAIN_REMOVE_SUBDOMAINS('mail.yahoo.com') yields 'yahoo.com'
* `DOMAIN_REMOVE_TLD`
  * Description: Remove top level domain suffix from a domain.
  * Input:
    * domain - fully qualified domain name
  * Returns: The domain without the TLD.  e.g. DOMAIN_REMOVE_TLD('mail.yahoo.co.uk') yields 'mail.yahoo'
* `DOMAIN_TO_TLD`
  * Description: Extract the top level domain from a domain
  * Input:
    * domain - fully qualified domain name
  * Returns: The TLD of the domain.  e.g. DOMAIN_TO_TLD('mail.yahoo.co.uk') yields 'co.uk'
* `ENDS_WITH`
  * Description: Determines whether a string ends with a prefix
  * Input:
    * string - The string to test
    * suffix - The proposed suffix
  * Returns: True if the string ends with the specified suffix and false otherwise.
* `ENRICHMENT_EXISTS`
  * Description: Interrogates the HBase table holding the simple hbase enrichment data and returns whether the enrichment type and indicator are in the table.
  * Input:
    * enrichment_type - The enrichment type
    * indicator - The string indicator to look up
    * nosql_table - The NoSQL Table to use
    * column_family - The Column Family to use
  * Returns: True if the enrichment indicator exists and false otherwise
* `ENRICHMENT_GET`
  * Description: Interrogates the HBase table holding the simple hbase enrichment data and retrieves the tabular value associated with the enrichment type and indicator.
  * Input:
    * enrichment_type - The enrichment type
    * indicator - The string indicator to look up
    * nosql_table - The NoSQL Table to use
    * column_family - The Column Family to use
  * Returns: A Map associated with the indicator and enrichment type.  Empty otherwise.
* `GET`
  * Description: Returns the i'th element of the list 
  * Input:
    * input - List
    * i - the index (0-based)
  * Returns: First element of the list
* `GET_FIRST`
  * Description: Returns the first element of the list
  * Input:
    * input - List
  * Returns: First element of the list
* `GET_LAST`
  * Description: Returns the last element of the list
  * Input:
    * input - List
  * Returns: Last element of the list
* `IN_SUBNET`
  * Description: Returns if an IP is within a subnet range.
  * Input:
    * ip - the IP address in String form
    * cidr+ - one or more IP ranges specified in CIDR notation (e.g. 192.168.0.0/24)
  * Returns: True if the IP address is within at least one of the network ranges and false otherwise
* `IS_DATE`
  * Description: Determines if a string passed is a date of a given format.
  * Input:
    * date - The date in string form.
    * format - The format of the date.
  * Returns: True if the date is of the specified format and false otherwise.
* `IS_DOMAIN`
  * Description: Tests if a string is a valid domain.  Domain names are evaluated according to the standards RFC1034 section 3, and RFC1123 section 2.1.
  * Input:
    * address - The String to test
  * Returns: True if the string is a valid domain and false otherwise.
* `IS_EMAIL`
  * Description: Tests if a string is a valid email address
  * Input:
    * address - The String to test
  * Returns: True if the string is a valid email address and false otherwise.
* `IS_EMPTY`
  * Description: Returns true if string or collection is empty and false otherwise
  * Input:
    * input - Object of string or collection type (e.g. list)
  * Returns: Boolean
* `IS_INTEGER`
  * Description: Determine if an object is an integer or not.
  * Input:
    * x - An object which we wish to test is an integer
  * Returns: True if the object can be converted to an integer and false otherwise.
* `IS_IP`
  * Description: Determine if an string is an IP or not.
  * Input:
    * ip - An object which we wish to test is an ip
    * type (optional) - one of IPV4 or IPV6.  The default is IPV4.
  * Returns: True if the string is an IP and false otherwise.
* `IS_URL`
  * Description: Tests if a string is a valid URL
  * Input:
    * url - The String to test
  * Returns: True if the string is a valid URL and false otherwise.
* `JOIN`
  * Description: Joins the components of the list with the specified delimiter.
  * Input:
    * list - List of Strings
    * delim - String delimiter
  * Returns: String
* `MAAS_GET_ENDPOINT`
  * Description: Inspects zookeeper and returns a map containing the name, version and url for the model referred to by the input params
  * Input:
    * model_name - the name of the model
    * model_version - the optional version of the model.  If it is not specified, the most current version is used.
  * Returns: A map containing the name, version, url for the REST endpoint (fields named name, version and url).  Note that the output of this function is suitable for input into the first argument of MAAS_MODEL_APPLY.
* `MAAS_MODEL_APPLY`
  * Description: Returns the output of a model deployed via model which is deployed at endpoint. NOTE: Results are cached at the client for 10 minutes.
  * Input:
    * endpoint - a map containing name, version, url for the REST endpoint
    * function - the optional endpoint path, default is 'apply'
    * model_args - dictionary of arguments for the model (these become request params).
  * Returns: The output of the model deployed as a REST endpoint in Map form.  Assumes REST endpoint returns a JSON Map.
* `MAP_EXISTS`
  * Description: Checks for existence of a key in a map.
  * Input:
    * key - The key to check for existence
    * map - The map to check for existence of the key
  * Returns: True if the key is found in the map and false otherwise.
* `MAP_GET`
  * Description: Gets the value associated with a key from a map
  * Input:
    * key - The key
    * map - The map
    * default - Optionally the default value to return if the key is not in the map.
  * Returns: The object associated with key in the map.  If there is no value associated, then default if specified and null if a default is not specified.
* `MONTH`
  * Description: The number representing the month.  The first month, January, has a value of 0.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The current month (0-based).
* `PROTOCOL_TO_NAME`
  * Description: Convert the IANA protocol number to the protocol name
  * Input:
    * IANA Number
  * Returns: The protocol name associated with the IANA number.
* `REGEXP_MATCH`
  * Description: Determines whether a regex matches a string
  * Input:
    * string - The string to test
    * pattern - The proposed regex pattern
  * Returns: True if the regex pattern matches the string and false otherwise.
* `SPLIT`
  * Description: Splits the string by the delimiter.
  * Input:
    * input - String to split
    * delim - String delimiter
  * Returns: List of Strings
* `STARTS_WITH`
  * Description: Determines whether a string starts with a prefix
  * Input:
    * string - The string to test
    * prefix - The proposed prefix
  * Returns: True if the string starts with the specified prefix and false otherwise.
* `STATS_ADD`
  * Description: Add one or more input values to those that are used to calculate the summary statistics.
  * Input:
    * stats - The Stellar statistics object.  If null, then a new one is initialized.
    * value+ - one or more numbers to add 
  * Returns: A StatisticsProvider object
* `STATS_COUNT`
  * Description: Calculates the count of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The count of the values in the window or NaN if the statistics object is null.
* `STATS_GEOMETRIC_MEAN`
  * Description: Calculates the geometric mean of the values accumulated (or in the window if a window is used). See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The geometric mean of the values in the window or NaN if the statistics object is null.
* `STATS_INIT`
  * Description: Initialize a Statistics object
  * Input:
    * window_size - The number of input data values to maintain in a rolling window in memory.  If equal to 0, then no rolling window is maintained. Using no rolling window is less memory intensive, but cannot calculate certain statistics like percentiles and kurtosis.
  * Returns: A StatisticsProvider object
* `STATS_KURTOSIS`
  * Description: Calculates the kurtosis of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The kurtosis of the values in the window or NaN if the statistics object is null.
* `STATS_MAX`
  * Description: Calculates the max of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The max of the values in the window or NaN if the statistics object is null.
* `STATS_MEAN`
  * Description: Calculates the mean of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The mean of the values in the window or NaN if the statistics object is null.
* `STATS_MERGE`
  * Description: Merge statistic providers
  * Input:
    * statisticsProviders - A list of statistics providers
  * Returns: A StatisticsProvider object
* `STATS_MIN`
  * Description: Calculates the min of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The min of the values in the window or NaN if the statistics object is null.
* `STATS_PERCENTILE`
  * Description: Computes the p'th percentile of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
    * p - a double where 0 <= p < 1 representing the percentile
  * Returns: The p'th percentile of the data or NaN if the statistics object is null
* `STATS_POPULATION_VARIANCE`
  * Description: Calculates the population variance of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The population variance of the values in the window or NaN if the statistics object is null.
* `STATS_QUADRATIC_MEAN`
  * Description: Calculates the quadratic mean of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The quadratic mean of the values in the window or NaN if the statistics object is null.
* `STATS_SD`
  * Description: Calculates the standard deviation of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The standard deviation of the values in the window or NaN if the statistics object is null.
* `STATS_SKEWNESS`
  * Description: Calculates the skewness of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The skewness of the values in the window or NaN if the statistics object is null.
* `STATS_SUM`
  * Description: Calculates the sum of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The sum of the values in the window or NaN if the statistics object is null.
* `STATS_SUM_LOGS`
  * Description: Calculates the sum of the (natural) log of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The sum of the (natural) log of the values in the window or NaN if the statistics object is null.
* `STATS_SUM_SQUARES`
  * Description: Calculates the sum of the squares of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The sum of the squares of the values in the window or NaN if the statistics object is null.
* `STATS_VARIANCE`
  * Description: Calculates the variance of the values accumulated (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object.
  * Returns: The variance of the values in the window or NaN if the statistics object is null.
* `TO_DOUBLE`
  * Description: Transforms the first argument to a double precision number
  * Input:
    * input - Object of string or numeric type
  * Returns: Double
* `TO_EPOCH_TIMESTAMP`
  * Description: Returns the epoch timestamp of the dateTime given the format. If the format does not have a timestamp and you wish to assume a given timestamp, you may specify the timezone optionally.
  * Input:
    * dateTime - DateTime in String format
    * format - DateTime format as a String
    * timezone - Optional timezone in String format
  * Returns: Boolean
* `TO_INTEGER`
  * Description: Transforms the first argument to an integer
  * Input:
    * input - Object of string or numeric type
  * Returns: Integer
* `TO_LOWER`
  * Description: Transforms the first argument to a lowercase string
  * Input:
    * input - String
  * Returns: String
* `TO_STRING`
  * Description: Transforms the first argument to a string
  * Input:
    * input - Object
  * Returns: String
* `TO_UPPER`
  * Description: Transforms the first argument to an uppercase string
  * Input:
    * input - String
  * Returns: String
* `TRIM`
  * Description: Trims whitespace from both sides of a string.
  * Input:
    * input - String
  * Returns: String
* `URL_TO_HOST`
  * Description: Extract the hostname from a URL.
  * Input:
    * url - URL in String form
  * Returns: The hostname from the URL as a String.  e.g. URL_TO_HOST('http://www.yahoo.com/foo') would yield 'www.yahoo.com'
* `URL_TO_PATH`
  * Description: Extract the path from a URL.
  * Input:
    * url - URL in String form
  * Returns: The path from the URL as a String.  e.g. URL_TO_PATH('http://www.yahoo.com/foo') would yield 'foo'
* `URL_TO_PORT`
  * Description: Extract the port from a URL.  If the port is not explicitly stated in the URL, then an implicit port is inferred based on the protocol.
  * Input:
    * url - URL in String form
  * Returns: The port used in the URL as an Integer.  e.g. URL_TO_PORT('http://www.yahoo.com/foo') would yield 80
* `URL_TO_PROTOCOL`
  * Description: Extract the protocol from a URL.
  * Input:
    * url - URL in String form
  * Returns: The protocol from the URL as a String. e.g. URL_TO_PROTOCOL('http://www.yahoo.com/foo') would yield 'http'
* `WEEK_OF_MONTH`
  * Description: The numbered week within the month.  The first week within the month has a value of 1.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The numbered week within the month.
* `WEEK_OF_YEAR`
  * Description: The numbered week within the year.  The first week in the year has a value of 1.
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The numbered week within the year.
* `YEAR`
  * Description: The number representing the year. 
  * Input:
    * dateTime - The datetime as a long representing the milliseconds since unix epoch
  * Returns: The current year

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

## Stellar Shell

A REPL (Read Eval Print Loop) for the Stellar language that helps in debugging, troubleshooting and learning Stellar.  The Stellar DSL (domain specific language) is used to act upon streaming data within Apache Storm.  It is difficult to troubleshoot Stellar when it can only be executed within a Storm topology.  This REPL is intended to help mitigate that problem by allowing a user to replicate data encountered in production, isolate initialization errors, or understand function resolution problems.

The shell supports customization via `~/.inputrc` as it is
backed by a proper readline implementation.  

Shell-like operations are supported such as 
* reverse search via ctrl-r
* autocomplete of Stellar functions and variables via tab
  * NOTE: Stellar functions are read via a classpath search which
    happens in the background.  Until that happens, autocomplete will not include function names. 
* emacs or vi keybindings for edit mode

### Getting Started

```
$ $METRON_HOME/bin/stellar

Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}

[Stellar]>>> %functions
BLOOM_ADD, BLOOM_EXISTS, BLOOM_INIT, BLOOM_MERGE, DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_YEAR, ...

[Stellar]>>> ?PROTOCOL_TO_NAME
PROTOCOL_TO_NAME
 desc: Convert the IANA protocol number to the protocol name       
 args: IANA Number                                                 
  ret: The protocol name associated with the IANA number.          

[Stellar]>>> ip.protocol := 6
6
[Stellar]>>> PROTOCOL_TO_NAME(ip.protocol)
TCP
```

### Command Line Options

```
$ $METRON_HOME/bin/stellar -h
usage: stellar
 -h,--help              Print help
 -irc,--inputrc <arg>   File containing the inputrc if not the default
                        ~/.inputrc
 -v,--variables <arg>   File containing a JSON Map of variables
 -z,--zookeeper <arg>   Zookeeper URL
 -na,--no_ansi          Make the input prompt not use ANSI colors.
```

#### `-v, --variables`
*Optional*

Optionally load a JSON map which contains variable assignments.  This is
intended to give you the ability to save off a message from Metron and
work on it via the REPL.

#### `-z, --zookeeper`

*Optional*

Attempts to connect to Zookeeper and read the Metron global configuration.  Stellar functions may require the global configuration to work properly.  If found, the global configuration values are printed to the console.

```
$ $METRON_HOME/bin/stellar -z node1:2181
Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
[Stellar]>>> 
```

### Variable Assignment

Stellar has no concept of variable assignment.  For testing and
debugging purposes, it is important to be able to create variables that
simulate data contained within incoming messages.  The REPL has created
a means for a user to perform variable assignment outside of the core
Stellar language.  This is done via the `:=` operator, such as 
`foo := 1 + 1` would assign the result of the stellar expression `1 + 1` to the
variable `foo`.

```
[Stellar]>>> foo := 2 + 2
4.0
[Stellar]>>> 2 + 2
4.0
```

### Magic Commands

The REPL has a set of magic commands that provide the REPL user with information about the Stellar execution environment.  The following magic commands are supported.

#### `%functions`

This command lists all functions resolvable in the Stellar environment.  Stellar searches the classpath for Stellar functions.  This can make it difficult in some cases to understand which functions are resolvable.  

```
[Stellar]>>> %functions
BLOOM_ADD, BLOOM_EXISTS, BLOOM_INIT, BLOOM_MERGE, DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_YEAR, 
DOMAIN_REMOVE_SUBDOMAINS, DOMAIN_REMOVE_TLD, DOMAIN_TO_TLD, ENDS_WITH, GET, GET_FIRST, 
GET_LAST, IN_SUBNET, IS_DATE, IS_DOMAIN, IS_EMAIL, IS_EMPTY, IS_INTEGER, IS_IP, IS_URL, 
JOIN, MAAS_GET_ENDPOINT, MAAS_MODEL_APPLY, MAP_EXISTS, MAP_GET, MONTH, PROTOCOL_TO_NAME, 
REGEXP_MATCH, SPLIT, STARTS_WITH, STATS_ADD, STATS_COUNT, STATS_GEOMETRIC_MEAN, STATS_INIT, 
STATS_KURTOSIS, STATS_MAX, STATS_MEAN, STATS_MERGE, STATS_MIN, STATS_PERCENTILE, 
STATS_POPULATION_VARIANCE, STATS_QUADRATIC_MEAN, STATS_SD, STATS_SKEWNESS, STATS_SUM, 
STATS_SUM_LOGS, STATS_SUM_SQUARES, STATS_VARIANCE, TO_DOUBLE, TO_EPOCH_TIMESTAMP, 
TO_INTEGER, TO_LOWER, TO_STRING, TO_UPPER, TRIM, URL_TO_HOST, URL_TO_PATH, URL_TO_PORT, 
URL_TO_PROTOCOL, WEEK_OF_MONTH, WEEK_OF_YEAR, YEAR
[Stellar]>>> 
```

#### `%vars` 

Lists all variables in the Stellar environment.

```
Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
[Stellar]>>> %vars
[Stellar]>>> foo := 2 + 2
4.0
[Stellar]>>> %vars
foo = 4.0
```

#### `?<function>`

Returns formatted documentation of the Stellar function.  Provides the description of the function along with the expected arguments.

```
[Stellar]>>> ?BLOOM_ADD
BLOOM_ADD
 desc: Adds an element to the bloom filter passed in               
 args: bloom - The bloom filter, value* - The values to add        
  ret: Bloom Filter                                                
[Stellar]>>> ?IS_EMAIL
IS_EMAIL
 desc: Tests if a string is a valid email address                  
 args: address - The String to test                                
  ret: True if the string is a valid email address and false otherwise.
[Stellar]>>> 
```

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
