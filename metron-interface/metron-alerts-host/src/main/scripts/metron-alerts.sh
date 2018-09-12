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

if [ -z "${METRON_SSL_PASSWORD}" ]; then
    echo "METRON_SSL_PASSWORD unset."
fi

METRON_VERSION=${project.version}
METRON_HOME="${METRON_HOME:-/usr/metron/${METRON_VERSION}}"
METRON_SYSCONFIG="${METRON_SYSCONFIG:-/etc/default/metron}"

echo "METRON_VERSION=${METRON_VERSION}"
echo "METRON_HOME=${METRON_HOME}"
echo "METRON_SYSCONFIG=${METRON_SYSCONFIG}"

if [ -f "$METRON_SYSCONFIG" ]; then
    echo "METRON_SYSCONFIG=${METRON_SYSCONFIG}"
    set -a
    . "$METRON_SYSCONFIG"
fi

echo "METRON_SPRING_PROFILES_ACTIVE=${METRON_SPRING_PROFILES_ACTIVE}"

METRON_CONFIG_LOCATION=" --spring.config.location=classpath:/application.yml,$METRON_HOME/config/alerts_ui.yml"
echo "METRON_CONFIG_LOCATION=${METRON_CONFIG_LOCATION}"
METRON_SPRING_OPTIONS+=${METRON_CONFIG_LOCATION}

# Find the metron alerts jar
files=( "${METRON_HOME}/lib/metron-alerts-host-*.jar" )
echo "Default metron-alerts-host jar is: ${files[0]}"
APP_JAR="${files[0]}"

export CONF_FOLDER=$METHRON_HOME/config
export LOG_FOLDER=/var/log/metron/
export PID_FOLDER=/var/run/metron/
export RUN_ARGS=$METRON_SPRING_OPTIONS
export APP_NAME=metron-alerts
export MODE=service
${APP_JAR} $1 
