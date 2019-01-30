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

METRON_VERSION=0.7.1
METRON_HOME=${METRON_HOME:-"/usr/metron/$METRON_VERSION"}
DATE_FORMAT=${DATE_FORMAT:-"yyyyMMdd"}
USER=$(whoami)
USER_HOMEDIR=${USER_HOMEDIR:-`hdfs getconf -confKey dfs.user.home.dir.prefix`/$USER}
QUERY_HOME=${QUERY_HOME:-"$USER_HOMEDIR/queries"}
WEBHDFS_HOSTNAME=${WEBHDFS_HOSTNAME:-`hdfs getconf -confKey dfs.namenode.http-address`}
QUERY=${QUERY:-$1}
START_TIME=${START_TIME:-$2}
END_TIME=${END_TIME:-$3}
RECORDS_PER_FILE=${RECORDS_PER_FILE:-10000}
NUMBER_OF_REDUCERS=${NUMBER_OF_REDUCERS:-10}
PCAP_DATA_PATH=${PCAP_DATA_PATH:-/apps/metron/pcap}
if [ -z "$QUERY" ]; then
  echo "You must specify a query."
  exit 1
fi
if [ -z "$START_TIME" ]; then
  echo "You must specify a start time."
  exit 2
fi
TIMESTAMP_EPOCH=$(date +%s%N | cut -b1-13)
if [ -f /proc/sys/kernel/random/uuid ];then
  UUID=$(cat /proc/sys/kernel/random/uuid | sed 's/-//g')
  PREFIX="$TIMESTAMP_EPOCH-$UUID"
else
  QUERY_HASH=$(echo $QUERY | md5sum | awk '{print $1}')
  PREFIX="$TIMESTAMP_EPOCH-$QUERY_HASH"
fi
if [ -z "$END_TIME" ]; then
  CMD=$($METRON_HOME/bin/pcap_query.sh query --prefix "$PREFIX" --query "$QUERY" -st "$START_TIME" -df "$DATE_FORMAT" -rpf "$RECORDS_PER_FILE" -nr "$NUMBER_OF_REDUCERS" -bp "$PCAP_DATA_PATH" \"2>&1)
  SUMMARY="Packets conforming to $QUERY starting at $START_TIME ending now"
else
  CMD=$($METRON_HOME/bin/pcap_query.sh query --prefix "$PREFIX" --query "$QUERY" -st "$START_TIME" -et "$END_TIME" -df "$DATE_FORMAT" -rpf "$RECORDS_PER_FILE" -nr "$NUMBER_OF_REDUCERS" -bp "$PCAP_DATA_PATH" 2>&1)
  SUMMARY="Packets conforming to $QUERY starting at $START_TIME ending at $END_TIME"
fi

FAILED=$(echo $CMD | grep "Unable to complete query due to errors")
if [ -z "$FAILED" ];then
  PATTERN="pcap-data-$PREFIX"
  hadoop fs -mkdir -p $QUERY_HOME/$PATTERN && hadoop fs -put $PATTERN* $QUERY_HOME/$PATTERN 
  FAILED=$?
  if [ $FAILED -eq 0 ];then
    echo "%html"
    echo "<h4>$SUMMARY</h4>"
    echo "<ul>"
    for i in $(ls $PATTERN*.pcap | sort -n);do
      FILENAME=$(echo $i | sed 's/+/%2B/g')
      SIZE=$(du -h $i | awk '{print $1}')
      echo "<li><a href=\"http://$WEBHDFS_HOSTNAME/webhdfs/v1$QUERY_HOME/$PATTERN/$FILENAME?op=OPEN\">$i</a> ($SIZE)</li>"
      rm $i
    done
    echo "</ul>"
    echo "<small>NOTE: There are $RECORDS_PER_FILE records per file</small>"
  else
    echo "Unable to create $QUERY_HOME/$PATTERN"
    exit 3
  fi
else
  echo "%html <pre>FAILED JOB:"
  echo "QUERY: $QUERY"
  echo "START_TIME: $START_TIME"
  echo "DATE_FORMAT: $DATE_FORMAT"
  echo "METRON_HOME: $METRON_HOME"
  echo "DATE_FORMAT: $DATE_FORMAT"
  echo "Output:"
  echo "$CMD"
  echo "</pre>"
fi
