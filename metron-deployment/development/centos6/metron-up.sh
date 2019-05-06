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

function help {
 echo " "
 echo "usage: ${0}"
 echo "    --force-docker-build            force build docker machine"
 echo "    --skip-tags='tag,tag2,tag3'     the ansible skip tags"
 echo "    --skip-vagrant-up               skip vagrant up"
 echo "    -h/--help                       Usage information."
 echo " "
 echo "example: to skip vagrant up and force docker build with two tags"
 echo "   build_and_run.sh --skip-vagrant-up --force-docker-build --skip-tags='solr,sensors'"
 echo " "
}


SKIP_VAGRANT_UP=false
FORCE_DOCKER_BUILD=false
A_SKIP_TAGS="sensors,solr"
SKIP_METRON_BUILD=false

# handle command line options
for i in "$@"; do
 case $i in
 #
 # SKIP_VAGRANT_UP
 #
 #
  --skip-vagrant-up)
   SKIP_VAGRANT_UP=true
   shift # past argument
  ;;

 #
 # SKIP_METRON_BUILD
 #
 #
  --skip-metron-build)
   SKIP_METRON_BUILD=true
   shift # past argument
  ;;

 #
 # FORCE_DOCKER_BUILD
 #
 #   --force-docker-build
 #
   --force-docker-build)
   FORCE_DOCKER_BUILD=true
   shift # past argument
  ;;

 #
 # SKIP_TAGS
 #
 #   --skip-tags='foo,bar'
 #
   --skip-tags=*)
   A_SKIP_TAGS="${i#*=}"
   shift # past argument=value
  ;;

 #
 # -h/--help
 #
  -h|--help)
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

echo "Running with "
echo "SKIP_VAGRANT_UP    = $SKIP_VAGRANT_UP"
echo "SKIP_METRON_BUILD  = $SKIP_METRON_BUILD"
echo "FORCE_DOCKER_BUILD = $FORCE_DOCKER_BUILD"
echo "SKIP_TAGS          = $A_SKIP_TAGS"
echo "==================================================="




VAGRANT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
HOST_SCRIPT_PATH=${VAGRANT_PATH}/host_scripts

# move over to the docker area
cd ../docker || exit 1

# Give the option to not build the docker container, which can take some time and not be necessary
if [[ "$FORCE_DOCKER_BUILD" = true ]]; then
 echo "docker build"
 docker build -t metron-build-docker:latest .
fi

# start the build container
#
# shellcheck disable=SC2145
"${HOST_SCRIPT_PATH}"/docker_run_build_container.sh --vagrant-path="${VAGRANT_PATH}" --skip-tags="${A_SKIP_TAGS[@]}"
rc=$?; if [[ ${rc} != 0 ]]; then
 exit ${rc};
fi
# exec the metron build process
if [[ "$SKIP_METRON_BUILD" = false ]]; then
 "${HOST_SCRIPT_PATH}"/docker_exec_build_metron.sh
 rc=$?; if [[ ${rc} != 0 ]]; then
  "${HOST_SCRIPT_PATH}"/stop_build_container.sh
  exit ${rc};
 fi
fi

# start the vagrant vm now that the build is complete ( the vm really slows the build down
cd "${VAGRANT_PATH}" || exit 1

if [[ "$SKIP_VAGRANT_UP" = false ]]; then
 vagrant up
 rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
fi

# exec the metron install process
"${HOST_SCRIPT_PATH}"/docker_exec_deploy_metron.sh

# we don't need to leave the container running
"${HOST_SCRIPT_PATH}"/stop_build_container.sh
exit ${rc};
