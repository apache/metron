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

if [ -z "${METRON_JDBC_PASSWORD}" ]; then
    echo "METRON_JDBC_PASSWORD unset. Exiting."
    exit 1
fi
## Join a list by a character
function join_by {
  local IFS="$1"
  shift
  echo "$*" 
}

METRON_VERSION=${project.version}
METRON_HOME="${METRON_HOME:-/usr/metron/${METRON_VERSION}}"
HBASE_HOME=${HBASE_HOME:-/usr/hdp/current/hbase-client}
METRON_REST_PORT=8082
METRON_SYSCONFIG="${METRON_SYSCONFIG:-/etc/default/metron}"
METRON_LOG_DIR="${METRON_LOG_DIR:-/var/log/metron}"
METRON_PID_FILE="${METRON_PID_FILE:-/var/run/metron/metron-rest.pid}"
PARSER_CONTRIB=${PARSER_CONTRIB:-$METRON_HOME/parser_contrib}
INDEXING_CONTRIB=${INDEXING_CONTRIB:-$METRON_HOME/indexing_contrib}
PARSER_LIB=$(find $METRON_HOME/lib/ -name metron-parsers*.jar)

echo "METRON_VERSION=${METRON_VERSION}"
echo "METRON_HOME=${METRON_HOME}"
echo "METRON_SYSCONFIG=${METRON_SYSCONFIG}"

if [ -f "$METRON_SYSCONFIG" ]; then
    echo "METRON_SYSCONFIG=${METRON_SYSCONFIG}"
    set -a
    . "$METRON_SYSCONFIG"
fi

METRON_REST_CLASSPATH="${METRON_REST_CLASSPATH:-$HADOOP_CONF_DIR:${HBASE_HOME}/conf}"

# Use a custom REST jar if provided, else pull the metron-rest jar
rest_jar_pattern="${METRON_HOME}/lib/metron-rest*.jar"
rest_files=( ${rest_jar_pattern} )
echo "Default metron-rest jar is: ${rest_files[0]}"
METRON_REST_CLASSPATH+=":${rest_files[0]}"
METRON_REST_CLASSPATH+=":$PARSER_LIB"

if [ -d "$PARSER_CONTRIB" ]; then
  contrib_jar_pattern="${PARSER_CONTRIB}/*.jar"
  contrib_list=( $contrib_jar_pattern ) # expand the glob to a list
  contrib_classpath=$(join_by : "${contrib_list[@]}") #join the list by a colon
  echo "Parser Contrib jars are: $contrib_classpath"
  METRON_REST_CLASSPATH+=":${contrib_classpath}"
fi

if [ -d "$INDEXING_CONTRIB" ]; then
  contrib_jar_pattern="${INDEXING_CONTRIB}/*.jar"
  contrib_list=( $contrib_jar_pattern ) # expand the glob to a list
  contrib_classpath=$(join_by : "${contrib_list[@]}") #join the list by a colon
  echo "Indexing Contrib jars are: $contrib_classpath"
  METRON_REST_CLASSPATH+=":${contrib_classpath}"
fi

echo "METRON_SPRING_PROFILES_ACTIVE=${METRON_SPRING_PROFILES_ACTIVE}"

# the vagrant Spring profile provides configuration values, otherwise configuration is provided by rest_application.yml
if [[ !(${METRON_SPRING_PROFILES_ACTIVE} == *"vagrant"*) ]]; then
    METRON_CONFIG_LOCATION=" --spring.config.location=$METRON_HOME/config/rest_application.yml,classpath:/application.yml"
    echo "METRON_CONFIG_LOCATION=${METRON_CONFIG_LOCATION}"
    METRON_SPRING_OPTIONS+=${METRON_CONFIG_LOCATION}
fi
METRON_SPRING_OPTIONS+=" --server.port=$METRON_REST_PORT"
if [ ${METRON_SPRING_PROFILES_ACTIVE} ]; then
    METRON_PROFILES_ACTIVE=" --spring.profiles.active=${METRON_SPRING_PROFILES_ACTIVE}"
    echo "METRON_PROFILES_ACTIVE=${METRON_PROFILES_ACTIVE}"
    METRON_SPRING_OPTIONS+=${METRON_PROFILES_ACTIVE}
fi

if [ ${METRON_JDBC_CLIENT_PATH} ]; then
    METRON_REST_CLASSPATH+=":${METRON_JDBC_CLIENT_PATH}"
fi

# Use a custom indexing jar if provided, else pull the metron-elasticsearch uber jar
if [ ${METRON_INDEX_CP} ]; then
    echo "Default metron indexing jar is: ${METRON_INDEX_CP}"
    METRON_REST_CLASSPATH+=":${METRON_INDEX_CP}"
else
    indexing_jar_pattern="${METRON_HOME}/lib/metron-elasticsearch*uber.jar"
    indexing_files=( ${indexing_jar_pattern} )
    echo "Default metron indexing jar is: ${indexing_files[0]}"
    METRON_REST_CLASSPATH+=":${indexing_files[0]}"
fi

echo "METRON_REST_CLASSPATH=${METRON_REST_CLASSPATH}"

echo "Starting application"
${JAVA_HOME}/bin/java ${METRON_JVMFLAGS} \
-cp ${METRON_REST_CLASSPATH} \
org.apache.metron.rest.MetronRestApplication \
${METRON_SPRING_OPTIONS} >> ${METRON_LOG_DIR}/metron-rest.log 2>&1 & echo $! > ${METRON_PID_FILE};
