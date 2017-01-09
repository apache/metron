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

ARG DOCKER_HOST="localhost"

ADD https://archive.apache.org/dist/kafka/0.10.0.0/kafka_2.11-0.10.0.0.tgz /opt/kafka_2.11-0.10.0.0.tgz
RUN tar -xzf /opt/kafka_2.11-0.10.0.0.tgz -C /opt
RUN echo -n 'advertised.listeners=PLAINTEXT://' >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN echo $DOCKER_HOST | sed "s/tcp:\\/\\///g" | sed "s/:.*/:9092/g" >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN echo 'delete.topic.enable=true' >> /opt/kafka_2.11-0.10.0.0/config/server.properties
RUN yum install -y java-1.8.0-openjdk lsof
ADD ./bin /opt/kafka_2.11-0.10.0.0/bin
ADD ./data /data

EXPOSE 2181 9092

WORKDIR /opt/kafka_2.11-0.10.0.0
CMD ./bin/start.sh
