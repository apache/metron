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


VAGRANT_PATH=`pwd`
echo "setting the ansible configuration path"
ANSIBLE_PATH=${VAGRANT_PATH}/ansible
echo ${ANSIBLE_PATH}
echo "setting the ssh key"
VAGRANT_KEY_PATH=`pwd`/.vagrant/machines/node1/virtualbox
echo ${VAGRANT_KEY_PATH}

# move over to the docker area
cd ../docker || exit 1
pwd

echo "===============Running Docker==============="
docker run -it \
 -v  ${VAGRANT_PATH}/../../..:/root/metron \
 -v ~/.m2:/root/.m2 \
 -v ${VAGRANT_PATH}:/root/vagrant \
 -v ${ANSIBLE_PATH}:/root/ansible_config \
 -v ${VAGRANT_KEY_PATH}:/root/vagrant_key \
 metron-build-docker:latest bash
