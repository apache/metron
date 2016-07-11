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
FROM centos:6.6
MAINTAINER Apache Metron

RUN yum install -y tar
RUN yum install -y wget
RUN yum groupinstall -y "Development tools"
RUN yum install -y zlib-dev openssl-devel sqlite-devel bzip2-devel libffi-devel
RUN wget https://www.python.org/ftp/python/2.7.10/Python-2.7.10.tgz -O /usr/src/Python-2.7.10.tgz
WORKDIR /usr/src
RUN tar xvf Python-2.7.10.tgz
WORKDIR /usr/src/Python-2.7.10
RUN ./configure
RUN make altinstall
WORKDIR /usr/src
RUN wget --no-check-certificate https://pypi.python.org/packages/source/s/setuptools/setuptools-11.3.tar.gz -O /usr/src/setuptools-11.3.tar.gz
RUN tar xvf setuptools-11.3.tar.gz
WORKDIR /usr/src/setuptools-11.3
RUN python2.7 setup.py install
RUN easy_install-2.7 pip
RUN pip2.7 install ansible==2.0.0.2
RUN pip2.7 install boto
COPY ansible.cfg /root
ENV ANSIBLE_CONFIG /root/ansible.cfg
RUN yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
RUN yum install -y which
RUN yum install -y nss
WORKDIR /usr/src
RUN wget http://apache.cs.utah.edu/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
RUN tar xzvf apache-maven-3.2.5-bin.tar.gz
RUN mv apache-maven-3.2.5 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/bin/mvn
WORKDIR /root

