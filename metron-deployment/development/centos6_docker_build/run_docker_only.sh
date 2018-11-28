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

VAGRANT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
ANSIBLE_PATH="${VAGRANT_PATH}/ansible"
VAGRANT_KEY_PATH=$(pwd)/.vagrant/machines/node1/virtualbox

# move over to the docker area
cd ../docker || exit 1

if [[ ! -d ~/.m2 ]]; then
    mkdir ~/.m2
fi

DATE=$(date)
LOG_DATE=${DATE// /_}
LOGNAME="metron-build-${LOG_DATE}.log"
echo "Log will be found on host at ${VAGRANT_PATH}/logs/$LOGNAME"

# get the node1 ip address so we can add it to the docker hosts
NODE1_IP=$(awk '/^\s*hosts/{flag=1; next} /}]/{flag=0} flag' "${VAGRANT_PATH}/Vagrantfile" | grep  "^\\s*ip:" | awk -F'"' '{print $2}')
if [[ -z "${NODE1_IP}" ]]; then echo "no node ip found" && exit 1; fi
echo "Using NODE1 IP ${NODE1_IP}"

# need to setup the Cypress cache
unameOut="$(uname -s)"
case "${unameOut}" in
 Linux*)     CYPRESS_CACHE=~/.cache/Cypress;;
 Darwin*)    CYPRESS_CACHE=~/Library/Caches/Cypress;;
esac

if [ -z "$CYPRESS_CACHE" ]; then
 echo "No Cypress Cache Found";
 echo "===============Running Docker==============="
 docker run -it \
   -v "${VAGRANT_PATH}/../../..:/root/metron" \
   -v ~/.m2:/root/.m2 \
   -v "${VAGRANT_PATH}:/root/vagrant" \
   -v "${ANSIBLE_PATH}:/root/ansible_config" \
   -v "${VAGRANT_KEY_PATH}:/root/vagrant_key" \
   -v "${VAGRANT_PATH}/logs:/root/logs" \
   -e ANSIBLE_CONFIG='/root/ansible_config/ansible.cfg' \
   -e ANSIBLE_LOG_PATH="/root/logs/${LOGNAME}" \
   -e ANSIBLE_SKIP_TAGS="${A_SKIP_TAGS}" \
   --add-host="node1:${NODE1_IP}" \
   metron-build-docker:latest bash

   rc=$?; if [[ ${rc} != 0 ]]; then
    exit ${rc};
   fi
else
 echo "Cypres Cache is set to '$CYPRESS_CACHE'";
 echo "===============Running Docker==============="
 docker run -it \
  -v "${VAGRANT_PATH}/../../..:/root/metron" \
  -v ~/.m2:/root/.m2 \
  -v "${VAGRANT_PATH}:/root/vagrant" \
  -v "${ANSIBLE_PATH}:/root/ansible_config" \
  -v "${VAGRANT_KEY_PATH}:/root/vagrant_key" \
  -v "${VAGRANT_PATH}/logs:/root/logs" \
  -v "${CYPRESS_CACHE}:/root/.cache/Cypress" \
  -e ANSIBLE_CONFIG='/root/ansible_config/ansible.cfg' \
  -e ANSIBLE_LOG_PATH="/root/logs/${LOGNAME}" \
  -e ANSIBLE_SKIP_TAGS="${A_SKIP_TAGS}" \
  --add-host="node1:${NODE1_IP}" \
  metron-build-docker:latest bash

 rc=$?; if [[ ${rc} != 0 ]]; then
   exit ${rc};
 fi
fi
