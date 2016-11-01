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
mysqldbuser=$1
mysqldbpasswd=$2
mysqldbhost=$3
mysqlqdminpassword=$4
myhostname=$(hostname -f)


echo "Adding user ${mysqldbuser}@${mysqldbhost} and ${mysqldbuser}@localhost"
expect <<EOF
log_user 0
# start mysql process using password prompt
spawn mysql -u root -p
expect "password:"
send "${mysqlqdminpassword}\r"
# echo all output until the end
expect "mysql>"
send "CREATE USER '${mysqldbuser}'@'${mysqldbhost}' IDENTIFIED BY '${mysqldbpasswd}';\r"
expect "mysql>"
send "CREATE USER '${mysqldbuser}'@'localhost' IDENTIFIED BY '${mysqldbpasswd}';\r"
expect "mysql>"
send "GRANT ALL PRIVILEGES ON GEO.* TO '${mysqldbuser}'@'%' IDENTIFIED BY '${mysqldbpasswd}';\r"
log_user 1
expect "mysql>"
send "GRANT ALL PRIVILEGES ON GEO.* TO '${mysqldbuser}'@'${mysqldbhost}';\r"
expect "mysql>"
send "GRANT ALL PRIVILEGES ON GEO.* TO '${mysqldbuser}'@'localhost';\r"
expect "mysql>"
send "flush privileges;\r"
send "\q"
EOF
