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

## Installing

A script is provided in the installation for installing Solr Cloud in quick-start mode in the [full dev environment for CentOS](../../metron-deployment/development/centos6).
The script is installed as part of the Solr RPM. Metron's Ambari MPack does not currently manage Solr installation in any way, so
you must first install the RPM in order to run the Solr setup script.

The script performs the following tasks

* Stops ES and Kibana
* Downloads Solr
* Installs Solr
* Starts Solr Cloud

_Note: for details on setting up Solr Cloud in production mode, see https://lucene.apache.org/solr/guide/6_6/taking-solr-to-production.html_

Login to the full dev environment as root and execute the following to install the Solr RPM.

```
rpm -ivh /localrepo/metron-solr-*.rpm
```

This will lay down the necessary files to setup Solr Cloud. Navigate to `$METRON_HOME/bin` and spin up Solr Cloud by running `install_solr.sh`.

After running this script, Elasticsearch and Kibana will have been stopped and you should now have an instance of Solr Cloud up and running at http://localhost:8983/solr/#/~cloud. This manner
of starting Solr will also spin up an embedded Zookeeper instance at port 9983. More information can be found [here](https://lucene.apache.org/solr/guide/6_6/getting-started-with-solrcloud.html)
