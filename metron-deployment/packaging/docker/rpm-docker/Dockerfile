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

RUN yum install -y tar
RUN yum install -y wget
RUN yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
WORKDIR /usr/src
RUN wget http://apache.cs.utah.edu/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
RUN tar xzvf apache-maven-3.2.5-bin.tar.gz
RUN mv apache-maven-3.2.5 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/bin/mvn
RUN yum -y install asciidoc rpm-build rpm2cpio tar unzip xmlto zip rpmlint && yum clean all
WORKDIR /root
