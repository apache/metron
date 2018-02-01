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

INSTALL_PREFIX="/usr/metron"
METRON_HOME="${INSTALL_PREFIX}/${FULL_VERSION}"
HOMEDIR="/root"
WORKDIR="${HOMEDIR}/target"
CONFIGDIR="${HOMEDIR}/debian"
DATE=`date '+%Y%m%d%H%M'`
PREPACKAGEDIR="${HOMEDIR}/prepackage"

# working directory
mkdir -p "${WORKDIR}"
cd "${WORKDIR}"

# for each metron tarball...
for TARBALL in metron*.tar.gz; do

    export PACKAGE=`echo ${TARBALL} | awk -F"-${FULL_VERSION}-" '{print $1}'`
    if [ "${PACKAGE}" = "metron-pcap-backend" ]; then
        # work around for inconsistent naming of 'metron-pcap-backend' ...
        #
        #  * the tarball is named 'metron-pcap-backend'
        #  * but the RPM is created as 'metron-pcap'
        #  * the mpack also expects the package to be named 'metron-pcap'
        #
        # ... rather than use the tarball name here, rewrite the name to be
        # consistent with the RPMs and MPack
        export PACKAGE="metron-pcap"
    fi
    export PACKAGE_WORKDIR="${WORKDIR}/${PACKAGE}_${FULL_VERSION}"
    echo "Building package; name=${PACKAGE}, tarball=${TARBALL}"

    # if the tarball does not exist, fail fast
    if [ ! -f "${TARBALL}" ]; then
        echo "ERROR: Missing ${TARBALL}"
        exit 1
    fi

    # extract the package contents
    mkdir -p ${PACKAGE_WORKDIR}/${METRON_HOME}
    tar xf ${TARBALL} -C ${PACKAGE_WORKDIR}/${METRON_HOME}
    rm -f ${TARBALL}

    # create the config directory
    PACKAGE_DEBIAN_DIR="${PACKAGE_WORKDIR}/DEBIAN"
    mkdir ${PACKAGE_DEBIAN_DIR}

    # all packages get the control files contained in `debian/metron`
    for CFILE in ${CONFIGDIR}/metron/*; do
        [ -e "$CFILE" ] || continue
        CFILENAME=`basename "${CFILE}"`
        DEST="${PACKAGE_DEBIAN_DIR}/${CFILENAME}"

        # copy over the control file (allowing for variable substitution)
        envsubst < ${CFILE} > ${DEST}

        # strip comments from the control file
        sed -i '/#[^!]*$/d' ${DEST}
    done

    # a package *may* have control files specific to it in `debian/$PACKAGE`
    for CFILE in ${CONFIGDIR}/${PACKAGE}/*; do
        [ -e "$CFILE" ] || continue
        CFILENAME=`basename "${CFILE}"`
        DEST="${PACKAGE_DEBIAN_DIR}/${CFILENAME}"

        # copy over the control file (allowing for variable substitution)
        envsubst < ${CFILE} > ${DEST}

        # strip comments from the control file (don't delete shebangs!)
        sed -i '/#[^!]*$/d' ${DEST}

        # permissions must be 0755 for maintain scripts like preinst and postinst
        chmod 0755 ${DEST}
    done

    # execute the prepackage script, if one exists for the package
    if [ -f "${PREPACKAGEDIR}/${PACKAGE}" ]; then
        source ${PREPACKAGEDIR}/${PACKAGE}
    fi

    # create the package
    dpkg-deb --verbose --build ${PACKAGE_WORKDIR}
    rm -rf ${PACKAGE_WORKDIR}
done
