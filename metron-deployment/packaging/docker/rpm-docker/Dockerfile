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

FROM centos:centos6

RUN yum install -y \
        tar \
        wget \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
    && cd /usr/src \
    && wget http://apache.cs.utah.edu/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz \
    && tar xzvf apache-maven-3.2.5-bin.tar.gz \
    && rm apache-maven-3.2.5-bin.tar.gz \
    && mv apache-maven-3.2.5 /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/bin/mvn \
    && yum install -y \
        asciidoc \
        rpm-build \
        rpm2cpio \
        tar \
        unzip \
        xmlto \
        zip \
        rpmlint \
    # install node so that the node dependencies can be packaged into the RPMs \
    && cd /root \
    && curl --silent --location https://rpm.nodesource.com/setup_6.x | bash - \
    && yum install -y \
        gcc-c++ \
        make \
        nodejs \
    # Remove packages just needed for builds \
    && yum remove -y \
        wget \
    # Clean up yum caches \
    && yum clean all

WORKDIR /root
