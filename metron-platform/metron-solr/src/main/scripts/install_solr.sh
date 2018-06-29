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

# This is provided for development purposes

# Full dev env setup script for Solr Cloud 6.6.2
# - Stops ES and Kibana
# - Downloads Solr
# - Installs Solr
# - Starts Solr Cloud

# Note: for production mode, see https://lucene.apache.org/solr/guide/6_6/taking-solr-to-production.html

service kibana stop
service elasticsearch stop

SOLR_VERSION=${global_solr_version}
SOLR_USER=solr
SOLR_SERVICE=$SOLR_USER
SOLR_VAR_DIR="/var/$SOLR_SERVICE"

# create user if not exists
solr_uid="`id -u "$SOLR_USER"`"
if [ $? -ne 0 ]; then
  echo "Creating new user: $SOLR_USER"
  adduser --system -U -m --home-dir "$SOLR_VAR_DIR" "$SOLR_USER"
fi
cd $SOLR_VAR_DIR
wget http://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.tgz
tar zxvf solr-${SOLR_VERSION}.tgz
chown -R $SOLR_USER:$SOLR_USER solr-${SOLR_VERSION}
cd solr-${SOLR_VERSION}
su $SOLR_USER -c "bin/solr -e cloud -noprompt"
sleep 5
bin/solr status
bin/solr healthcheck -c gettingstarted

# These commands can be used for running multiple Solr services on a single node for cloud mode
# This approach extracts the install script from the tarball and will setup the solr user along
# with init.d service scripts and then startup the services.

# tar xzf solr-${SOLR_VERSION}.tgz solr-${SOLR_VERSION}/bin/install_solr_service.sh --strip-components=2
# sudo bash ./install_solr_service.sh solr-${SOLR_VERSION}.tgz -n
# sudo bash ./install_solr_service.sh solr-${SOLR_VERSION}.tgz -s solr2 -p 8984 -n
# echo "export ZK_HOST=node1:2181" >> /etc/default/solr.in.sh

