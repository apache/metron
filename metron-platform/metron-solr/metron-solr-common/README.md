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

* [Introduction](#Introduction)
* [Configuration](#Configuration)
    * [The Indexing Topology](#The-Indexing-Topology)
    * [Performance: Server-side versus Client-side commits](#Performance:_Server-side_versus_Client-side_commits)
    * [Managing the risk of data loss](#Managing_the_risk_of_data_loss)
    * [Disabling client-side commits in Metron SOLR](#Disabling_client-side_commits_in_Metron_SOLR)
    * [Configuring server-side commits](#Configuring_server-side_commits)
* [Installing](#Installing)
* [Schemas](#Schemas)
* [Collections](#Collections)

## Introduction

Metron ships with Solr 6.6.2 support. Solr Cloud can be used as the real-time portion of the datastore resulting from [metron-indexing](../metron-indexing/README.md).

## Configuration

### The Indexing Topology

Solr is a viable option for indexing data in Metron and, similar to the Elasticsearch Writer, can be configured
via the global config.  The following settings are possible as part of the global config:
* `solr.zookeeper`
  * The zookeeper quorum associated with the SolrCloud instance.  This is a required field with no default.
* `solr.commitPerBatch`
  * This is a boolean which defines whether the writer commits every batch.  The default is `false`.
* `solr.commit.soft`
  * This is a boolean which defines whether the writer makes a soft commit or a durable commit.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `false`.
* `solr.commit.waitSearcher`
  * This is a boolean which defines whether the writer blocks the commit until the data is available to search.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `false`.
* `solr.commit.waitFlush`
  * This is a boolean which defines whether the writer blocks the commit until the data is flushed.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `false`.
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

### Performance: Server-side versus Client-side commits
It is important to note that SOLR is not a ACID compliant database, in particular there is no isolation between transactions.
This has a major impact on performance if client-side commits are configured, as a commit causes the entire collection to check-pointed
and written to disk, pausing any other client writing data to the same collection.  

In Metron, it is possible that dozens of storm spouts are writing data to the same SOLR collection simultaneously. 
Each of these spouts triggering a client-side commit on the same SOLR collection can have a catastrophic effect on performance.

SOLR can manage this issue by removing the responsibility of committing data from the clients, and letting the server 
trigger regular commits on a collection to flush data to disk.  Because this is server-side as opposed to client-side functionality, it 
 is controlled via the following parameters in each Collection's `solrconfig.xml` configuration file:
 
* autoCommit : Also called a 'hard' autocommit, this regularly synchronizes ingested data to disk to guarantee data persistence. 
* autoSoftCommit : Allows data to become visible to searchers without requiring an expensive commit to disk.

`openSearcher=true` is an expensive (hard) autoCommit option that triggers written data to be merged into on-disk indexes and become visible to searchers.
It is the equivalent of a soft and hard commit performed at the same time. It is rarely used for Near Real Time (NRT) search scenarios


The standard mantra for configuring SOLR for Near Real-Time Search is:

* autoCommit (with `openSearcher=false`) for persisting data,
* autoSoftCommit for making data visible.

These functions can (and nearly always do) have different time periods configured for them.
For example: 
* `autoCommit` (with `openSearcher=false` and `maxTime=30000` milliseconds) to persist data,
* `autoSoftCommit` (with `maxTime=120000` milliseconds) to make newly ingested data visible.

### Managing the risk of data loss
Experienced admins at this stage would now be asking the question as to what happens if SOLR crashes before a hard commit occurs to 
persist the data.  While SOLR does write data to its transaction log as soon as it is received, it does not fsync this log to disk until 
a hard commit is requested. Thus a hardware crash could technically risk the data collected up to the interval that hard autoCommits 
are configured for.

If the potential for data loss is unacceptable to the business then a common architecture used to manage the risk of
data loss is to have one or more replicas of each SOLR collection. When ingesting data into a collection that has replicas, SOLR will 
not return a result to the client until the data has been passed to each replica in a collection. Replicas immediately 
return acknowledgement as soon as the data is stored in local memory buffers. The configured autoCommit/autoSoftCommit intervals 
later process and store the data on the replica in exactly the same way it is processed and stored on the primary node.

So if you are using collection replicas you are protected against individual machine failures 
by the fact that your data is present in the main memory (immediately) and disks (after an interval of time) of other replicas. 
This type of architecture is similar to how other distributed systems like Kafka manages the performance/reliability trade-offs 

### Disabling client-side commits in Metron SOLR
To disable client-side commits in Metron's Storm spouts, make sure `solr.commitPerBatch = false` is set in Metron's
global json configuration section.  For details on how to change Metron's global configuration, please refer to the documentation
for Metron's `zk_load_config.sh` script.


### Configuring server-side commits
1. Make sure that client-side SOLR commits are disabled in Metron

1. You will need to change each collection's `solrconfig.xml` as described in the next step. Use either:

    1. The destructive option: update the collection template in Metron's schema directory (`$METRON_HOME/config/schema`) and delete and re-create the schema via Metron's 
delete-collection/create-collection [scripts](#Collections), or

    1. The update option: utilize SOLR's [Config Rest API](https://lucene.apache.org/solr/guide/7_4/config-api.html) to 
    dynamically modify an existing target collection's `solrconfig.xml` file. An example of using this API to update values is:
        ```
        curl http://<SOLR_HOST>>:<SOLR_PORT>/solr/<COLLECTION_NAME>>/config \
        -H 'Content-type:application/json' -d'{ "set-property" : \
        {"updateHandler.autoCommit.maxTime":120000, \
        "updateHandler.autoCommit.openSearcher":false,\
        "updateHandler.autoSoftCommit.maxTime":30000 }}'
        ```    
        Please note that this techniques uses Solr's configuration [variable substitution](https://lucene.apache.org/solr/guide/7_4/configuring-solrconfig-xml.html),
        and thus only works if the relevant field values in `solrconfig.xml` have not been overridden by the admin with hard-coded values. 
    
1. The following items in each collection's `solrconfg.xml` file need to be configured:    
```
<autoCommit>
  <maxTime>AUTOCOMMIT_INTERVAL_IN_MILLISECONDS</maxTime>
  <openSearcher>false</openSearcher>
</autoCommit>
<autoSoftCommit>
  <maxTime>AUTOSOFTCOMMIT_INTERVAL_IN_MILLISECONDS</maxTime>
</autoSoftCommit>
```  
 


## Installing

Solr is installed in the [full dev environment for CentOS](../../metron-deployment/development/centos6) by default but is not started initially.  Navigate to `$METRON_HOME/bin` 
and start Solr Cloud by running `start_solr.sh`.  

Metron's Ambari MPack installs several scripts in `$METRON_HOME/bin` that can be used to manage Solr.  A script is also provided for installing Solr Cloud outside of full dev.
The script performs the following tasks

* Stops ES and Kibana
* Downloads Solr
* Installs Solr
* Starts Solr Cloud

_Note: for details on setting up Solr Cloud in production mode, see https://lucene.apache.org/solr/guide/6_6/taking-solr-to-production.html_

Navigate to `$METRON_HOME/bin` and spin up Solr Cloud by running `install_solr.sh`.  After running this script, 
Elasticsearch and Kibana will have been stopped and you should now have an instance of Solr Cloud up and running at http://localhost:8983/solr/#/~cloud.  This manner of starting Solr
will also spin up an embedded Zookeeper instance at port 9983. More information can be found [here](https://lucene.apache.org/solr/guide/6_6/getting-started-with-solrcloud.html)

Solr can also be installed using [HDP Search 3](https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.4/bk_solr-search-installation/content/ch_hdp_search_30.html).  HDP Search 3 sets the Zookeeper root to 
`/solr` so this will need to be added to each url in the comma-separated list in Ambari UI -> Services -> Metron -> Configs -> Index Settings -> Solr Zookeeper Urls.  For example, in full dev
this would be `node1:2181/solr`.

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
you should refer to the Solr documentation [https://lucene.apache.org/solr/guide/6_6](here) in general
and [here](https://lucene.apache.org/solr/guide/6_6/documents-fields-and-schema-design.html) if you'd like to know more about schemas in Solr.

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
export ZOOKEEPER=node1:2181/solr
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
These scripts use the [Solr Collection API](http://lucene.apache.org/solr/guide/6_6/collections-api.html).