#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
METRON_VERSION=${project.version}
METRON_HOME=/usr/metron/$METRON_VERSION
ZOOKEEPER=${ZOOKEEPER:-localhost:2181}
ZOOKEEPER_HOME=${ZOOKEEPER_HOME:-/usr/hdp/current/zookeeper-client}
SECURITY_ENABLED=${SECURITY_ENABLED:-false}
NEGOTIATE=''
if [ ${SECURITY_ENABLED,,} == 'true' ]; then
    NEGOTIATE=' --negotiate -u : '
fi

# Get the first Solr node from the list of live nodes in Zookeeper
SOLR_NODE=`$ZOOKEEPER_HOME/bin/zkCli.sh -server $ZOOKEEPER ls /live_nodes | tail -n 1 | sed 's/\[\([^,]*\).*\]/\1/' | sed 's/_solr//'`

# Delete the collection
curl -X GET $NEGOTIATE "http://$SOLR_NODE/solr/admin/collections?action=DELETE&name=$1"
