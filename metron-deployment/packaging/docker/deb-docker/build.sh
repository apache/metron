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

export FULL_VERSION=$1
export VERSION=$(echo ${FULL_VERSION} | tr -d '"'"'[:alpha:]'"'"')
export DISTRIBUTION="trusty"

echo "FULL_VERSION: ${FULL_VERSION}"
echo "VERSION: ${VERSION}"

INSTALL_PREFIX="/usr/metron"
METRON_HOME="${INSTALL_PREFIX}/${FULL_VERSION}"
HOMEDIR="/root"
WORKDIR="${HOMEDIR}/target"
CONFIGDIR="${HOMEDIR}/debian/"
DATE=`date '+%Y%m%d%H%M'`
PREPACKAGEDIR="${HOMEDIR}/prepackage"

# working directory
mkdir -p "${WORKDIR}"
cd "${WORKDIR}"

# for each metron tarball...
for TARBALL in metron*.tar.gz; do

    export PACKAGE=`echo ${TARBALL} | awk -F"-${FULL_VERSION}-" '{print $1}'`
    export PACKAGE_WORKDIR="${WORKDIR}/${PACKAGE}_${FULL_VERSION}"
    echo "Building package; name=${PACKAGE}, tarball=${TARBALL}"

    # extract the package contents
    mkdir -p ${PACKAGE_WORKDIR}/${METRON_HOME}
    tar xf ${TARBALL} -C ${PACKAGE_WORKDIR}/${METRON_HOME}
    rm -f ${TARBALL}

    # create the config directory
    PACKAGE_DEBIAN_DIR="${PACKAGE_WORKDIR}/DEBIAN"
    mkdir ${PACKAGE_DEBIAN_DIR}

    # create the configuration files
    envsubst < ${CONFIGDIR}/control > ${PACKAGE_DEBIAN_DIR}/control
    envsubst < ${CONFIGDIR}/changelog > ${PACKAGE_DEBIAN_DIR}/changelog
    envsubst < ${CONFIGDIR}/copyright > ${PACKAGE_DEBIAN_DIR}/copyright

    # strip comments from the config files
    sed -i 's/#.*$//g' ${PACKAGE_DEBIAN_DIR}/control
    sed -i 's/#.*$//g' ${PACKAGE_DEBIAN_DIR}/changelog
    sed -i 's/#.*$//g' ${PACKAGE_DEBIAN_DIR}/copyright

    # execute the prepackage script, if one exists
    if [ -f "${PREPACKAGEDIR}/${PACKAGE}" ]; then
        source ${PREPACKAGEDIR}/${PACKAGE}
    fi

    # create the package
    dpkg-deb --verbose --build ${PACKAGE_WORKDIR}
    rm -rf ${PACKAGE_WORKDIR}
done
