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
FROM centos:centos6.9
MAINTAINER Apache Metron

COPY ansible.cfg /root/
ENV ANSIBLE_CONFIG /root/ansible.cfg

RUN yum install -y \
        tar \
        wget \
    # base development tools required \
    && yum groupinstall -y \
        "Development tools" \
    # Install Software Collections repo (needs to be done in separate command) \
    && yum install -y \
        centos-release-scl \
    # newer cpp 11 support required for building node modules \
    && yum install -y \
        devtoolset-4-gcc-c++ \
        devtoolset-4-gcc \
        zlib-dev \
        openssl-devel \
        sqlite-devel \
        bzip2-devel \
        libffi-devel \
    # install python 2.7.11 but do not make it the default \
    && wget https://www.python.org/ftp/python/2.7.11/Python-2.7.11.tgz -O /usr/src/Python-2.7.11.tgz \
    && cd /usr/src \
    && tar xvf Python-2.7.11.tgz \
    && rm -rf Python-2.7.11.tgz \
    && cd /usr/src/Python-2.7.11 \
    && ./configure \
    && make altinstall \
    && cd /usr/src \
    && wget --no-check-certificate https://pypi.python.org/packages/source/s/setuptools/setuptools-11.3.tar.gz -O /usr/src/setuptools-11.3.tar.gz \
    && tar xvf setuptools-11.3.tar.gz \
    && rm setuptools-11.3.tar.gz \
    && cd /usr/src/setuptools-11.3 \
    && python2.7 setup.py install \
    && easy_install-2.7 pip \
    # install ansible and set the configuration var \
    && pip2.7 install \
        ansible==2.0.0.2 \
        boto \
    # java \
    && yum install -y \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        which \
        nss \
    && cd /usr/src \
    # setup maven \
    && wget http://apache.cs.utah.edu/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz \
    && tar xzvf apache-maven-3.3.9-bin.tar.gz \
    && rm apache-maven-3.3.9-bin.tar.gz \
    && mv apache-maven-3.3.9 /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/bin/mvn \
    # install rpm tools required to build rpms \
    && yum install -y \
        asciidoc \
        rpm-build \
        rpm2cpio \
        tar \
        unzip \
        xmlto \
        zip \
        rpmlint \
        make \
    # create a .bashrc for root, enabling the cpp 11 toolset \
    && touch /root/.bashrc \
    && echo '/opt/rh/devtoolset-4/enable' >> /root/.bashrc \
    # install node so that the node dependencies can be packaged into the RPMs \
    && curl --silent --location https://rpm.nodesource.com/setup_6.x | bash - \
    && yum install -y \
        nodejs \
    # Remove packages just needed for builds \
    && yum remove -y \
        wget \
    # Clean up yum caches \
    && yum clean all

WORKDIR /root
