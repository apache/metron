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
set -u # nounset
set -e # errexit
set -E # errtrap
set -o pipefail

# Stops the Docker container with a given name

function help {
  echo " "
  echo "usage: ${0}"
  echo "    --container-name                [REQUIRED] The Docker container name"
  echo "    -h/--help                       Usage information."
  echo " "
}

CONTAINER_NAME=

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
  # -h/--help
  #
    -h | --help)
      help
      exit 0
      shift # past argument with no value
    ;;

  #
  # Unknown option
  #
    *)
      UNKNOWN_OPTION="${i#*=}"
      echo "Error: unknown option: $UNKNOWN_OPTION"
      help
    ;;
  esac
done

if [[ -z "$CONTAINER_NAME" ]]; then
  echo "CONTAINER_NAME must be passed"
  exit 1
fi

echo "Running stop_container with"
echo "CONTAINER_NAME= $CONTAINER_NAME"
echo "==================================================="

docker stop "${CONTAINER_NAME}"
rc=$?; if [[ ${rc} != 0 ]]; then
  exit ${rc}
fi

docker rm "${CONTAINER_NAME}"

