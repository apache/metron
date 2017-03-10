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

from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.core.resources import User

def slave():
    import params

    User(params.elastic_user, action = "create", groups = params.elastic_group)
    params.path_data = params.path_data.replace('"', '')
    data_path = params.path_data.replace(' ', '').split(',')
    data_path[:] = [x.replace('"', '') for x in data_path]

    directories = [params.log_dir, params.pid_dir, params.conf_dir]
    directories = directories + data_path

    Directory(directories,
              create_parents=True,
              mode=0755,
              owner=params.elastic_user,
              group=params.elastic_group,
              cd_access="a"
              )

    File("{0}/elastic-env.sh".format(params.conf_dir),
         owner=params.elastic_user,
         content=InlineTemplate(params.elastic_env_sh_template)
         )

    configurations = params.config['configurations']['elastic-site']

    File("{0}/elasticsearch.yml".format(params.conf_dir),
         content=Template(
             "elasticsearch.slave.yaml.j2",
             configurations=configurations),
         owner=params.elastic_user,
         group=params.elastic_group
         )

    print "Master sysconfig: /etc/sysconfig/elasticsearch"
    File(format("/etc/sysconfig/elasticsearch"),
         owner="root",
         group="root",
         content=InlineTemplate(params.sysconfig_template)
         )
