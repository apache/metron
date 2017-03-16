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
SCRIPTS_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export METRON_REST_URL=${METRON_REST_URL:-http://localhost:8080}
export MANAGEMENT_UI_PORT=${MANAGEMENT_UI_PORT:-4200}
npm version > /dev/null
if [ $? -eq 0 ]; then
    npm install http-server
    ./node_modules/http-server/bin/http-server ./dist/ --proxy $METRON_REST_URL -p $MANAGEMENT_UI_PORT
else
    echo 'Error:  npm required to start http-server'
fi