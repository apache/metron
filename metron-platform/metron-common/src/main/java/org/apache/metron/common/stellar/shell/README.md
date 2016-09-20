# Stellar Shell

A REPL (Read Eval Print Loop) for the Stellar language that helps in debugging, troubleshooting and learning Stellar.  The Stellar DSL (domain specific language) is used to act upon streaming data within Apache Storm.  It is difficult to troubleshoot Stellar when it can only be executed within a Storm topology.  This REPL is intended to help mitigate that problem by allowing a user to replicate data encountered in production, isolate initialization errors, or understand function resolution problems.

### Getting Started

```
$ /usr/metron/<version>/bin/stellar

Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}

>>> %functions
BLOOM_ADD, BLOOM_EXISTS, BLOOM_INIT, BLOOM_MERGE, DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_YEAR, ...

>>> ?PROTOCOL_TO_NAME
PROTOCOL_TO_NAME
 desc: Convert the IANA protocol number to the protocol name       
 args: IANA Number                                                 
  ret: The protocol name associated with the IANA number.          

>>> 6
[=] 6
[?] save as: ip.protocol

>>> %vars
ip.protocol = 6

>>> PROTOCOL_TO_NAME(ip.protocol)
[=] TCP
```

### Command Line Options

```
$ /usr/metron/<version>/bin/stellar -h

usage: stellar
 -h,--help              Print help
 -z,--zookeeper <arg>   Zookeeper URL
```

#### -z, --zookeeper

*Optional*

Attempts to connect to Zookeeper and read the Metron global configuration.  Stellar functions may require the global configuration to work properly.  If found, the global configuration values are printed to the console.

```
$ /usr/metron/<version>/bin/stellar -z node1:2181
Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
>>> 
```

### Variable Assignment

Stellar has no concept of variable assignment.  For testing and debugging purposes, it is important to be able to create variables that simulate data contained within incoming messages.  The REPL has created a means for a user to perform variable assignment outside of the core Stellar language.  

After execution of a Stellar expression, the REPL will prompt for the name of a variable to save the expression result to.  If no name is provided, the result is not saved.

```
>>> 2+2
[=] 4.0
[?] save as: foo
>>> %vars
foo = 4.0
>>> 
```

### Magic Commands

The REPL has a set of magic commands that provide the REPL user with information about the Stellar execution environment.  The following magic commands are supported.

#### `%functions`

This command lists all functions resolvable in the Stellar environment.  Stellar searches the classpath for Stellar functions.  This can make it difficult in some cases to understand which functions are resolvable.  

```
>>> %functions
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
>>> 
```

#### `%vars` 

Lists all variables in the Stellar environment.

```
Stellar, Go!
{es.clustername=metron, es.ip=node1, es.port=9300, es.date.format=yyyy.MM.dd.HH}
>>> %vars
>>> 2+2
[=] 4.0
[?] save as: foo
>>> %vars
foo = 4.0
>>> 
```

#### `?<function>`

Returns formatted documentation of the Stellar function.  Provides the description of the function along with the expected arguments.

```
>>> ?BLOOM_ADD
BLOOM_ADD
 desc: Adds an element to the bloom filter passed in               
 args: bloom - The bloom filter, value* - The values to add        
  ret: Bloom Filter                                                
>>> ?IS_EMAIL
IS_EMAIL
 desc: Tests if a string is a valid email address                  
 args: address - The String to test                                
  ret: True if the string is a valid email address and false otherwise.
>>> 
```