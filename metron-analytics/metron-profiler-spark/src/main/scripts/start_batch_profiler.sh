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
PROFILER_JAR=${METRON_HOME}/lib/${project.artifactId}-${METRON_VERSION}.jar
MAIN_CLASS=org.apache.metron.profiler.spark.cli.BatchProfilerCLI
PROFILER_PROPS=${PROFILER_PROPS:-"${METRON_HOME}/config/batch-profiler.properties"}
PROFILES_FILE=${PROFILES:-"${METRON_HOME}/config/zookeeper/profiler.json"}
SPARK_HOME=${SPARK_HOME:-"/usr/hdp/current/spark2-client"}

${SPARK_HOME}/bin/spark-submit \
    --class ${MAIN_CLASS} \
    --properties-file ${PROFILER_PROPS} \
    ${PROFILER_JAR} \
    --config ${PROFILER_PROPS} \
    --profiles ${PROFILES_FILE}
