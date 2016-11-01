#!/bin/sh
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
from ../scripts/params import params

geoipscript=$1
geoipurl=$2
mysqlqdminpassword=$3

# Download and extract the actual GeoIP files
mkdir -p /tmp/geoip
mkdir -p /var/lib/mysql-files/

# Stage the GeoIP data
pushd /tmp/geoip
curl -O ${geoipurl}
tar xf GeoLiteCity-latest.tar.xz
cp /tmp/geoip/*/*.csv /var/lib/mysql-files/
popd

# Load MySQL with the GeoIP data
expect <<EOF
log_user 0
# start mysql process using password prompt
spawn mysql -u root -p
expect "password:"
send "${mysqlqdminpassword}\r"
# echo all output until the end
log_user 1
expect "mysql>"
send "source ${geoipscript}\r"
expect "mysql>"
send "show databases;\r"
send "\q"
EOF
