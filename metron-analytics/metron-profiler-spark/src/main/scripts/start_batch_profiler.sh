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
METRON_HOME=/usr/metron/${METRON_VERSION}
PROFILER_JAR=${METRON_HOME}/lib/${project.artifactId}-${METRON_VERSION}-uber.jar
STELLAR_JAR=${METRON_HOME}/lib/stellar-common-$METRON_VERSION-uber.jar
MAIN_CLASS=org.apache.metron.profiler.spark.cli.BatchProfilerCLI
PROFILER_PROPS=${PROFILER_PROPS:-"${METRON_HOME}/config/batch-profiler.properties"}
SPARK_HOME=${SPARK_HOME:-"/usr/hdp/current/spark2-client"}

PROFILES_FILE=${PROFILES:-"${METRON_HOME}/config/zookeeper/profiler.json"}
ZOOKEEPER_LOCATION=${ZOOKEEPER:-"node1:2181"}

# allow for an override on event time source via environment variable
if [ -n "$SPARK_PROFILER_EVENT_TIMESTAMP_FIELD" ]; then
  EVENT_TIMESTAMP="--timestampfield ${SPARK_PROFILER_EVENT_TIMESTAMP_FIELD}"
fi

if [ -n "$SPARK_PROFILER_USE_ZOOKEEPER" ]; then
  PROFILES_LOCATION="--zookeeper ${ZOOKEEPER_LOCATION}"
else
  PROFILES_LOCATION="--profiles ${PROFILES_FILE}"
fi

${SPARK_HOME}/bin/spark-submit \
    --class ${MAIN_CLASS} \
    --jars ${STELLAR_JAR} \
    --properties-file ${PROFILER_PROPS} \
    ${PROFILER_JAR} \
    --config ${PROFILER_PROPS} \
    ${PROFILES_LOCATION} ${EVENT_TIMESTAMP} \
    "$@"
