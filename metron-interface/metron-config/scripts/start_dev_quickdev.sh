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

if [ $# -eq 0 ]
  then
    echo "usage: start_dev_quickdev.sh <Url where metron rest application is available>"
    exit
fi

cp $SCRIPTS_ROOT/../proxy.conf.json $SCRIPTS_ROOT/../proxy.quickdev.conf.json
sed -i '' "s@http://localhost:8080@$1@g" $SCRIPTS_ROOT/../proxy.quickdev.conf.json
$SCRIPTS_ROOT/../node_modules/angular-cli/bin/ng serve --proxy-config proxy.quickdev.conf.json
