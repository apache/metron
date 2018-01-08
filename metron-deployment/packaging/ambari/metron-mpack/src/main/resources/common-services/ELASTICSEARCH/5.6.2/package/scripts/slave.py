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
from resource_management.core.logger import Logger

def slave():
    import params

    Logger.info("Creating user: {0}:{1}".format(params.elastic_user, params.elastic_group))
    User(params.elastic_user, action = "create", groups = params.elastic_group)

    params.path_data = params.path_data.replace('"', '')
    data_path = params.path_data.replace(' ', '').split(',')
    data_path[:] = [x.replace('"', '') for x in data_path]
    directories = [params.log_dir, params.pid_dir, params.conf_dir]
    directories = directories + data_path

    Logger.info("Creating directories: {0}".format(directories))
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

    elastic_site = params.config['configurations']['elastic-site']
    path = "{0}/elasticsearch.yml".format(params.conf_dir)
    Logger.info("Creating ES slave configuration.")
    File(path,
         content=Template(
             "elasticsearch.slave.yaml.j2",
             configurations=elastic_site),
         owner=params.elastic_user,
         group=params.elastic_group
         )

    Logger.info("Slave sysconfig: /etc/sysconfig/elasticsearch")
    File(format("/etc/sysconfig/elasticsearch"),
         owner="root",
         group="root",
         content=InlineTemplate(params.sysconfig_template)
         )

    elastic_env = params.config['configurations']['elastic-env']
    Logger.info("Slave PAM limits: {0}".format(params.limits_conf_file))
    File(params.limits_conf_file,
         content=Template(
              'elasticsearch_limits.conf.j2',
              configurations=elastic_env),
         owner="root",
         group="root"
         )
