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
FROM fhuz/docker-storm:latest

ARG METRON_VERSION

ENV METRON_VERSION $METRON_VERSION
ENV METRON_HOME /usr/metron/$METRON_VERSION/

ADD ./bin $METRON_HOME/bin
ADD ./parser /parser
ADD ./enrichment /enrichment
ADD ./indexing /indexing
ADD ./elasticsearch /elasticsearch
RUN mkdir -p $METRON_HOME
RUN tar -xzf /parser/metron-parsing-storm-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/

RUN tar -xzf /enrichment/metron-enrichment-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
RUN sed -i -e "s/kafka.zk=.*:/kafka.zk=kafkazk:/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN sed -i -e "s/kafka.broker=.*/kafka.broker=kafkazk:9092/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN sed -i -e "s/threat.intel.tracker.table=.*/threat.intel.tracker.table=access_tracker/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN sed -i -e "s/threat.intel.tracker.cf=.*/threat.intel.tracker.cf=cf/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN sed -i -e "s/threat.intel.ip.table=.*/threat.intel.ip.table=ip/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN sed -i -e "s/threat.intel.ip.cf=.*/threat.intel.ip.cf=cf/g" /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN echo "threat.intel.simple.hbase.table=threatintel" >> /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN echo "threat.intel.simple.hbase.cf=cf" >> /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN echo "enrichment.simple.hbase.table=enrichment" >> /usr/metron/$METRON_VERSION/config/enrichment.properties
RUN echo "enrichment.simple.hbase.cf=cf\n" >> /usr/metron/$METRON_VERSION/config/enrichment.properties

RUN tar -xzf /indexing/metron-indexing-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/

RUN tar -xzf /elasticsearch/metron-elasticsearch-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/
RUN sed -i -e "s/kafka.zk=.*:/kafka.zk=kafkazk:/g" /usr/metron/$METRON_VERSION/config/elasticsearch.properties
RUN sed -i -e "s/kafka.broker=.*/kafka.broker=kafkazk:9092/g" /usr/metron/$METRON_VERSION/config/elasticsearch.properties
RUN sed -i -e "s/es.ip=.*/es.ip=metron-elasticsearch/g" /usr/metron/$METRON_VERSION/config/elasticsearch.properties
RUN sed -i -e "s/bolt.hdfs.file.system.url=.*/bolt.hdfs.file.system.url=hdfs:\/\/hadoop:9000/g" /usr/metron/$METRON_VERSION/config/elasticsearch.properties
RUN sed -i -e "s/index.hdfs.output=.*/index.hdfs.output=\/apps\/metron\/indexing\/indexed/g" /usr/metron/$METRON_VERSION/config/elasticsearch.properties

EXPOSE 8080 8000
EXPOSE 8081 8081

WORKDIR $METRON_HOME
