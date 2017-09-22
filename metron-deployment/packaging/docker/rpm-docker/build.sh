#!/bin/bash
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

# To avoid needing an epoch tag to update, BETA (or other prerelease tags) need to be in
# Spec's release, not in the version.  Make sure this is split out based on the acutal version.
# E.g. 0.2.0BETA becomes 0.2.0 version and BETA prerelease.
# Empty string is acceptable when there is no prerelease tag.

FULL_VERSION=$1
echo "FULL_VERSION: ${FULL_VERSION}"
VERSION=$(echo ${FULL_VERSION} | tr -d '"'"'[:alpha:]'"'"')
echo "VERSION: ${VERSION}"
PRERELEASE=$(echo ${FULL_VERSION} | tr -d '"'"'[:digit:]\.'"'"')
echo "PRERELEASE: ${PRERELEASE}"

# Account for non-existent file owner in container
# Ignore UID=0, root exists in all containers
OWNER_UID=`ls -n SPECS/metron.spec | awk -F' ' '{ print $3 }'`
id $OWNER_UID >/dev/null 2>&1
if [ $? -ne 0 ] && [ $OWNER_UID -ne 0 ]; then
    useradd -u $OWNER_UID builder
fi

rm -rf SRPMS/ RPMS/

QA_SKIP_BUILD_ROOT=1 rpmbuild -v -ba --define "_topdir $(pwd)" --define "_version ${VERSION}" --define "_prerelease ${PRERELEASE}" SPECS/metron.spec
if [ $? -ne 0 ]; then
  echo "RPM build errors encountered" >&2
  exit 1
fi

rpmlint -i SPECS/metron.spec RPMS/*/metron* SRPMS/metron

# Ensure original user permissions are maintained after build
if [ $OWNER_UID -ne 0 ]; then
  chown -R $OWNER_UID *
fi
