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

ARG DOCKER_HOST
ARG BROKER_IP_ADDR
ARG METRON_VERSION

ENV METRON_VERSION $METRON_VERSION
ENV METRON_HOME /usr/metron/$METRON_VERSION/
ENV ZK_CLIENT_JARS /opt/kafka_2.11-0.10.0.0/libs

ADD https://archive.apache.org/dist/kafka/0.10.0.0/kafka_2.11-0.10.0.0.tgz /opt/kafka_2.11-0.10.0.0.tgz
RUN tar -xzf /opt/kafka_2.11-0.10.0.0.tgz -C /opt
RUN echo -n 'advertised.listeners=PLAINTEXT://' >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN echo $DOCKER_HOST | sed "s/^$/"$BROKER_IP_ADDR":/g" | sed "s/tcp:\\/\\///g" | sed "s/:.*/:9092/g" >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN echo 'delete.topic.enable=true' >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN yum install -y java-1.8.0-openjdk lsof

RUN mkdir -p $METRON_HOME
ADD ./bin /opt/kafka_2.11-0.10.0.0/bin
RUN chmod 755 /opt/kafka_2.11-0.10.0.0/bin/wait-for-it.sh
ADD ./common /common
ADD ./parser /parser
ADD ./enrichment /enrichment
ADD ./data /data
RUN tar -xzf /common/metron-common-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
RUN tar -xzf /parser/metron-parsers-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
RUN tar -xzf /enrichment/metron-enrichment-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
ADD ./conf /$METRON_HOME/config/zookeeper

EXPOSE 2181 9092

WORKDIR /opt/kafka_2.11-0.10.0.0
CMD ./bin/start.sh
