#!/bin/bash

ROOT_DIR=metron-debug
METRON_DIR=$ROOT_DIR/metron
STORM_DIR=$ROOT_DIR/storm
KAFKA_DIR=$ROOT_DIR/kafka
BUNDLE_NAME=metron-debug

# check env vars
# TODO


if [ ! -d "$ROOT_DIR" ]; then
    mkdir $ROOT_DIR
fi
if [ ! -d "$METRON_DIR" ]; then
    mkdir $METRON_DIR
fi
if [ ! -d "$STORM_DIR" ]; then
    mkdir $STORM_DIR
fi
if [ ! -d "$KAFKA_DIR" ]; then
    mkdir $KAFKA_DIR
fi

# Storm
echo "Retrieving Storm detail"
# get Storm cluster summary info including version
echo "Retrieving Storm cluster summary"
curl -s -XGET ${STORM_UI}'/api/v1/cluster/summary' | python -m json.tool > $STORM_DIR/cluster-summary.json
echo "...done"

# get overall Storm cluster configuration
echo "Retrieving Storm cluster configuration"
curl -s -XGET ${STORM_UI}'/api/v1/cluster/configuration' | python -m json.tool > $STORM_DIR/cluster-configuration.json
echo "...done"

# get list of topologies and brief summary detail
echo "Retrieving Storm topology summary"
curl -s -XGET ${STORM_UI}'/api/v1/topology/summary' | python -m json.tool > $STORM_DIR/topology-summary.json
echo "...done"

# get all topology runtime settings. Plugin the ID for your topology, which you can get from the topology summary command or from the Storm UI. Passing sys=1 will also return system stats.
echo "Retrieving Storm topology detail"
for topology_id in $(python get-ids.py); do
    echo "${topology_id}"
    curl -s -XGET ${STORM_UI}'/api/v1/topology/'${topology_id}'?sys=1' | python -m json.tool > $STORM_DIR/topology-${topology_id}-summary.json 2>/dev/null
done
echo "...done"

# Kafka
echo "Retrieving Kafka detail"
echo "Retrieving Kafka broker info"
KAFKA_BROKER_FILE=$KAFKA_DIR/kafka-broker-info.txt
linedelim="-----------------------------";
for broker in $($HDP_HOME/kafka-broker/bin/zookeeper-shell.sh $ZOOKEEPER <<< "ls /brokers/ids" | grep -e '\[.*\]' | tr -d [] | tr , ' '); do 
    echo "METADATA FOR BROKER ID [$broker]" >> $KAFKA_BROKER_FILE;
    echo "$linedelim" >> $KAFKA_BROKER_FILE;
    echo "get /brokers/ids/$broker" | $HDP_HOME/kafka-broker/bin/zookeeper-shell.sh $ZOOKEEPER >> $KAFKA_BROKER_FILE 2>&1;
    echo "$linedelim" >> $KAFKA_BROKER_FILE;
done
echo "...done"

# Get list of Kafka topics
echo "Retrieving Kafka topics list"
${HDP_HOME}/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list >> $KAFKA_DIR/kafka-topics.txt
echo "...done"
# Get Kafka topic details
echo "Retrieving Kafka enrichment topic details"
${HDP_HOME}/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --topic enrichments --describe >> $KAFKA_DIR/kafka-enrichments-topic.txt
echo "...done"
echo "Retrieving Kafka indexing topic details"
${HDP_HOME}/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --topic indexing --describe >> $KAFKA_DIR/kafka-indexing-topic.txt
echo "...done"

# Metron
echo "Retrieving Metron configuration and flux files"
cp -R $METRON_HOME/config $METRON_DIR
cp -R $METRON_HOME/flux $METRON_DIR
$METRON_HOME/bin/zk_load_configs.sh -m DUMP -z $ZOOKEEPER >> $METRON_DIR/zk-configs.txt
echo "...done"

filetimestamp=$(date +"%Y-%m-%d_%H-%M-%S")
BUNDLE_NAME=${BUNDLE_NAME}-${filetimestamp}.tgz
echo "Bundling resources"
tar czvf ${BUNDLE_NAME} $ROOT_DIR
echo "...done"

echo "Metron detail file=$PWD/$BUNDLE_NAME"

