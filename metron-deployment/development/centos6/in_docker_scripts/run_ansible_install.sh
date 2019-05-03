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
# This script runs IN the docker container
#

cd /root/metron || exit 1

# make sure we have the right c++ tools

#shellcheck disable=SC1091
source /opt/rh/devtoolset-6/enable

# fixup known_hosts if it is there
if [[  -f ~/.ssh/known_hosts ]]; then
 sed -i -e '/^node1.*/d' ~/.ssh/known_hosts
fi

ansible-playbook  \
 -i /root/ansible_config/inventory \
 --private-key="/root/vagrant_key/private_key" \
 --user="vagrant" \
 --become \
 --extra-vars="ansible_ssh_private_key_file=/root/vagrant_key/private_key" \
 /root/ansible_config/playbook.yml