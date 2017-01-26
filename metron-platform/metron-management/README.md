#Stellar REPL Management Utilities

In order to augment the functionality of the Stellar REPL, a few
management functions surrounding the management of the configurations
and the management of Stellar transformations in the following areas
have been added:
* Stellar field transformations in the Parsers
* Stellar enrichments in the Enrichment topology
* Stellar threat triage rules

Additionally, some shell functions have been added to 
* provide the ability to refer to the Stellar expression used to create a variable
* print structured data in a way that is easier to view (i.e. tabular)

This functionality is exposed as a pack of Stellar functions in this
project.

## Function Details

The functions are split roughly into a few sections:
* Shell functions - Functions surrounding interacting with the shell in either a nicer way or a more functional way.
* Grok Functions - Functions that allow you to evaluate grok expressions.
* File functions - Functions around interacting with local or HDFS files
* Configuration functions - Functions surrounding pulling and pushing configs from zookeeper
* Parser functions - Functions surrounding adding, viewing, and removing Parser functions.
* Enrichment functions - Functions surrounding adding, viewing and removing Stellar enrichments as well as managing batch size and index names for the enrichment topology configuration
* Threat Triage functions - Functions surrounding adding, viewing and removing threat triage functions.

### Grok Functions

* `GROK_EVAL`
  * Description: Evaluate a grok expression for a statement.
  * Input:
    * grokExpression - The grok expression to evaluate
    * data - Either a data message or a list of data messages to evaluate using the grokExpression
  * Returns: The Map associated with the grok expression being evaluated on the list of messages.
* `GROK_PREDICT`
  * Description: Discover a grok statement for an input doc
  * Input:
    * data - The data to discover a grok expression from
  * Returns: A grok expression that should match the data.

### File Functions

* Local Files
  * `LOCAL_LS`
    * Description: Lists the contents of a directory.
    * Input:
      * path - The path of the file
    * Returns: The contents of the directory in tabular form sorted by last modification date.
  * `LOCAL_RM`
    * Description: Removes the path
    * Input:
      * path - The path of the file or directory.
      * recursive - Recursively remove or not (optional and defaulted to false)
    * Returns: boolean - true if successful, false otherwise
  * `LOCAL_READ`
    * Description: Retrieves the contents as a string of a file.
    * Input:
      * path - The path of the file
    * Returns: The contents of the file and null otherwise.
  * `LOCAL_READ_LINES`
    * Description: Retrieves the contents of a file as a list of strings.
    * Input:
      * path - The path of the file
    * Returns: A list of lines
  * `LOCAL_WRITE`
    * Description: Writes the contents of a string to a local file
    * Input:
      * content - The content to write out
      * path - The path of the file
    * Returns: true if the file was written and false otherwise.
* HDFS Files
  * `HDFS_LS`
    * Description: Lists the contents of a directory in HDFS.
    * Input:
      * path - The path of the file
    * Returns: The contents of the directory in tabular form sorted by last modification date.
  * `HDFS_RM`
    * Description: Removes the path in HDFS.
    * Input:
      * path - The path of the file or directory.
      * recursive - Recursively remove or not (optional and defaulted to false)
    * Returns: boolean - true if successful, false otherwise
  * `HDFS_READ`
    * Description: Retrieves the contents as a string of a file in HDFS.
    * Input:
      * path - The path of the file
    * Returns: The contents of the file and null otherwise.
  * `HDFS_READ_LINES`
    * Description: Retrieves the contents of a HDFS file as a list of strings.
    * Input:
      * path - The path of the file
    * Returns: A list of lines
  * `HDFS_WRITE`
    * Description: Writes the contents of a string to a HDFS file
    * Input:
      * content - The content to write out
      * path - The path of the file
    * Returns: true if the file was written and false otherwise.

### Shell Functions 

* `SHELL_EDIT`
  * Description: Open an editor (optionally initialized with text) and return whatever is saved from the editor.  The editor to use is pulled from `EDITOR` or `VISUAL` environment variable.
  * Input:
    * string - (Optional) A string whose content is used to initialize the editor.
  * Returns: The content that the editor saved after editor exit.
* `SHELL_GET_EXPRESSION`
  * Description: Get a stellar expression from a variable
  * Input:
    * variable - variable name
  * Returns: The stellar expression associated with the variable.
* `SHELL_LIST_VARS`
  * Description: Return the variables in a tabular form
  * Input:
    * wrap : Length of string to wrap the columns
  * Returns: A tabular representation of the variables.
* `SHELL_MAP2TABLE`
  * Description: Take a map and return a table
  * Input:
    * map - Map
  * Returns: The map in table form
* `SHELL_VARS2MAP`
  * Description: Take a set of variables and return a map
  * Input:
    * variables* - variable names to use to create map 
  * Returns: A map associating the variable name with the stellar expression.


### Configuration Functions

* `CONFIG_GET`
  * Description: Retrieve a Metron configuration from zookeeper.
  * Input:
    * type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER
    * sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)
    * emptyIfNotPresent - If true, then return an empty, minimally viable config
  * Returns: The String representation of the config in zookeeper
* `CONFIG_PUT`
  * Description: Updates a Metron config to Zookeeper.
  * Input:
    * type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER
    * config - The config (a string in JSON form) to update
    * sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)
  * Returns: The String representation of the config in zookeeper

### Parser Functions

* `PARSER_STELLAR_TRANSFORM_ADD`
  * Description: Add stellar field transformation.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * stellarTransforms - A Map associating fields to stellar expressions
  * Returns: The String representation of the config in zookeeper
* `PARSER_STELLAR_TRANSFORM_PRINT`
  * Description: Retrieve stellar field transformations.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
  * Returns: The String representation of the transformations
* `PARSER_STELLAR_TRANSFORM_REMOVE`
  * Description: Remove stellar field transformation.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * stellarTransforms - A list of stellar transforms to remove
  * Returns: The String representation of the config in zookeeper


### Indexing Functions

* `INDEXING_SET_BATCH`
  * Description: Set batch size
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * writer - The writer to update (e.g. elasticsearch, solr or hdfs)
    * size - batch size (integer)
  * Returns: The String representation of the config in zookeeper
* `INDEXING_SET_ENABLED`
  * Description: Enable or disable an indexing writer for a sensor.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * writer - The writer to update (e.g. elasticsearch, solr or hdfs)
    * enabled? - boolean indicating whether the writer is enabled.  If omitted, then it will set enabled.
  * Returns: The String representation of the config in zookeeper
* `INDEXING_SET_INDEX`
  * Description: Set the index for the sensor
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * writer - The writer to update (e.g. elasticsearch, solr or hdfs)
    * sensor - sensor name
  * Returns: The String representation of the config in zookeeper

### Enrichment Functions

* `ENRICHMENT_STELLAR_TRANSFORM_ADD`
  * Description: Add stellar field transformation.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * type - ENRICHMENT or THREAT_INTEL
    * stellarTransforms - A Map associating fields to stellar expressions
    * group - Group to add to (optional)
  * Returns: The String representation of the config in zookeeper
* `ENRICHMENT_STELLAR_TRANSFORM_PRINT`
  * Description: Retrieve stellar enrichment transformations.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * type - ENRICHMENT or THREAT_INTEL
  * Returns: The String representation of the transformations
* `ENRICHMENT_STELLAR_TRANSFORM_REMOVE`
  * Description: Remove one or more stellar field transformations.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * type - ENRICHMENT or THREAT_INTEL
    * stellarTransforms - A list of removals
    * group - Group to remove from (optional)
  * Returns: The String representation of the config in zookeeper

### Threat Triage Functions

* `THREAT_TRIAGE_ADD`
  * Description: Add a threat triage rule.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * stellarTransforms - A Map associating stellar rules to scores
    * triageRules - Map (or list of Maps) representing a triage rule.  It must contain 'rule' and 'score' keys, the stellar expression for the rule and triage score respectively.  It may contain 'name' and 'comment', the name of the rule and comment associated with the rule respectively."
  * Returns: The String representation of the threat triage rules
* `THREAT_TRIAGE_PRINT`
  * Description: Retrieve stellar enrichment transformations.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
  * Returns: The String representation of the threat triage rules
* `THREAT_TRIAGE_REMOVE`
  * Description: Remove stellar threat triage rule(s).
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * rules - A list of stellar rules or rule names to remove
  * Returns: The String representation of the enrichment config
* `THREAT_TRIAGE_SET_AGGREGATOR`
  * Description: Set the threat triage aggregator.
  * Input:
    * sensorConfig - Sensor config to add transformation to.
    * aggregator - Aggregator to use.  One of MIN, MAX, MEAN, SUM, POSITIVE_MEAN
    * aggregatorConfig - Optional config for aggregator
  * Returns: The String representation of the enrichment config

## Deployment Instructions

Deployment is as simple as dropping the jar created by this project into
`$METRON_HOME/lib` and starting the Stellar shell via
`$METRON_HOME/bin/stellar`

## Examples
Included for description and education purposes are a couple example Stellar REPL transcripts
with helpful comments to illustrate some common operations.

### Iterate in finding a valid Grok pattern
```
Stellar, Go!
Please note that functions are loading lazily in the background and will be unavailable until loaded fully.
[Stellar]>>> # We are going to debug a squid grok statement with a bug in it
[Stellar]>>> squid_grok_orig := '%{NUMBER:timestamp} %{SPACE:UNWANTED}  %{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} 
 - %{WORD:UNWANTED}/%{IP:ip_dst_addr} %{WORD:UNWANTED}/%{WORD:UNWANTED}'
[Stellar]>>> # We have gone ot a couple of domains in squid:
[Stellar]>>> #   1475022887.362    256 127.0.0.1 TCP_MISS/301 803 GET http://www.youtube.com/ - DIRECT/216.58.216.238 text/html
[Stellar]>>> #   1475022915.731      1 127.0.0.1 NONE/400 3520 GET flimflammadeupdomain.com - NONE/- text/html
[Stellar]>>> #   1475022938.661      0 127.0.0.1 NONE/400 3500 GET www.google.com - NONE/- text/html
[Stellar]>>> # Note that flimflammadeupdomain.com and www.google.com did not resolve to IPs
[Stellar]>>> # We can load up these messages from disk into a list of messages
[Stellar]>>> messages := LOCAL_READ_LINES( '/var/log/squid/access.log')
27687 [Thread-1] INFO  o.r.Reflections - Reflections took 26542 ms to scan 22 urls, producing 17906 keys and 121560 values 
27837 [Thread-1] INFO  o.a.m.c.d.FunctionResolverSingleton - Found 97 Stellar Functions...
Functions loaded, you may refer to functions now...
[Stellar]>>> # and evaluate the messages against our grok statement
[Stellar]>>> GROK_EVAL(squid_grok_orig, messages)
╔══════════╤═════════╤═════════╤═════════╤════════════════╤═════════════╤═════════╤════════════════╤═════════════════════════╗
║ action   │ bytes   │ code    │ elapsed │ ip_dst_addr    │ ip_src_addr │ method  │ timestamp      │ url                     ║
╠══════════╪═════════╪═════════╪═════════╪════════════════╪═════════════╪═════════╪════════════════╪═════════════════════════╣
║ TCP_MISS │ 803     │ 301     │ 256     │ 216.58.216.238 │ 127.0.0.1   │ GET     │ 1475022887.362 │ http://www.youtube.com/ ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╚══════════╧═════════╧═════════╧═════════╧════════════════╧═════════════╧═════════╧════════════════╧═════════════════════════╝

[Stellar]>>> # Uh oh, looks like the messages without destination IPs do not parse
[Stellar]>>> # We can start peeling off groups from the end of the message until things parse
[Stellar]>>> squid_grok := '%{NUMBER:timestamp} %{SPACE:UNWANTED}  %{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{ 
WORD:UNWANTED}/%{IP:ip_dst_addr}'
[Stellar]>>> GROK_EVAL(squid_grok, messages)
╔══════════╤═════════╤═════════╤═════════╤════════════════╤═════════════╤═════════╤════════════════╤═════════════════════════╗
║ action   │ bytes   │ code    │ elapsed │ ip_dst_addr    │ ip_src_addr │ method  │ timestamp      │ url                     ║
╠══════════╪═════════╪═════════╪═════════╪════════════════╪═════════════╪═════════╪════════════════╪═════════════════════════╣
║ TCP_MISS │ 803     │ 301     │ 256     │ 216.58.216.238 │ 127.0.0.1   │ GET     │ 1475022887.362 │ http://www.youtube.com/ ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╚══════════╧═════════╧═════════╧═════════╧════════════════╧═════════════╧═════════╧════════════════╧═════════════════════════╝

[Stellar]>>> # Still looks like it is having issues...
[Stellar]>>> squid_grok := '%{NUMBER:timestamp} %{SPACE:UNWANTED}  %{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{ 
WORD:UNWANTED}/%{IP:ip_dst_addr}'
[Stellar]>>> GROK_EVAL(squid_grok, messages)
╔══════════╤═════════╤═════════╤═════════╤════════════════╤═════════════╤═════════╤════════════════╤═════════════════════════╗
║ action   │ bytes   │ code    │ elapsed │ ip_dst_addr    │ ip_src_addr │ method  │ timestamp      │ url                     ║
╠══════════╪═════════╪═════════╪═════════╪════════════════╪═════════════╪═════════╪════════════════╪═════════════════════════╣
║ TCP_MISS │ 803     │ 301     │ 256     │ 216.58.216.238 │ 127.0.0.1   │ GET     │ 1475022887.362 │ http://www.youtube.com/ ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╟──────────┼─────────┼─────────┼─────────┼────────────────┼─────────────┼─────────┼────────────────┼─────────────────────────╢
║ MISSING  │ MISSING │ MISSING │ MISSING │ MISSING        │ MISSING     │ MISSING │ MISSING        │ MISSING                 ║
╚══════════╧═════════╧═════════╧═════════╧════════════════╧═════════════╧═════════╧════════════════╧═════════════════════════╝

[Stellar]>>> # Still looks wrong.  Hmm, I bet it is due to that dst_addr not being there; we can make it optional
[Stellar]>>> squid_grok := '%{NUMBER:timestamp} %{SPACE:UNWANTED}  %{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{ 
WORD:UNWANTED}/(%{IP:ip_dst_addr})?'
[Stellar]>>> GROK_EVAL(squid_grok, messages)
╔══════════╤═══════╤══════╤═════════╤════════════════╤═════════════╤════════╤════════════════╤══════════════════════════╗
║ action   │ bytes │ code │ elapsed │ ip_dst_addr    │ ip_src_addr │ method │ timestamp      │ url                      ║
╠══════════╪═══════╪══════╪═════════╪════════════════╪═════════════╪════════╪════════════════╪══════════════════════════╣
║ TCP_MISS │ 803   │ 301  │ 256     │ 216.58.216.238 │ 127.0.0.1   │ GET    │ 1475022887.362 │ http://www.youtube.com/  ║
╟──────────┼───────┼──────┼─────────┼────────────────┼─────────────┼────────┼────────────────┼──────────────────────────╢
║ NONE     │ 3520  │ 400  │ 1       │ null           │ 127.0.0.1   │ GET    │ 1475022915.731 │ flimflammadeupdomain.com ║
╟──────────┼───────┼──────┼─────────┼────────────────┼─────────────┼────────┼────────────────┼──────────────────────────╢
║ NONE     │ 3500  │ 400  │ 0       │ null           │ 127.0.0.1   │ GET    │ 1475022938.661 │ www.google.com           ║
╚══════════╧═══════╧══════╧═════════╧════════════════╧═════════════╧════════╧════════════════╧══════════════════════════╝

[Stellar]>>> # Ahh, that is much better.
[Stellar]>>> 
[Stellar]>>> 
```

### Manage Stellar Field Transformations

```
964  [main] INFO  o.a.c.f.i.CuratorFrameworkImpl - Starting
1025 [main-EventThread] INFO  o.a.c.f.s.ConnectionStateManager - State change: CONNECTED
Stellar, Go!
Please note that functions are loading lazily in the background and will be unavailable until loaded fully.
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
[Stellar]>>> # First we get the squid parser config from zookeeper
[Stellar]>>> squid_parser_config := CONFIG_GET('PARSER', 'squid')
29089 [Thread-1] INFO  o.r.Reflections - Reflections took 26765 ms to scan 22 urls, producing 17898 keys and 121518 values 
29177 [Thread-1] INFO  o.a.m.c.d.FunctionResolverSingleton - Found 83 Stellar Functions...
Functions loaded, you may refer to functions now...
[Stellar]>>> # See what kind of transformations it already has
[Stellar]>>> PARSER_STELLAR_TRANSFORM_PRINT(squid_parser_config)
╔═══════════════════════════╤═════════════════════════════════════════╗
║ Field                     │ Transformation                          ║
╠═══════════════════════════╪═════════════════════════════════════════╣
║ full_hostname             │ URL_TO_HOST(url)                        ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname) ║
╚═══════════════════════════╧═════════════════════════════════════════╝

[Stellar]>>> #Just to make sure it looks right, we can view the JSON
[Stellar]>>> squid_parser_config
{
  "parserClassName": "org.apache.metron.parsers.GrokParser",
  "sensorTopic": "squid",
  "parserConfig": {
    "grokPath": "/patterns/squid",
    "patternLabel": "SQUID_DELIMITED",
    "timestampField": "timestamp"
  },
  "fieldTransformations" : [
    {
      "transformation" : "STELLAR"
    ,"output" : [ "full_hostname", "domain_without_subdomains" ]
    ,"config" : {
      "full_hostname" : "URL_TO_HOST(url)"
      ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
                }
    }
                           ]
}

[Stellar]>>> # Add another transformation in there
[Stellar]>>> domain_without_subdomains := 'cnn.com'
[Stellar]>>> upper_domain := TO_UPPER(domain_without_subdomains)
[Stellar]>>> # Now we can look at our variables and see what expressions created them 
[Stellar]>>> # NOTE: the 40 is the max char for a word
[Stellar]>>> SHELL_LIST_VARS( 40 )
╔═══════════════════════════╤═══════════════════════════════════════════╤═════════════════════════════════════╗
║ VARIABLE                  │ VALUE                                     │ EXPRESSION                          ║
╠═══════════════════════════╪═══════════════════════════════════════════╪═════════════════════════════════════╣
║ squid_parser_config       │ {                                         │ CONFIG_GET('PARSER', 'squid')       ║
║                           │   "parserClassName":                      │                                     ║
║                           │ "org.apache.metron.parsers.GrokParser",   │                                     ║
║                           │                                           │                                     ║
║                           │ "sensorTopic": "squid",                   │                                     ║
║                           │                                           │                                     ║
║                           │ "parserConfig": {                         │                                     ║
║                           │     "grokPath":                           │                                     ║
║                           │ "/patterns/squid",                        │                                     ║
║                           │     "patternLabel":                       │                                     ║
║                           │ "SQUID_DELIMITED",                        │                                     ║
║                           │     "timestampField":                     │                                     ║
║                           │ "timestamp"                               │                                     ║
║                           │   },                                      │                                     ║
║                           │                                           │                                     ║
║                           │ "fieldTransformations" : [                │                                     ║
║                           │     {                                     │                                     ║
║                           │                                           │                                     ║
║                           │ "transformation" : "STELLAR"              │                                     ║
║                           │                                           │                                     ║
║                           │ ,"output" : [ "full_hostname",            │                                     ║
║                           │ "domain_without_subdomains" ]             │                                     ║
║                           │                                           │                                     ║
║                           │ ,"config" : {                             │                                     ║
║                           │       "full_hostname" :                   │                                     ║
║                           │ "URL_TO_HOST(url)"                        │                                     ║
║                           │                                           │                                     ║
║                           │ ,"domain_without_subdomains" :            │                                     ║
║                           │ "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)" │                                     ║
║                           │                                           │                                     ║
║                           │  }                                        │                                     ║
║                           │     }                                     │                                     ║
║                           │                            ]              │                                     ║
║                           │ }                                         │                                     ║
╟───────────────────────────┼───────────────────────────────────────────┼─────────────────────────────────────╢
║ domain_without_subdomains │ cnn.com                                   │ 'cnn.com'                           ║
╟───────────────────────────┼───────────────────────────────────────────┼─────────────────────────────────────╢
║ upper_domain              │ CNN.COM                                   │ TO_UPPER(domain_without_subdomains) ║
╚═══════════════════════════╧═══════════════════════════════════════════╧═════════════════════════════════════╝

[Stellar]>>> # We can add upper_domain as a transformation to the parser now by
[Stellar]>>> ?PARSER_STELLAR_TRANSFORM_ADD
PARSER_STELLAR_TRANSFORM_ADD
Description: Add stellar field transformation.                           

Arguments:
    sensorConfig - Sensor config to add transformation to.      
    stellarTransforms - A Map associating fields to stellar expressions

Returns: The String representation of the config in zookeeper        
[Stellar]>>> # We will use the SHELL_VARS2MAP to construct our map associating the field name with the expression
[Stellar]>>> ?SHELL_VARS2MAP
SHELL_VARS2MAP
Description: Take a set of variables and return a map                    

Arguments:
    variables* - variable names to use to create map            

Returns: A map associating the variable name with the stellar expression.
[Stellar]>>> squid_parser_config_new := PARSER_STELLAR_TRANSFORM_ADD( squid_parser_config, SHELL_VARS2MAP('upper_domain') )
[Stellar]>>> #Now we can make sure that we have the transformation added
[Stellar]>>> PARSER_STELLAR_TRANSFORM_PRINT(squid_parser_config_new)
╔═══════════════════════════╤═════════════════════════════════════════╗
║ Field                     │ Transformation                          ║
╠═══════════════════════════╪═════════════════════════════════════════╣
║ full_hostname             │ URL_TO_HOST(url)                        ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname) ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ upper_domain              │ TO_UPPER(domain_without_subdomains)     ║
╚═══════════════════════════╧═════════════════════════════════════════╝

[Stellar]>>> #And finally, we push the configs back to zookeeper
[Stellar]>>> CONFIG_PUT('PARSER', squid_parser_config_new, 'squid')
[Stellar]>>> #Now we can make sure that we have the transformation added into zookeeper
[Stellar]>>> PARSER_STELLAR_TRANSFORM_PRINT(CONFIG_GET('PARSER', 'squid'))
╔═══════════════════════════╤═════════════════════════════════════════╗
║ Field                     │ Transformation                          ║
╠═══════════════════════════╪═════════════════════════════════════════╣
║ full_hostname             │ URL_TO_HOST(url)                        ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname) ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ upper_domain              │ TO_UPPER(domain_without_subdomains)     ║
╚═══════════════════════════╧═════════════════════════════════════════╝

[Stellar]>>> #Now that we have added it, we can change our mind and remove it
[Stellar]>>> ?PARSER_STELLAR_TRANSFORM_REMOVE
PARSER_STELLAR_TRANSFORM_REMOVE
Description: Remove stellar field transformation.                        

Arguments:
    sensorConfig - Sensor config to add transformation to.      
    stellarTransforms - A list of stellar transforms to remove  

Returns: The String representation of the config in zookeeper        
[Stellar]>>> squid_parser_config_new := PARSER_STELLAR_TRANSFORM_REMOVE( CONFIG_GET('PARSER', 'squid'), [ 'upper_domain' ] )
[Stellar]>>> PARSER_STELLAR_TRANSFORM_PRINT(squid_parser_config_new)
╔═══════════════════════════╤═════════════════════════════════════════╗
║ Field                     │ Transformation                          ║
╠═══════════════════════════╪═════════════════════════════════════════╣
║ full_hostname             │ URL_TO_HOST(url)                        ║
╟───────────────────────────┼─────────────────────────────────────────╢
║ domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname) ║
╚═══════════════════════════╧═════════════════════════════════════════╝

[Stellar]>>> #We can now push up the config to zookeeper
[Stellar]>>> CONFIG_PUT('PARSER', squid_parser_config_new, 'squid')
[Stellar]>>> #It should be just as we started the exercise
[Stellar]>>> CONFIG_GET('PARSER', 'squid')
{
  "parserClassName" : "org.apache.metron.parsers.GrokParser",
  "sensorTopic" : "squid",
  "parserConfig" : {
    "grokPath" : "/patterns/squid",
    "patternLabel" : "SQUID_DELIMITED",
    "timestampField" : "timestamp"
  },
  "fieldTransformations" : [ {
    "input" : [ ],
    "output" : [ "full_hostname", "domain_without_subdomains" ],
    "transformation" : "STELLAR",
    "config" : {
      "full_hostname" : "URL_TO_HOST(url)",
      "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
    }
  } ]
}
[Stellar]>>> #And quit the REPL
[Stellar]>>> quit
```

### Manage Stellar Enrichments
```
1010 [main] INFO  o.a.c.f.i.CuratorFrameworkImpl - Starting
1077 [main-EventThread] INFO  o.a.c.f.s.ConnectionStateManager - State change: CONNECTED
Stellar, Go!
Please note that functions are loading lazily in the background and will be unavailable until loaded fully.
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
[Stellar]>>> # First we get the squid enrichment config from zookeeper.
[Stellar]>>> # If it is not there, which it is not by default, a suitable default
[Stellar]>>> # config will be specified.
[Stellar]>>> squid_enrichment_config := CONFIG_GET('ENRICHMENT', 'squid')
26307 [Thread-1] INFO  o.r.Reflections - Reflections took 24845 ms to scan 22 urls, producing 17898 keys and 121520 values 
26389 [Thread-1] INFO  o.a.m.c.d.FunctionResolverSingleton - Found 84 Stellar Functions...
Functions loaded, you may refer to functions now...
[Stellar]>>> # Just to make sure it looks right, we can view the JSON
[Stellar]>>> squid_enrichment_config
{
  "enrichment" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> # Now that we have a config, we can add an enrichment to the Stellar adapter
[Stellar]>>> # We should make sure that the current enrichment does not have any already
[Stellar]>>> ?ENRICHMENT_STELLAR_TRANSFORM_PRINT
ENRICHMENT_STELLAR_TRANSFORM_PRINT
Description: Retrieve stellar enrichment transformations.                

Arguments:
    sensorConfig - Sensor config to add transformation to.      
    type - ENRICHMENT or THREAT_INTEL                           

Returns: The String representation of the transformations            
[Stellar]>>> # Since there are two places we can add enrichments, we should check both
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config, 'ENRICHMENT')
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝

[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config, 'THREAT_INTEL')
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝

[Stellar]>>> # For my enrichment, I want to add a field indicating that the src address is local or not
[Stellar]>>> # I will define local as part of '192.168.0.0/21'
[Stellar]>>> # We must be careful about the ip_src_addr, what if it is malformed?
[Stellar]>>> ip_src_addr := '200.20.10.1'
[Stellar]>>> IN_SUBNET( ip_src_addr, '192.168.0.0/21')
false
[Stellar]>>> # Just as we expected.  Now we can try a local address
[Stellar]>>> ip_src_addr := '192.168.0.1'
[Stellar]>>> IN_SUBNET( ip_src_addr, '192.168.0.0/21')
true
[Stellar]>>> # Just as we expected.  Now we can try some malformed ones
[Stellar]>>> ip_src_addr := NULL
[Stellar]>>> IN_SUBNET( ip_src_addr, '192.168.0.0/21')
false
[Stellar]>>> # So far, so good
[Stellar]>>> ip_src_addr := 'foo bar'
[Stellar]>>> IN_SUBNET( ip_src_addr, '192.168.0.0/21')
[!] Unable to execute: Could not parse [foo bar]
[Stellar]>>> # uh oh, that was terrible, we will have to adjust and be a bit more defensive
[Stellar]>>> IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
false
[Stellar]>>> ip_src_addr := NULL
[Stellar]>>> IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
false
[Stellar]>>> ip_src_addr := '192.168.0.1'
[Stellar]>>> IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
true
[Stellar]>>> ip_src_addr := '200.20.10.1'
[Stellar]>>> IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
false
[Stellar]>>> # I think we are ready, we will call it is_local
[Stellar]>>> is_local := IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
[Stellar]>>> # Now we can add the function to the ENRICHMENT phase of the enrichment topology
[Stellar]>>> ?ENRICHMENT_STELLAR_TRANSFORM_ADD
ENRICHMENT_STELLAR_TRANSFORM_ADD
Description: Add stellar field transformation.                           

Arguments:
    sensorConfig - Sensor config to add transformation to.      
    type - ENRICHMENT or THREAT_INTEL                           
    stellarTransforms - A Map associating fields to stellar expressions
    group - Group to add to (optional)                          

Returns: The String representation of the config in zookeeper        
[Stellar]>>> squid_enrichment_config_new := ENRICHMENT_STELLAR_TRANSFORM_ADD( squid_enrichment_config, 'ENRICHMENT', SHELL_VARS2MAP( 'is_local' ) )
[Stellar]>>> # Make sure that it is really there
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'ENRICHMENT')
╔═══════════╤══════════╤════════════════════════════════════════════════════════════════════════════════╗
║ Group     │ Field    │ Transformation                                                                 ║
╠═══════════╪══════════╪════════════════════════════════════════════════════════════════════════════════╣
║ (default) │ is_local │ IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21') ║
╚═══════════╧══════════╧════════════════════════════════════════════════════════════════════════════════╝

[Stellar]>>> # and not in threat intel
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'THREAT_INTEL')
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝

[Stellar]>>> # And see it in the JSON
[Stellar]>>> squid_enrichment_config_new
{
  "index" : "squid",
  "batchSize" : 100,
  "enrichment" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "is_local" : "IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> # Now we can push the configs to zookeeper
[Stellar]>>> CONFIG_PUT( 'ENRICHMENT', squid_enrichment_config_new, 'squid')
[Stellar]>>> # And validate that our JSON looks a lot better
[Stellar]>>> CONFIG_GET( 'ENRICHMENT', 'squid')
{
  "index" : "squid",
  "batchSize" : 100,
  "enrichment" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "is_local" : "IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> # You know what?  I changed my mind, I do not want that enrichment, so we can remove it pretty easily.
[Stellar]>>> ?ENRICHMENT_STELLAR_TRANSFORM_REMOVE
ENRICHMENT_STELLAR_TRANSFORM_REMOVE
Description: Remove one or more stellar field transformations.           

Arguments:
    sensorConfig - Sensor config to add transformation to.      
    type - ENRICHMENT or THREAT_INTEL                           
    stellarTransforms - A list of removals                      
    group - Group to remove from (optional)                     

Returns: The String representation of the config in zookeeper        
[Stellar]>>> squid_enrichment_config_new := ENRICHMENT_STELLAR_TRANSFORM_REMOVE( squid_enrichment_config_new, 'ENRICHMENT', [ 'is_local' ] )
[Stellar]>>> # Make sure that it is really gone 
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'ENRICHMENT')
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝

[Stellar]>>> # and not in threat intel
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'THREAT_INTEL')
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝

[Stellar]>>> # Ok, I feel comfortable updating now
[Stellar]>>> CONFIG_PUT( 'ENRICHMENT', squid_enrichment_config_new, 'squid')
[Stellar]>>> CONFIG_GET( 'ENRICHMENT', 'squid')
{
  "index" : "squid",
  "batchSize" : 100,
  "enrichment" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : { },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> 
```

### Manage Threat Triage Rules
```
987  [main] INFO  o.a.c.f.i.CuratorFrameworkImpl - Starting
1047 [main-EventThread] INFO  o.a.c.f.s.ConnectionStateManager - State change: CONNECTED
Stellar, Go!
Please note that functions are loading lazily in the background and will be unavailable until loaded fully.
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
[Stellar]>>> # First we get the squid enrichment config from zookeeper.
[Stellar]>>> # If it is not there, which it is not by default, a suitable default
[Stellar]>>> # config will be specified.
[Stellar]>>> squid_enrichment_config := CONFIG_GET('ENRICHMENT', 'squid')
26751 [Thread-1] INFO  o.r.Reflections - Reflections took 24407 ms to scan 22 urls, producing 17898 keys and 121520 values 
26828 [Thread-1] INFO  o.a.m.c.d.FunctionResolverSingleton - Found 84 Stellar Functions...
Functions loaded, you may refer to functions now...
[Stellar]>>> # We should not have any threat triage rules
[Stellar]>>> THREAT_TRIAGE_PRINT(squid_enrichment_config)
╔═════════════╤═══════╗
║ Triage Rule │ Score ║
╠═════════════╧═══════╣
║ (empty)             ║
╚═════════════════════╝

[Stellar]>>> # I have followed the blog post at https://cwiki.apache.org/confluence/display/METRON/2016/04/28/Metron+Tutorial+-+Fundamentals+Part+2%3A+Creating+a+New+Enrichment
[Stellar]>>> # and have some enrichment reference data loaded into hbase, 
[Stellar]>>> # so we should be able to retrieve that as an enrichment using the ENRICHMENT_GET
[Stellar]>>> # function to call out to hbase from stellar
[Stellar]>>> ?ENRICHMENT_GET
ENRICHMENT_GET
Description: Interrogates the HBase table holding the simple hbase enrichment data and retrieves the tabular value associated with the enrichment type and indicator.

Arguments:
    enrichment_type - The enrichment type                       
    indicator - The string indicator to look up                 
    nosql_table - The NoSQL Table to use                        
    column_family - The Column Family to use                    

Returns: A Map associated with the indicator and enrichment type.  Empty otherwise.
[Stellar]>>> domain_without_subdomains := 'cnn.com'
[Stellar]>>> whois_info := ENRICHMENT_GET('whois', domain_without_subdomains, 'enrichment', 't')
[Stellar]>>> # Now I know that the whois_info is actually a map of values, so we can use the SHELL_MAP2TABLE function
[Stellar]>>> # to get a nicer view on this data
[Stellar]>>> SHELL_MAP2TABLE(whois_info)
╔══════════════════════════╤══════════════════════════════════╗
║ KEY                      │ VALUE                            ║
╠══════════════════════════╪══════════════════════════════════╣
║ owner                    │ Turner Broadcasting System, Inc. ║
╟──────────────────────────┼──────────────────────────────────╢
║ registrar                │ Domain Name Manager              ║
╟──────────────────────────┼──────────────────────────────────╢
║ domain_created_timestamp │ 748695600000                     ║
╟──────────────────────────┼──────────────────────────────────╢
║ home_country             │ US                               ║
╟──────────────────────────┼──────────────────────────────────╢
║ domain                   │ cnn.com                          ║
╚══════════════════════════╧══════════════════════════════════╝

[Stellar]>>> #Looks good
[Stellar]>>> squid_enrichment_config_new := ENRICHMENT_STELLAR_TRANSFORM_ADD( squid_enrichment_config, 'ENRICHMENT', SHELL_VARS2MAP( 'whois_info' ) )
[Stellar]>>> # Just for illustration, we can create a threat alert if the country of the domain registered
[Stellar]>>> # is non-US, then we can make an alert.  To do that, we need to create an is_alert field on the message.
[Stellar]>>> #
[Stellar]>>> # I know that maps get folded into the message, so that whois_info enrichment is going to create a few fields:
[Stellar]>>> #  * domain mapped to whois_info.domain
[Stellar]>>> #  * registrar mapped to whois_info.registrar
[Stellar]>>> #  * home_country mapped to whois_info.home_country
[Stellar]>>> #  * owner mapped to whois_info.owner
[Stellar]>>> whois_info.home_country := 'US'
[Stellar]>>> is_alert := whois_info.home_country != 'US'
[Stellar]>>> # Now we can add this Stellar enrichment to the THREAT_INTEL portion of the enrichment topology
[Stellar]>>> squid_enrichment_config_new := ENRICHMENT_STELLAR_TRANSFORM_ADD( squid_enrichment_config_new, 'THREAT_INTEL', SHELL_VARS2MAP( 'is_alert' ) )
[Stellar]>>> # We should recap to make sure we know what enrichments we have
[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'ENRICHMENT')
╔═══════════╤════════════╤═══════════════════════════════════════════════════════════════════════╗
║ Group     │ Field      │ Transformation                                                        ║
╠═══════════╪════════════╪═══════════════════════════════════════════════════════════════════════╣
║ (default) │ whois_info │ ENRICHMENT_GET('whois', domain_without_subdomains, 'enrichment', 't') ║
╚═══════════╧════════════╧═══════════════════════════════════════════════════════════════════════╝

[Stellar]>>> ENRICHMENT_STELLAR_TRANSFORM_PRINT(squid_enrichment_config_new, 'THREAT_INTEL')
╔═══════════╤══════════╤═════════════════════════════════╗
║ Group     │ Field    │ Transformation                  ║
╠═══════════╪══════════╪═════════════════════════════════╣
║ (default) │ is_alert │ whois_info.home_country != 'US' ║
╚═══════════╧══════════╧═════════════════════════════════╝

[Stellar]>>> # Now with this, we can create a rule or two to triage these alerts.
[Stellar]>>> # This means associating a rule as described by a stellar expression that returns true or false with a score
[Stellar]>>> # Also associated with this ruleset is an aggregation function, the default of which is MAX.
[Stellar]>>> # Now we can make a couple rules:
[Stellar]>>> #  * If the message is an alert and from a non-us whois source, we can set the level to 10
[Stellar]>>> #  * If the message is an alert and non-local, we can set the level to 20
[Stellar]>>> #  * If the message is an alert and both non-local and non-us, then we can set the level to 50
[Stellar]>>> # If multiple rules hit, then we should take the max (default behavior)
[Stellar]>>> non_us := whois_info.home_country != 'US'
[Stellar]>>> is_local := IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
[Stellar]>>> is_both := whois_info.home_country != 'US' && IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')
[Stellar]>>> rules := [ { 'name' : 'is non-us', 'rule' : SHELL_GET_EXPRESSION('non_us'), 'score' : 10 } , { 'name' : 'is local', 'rule' : SHELL_GET_EXPRESSION('is_local '), 'score' : 20 } , { 'name' : 'both non-us and local', 'comment' : 'union of both rules.',  'rule' : SHELL_GET_EXPRESSION('is_both'), 'score' : 50 } ]  
[Stellar]>>> # Now that we have our rules staged, we can add them to our config.
[Stellar]>>> squid_enrichment_config_new := THREAT_TRIAGE_ADD( squid_enrichment_config_new, rules )
[Stellar]>>> THREAT_TRIAGE_PRINT(squid_enrichment_config_new)
╔═══════════════════════╤══════════════════════╤═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╤═══════╗
║ Name                  │ Comment              │ Triage Rule                                                                                                       │ Score ║
╠═══════════════════════╪══════════════════════╪═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╪═══════╣
║ is non-us             │                      │ whois_info.home_country != 'US'                                                                                   │ 10    ║
╟───────────────────────┼──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────╢
║ is local              │                      │ IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')                                    │ 20    ║
╟───────────────────────┼──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────╢
║ both non-us and local │ union of both rules. │ whois_info.home_country != 'US' && IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21') │ 50    ║
╚═══════════════════════╧══════════════════════╧═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╧═══════╝

Aggregation: MAX
[Stellar]>>> # Looks good, we can push the configs up
[Stellar]>>> CONFIG_PUT('ENRICHMENT', squid_enrichment_config_new, 'squid')
[Stellar]>>> # And admire the resulting JSON that you did not have to edit directly.
[Stellar]>>> CONFIG_GET('ENRICHMENT', 'squid')
{
  "enrichment" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "whois_info" : "ENRICHMENT_GET('whois', domain_without_subdomains, 'enrichment', 't')"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "is_alert" : "whois_info.home_country != 'US'"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ {
        "name" : "is non-us",
        "rule" : "whois_info.home_country != 'US'",
        "score" : 10.0
      }, {
        "name" : "is local",
        "rule" : "IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')",
        "score" : 20.0
      }, {
        "name" : "both non-us and local",
        "comment" : "union of both rules.",
        "rule" : "whois_info.home_country != 'US' && IN_SUBNET( if IS_IP(ip_src_addr) then ip_src_addr else NULL, '192.168.0.0/21')",
        "score" : 50.0
      } ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> # Now that we have admired it, we can remove the rules
[Stellar]>>> squid_enrichment_config_new := THREAT_TRIAGE_REMOVE( squid_enrichment_config_new, [ SHELL_GET_EXPRESSION('non_us') , SHELL_GET_EXPRESSION('is_local') , SHELL_GET_EXPRES 
SION('is_both') ] )
[Stellar]>>> THREAT_TRIAGE_PRINT(squid_enrichment_config_new)
╔══════╤═════════╤═════════════╤═══════╗
║ Name │ Comment │ Triage Rule │ Score ║
╠══════╧═════════╧═════════════╧═══════╣
║ (empty)                              ║
╚══════════════════════════════════════╝

[Stellar]>>> # and push configs
[Stellar]>>> CONFIG_PUT('ENRICHMENT', squid_enrichment_config_new, 'squid')
[Stellar]>>> # And admire the resulting JSON that is devoid of threat triage rules.
[Stellar]>>> CONFIG_GET('ENRICHMENT', 'squid')
{
  "enrichment" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "whois_info" : "ENRICHMENT_GET('whois', domain_without_subdomains, 'enrichment', 't')"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { }
  },
  "threatIntel" : {
    "fieldMap" : {
      "stellar" : {
        "config" : {
          "is_alert" : "whois_info.home_country != 'US'"
        }
      }
    },
    "fieldToTypeMap" : { },
    "config" : { },
    "triageConfig" : {
      "riskLevelRules" : [ ],
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}
[Stellar]>>> 
```
