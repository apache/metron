#!/usr/bin/env bash
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
$METRON_HOME/bin/zk_load_configs.sh -z zookeeper:2181 -m PUSH -i $METRON_HOME/config/zookeeper

METRON_REST_CLASSPATH="$METRON_HOME/lib/metron-rest-$METRON_VERSION.jar:$METRON_HOME/lib/metron-parsers-$METRON_VERSION-uber.jar:$METRON_HOME/lib/metron-elasticsearch-$METRON_VERSION-uber.jar"

java -cp $METRON_REST_CLASSPATH org.apache.metron.rest.MetronRestApplication --spring.config.location=$METRON_HOME/config/application-docker.yml --spring.profiles.active=dev --server.port=8082
