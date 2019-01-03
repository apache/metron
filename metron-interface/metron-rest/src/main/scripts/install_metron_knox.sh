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
METRON_HOME=${METRON_HOME:-/usr/metron/${METRON_VERSION}}
KNOX_USER=${KNOX_USER:-knox}
KNOX_GROUP=${KNOX_GROUP:-knox}
KNOX_HOME=${KNOX_HOME:-/usr/hdp/current/knox-server}
KNOX_METRON_REST_DIR=$KNOX_HOME/data/services/metron-rest
KNOX_METRON_ALERTS_DIR=$KNOX_HOME/data/services/metron-alerts
KNOX_METRON_MANAGEMENT_DIR=$KNOX_HOME/data/services/metron-management

if [ -d "$KNOX_HOME" ]
then
    mkdir -p $KNOX_METRON_REST_DIR/$METRON_VERSION
    mkdir -p $KNOX_METRON_ALERTS_DIR/$METRON_VERSION
    mkdir -p $KNOX_METRON_MANAGEMENT_DIR/$METRON_VERSION

    cp $METRON_HOME/config/knox/data/services/rest/* $KNOX_METRON_REST_DIR/$METRON_VERSION
    cp $METRON_HOME/config/knox/data/services/alerts/* $KNOX_METRON_ALERTS_DIR/$METRON_VERSION
    cp $METRON_HOME/config/knox/data/services/management/* $KNOX_METRON_MANAGEMENT_DIR/$METRON_VERSION
    cp $METRON_HOME/config/knox/conf/topologies/metron.xml $KNOX_HOME/conf/topologies

    sudo chown -R $KNOX_USER:$KNOX_GROUP $KNOX_METRON_REST_DIR
    sudo chown -R $KNOX_USER:$KNOX_GROUP $KNOX_METRON_ALERTS_DIR
    sudo chown -R $KNOX_USER:$KNOX_GROUP $KNOX_METRON_MANAGEMENT_DIR
else
    echo "$KNOX_HOME does not exist. Skipping Metron Knox installation."
fi