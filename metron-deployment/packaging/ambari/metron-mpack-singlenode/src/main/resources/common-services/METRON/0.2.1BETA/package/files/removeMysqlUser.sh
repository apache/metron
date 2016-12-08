#!/usr/bin/env bash
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

mysqldbuser=$1
userhost=$2
mysqlqdminpassword=$3
myhostname=$(hostname -f)
sudo_prefix="/var/lib/ambari-agent/ambari-sudo.sh -H -E"

echo "Removing user $mysqldbuser@$userhost"
expect <<EOF
log_user 0
# start mysql process using password prompt
spawn mysql -u root -p
expect "password:"
send "${mysqlqdminpassword}\r"
# echo all output until the end
log_user 1
expect "mysql>"
send "DROP USER '${mysqldbuser}'@'${userhost}';\r"
expect "mysql>"
send "flush privileges;;\r"
send "\q"
EOF