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

Kibana Params configurations

"""

from urlparse import urlparse

from resource_management.libraries.functions import format
from resource_management.libraries.script import Script

# server configurations
config = Script.get_config()

kibana_home = '/usr/share/kibana/'
kibana_bin = '/usr/share/kibana/bin/'

conf_dir = "/etc/kibana"
kibana_user = config['configurations']['kibana-env']['kibana_user']
kibana_group = config['configurations']['kibana-env']['kibana_group']
log_dir = config['configurations']['kibana-env']['kibana_log_dir']
pid_dir = config['configurations']['kibana-env']['kibana_pid_dir']
pid_file = format("{pid_dir}/kibanasearch.pid")
es_url = config['configurations']['kibana-env']['kibana_es_url']
parsed = urlparse(es_url)
es_host = parsed.netloc.split(':')[0]
es_port = parsed.netloc.split(':')[1]
kibana_port = config['configurations']['kibana-env']['kibana_server_port']
kibana_server_host = config['configurations']['kibana-env']['kibana_server_host']
kibana_default_application = config['configurations']['kibana-env']['kibana_default_application']
hostname = config['hostname']
java64_home = config['hostLevelParams']['java_home']
kibana_yml_template = config['configurations']['kibana-site']['content']

