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
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import default, format
from resource_management.libraries.functions.version import format_stack_version

config = Script.get_config()

hostname = config['hostname']
metron_user = config['configurations']['metron-env']['metron_user']
metron_home = config['configurations']['metron-env']['metron_home']
metron_zookeeper_config_dir = config['configurations']['metron-env']['metron_zookeeper_config_dir']
metron_zookeeper_config_path = format('{metron_home}/{metron_zookeeper_config_dir}')
# indicates if zk_load_configs.sh --mode PUSH has been executed
zk_configured_flag_file = metron_zookeeper_config_path + '/../metron_zookeeper_configured'

# Parsers
parsers = config['configurations']['metron-parsers-env']['parsers']
parsers_configured_flag_file = metron_zookeeper_config_path + '/../metron_parsers_configured'
parsers_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_parsers_acl_configured'

# Enrichment
metron_enrichment_topology = 'enrichment'
enrichment_input_topic = config['configurations']['metron-enrichment-env']['enrichment_input_topic']
enrichment_kafka_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_kafka_configured'
enrichment_kafka_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_kafka_acl_configured'
enrichment_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_hbase_configured'
enrichment_hbase_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_hbase_acl_configured'
enrichment_geo_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_geo_configured'

enrichment_hbase_table = config['configurations']['metron-enrichment-env']['enrichment_hbase_table']
enrichment_hbase_cf = config['configurations']['metron-enrichment-env']['enrichment_hbase_cf']
threatintel_hbase_table = config['configurations']['metron-enrichment-env']['threatintel_hbase_table']
threatintel_hbase_cf = config['configurations']['metron-enrichment-env']['threatintel_hbase_cf']
update_hbase_table = config['configurations']['metron-indexing-env']['update_hbase_table']
update_hbase_cf = config['configurations']['metron-indexing-env']['update_hbase_cf']

# Profiler
metron_profiler_topology = 'profiler'
profiler_input_topic = config['configurations']['metron-enrichment-env']['enrichment_output_topic']
profiler_hbase_table = config['configurations']['metron-profiler-env']['profiler_hbase_table']
profiler_hbase_cf = config['configurations']['metron-profiler-env']['profiler_hbase_cf']
profiler_configured_flag_file = metron_zookeeper_config_path + '/../metron_profiler_configured'
profiler_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_profiler_acl_configured'
profiler_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_profiler_hbase_configured'
profiler_hbase_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_profiler_hbase_acl_configured'


# Indexing
metron_batch_indexing_topology = 'batch_indexing'
metron_random_access_indexing_topology = 'random_access_indexing'
indexing_input_topic = config['configurations']['metron-indexing-env']['indexing_input_topic']
indexing_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_configured'
indexing_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_acl_configured'
indexing_hdfs_perm_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_hdfs_perm_configured'
indexing_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_hbase_configured'
indexing_hbase_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_hbase_acl_configured'

# Elasticsearch
elasticsearch_template_installed_flag_file = metron_zookeeper_config_path + '/../metron_elasticsearch_template_installed_flag_file'

# Solr
solr_schema_installed_flag_file = metron_zookeeper_config_path + '/../metron_solr_schema_installed_flag_file'

# REST
metron_rest_host = default("/clusterHostInfo/metron_rest_hosts", [hostname])[0]
metron_rest_port = config['configurations']['metron-rest-env']['metron_rest_port']
rest_kafka_configured_flag_file = metron_zookeeper_config_path + '/../metron_rest_kafka_configured'
rest_kafka_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_rest_kafka_acl_configured'
rest_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_rest_hbase_configured'
rest_hbase_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_rest_hbase_acl_configured'
user_settings_hbase_table = config['configurations']['metron-rest-env']['user_settings_hbase_table']
user_settings_hbase_cf = config['configurations']['metron-rest-env']['user_settings_hbase_cf']

# Alerts UI
metron_alerts_ui_host = default("/clusterHostInfo/metron_alerts_ui_hosts", [hostname])[0]
metron_alerts_ui_port = config['configurations']['metron-alerts-ui-env']['metron_alerts_ui_port']

# Management UI
metron_management_ui_host = default("/clusterHostInfo/metron_management_ui_hosts", [hostname])[0]
metron_management_ui_port = config['configurations']['metron-management-ui-env']['metron_management_ui_port']

# Storm
storm_rest_addr = config['configurations']['metron-env']['storm_rest_addr']

# Zeppelin
zeppelin_server_url = config['configurations']['metron-env']['zeppelin_server_url']
zeppelin_shiro_ini_content = config['configurations']['zeppelin-shiro-ini']['shiro_ini_content']

# Security
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version_formatted = format_stack_version(stack_version_unformatted)

security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
tmp_dir = Script.get_tmp_dir()

metron_user = config['configurations']['metron-env']['metron_user']

metron_principal_name = config['configurations']['metron-env']['metron_principal_name']
metron_keytab_path = config['configurations']['metron-env']['metron_service_keytab']

# Pcap
metron_pcap_topology = 'pcap'
pcap_input_topic = config['configurations']['metron-pcap-env']['spout_kafka_topic_pcap']
pcap_configured_flag_file = metron_zookeeper_config_path + '/../metron_pcap_configured'
pcap_perm_configured_flag_file = metron_zookeeper_config_path + '/../metron_pcap_perm_configured'
pcap_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_pcap_acl_configured'

# MapReduce
metron_user_hdfs_dir_configured_flag_file = metron_zookeeper_config_path + '/../metron_user_hdfs_dir_configured'

# Knox
metron_knox_installed_flag_file = metron_zookeeper_config_path + '/../metron_knox_installed'