#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
if [ $# -lt 2 ]
  then
    echo "Usage:  produce-data.sh data_path topic [message_delay_in_seconds]"
    exit 0
fi

FILE_PATH=$1
TOPIC=$2
DELAY=${3:-1}
echo "Emitting data in $FILE_PATH to Kafka topic $TOPIC every $DELAY second(s)"
exec ./bin/output-data.sh $FILE_PATH $DELAY | ./bin/kafka-console-producer.sh --broker-list localhost:9092 --topic $TOPIC > /dev/null
