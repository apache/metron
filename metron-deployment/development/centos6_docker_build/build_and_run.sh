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

# Give the option to skip vagrant up, in case they already have something running
read -p "  run vagrant up? [yN] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
 vagrant up
 rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
fi

VAGRANT_PATH=`pwd`
ANSIBLE_PATH=${VAGRANT_PATH}/ansible
VAGRANT_KEY_PATH=`pwd`/.vagrant/machines/node1/virtualbox

# move over to the docker area
cd ../docker || exit 1

# Give the option to not build the docker container, which can take some time and not be necessary
read -p "  build docker container? [yN] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
 echo "docker build"
 docker build -t metron-build-docker:latest .
fi

if [[ ! -d ~/.m2 ]]; then
 mkdir ~/.m2
fi
DATE=`date`
LOG_DATE=${DATE// /_}
LOGNAME="metron-build-${LOG_DATE}.log"
echo "===============Running Docker==============="
docker run -it \
 -v ${VAGRANT_PATH}/../../..:/root/metron \
 -v ~/.m2:/root/.m2 \
 -v ${VAGRANT_PATH}:/root/vagrant \
 -v ${ANSIBLE_PATH}:/root/ansible_config \
 -v ${VAGRANT_KEY_PATH}:/root/vagrant_key \
 -v ${VAGRANT_PATH}/logs:/root/logs \
 -e ANSIBLE_CONFIG='/root/ansible_config/ansible.cfg' \
 -e ANSIBLE_LOG_PATH="/root/logs/${LOGNAME}" \
 metron-build-docker:latest bash -c /root/vagrant/docker_run_ansible.sh

rc=$?; if [[ ${rc} != 0 ]]; then
 exit ${rc};
fi
