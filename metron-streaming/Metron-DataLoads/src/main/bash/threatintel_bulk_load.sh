#!/bin/bash

BIGTOP_DEFAULTS_DIR=${BIGTOP_DEFAULTS_DIR-/etc/default}
[ -n "${BIGTOP_DEFAULTS_DIR}" -a -r ${BIGTOP_DEFAULTS_DIR}/hbase ] && . ${BIGTOP_DEFAULTS_DIR}/hbase

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

export HBASE_HOME=${HBASE_HOME:-/usr/hdp/current/hbase-client}
HADOOP_CLASSPATH=${HBASE_HOME}/lib/hbase-server.jar:`${HBASE_HOME}/bin/hbase classpath`
for jar in $(echo $HADOOP_CLASSPATH | sed 's/:/ /g');do
  if [ -f $jar ];then
    LIBJARS="$jar,$LIBJARS"
  fi
done
export HADOOP_CLASSPATH
hadoop jar /usr/metron/0.6BETA/lib/Metron-DataLoads-0.6BETA.jar org.apache.metron.dataloads.ThreatIntelBulkLoader -libjars ${LIBJARS} "$@"
