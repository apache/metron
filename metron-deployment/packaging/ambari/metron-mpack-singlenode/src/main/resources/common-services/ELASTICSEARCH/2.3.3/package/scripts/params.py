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

from resource_management.libraries.script import Script

# server configurations
config = Script.get_config()

elastic_home = config['configurations']['elastic-sysconfig']['elastic_home']
data_dir = config['configurations']['elastic-sysconfig']['data_dir']
work_dir = config['configurations']['elastic-sysconfig']['work_dir']
conf_dir = config['configurations']['elastic-env']['conf_dir']
heap_size = config['configurations']['elastic-sysconfig']['heap_size']
max_open_files = config['configurations']['elastic-sysconfig']['max_open_files']
max_map_count = config['configurations']['elastic-sysconfig']['max_map_count']

elastic_user = config['configurations']['elastic-env']['elastic_user']
user_group = config['configurations']['elastic-env']['user_group']
log_dir = config['configurations']['elastic-env']['elastic_log_dir']
pid_dir = config['configurations']['elastic-sysconfig']['elastic_pid_dir']
# un-used -- pid_file = "{0}/elasticsearch.pid".format(elastic_pid_dir)
hostname = config['hostname']
java64_home = config['hostLevelParams']['java_home']
elastic_env_sh_template = config['configurations']['elastic-env']['content']
sysconfig_template = config['configurations']['elastic-sysconfig']['content']

cluster_name = config['configurations']['elastic-site']['es_cluster_name']
zen_discovery_ping_unicast_hosts = config['configurations']['elastic-site']['es_cluster_hosts']
path_data = config['configurations']['elastic-site']['path_data']
http_port = config['configurations']['elastic-site']['http_port']
transport_tcp_port = config['configurations']['elastic-site']['transport_tcp_port']
index_number_of_shards = config['configurations']['elastic-site']['index_number_of_shards']
index_number_of_replicas = config['configurations']['elastic-site']['index_number_of_replicas']
network_host = config['configurations']['elastic-site']['network_bindings']
