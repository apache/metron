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
# Solr in Metron

## Table of Contents

* [Introduction](#introduction)
* [Configuration](#configuration)
* [Installing](#installing)
* [Schemas](#schemas)
* [Collections](#collections)

## Introduction

Metron ships with Solr 7.4.0 support. Solr Cloud can be used as the real-time portion of the datastore resulting from [metron-indexing](../metron-indexing/README.md).

## Configuration

### The Indexing Topology

Solr is a viable option for indexing data in Metron and, similar to the Elasticsearch Writer, can be configured
via the global config.  The following settings are possible as part of the global config:
* `solr.zookeeper`
  * The zookeeper quorum associated with the SolrCloud instance.  This is a required field with no default.
* `solr.commitPerBatch`
  * This is a boolean which defines whether the writer commits every batch.  The default is `true`.
  * _WARNING_: If you set this to `false`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.soft`
  * This is a boolean which defines whether the writer makes a soft commit or a durable commit.  See [here](https://lucene.apache.org/solr/guide/7_4/near-real-time-searching.html)  The default is `false`.
  * _WARNING_: If you set this to `true`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.waitSearcher`
  * This is a boolean which defines whether the writer blocks the commit until the data is available to search.  See [here](https://lucene.apache.org/solr/guide/7_4/near-real-time-searching.html)  The default is `true`.
  * _WARNING_: If you set this to `false`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.waitFlush`
  * This is a boolean which defines whether the writer blocks the commit until the data is flushed.  See [here](https://lucene.apache.org/solr/guide/7_4/near-real-time-searching.html)  The default is `true`.
  * _WARNING_: If you set this to `false`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.collection`
  * The default solr collection (if unspecified, the name is `metron`).  By default, sensors will write to a collection associated with the index name in the
  indexing config for that sensor.  If that index name is the empty string, then the default collection will be used.
* `solr.http.config`
  * This is a map which allows users to configure the Solr client's HTTP client.
  * Possible fields here are:
    * `socketTimeout` : Socket timeout measured in ms, closes a socket if read takes longer than x ms to complete
    throws `java.net.SocketTimeoutException: Read timed out exception`
    * `connTimeout` : Connection timeout measures in ms, closes a socket if connection cannot be established within x ms
    with a `java.net.SocketTimeoutException: Connection timed out`
    * `maxConectionsPerHost` : Maximum connections allowed per host
    * `maxConnections` :  Maximum total connections allowed
    * `retry` : Retry http requests on error
    * `allowCompression` :  Allow compression (deflate,gzip) if server supports it
    * `followRedirects` : Follow redirects
    * `httpBasicAuthUser` : Basic auth username
    * `httpBasicAuthPassword` : Basic auth password
    * `solr.ssl.checkPeerName` : Check peer name


## Installing

Solr is installed in the [full dev environment for CentOS](../../metron-deployment/development/centos7) by default but is not started initially.  Navigate to `$METRON_HOME/bin` 
and start Solr Cloud by running `start_solr.sh`.  

Metron's Ambari MPack installs several scripts in `$METRON_HOME/bin` that can be used to manage Solr.  A script is also provided for installing Solr Cloud outside of full dev.
The script performs the following tasks

* Stops ES and Kibana
* Downloads Solr
* Installs Solr
* Starts Solr Cloud

Note: for details on setting up Solr Cloud in production mode, see https://lucene.apache.org/solr/guide/7_4/taking-solr-to-production.html

Navigate to `$METRON_HOME/bin` and spin up Solr Cloud by running `install_solr.sh`.  After running this script,
Elasticsearch and Kibana will have been stopped and you should now have an instance of Solr Cloud up and running at http://localhost:8983/solr/#/~cloud.  This manner of starting Solr
will also spin up an embedded Zookeeper instance at port 9983. More information can be found [here](https://lucene.apache.org/solr/guide/7_4/getting-started-with-solrcloud.html)

## Enabling Solr

Elasticsearch is the real-time store used by default in Metron.  Solr can be enabled following these steps:

1. Stop the Metron Indexing component in Ambari.
1. Update Ambari UI -> Services -> Metron -> Configs -> Index Settings -> Solr Zookeeper Urls to match the Solr installation described in the previous section.
1. Change Ambari UI -> Services -> Metron -> Configs -> Indexing -> Index Writer - Random Access -> Random Access Search Engine to `Solr`.
1. Change Ambari UI -> Services -> Metron -> Configs -> REST -> Source Type Field Name to `source.type`.
1. Change Ambari UI -> Services -> Metron -> Configs -> REST -> Threat Triage Score Field Name to `threat.triage.score`.
1. Start the Metron Indexing component in Ambari.
1. Restart Metron REST and the Alerts UI in Ambari.

This will automatically create collections for the schemas shipped with Metron:

* bro
* snort
* yaf
* error (used internally by Metron)
* metaalert (used internall by Metron)

Any other collections must be created manually before starting the Indexing component.  Alerts should be present in the Alerts UI after enabling Solr.

## Schemas

As of now, we have mapped out the Schemas in `src/main/config/schema`.
Ambari will eventually install these, but at the moment it's manual and
you should refer to the Solr documentation [https://lucene.apache.org/solr/guide/7_4](here) in general
and [here](https://lucene.apache.org/solr/guide/7_4/documents-fields-and-schema-design.html) if you'd like to know more about schemas in Solr.

In Metron's Solr DAO implementation, document updates involve reading a document, applying the update and replacing the original by reindexing the whole document.  
Indexing LatLonType and PointType field types stores data in internal fields that should not be returned in search results.  For these fields a dynamic field type matching the suffix needs to be added to store the data points.
Solr 6+ comes with a new LatLonPointSpatialField field type that should be used instead of LatLonType if possible.  Otherwise, a LatLongType field should be defined as:
```
<dynamicField name="*.location_point" type="location" multiValued="false" docValues="false"/>
<dynamicField name="*_coordinate" type="pdouble" indexed="true" stored="false" docValues="false"/>
<fieldType name="location" class="solr.LatLonType" subFieldSuffix="_coordinate"/>
```
A PointType field should be defined as:
```
<dynamicField name="*.point" type="point" multiValued="false" docValues="false"/>
<dynamicField name="*_point" type="pdouble" indexed="true" stored="false" docValues="false"/>
<fieldType name="point" class="solr.PointType" subFieldSuffix="_point"/>
```
If any copy fields are defined, stored and docValues should be set to false.

## Collections

Convenience scripts are provided with Metron to create and delete collections.  Ambari uses these scripts to automatically create collections.  To use them outside of Ambari, a few environment variables must be set first:
```
# Path to the zookeeper node used by Solr
export ZOOKEEPER=node1:9983
# Set to true if Kerberos is enabled
export SECURITY_ENABLED=true
```
The scripts can then be called directly with the collection name as the first argument .  For example, to create the bro collection:
```
$METRON_HOME/bin/create_collection.sh bro
```
To delete the bro collection:
```
$METRON_HOME/bin/delete_collection.sh bro
```
The `create_collection.sh` script depends on schemas installed in `$METRON_HOME/config/schema`.  There are several schemas that come with Metron:

* bro
* snort
* yaf
* metaalert
* error

Additional schemas should be installed in that location if using the `create_collection.sh` script.  Any collection can be deleted with the `delete_collection.sh` script.
These scripts use the [Solr Collection API](http://lucene.apache.org/solr/guide/7_4/collections-api.html).

## Time routed alias support
An alias is a pointer that points to a Collection.  Sending a document to an alias sends it to the collection the alias points too.
The collection an alias points to can be changed with a single, low-cost operation. Time Routed Aliases (TRAs) is a SolrCloud feature 
that manages an alias and a time sequential series of collections.

A TRA automatically creates new collections and (optionally) deletes old ones as it routes documents to the correct collection 
based on the timestamp of the event. This approach allows for indefinite indexing of data without degradation of performance otherwise 
experienced due to the continuous growth of a single index.

A TRA is defined with a minimum time and a defined interval period and SOLR provides a collection for each interval for a 
contiguous set of datetime intervals from the start date to the maximum received document date. Collections are created to host documents based on examining the document's event-time. If a document does not currently 
have a collection created for it, then starting at the minimum date SOLR will create a collection for each interval that does not have one
 up until the interval period needed to store the current document.  

See SOLR documentation [\(1\)](https://lucene.apache.org/solr/guide/7_4/time-routed-aliases.html) 
[\(2\)](https://lucene.apache.org/solr/guide/7_4/collections-api.html#createalias) for more information.

### Setting up Time routed alias support

Using SOLR's Time-based routing requires using SOLR's native Datetime types.  At the moment, Metron uses the LongTrie field type
to store dates, which is not a SOLR native datetime type.  At a later stage the Metron code-base will be changed to use SOLR native datetime types, 
but for now a workaround procedure has been created to allow for the use of Time-based routing, but still allows Metron to use LongTrie type.
This procedure only works for new collections, and is as follows:

1. Add the following field type definition near the end of the schema.xml document (the entry must be inside the schema tags)
    ```
      <fieldType name="datetime" stored="false" indexed="false" multiValued="false" docValues="true" class="solr.DatePointField"/>
    ```
   
1. Add the following field definition near the start of the schema.xml document (the entry must be inside the schema tags)
    ```
      <field name="datetime" type="datetime" />
    ```
   
1. Create the configset for the collection:  Assuming that the relevant collections schema.xml and solrconfig.xml are located in 
`$METRON_HOME/config/schema/<collection name>` folder, use the following command:
    ```
      $METRON_HOME/bin/create_configset collectΩΩion name>
    ```
   
1. Create the time-based routing alias for the collection:
Assuming the following values:
    * SOLR_HOST:  Host SOLR is installed on
    * ALIAS_NAME:  Name of the new alias
    * ROUTER_START: Beginning time-period datetime in ISO-8601 standard - milliseconds potion of the date must be 0, some examples are 
'2018-01-14T21:00:00:00', 'NOW/SECOND', 'NOW/DAY'
    * ROUTER_FIELD: The name of the field in the incoming document that contains the datetime to route on - field must be of SOLR type DateTrie or DatePoint. 
    For METRON this is standardised as field `datetime`.
    * ROUTER_INTERVAL: SOLR Date math format. The interval of time that each collection holds. eg "+1DAY", "+6HOUR", "+1WEEK" (`+` must be URL encoded to `%2B` )
    * ROUTER_MAXFUTUREMS: Optional field containing the number of milliseconds into the future that it is considered valid to have an event time for.
    Documents with event time exceeding this time period in the future are considered invalid and an error is returned.  Used as a sanity check to prevent 
    the creation of unnecessary collections due to corrupted datetimes in events. Defaults to 10 minutes into the future.
    * ROUTER_AUTODELETEAGE: Optional field in SOLR Date math format. If this field is present, any time a collection is created, 
    the oldest collections are assessed for deletion. Collections are deleted if the datetime interval they represent is older then
     NOW - AUTODELETE_INTERVAL.  eg -2WEEK, -3MONTH, -1YEAR 
    * CONFIGSET: Name of the colleciton configset that was created in the previous step - this is used a template for new collections.
    * create-collection.*: These allow for Create collection options (e.g. numShards or numReplicas) to be specified directly in the 
    create alias command.  
    
    Then the following command will create a time-routed alias.
    ```
    curl http://$SOLR_HOST:8983/solr/admin/collections?action=CREATEALIAS\
   &name=$ALIAS_NAME\
   &router.start=$ROUTER_START\
   &router.field=$ROUTER_FIELD\
   &router.name=time\
   &router.interval=$ROUTER_INTERVAL\
   &router.maxFutureMs=$ROUTER_MAXFUTUREMS\
   &create-collection.collection.configName=$CONFIGSET\
   &create-collection.numShards=2
    ```
1. Add a Metron Parser Stellar field transformation to the parser config that adds a correctly formatted datetime string to the event as it is being parsed.
    1. Set environment variables for later reference
        ```
        source /etc/defaults/metron
        export HDP_HOME="/usr/hdp/current"
        export PARSER_NAME= <The name of the relevant parser>
        ```
    1. Pull the most recent sensor parser config from zookeeper
        ```
        ${METRON_HOME}/bin/zk_load_configs.sh -i ${METRON_HOME}/config/zookeeper -m PULL -c PARSER -n $PARSER_NAME -z $ZOOKEEPER
        ```
    1. Open the file to the relevant sensor parser at `$METRON_HOME/config/zookeeper/parsers/$PARSER_NAME.json`
    
    1. Add to the sensor parser config json field the following transformation
        ```
          "fieldTransformations" : [{
        input
              "transformation" : "STELLAR"
            ,"output" : [ "datetime"  ]
            ,"config" : {
              "datetime" : "DATE_FORMAT("yyyy-MM-dd'T'HH:mm:ss.SSSX",timestamp)"
             }
            }]
        ```
    1.  Push the configuration back to zookeeper
        ```
         ${METRON_HOME}/bin/zk_load_configs.sh -i ${METRON_HOME}/config/zookeeper -m PUSH -c PARSER -n $PARSER_NAME -z $ZOOKEEPER  
        ```
    1.  Run kafka console to monitor correct operation of the field transformation
        ```
        ${HDP_HOME}/kafka-broker/bin/kafka-console-consumer.sh --bootstrap-server $BROKERLIST --topic $PARSER_NAME 
        ```    

1. Config Metron SOLR indexing to push documents to the newly created Collection Alias.
    1. Pull the most recent index config from zookeeper
    ```
      ${METRON_HOME}/bin/zk_load_configs.sh -i ${METRON_HOME}/config/zookeeper -m PULL -c INDEXING -n $PARSER_NAME -z $ZOOKEEPER
    ```
    1. Edit the file ${METRON_HOME}/config/zookeeper/indexing/$PARSER_NAME.json
    1. Update the solr/index field to the `ALIAS_NAME` value you configured for the SOLR time-based routing alias.
    1. Push the configuration back to zookeeper
    ```
      ${METRON_HOME}/bin/zk_load_configs.sh -i ${METRON_HOME}/config/zookeeper -m PUSH -c INDEXING -n $PARSER_NAME -z $ZOOKEEPER
    ```

1. 


