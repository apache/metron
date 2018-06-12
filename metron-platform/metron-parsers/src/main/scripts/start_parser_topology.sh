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
TOPOLOGY_JAR=metron-parsers-$METRON_VERSION-uber.jar
PARSER_CONTRIB=${PARSER_CONTRIB:-$METRON_HOME/parser_contrib}
EXTRA_ARGS="$@"

if [ -d "$PARSER_CONTRIB" ]; then

  export STORM_EXT_CLASSPATH=$METRON_HOME/lib/$TOPOLOGY_JAR
  export EXTRA_JARS=$(ls -m $PARSER_CONTRIB/*.jar | tr -d ' ' | tr -d '\n' | sed 's/\/\//\//g')

  STORM_VERSION=`storm version | grep Storm | awk '{print $2}' | cut -d. -f1,2,3`
  STORM_COMPAT_CUTOFF="1.1.0"
  STORM_COMPAT_VERSION=`printf "$STORM_COMPAT_CUTOFF\n$STORM_VERSION" | sort -V | head -n1`

  if [ "$STORM_COMPAT_VERSION" = "$STORM_COMPAT_CUTOFF" ]; then
    echo "Third party parser support for Storm >= 1.1.0; found Storm $STORM_VERSION"
    EXTRA_ARGS+=" --jars $EXTRA_JARS"
  else
    echo "Legacy third party parser support for Storm < 1.1.0; found Storm $STORM_VERSION"
    EXTRA_ARGS+=" -c client.jartransformer.class=org.apache.metron.parsers.topology.MergeAndShadeTransformer"
  fi
fi

echo "Submitting parser topology; args='$EXTRA_ARGS'"
storm jar $METRON_HOME/lib/$TOPOLOGY_JAR org.apache.metron.parsers.topology.ParserTopologyCLI $EXTRA_ARGS
