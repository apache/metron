#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from resource_management.libraries.functions.format import format


# Used to add metron access to the needed components
def mysql_adduser():
    from params import params

    File(params.mysql_adduser_path,
         mode=0755,
         content=StaticFile('addMysqlUser.sh')
         )

    add_user_cmd = format("bash -x {mysql_adduser_path} {metron_user} {enrichment_metron_user_passwd!p} {mysql_host} {mysql_admin_password!p}")
    Execute(add_user_cmd,
            tries=3,
            try_sleep=5,
            logoutput=False,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
            )


# Removes hive metron from components
def mysql_deluser():
    from params import params

    File(params.mysql_deluser_path,
         mode=0755,
         content=StaticFile('removeMysqlUser.sh')
         )

    del_user_cmd = format("bash -x {mysql_deluser_path} {metron_user} {mysql_host} {mysql_admin_password!p}")
    Execute(del_user_cmd,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            )
