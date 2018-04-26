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
* [Installing](#installing)

## Introduction

Metron ships with Solr 6.6.2 support. Solr Cloud can be used as the real-time portion of the datastore resulting from [metron-indexing](../metron-indexing/README.md).

## Configuration

### The Indexing Topology

Solr is a viable option for the `random access topology` and, similar to the Elasticsearch Writer, can be configured
via the global config.  The following settings are possible as part of the global config:
* `solr.zookeeper`
  * The zookeeper quorum associated with the SolrCloud instance.  This is a required field with no default.
* `solr.commitPerBatch`
  * This is a boolean which defines whether the writer commits every batch.  The default is `true`.
  * _WARNING_: If you set this to `false`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.soft`
  * This is a boolean which defines whether the writer makes a soft commit or a durable commit.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `false`.
  * _WARNING_: If you set this to `true`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.waitSearcher`
  * This is a boolean which defines whether the writer blocks the commit until the data is available to search.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `true`.
  * _WARNING_: If you set this to `false`, then commits will happen based on the SolrClient's internal mechanism and
    worker failure *may* result data being acknowledged in storm but not written in Solr.
* `solr.commit.waitFlush`
  * This is a boolean which defines whether the writer blocks the commit until the data is flushed.  See [here](https://lucene.apache.org/solr/guide/6_6/near-real-time-searching.html#NearRealTimeSearching-AutoCommits)  The default is `true`.
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