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

# Parsers
parsers = config['configurations']['metron-env']['parsers']
metron_home = config['configurations']['metron-env']['metron_home']
metron_zookeeper_config_dir = config['configurations']['metron-env']['metron_zookeeper_config_dir']
metron_zookeeper_config_path = format('{metron_home}/{metron_zookeeper_config_dir}')
parsers_configured_flag_file = metron_zookeeper_config_path + '/../metron_parsers_configured'
parsers_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_parsers_acl_configured'

# Enrichment
metron_enrichment_topology = 'enrichment'
metron_enrichment_topic = 'enrichments'

enrichment_table = 'enrichment'
enrichment_cf = 't'
threatintel_table = 'threatintel'
threatintel_cf = 't'

# Indexing
metron_indexing_topology = config['configurations']['metron-env']['metron_indexing_topology']
indexing_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_configured'
indexing_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_indexing_acl_configured'

# Enrichment
enrichment_kafka_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_kafka_configured'
enrichment_kafka_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_kafka_acl_configured'
enrichment_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_hbase_configured'
enrichment_hbase_acl_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_hbase_acl_configured'
enrichment_geo_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_geo_configured'

# Storm
storm_rest_addr = config['configurations']['metron-env']['storm_rest_addr']

# Zeppelin
zeppelin_server_url = config['configurations']['metron-env']['zeppelin_server_url']

# Security
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version_formatted = format_stack_version(stack_version_unformatted)
hostname = config['hostname']
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
tmp_dir = Script.get_tmp_dir()

if security_enabled:
    storm_principal_name = config['configurations']['storm-env']['storm_principal_name']
    storm_keytab_path = config['configurations']['storm-env']['storm_keytab']
