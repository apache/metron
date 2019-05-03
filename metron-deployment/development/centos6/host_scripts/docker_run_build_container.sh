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

shopt -s nocasematch
set -e # errexit
set -E # errtrap
set -o pipefail

#
# Runs the bro container
#

function help {
  echo " "
  echo "usage: ${0}"
  echo "    --container-name                [OPTIONAL] The Docker container name. Default: bro"
  echo "    --network-name                  [OPTIONAL] The Docker network name. Default: bro-network"
  echo "    --scripts-path                  [OPTIONAL] The path with the scripts you may run in the container. These are your scripts, not the built in scripts"
  echo "    --data-path                     [OPTIONAL] The name of the directory to map to /root/data in the container"
  echo "    --test-output-path              [REQUIRED] The path to log test data to"
  echo "    --docker-parameter              [OPTIONAL, MULTIPLE] Each parameter with this name will be passed to docker run"
  echo "    -h/--help                       Usage information."
  echo " "
}

BRO_PLUGIN_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null && cd ../.. && pwd)"
CONTAINER_NAME=bro
NETWORK_NAME=bro-network
OUR_SCRIPTS_PATH="${BRO_PLUGIN_PATH}/docker/in_docker_scripts"
SCRIPTS_PATH=
DATA_PATH=
TEST_OUTPUT_PATH=

declare -a DOCKER_PARAMETERS

# Handle command line options
for i in "$@"; do
  case $i in
  #
  # CONTAINER_NAME
  #
  #   --container-name
  #
    --container-name=*)
      CONTAINER_NAME="${i#*=}"
      shift # past argument=value
    ;;

  #
  # NETWORK_NAME
  #
  #   --network-name
  #
    --network-name=*)
      NETWORK_NAME="${i#*=}"
      shift # past argument=value
    ;;

  #
  # DATA_PATH
  #
  #   --data-path
  #
    --data-path=*)
      DATA_PATH="${i#*=}"
      shift # past argument=value
    ;;

  #
  # SCRIPTS_PATH
  #
  #   --scripts-path
  #
    --scripts-path=*)
      SCRIPTS_PATH="${i#*=}"
      shift # past argument=value
    ;;

  #
  # TEST_OUTPUT_PATH
  #
  #   --test-output-path
  #
    --test-output-path=*)
      TEST_OUTPUT_PATH="${i#*=}"
      shift # past argument=value
    ;;

  #
  # DOCKER_PARAMETERS
  #
  #   --docker-parameter
  #
    --docker-parameter=*)
      DOCKER_PARAMETERS=( "${DOCKER_PARAMETERS[@]}" "${i#*=}" )
      shift # past argument=value
    ;;

  #
  # -h/--help
  #
    -h | --help)
      help
      exit 0
      shift # past argument with no value
    ;;
  esac
done

echo "Running docker_run_bro_container with "
echo "CONTAINER_NAME = $CONTAINER_NAME"
echo "NETWORK_NAME = ${NETWORK_NAME}"
echo "SCRIPT_PATH = ${SCRIPTS_PATH}"
echo "DATA_PATH = ${DATA_PATH}"
echo "TEST_OUTPUT_PATH = ${TEST_OUTPUT_PATH}"
echo "DOCKER_PARAMETERS = " "${DOCKER_PARAMETERS[@]}"
echo "==================================================="


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
  Darwin*)    CYPRESS_CACHE=~/.Library/caches/Cypress;;
esac

# Build the docker command line
declare -a DOCKER_CMD_BASE
#DOCKER_CMD="bash -c /root/scripts/docker_run_ansible_install.sh"
DOCKER_CMD="bash"
DOCKER_CMD_BASE[0]="docker run -d -t --name MetronBuild "
DOCKER_CMD_BASE[1]="-v \"${VAGRANT_PATH}/../../..:/root/metron\" "
DOCKER_CMD_BASE[2]="-v ~/.m2:/root/.m2 "
DOCKER_CMD_BASE[3]="-v \"${VAGRANT_PATH}:/root/vagrant\" "
DOCKER_CMD_BASE[4]="-v \"${ANSIBLE_PATH}:/root/ansible_config\" "
DOCKER_CMD_BASE[5]="-v \"${VAGRANT_KEY_PATH}:/root/vagrant_key\" "
DOCKER_CMD_BASE[6]="-v \"${VAGRANT_PATH}/logs:/root/logs\" "
DOCKER_CMD_BASE[7]="-v \"${DOCKER_SCRIPT_PATH}:/root/scripts\" "
DOCKER_CMD_BASE[8]="-e ANSIBLE_CONFIG=\"/root/ansible_config/ansible.cfg\" "
DOCKER_CMD_BASE[9]="-e ANSIBLE_LOG_PATH=\"/root/logs/${LOGNAME}\" "
DOCKER_CMD_BASE[10]="-e ANSIBLE_SKIP_TAGS=\"${A_SKIP_TAGS}\" "
DOCKER_CMD_BASE[11]="--add-host=\"node1:${NODE1_IP}\" "

if [ ! -z "$CYPRESS_CACHE" ]; then
  echo "Cypres Cache is set to '$CYPRESS_CACHE'";
  DOCKER_CMD_BASE[12]="-v \"${CYPRESS_CACHE} :/root/.cache/Cypress\" "
else
  echo "No Cypress Cache Found";
fi

echo "===============Running Docker==============="
echo ""
echo "eval command is: "
echo "${DOCKER_CMD_BASE[@]}" "${DOCKER_CMD}"
echo ""
echo "============================================"
echo ""
eval "${DOCKER_CMD_BASE[@]}" metron-build-docker:latest "${DOCKER_CMD}"
rc=$?; if [[ ${rc} != 0 ]]; then
  exit ${rc};
fi

echo "Started build container"
echo " "
echo " "

