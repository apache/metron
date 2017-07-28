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
FROM centos

ARG METRON_VERSION

ENV METRON_VERSION $METRON_VERSION
ENV JAVA_HOME /usr
ENV HBASE_HOME /opt/hbase
ENV HBASE_MANAGES_ZK false
ENV METRON_HOME /usr/metron/$METRON_VERSION/

ADD ./data /data
ADD ./data-management /data-management
RUN mkdir -p $METRON_HOME
RUN tar -xzf /data-management/metron-data-management-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
RUN curl -sL http://archive.apache.org/dist/hbase/1.1.6/hbase-1.1.6-bin.tar.gz | tar -xzC /tmp
RUN mv /tmp/hbase-1.1.6 /opt/hbase
RUN yum install -y java-1.8.0-openjdk lsof
ADD ./conf/enrichment-extractor.json /conf/enrichment-extractor.json
ADD ./conf/threatintel-extractor.json /conf/threatintel-extractor.json
ADD ./conf/hbase-site.docker.xml $HBASE_HOME/conf/hbase-site.xml
ADD ./bin $HBASE_HOME/bin
RUN chmod 755 $HBASE_HOME/bin/wait-for-it.sh

EXPOSE 8080 8085 9090 9095 16000 16010 16201 16301

WORKDIR /opt/hbase
CMD ./bin/start.sh
