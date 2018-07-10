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
# extracts information from the host environment that is useful for
# troubleshooting Apache Metron deployments
#
CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# extract metron version from the pom
METRON_VERSION=`cat $CWD/../../pom.xml | grep "<version>" | head -1 | sed -ne '/version/{s/.*<version>\(.*\)<\/version>.*/\1/p;q;}'`
echo "Metron $METRON_VERSION"

# is this a git repo?
IS_GIT_REPO=`git rev-parse --is-inside-work-tree`
if [ "$IS_GIT_REPO" == "true" ]; then

  # current branch
  echo "--"
  git branch | grep "*"

  # last commit
  echo "--"
  git --no-pager log -n 1

  # local changes since last commit
  echo "--"
  git --no-pager diff --stat
fi

# ansible
echo "--"
ansible --version

# vagrant
echo "--"
vagrant --version

# vagrant hostmanager plugin
echo "--"
vagrant plugin list | grep vagrant-hostmanager || echo "The Vagrant Hostmanager Plugin is NOT installed"

# python
echo "--"
python --version 2>&1

# maven
echo "--"
mvn --version

# docker
echo "--"
docker --version
if docker info 2>&1  | grep -q 'Cannot connect to the Docker daemon'
then
  echo "WARNING: Docker daemon is not running"
fi

# node
echo "--"
echo "node"
node --version

# npm
echo "--"
echo "npm"
npm --version

# C++ compiler
echo "--"
if [[ $(command -v g++) && $(g++ --version 2>/dev/null) ]]; then
  g++ --version

  # check C++11 compliance
  echo "--"
  OBJFILE=/tmp/test
  CPPFILE=/tmp/test.cpp
  cat > $CPPFILE <<- EOM
#include <iostream>
using namespace std;
int main() {
    cout << "Hello World!" << endl;
    return 0;
}
EOM
  g++ -std=c++11 $CPPFILE -o $OBJFILE &>/dev/null
  if [ $? -eq 0 ]; then
      echo "Compiler is C++11 compliant"
  else
      echo "Warning: Compiler is NOT C++11 compliant"
  fi
  rm -f $CPPFILE $OBJFILE
elif [[ $(command -v g++) ]]; then
  echo "Warning: g++ not properly configured"
else
  echo "Warning: g++ not found"
fi

# operating system
echo "--"
uname -a

# system resources
echo "--"
case "${OSTYPE}" in
  linux*)
    cat /proc/meminfo  | grep -i MemTotal | awk '{print "Total System Memory = " $2/1024 " MB"}'
    cat /proc/cpuinfo | egrep 'model\ name' | uniq | cut -d: -f2 | awk '{print "Processor Model:" $0}'
    cat /proc/cpuinfo | egrep 'cpu\ MHz' | uniq | cut -d: -f2 | awk '{print "Processor Speed:" $0 " MHz"}'
    cat /proc/cpuinfo | grep -i '^processor' | wc -l | awk '{print "Total Physical Processors: " $0}'
    cat /proc/cpuinfo | grep -i cores | cut -d: -f2 | awk '{corecount+=$1} END {print "Total cores: " corecount}'
    echo "Disk information:"
    df -h | grep "^/"
    if [[ $(egrep '(vmx|svm)' /proc/cpuinfo) ]]; then
      echo "This CPU appears to support virtualization"
    else
      echo "This CPU may not support virtualization"
    fi
    ;;
  darwin*)
    sysctl hw.memsize | awk '{print "Total System Memory = " $2/1048576 " MB"}'
    sysctl machdep.cpu | grep 'machdep.cpu.brand_string' | cut -d: -f2 | cut -d\@ -f1 | awk '{print "Processor Model:" $0}'
    sysctl machdep.cpu | grep 'machdep.cpu.brand_string' | cut -d: -f2 | cut -d\@ -f2 | awk '{print "Processor Speed:" $0}'
    sysctl hw.physicalcpu | cut -d: -f2 | awk '{print "Total Physical Processors:" $0}'
    sysctl machdep.cpu | grep 'machdep.cpu.core_count' | cut -d: -f2 | cut -d\@ -f2 | awk '{print "Total cores:" $0}'
    echo "Disk information:"
    df -h | grep "^/"
    if [[ $(sysctl kern.hv_support | awk -F' ' '{print $2}') == "1" && $(sysctl -a | grep machdep.cpu.features | grep VMX) ]]; then
      echo "This CPU appears to support virtualization"
    else
      echo "This CPU may not support virtualization"
    fi
    ;;
  *)
    echo "Unable to detect system resources for ${OSTYPE}"
    ;;
esac
