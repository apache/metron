#!/usr/bin/env bash
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

#
# Builds Metron platform jars, instantiates hosts, and deploys Metron to those
# hosts on Amazon EC2
#
LOGFILE="./ansible.log"
DEPLOYDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
EXTRA_ARGS="-v ${@:1}"
NOW=`date`
DEFAULT_ENV="metron-test"
DEFAULT_ENV_FILE="./.metron-env"

# ensure aws access key is defined
if [ -z "$AWS_ACCESS_KEY_ID" ]; then
  echo "Error: missing AWS_ACCESS_KEY_ID"
  exit 1
fi

# ensure aws access key is defined
if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
  echo "Error: missing AWS_SECRET_ACCESS_KEY"
  exit 1
fi

# retrieve environment name from previous run
if [ -f $DEFAULT_ENV_FILE ]; then
  ENV=`cat $DEFAULT_ENV_FILE`
else
  ENV=$DEFAULT_ENV
fi

# prompt the user for an environment name
read -p "Metron Environment [$ENV]: " INPUT
[ -n "$INPUT" ] && ENV=$INPUT

# store the environment name for the next run
echo "$ENV" > $DEFAULT_ENV_FILE

# log information about the host platform
echo "=============================================================" >> $LOGFILE
echo "Launching Metron[$ENV] @ $NOW"... >> $LOGFILE
$DEPLOYDIR/../scripts/platform-info.sh >> $LOGFILE

# build metron
cd ../..
mvn package -DskipTests

# deploy metron
cd $DEPLOYDIR
export EC2_INI_PATH=conf/ec2.ini
ansible-playbook -i ec2.py playbook.yml \
  --skip-tags="solr" \
  --extra-vars="env=$ENV" \
  $EXTRA_ARGS
