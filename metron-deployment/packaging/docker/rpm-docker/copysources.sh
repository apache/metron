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

# Utility file for copying sources - will be replaced in a later PR
mkdir SOURCES
BASE_DIR="../../../../metron-platform"
files=(
${BASE_DIR}/metron-common/target/metron-common-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-parsers/target/metron-parsers-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-enrichment/target/metron-enrichment-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-data-management/target/metron-data-management-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-solr/target/metron-solr-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-elasticsearch/target/metron-elasticsearch-0.2.0BETA-archive.tar.gz
${BASE_DIR}/metron-pcap-backend/target/metron-pcap-backend-0.2.0BETA-archive.tar.gz
)
for f in ${files[@]}
do
    cp $f SOURCES/
done
