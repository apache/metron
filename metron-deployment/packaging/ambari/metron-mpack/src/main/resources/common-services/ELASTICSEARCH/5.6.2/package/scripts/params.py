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

def yamlify_variables(var) :
  if isinstance(var, type(True)):
    return str(var).lower()
  else:
    return var

# server configurations
config = Script.get_config()

masters_also_are_datanodes = config['configurations']['elastic-site']['masters_also_are_datanodes']
elastic_home = config['configurations']['elastic-sysconfig']['elastic_home']
data_dir = config['configurations']['elastic-sysconfig']['data_dir']
work_dir = config['configurations']['elastic-sysconfig']['work_dir']
conf_dir = config['configurations']['elastic-sysconfig']['conf_dir']
heap_size = config['configurations']['elastic-sysconfig']['heap_size']
max_open_files = config['configurations']['elastic-sysconfig']['max_open_files']
max_map_count = config['configurations']['elastic-sysconfig']['max_map_count']

elastic_user = config['configurations']['elastic-env']['elastic_user']
elastic_group = config['configurations']['elastic-env']['elastic_group']
log_dir = config['configurations']['elastic-env']['elastic_log_dir']
pid_dir = config['configurations']['elastic-env']['elastic_pid_dir']

hostname = config['hostname']
java64_home = config['hostLevelParams']['java_home']
elastic_env_sh_template = config['configurations']['elastic-env']['content']
sysconfig_template = config['configurations']['elastic-sysconfig']['content']

cluster_name = config['configurations']['elastic-site']['cluster_name']
zen_discovery_ping_unicast_hosts = config['configurations']['elastic-site']['zen_discovery_ping_unicast_hosts']

path_data = config['configurations']['elastic-site']['path_data']
http_cors_enabled = config['configurations']['elastic-site']['http_cors_enabled']
http_port = config['configurations']['elastic-site']['http_port']
transport_tcp_port = config['configurations']['elastic-site']['transport_tcp_port']

recover_after_time = config['configurations']['elastic-site']['recover_after_time']
gateway_recover_after_data_nodes = config['configurations']['elastic-site']['gateway_recover_after_data_nodes']
expected_data_nodes = config['configurations']['elastic-site']['expected_data_nodes']
index_merge_scheduler_max_thread_count = config['configurations']['elastic-site']['index_merge_scheduler_max_thread_count']
index_translog_flush_threshold_size = config['configurations']['elastic-site']['index_translog_flush_threshold_size']
index_refresh_interval = config['configurations']['elastic-site']['index_refresh_interval']
indices_memory_index_store_throttle_type = config['configurations']['elastic-site']['indices_memory_index_store_throttle_type']
index_number_of_shards = config['configurations']['elastic-site']['index_number_of_shards']
index_number_of_replicas = config['configurations']['elastic-site']['index_number_of_replicas']
indices_memory_index_buffer_size = config['configurations']['elastic-site']['indices_memory_index_buffer_size']
bootstrap_memory_lock = yamlify_variables(config['configurations']['elastic-site']['bootstrap_memory_lock'])
threadpool_bulk_queue_size = config['configurations']['elastic-site']['threadpool_bulk_queue_size']
cluster_routing_allocation_node_concurrent_recoveries = config['configurations']['elastic-site']['cluster_routing_allocation_node_concurrent_recoveries']
cluster_routing_allocation_disk_watermark_low = config['configurations']['elastic-site']['cluster_routing_allocation_disk_watermark_low']
cluster_routing_allocation_disk_threshold_enabled = yamlify_variables(config['configurations']['elastic-site']['cluster_routing_allocation_disk_threshold_enabled'])
cluster_routing_allocation_disk_watermark_high = config['configurations']['elastic-site']['cluster_routing_allocation_disk_watermark_high']
indices_fielddata_cache_size = config['configurations']['elastic-site']['indices_fielddata_cache_size']
indices_cluster_send_refresh_mapping = yamlify_variables(config['configurations']['elastic-site']['indices_cluster_send_refresh_mapping'])
threadpool_index_queue_size = config['configurations']['elastic-site']['threadpool_index_queue_size']

discovery_zen_ping_timeout = config['configurations']['elastic-site']['discovery_zen_ping_timeout']
discovery_zen_fd_ping_interval = config['configurations']['elastic-site']['discovery_zen_fd_ping_interval']
discovery_zen_fd_ping_timeout = config['configurations']['elastic-site']['discovery_zen_fd_ping_timeout']
discovery_zen_fd_ping_retries = config['configurations']['elastic-site']['discovery_zen_fd_ping_retries']

network_host = config['configurations']['elastic-site']['network_host']
network_publish_host = config['configurations']['elastic-site']['network_publish_host']

limits_conf_dir = "/etc/security/limits.d"
limits_conf_file = limits_conf_dir + "/elasticsearch.conf"
elastic_user_nofile_limit = config['configurations']['elastic-env']['elastic_user_nofile_limit']
elastic_user_nproc_limit = config['configurations']['elastic-env']['elastic_user_nproc_limit']
elastic_user_memlock_soft_limit = config['configurations']['elastic-env']['elastic_user_memlock_soft_limit']
elastic_user_memlock_hard_limit = config['configurations']['elastic-env']['elastic_user_memlock_hard_limit']

# the status check (service elasticsearch status) cannot be run by the 'elasticsearch'
# user due to the default permissions that are set when the package is installed.  the
# status check must be run as root
elastic_status_check_user = 'root'

# when using the RPM or Debian packages on systems that use systemd, system limits
# must be specified via systemd.
# see https://www.elastic.co/guide/en/elasticsearch/reference/5.6/setting-system-settings.html#systemd
systemd_parent_dir = '/etc/systemd/system/'
systemd_elasticsearch_dir = systemd_parent_dir + 'elasticsearch.service.d/'
systemd_override_file = systemd_elasticsearch_dir + 'override.conf'
systemd_override_template = config['configurations']['elastic-systemd']['content']

heap_size = config['configurations']['elastic-jvm-options']['heap_size']
jvm_options_template = config['configurations']['elastic-jvm-options']['content']
