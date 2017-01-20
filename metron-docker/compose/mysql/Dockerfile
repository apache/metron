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
FROM mysql:5.7

ARG METRON_VERSION

ENV METRON_VERSION $METRON_VERSION
ENV METRON_HOME /usr/metron/$METRON_VERSION/

ADD http://geolite.maxmind.com/download/geoip/database/GeoLiteCity_CSV/GeoLiteCity-latest.tar.xz /tmp/geoip/GeoLiteCity-latest.tar.xz
ADD ./bin /usr/local/bin
RUN chmod 755 /usr/local/bin/wait-for-it.sh
ADD ./enrichment /enrichment

RUN apt-get update
RUN apt-get install xz-utils
RUN tar xf /tmp/geoip/GeoLiteCity-latest.tar.xz -C /tmp/geoip/
RUN cp /tmp/geoip/GeoLiteCity_*/* /var/lib/mysql-files
RUN mkdir -p $METRON_HOME
RUN tar -xzf /enrichment/metron-enrichment-$METRON_VERSION-archive.tar.gz -C /usr/metron/$METRON_VERSION/

WORKDIR /usr/local
CMD ./bin/start.sh
