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

rm -rf SRPMS/ RPMS/ && \
rpmbuild -v -ba --define "_topdir $(pwd)" --define "_version ${VERSION}" --define "_prerelease ${PRERELEASE}" SPECS/metron.spec && \
rpmlint -i SPECS/metron.spec RPMS/*/metron* SRPMS/metron
