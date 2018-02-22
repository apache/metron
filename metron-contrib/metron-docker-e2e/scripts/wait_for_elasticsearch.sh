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
ELASTICSEARCH_HOST=$1
ELASTICSEARCH_PORT=$2
DELAY=5
MAX_ATTEMPTS=24
COUNTER=0
while [ $COUNTER -lt $MAX_ATTEMPTS ]; do
  curl -s --output /dev/null -XGET http://$ELASTICSEARCH_HOST:$ELASTICSEARCH_PORT && echo Elasticsearch is up after waiting "$(($DELAY * $COUNTER))" seconds && break
  sleep $DELAY
  let COUNTER=COUNTER+1
done
if [ $COUNTER -eq $MAX_ATTEMPTS  ]; then echo Could not reach REST after "$(($DELAY * $COUNTER))" seconds; fi
