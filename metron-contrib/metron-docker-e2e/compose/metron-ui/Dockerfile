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
FROM node:carbon

ARG METRON_VERSION

ENV METRON_VERSION $METRON_VERSION
ENV METRON_HOME /usr/metron/$METRON_VERSION

RUN mkdir -p $METRON_HOME
ADD ./bin $METRON_HOME/bin
COPY ./packages/* /packages/
RUN find /packages -type f -name '*.tar.gz' -exec tar -xzf {} -C /usr/metron/$METRON_VERSION/ \;
ADD ./config $METRON_HOME/config
RUN npm install --prefix="$METRON_HOME/web/expressjs" --only=production

EXPOSE 4201 4201

WORKDIR $METRON_HOME
CMD ./bin/start.sh
