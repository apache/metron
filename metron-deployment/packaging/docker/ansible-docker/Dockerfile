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

RUN yum install -y tar
RUN yum install -y wget
# base development tools required
RUN yum groupinstall -y "Development tools"
# newer cpp 11 support required for building node modules
RUN yum install -y centos-release-scl
RUN yum install -y devtoolset-4-gcc-c++ devtoolset-4-gcc
RUN yum install -y zlib-dev openssl-devel sqlite-devel bzip2-devel libffi-devel
# install python 2.7.11 but do not make it the default
RUN wget https://www.python.org/ftp/python/2.7.11/Python-2.7.11.tgz -O /usr/src/Python-2.7.11.tgz
WORKDIR /usr/src
RUN tar xvf Python-2.7.11.tgz
WORKDIR /usr/src/Python-2.7.11
RUN ./configure
RUN make altinstall
WORKDIR /usr/src
RUN wget --no-check-certificate https://pypi.python.org/packages/source/s/setuptools/setuptools-11.3.tar.gz -O /usr/src/setuptools-11.3.tar.gz
RUN tar xvf setuptools-11.3.tar.gz
WORKDIR /usr/src/setuptools-11.3
RUN python2.7 setup.py install
RUN easy_install-2.7 pip
# install ansible and set the configuration var
RUN pip2.7 install ansible==2.0.0.2
RUN pip2.7 install boto
COPY ansible.cfg /root/
ENV ANSIBLE_CONFIG /root/ansible.cfg
# java
RUN yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
RUN yum install -y which
RUN yum install -y nss
WORKDIR /usr/src
# setup maven
RUN wget http://apache.cs.utah.edu/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
RUN tar xzvf apache-maven-3.3.9-bin.tar.gz
RUN mv apache-maven-3.3.9 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/bin/mvn
# install rpm tools required to build rpms
RUN yum -y install asciidoc rpm-build rpm2cpio tar unzip xmlto zip rpmlint make && yum clean all
# create a .bashrc for root, enabling the cpp 11 toolset
RUN touch /root/.bashrc \
 && cat '/opt/rh/devtoolset-4/enable' >> /root/.bashrc
# install node so that the node dependencies can be packaged into the RPMs
RUN curl --silent --location https://rpm.nodesource.com/setup_6.x | bash -
RUN yum -y install nodejs
WORKDIR /root

