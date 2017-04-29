#!/bin/bash 
METRON_HOME=${METRON_HOME:-"/usr/metron/0.4.0"}
DATE_FORMAT=${DATE_FORMAT:-"yyyyMMdd"}
USER=$(whoami)
USER_HOMEDIR=${USER_HOMEDIR:-`hdfs getconf -confKey dfs.user.home.dir.prefix`/$USER}
QUERY_HOME=${QUERY_HOME:-"$USER_HOMEDIR/queries"}
WEBHDFS_HOSTNAME=${WEBHDFS_HOSTNAME:-`hdfs getconf -confKey dfs.namenode.http-address`}
RAW_QUERY=${QUERY:-$1}
QUERY=$RAW_QUERY #$(printf '%q' $RAW_QUERY)
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
if [ -z "$END_TIME" ]; then
  CMD=$($METRON_HOME/bin/pcap_query.sh query --query "$QUERY" -st "$START_TIME" -df "$DATE_FORMAT" -rpf "$RECORDS_PER_FILE" -nr "$NUMBER_OF_REDUCERS" -bp "$PCAP_DATA_PATH" \"2>&1)
  SUMMARY="Packets conforming to $RAW_QUERY starting at $START_TIME ending now"
else
  CMD=$($METRON_HOME/bin/pcap_query.sh query --query "$QUERY" -st "$START_TIME" -et "$END_TIME" -df "$DATE_FORMAT" -rpf "$RECORDS_PER_FILE" -nr "$NUMBER_OF_REDUCERS" -bp "$PCAP_DATA_PATH" 2>&1)
  SUMMARY="Packets conforming to $RAW_QUERY starting at $START_TIME ending at $END_TIME"
fi

FAILED=$(echo $CMD | grep "Unable to complete query due to errors")
if [ -z "$FAILED" ];then
  PATTERN=$(ls -ltr *.pcap | tail -n 1 | rev | awk '{print $1}' | rev | awk -F+ '{print $1}')
  hadoop fs -mkdir -p $QUERY_HOME/$PATTERN && hadoop fs -put $PATTERN* $QUERY_HOME/$PATTERN 
  FAILED=$?
  if [ $FAILED -eq 0 ];then
    echo "%table"
    echo $SUMMARY
    for i in $(ls $PATTERN*.pcap);do
      FILENAME=$(echo $i | sed 's/+/%2B/g')
      echo "%html <a href=\"http://$WEBHDFS_HOSTNAME/webhdfs/v1$QUERY_HOME/$PATTERN/$FILENAME?op=OPEN\">$i</a>"
      rm $i
    done
  else
    echo "Unable to create $QUERY_HOME/$PATTERN"
    exit 3
  fi
else
  echo "%html <pre>FAILED JOB:"
  echo "QUERY: $RAW_QUERY"
  echo "START_TIME: $START_TIME"
  echo "DATE_FORMAT: $DATE_FORMAT"
  echo "METRON_HOME: $METRON_HOME"
  echo "DATE_FORMAT: $DATE_FORMAT"
  echo "Output:"
  echo "$CMD"
  echo "</pre>"
fi
