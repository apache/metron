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

# exit immediately on error
set -e

# start elasticsearch as non-root user
chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/data
chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/logs
gosu elasticsearch /usr/share/elasticsearch/bin/elasticsearch -d

# wait for elasticsearch to start
/wait-for-it.sh localhost:9200 -t 30

# load elasticsearch templates
for template_file in `ls -1 /es_templates`; do
    template_name=`echo $template_file | sed 's/\.template//g'`
    curl -XPUT --data @/es_templates/$template_file http://localhost:9200/_template/$template_name
done

# pass through CMD as PID 1
exec "$@"
