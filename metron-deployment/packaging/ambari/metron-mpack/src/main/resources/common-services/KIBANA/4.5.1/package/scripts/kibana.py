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

kibana service params.

"""

from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template


def kibana():
    print "INSIDE THE %s" % __file__
    import params

    directories = [params.log_dir, params.pid_dir, params.conf_dir]

    Directory(directories,
              # recursive=True,
              mode=0755,
              owner=params.kibana_user,
              group=params.kibana_user
              )

    print "Master env: ""{}/kibana.yml".format(params.conf_dir)
    File("{}/kibana.yml".format(params.conf_dir),
         owner=params.kibana_user,
         content=InlineTemplate(params.kibana_yml_template)
         )

