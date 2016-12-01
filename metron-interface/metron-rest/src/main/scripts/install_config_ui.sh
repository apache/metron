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
SCRIPTS_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT=$SCRIPTS_ROOT/../../..
CONFIG_UI_ROOT=$PROJECT_ROOT/../metron-config
npm version > /dev/null
if [ $? -eq 0 ]; then
    $CONFIG_UI_ROOT/scripts/build.sh
    cp -R $CONFIG_UI_ROOT/dist/ $PROJECT_ROOT/target/classes/static
    cp $CONFIG_UI_ROOT/src/favicon.ico $PROJECT_ROOT/target/classes/static
else
    echo 'Warning:  The config UI could not be built. npm must be installed.'
fi
