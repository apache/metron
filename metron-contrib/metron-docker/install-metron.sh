#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
METRON_DOCKER_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $METRON_DOCKER_ROOT/.env
METRON_PLATFORM_ROOT=$METRON_DOCKER_ROOT/../metron-platform
if [ $# -gt 0 ] && [ $1 == "-b" ]
    then cd $METRON_PLATFORM_ROOT && mvn clean package -DskipTests
fi
mkdir -p $METRON_DOCKER_ROOT/hbase/data-management
mkdir -p $METRON_DOCKER_ROOT/storm/parser/
mkdir -p $METRON_DOCKER_ROOT/storm/enrichment/
mkdir -p $METRON_DOCKER_ROOT/storm/indexing/
mkdir -p $METRON_DOCKER_ROOT/storm/elasticsearch/
echo Installing HBase dependencies
cp $METRON_PLATFORM_ROOT/metron-data-management/target/metron-data-management-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/hbase/data-management
echo Installing Storm dependencies
cp $METRON_PLATFORM_ROOT/metron-parsers/metron-parsers-common/target/metron-parsers-common-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/storm/parser/
cp $METRON_PLATFORM_ROOT/metron-parsers/metron-parsing-storm/target/metron-parsing-storm-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/storm/parser/
cp $METRON_PLATFORM_ROOT/metron-enrichment/target/metron-enrichment-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/storm/enrichment/
cp $METRON_PLATFORM_ROOT/metron-indexing/target/metron-indexing-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/storm/indexing/
echo Installing Elasticsearch dependencies
cp $METRON_PLATFORM_ROOT/metron-elasticsearch/target/metron-elasticsearch-$METRON_VERSION-archive.tar.gz $METRON_DOCKER_ROOT/storm/elasticsearch/
