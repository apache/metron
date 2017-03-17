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

from ambari_commons import OSCheck
from resource_management.libraries.functions import format
from resource_management.libraries.script import Script

config = Script.get_config()

# Parsers
parsers = config['configurations']['metron-env']['parsers']
all_parsers = "asa,bro,cef,fireeye,ise,lancope,logstash,paloalto,snort,sourcefire,squid,websphere,yaf"
all_alt_parsers = ""
metron_home = config['configurations']['metron-env']['metron_home']
local_metron_extensions_lib = format(format(config['configurations']['metron-env']['local_metron_extension_lib']))
hdfs_metron_apps_extensions_working = format(format(config['configurations']['metron-env']['metron_apps_extensions_working_hdfs_dir']))
hdfs_metron_apps_extensions_lib = format(format(config['configurations']['metron-env']['metron_apps_extensions_lib_hdfs_dir']))
hdfs_metron_apps_extensions_alt_lib = format(format(config['configurations']['metron-env']['metron_apps_extensions_alt_lib_hdfs_dir']))
metron_extensions_etc = format(format(config['configurations']['metron-env']['metron_extension_etc']))
metron_extensions_etc_parsers = format(format(config['configurations']['metron-env']['metron_extension_etc_parsers']))
metron_extensions_alt_etc = format(format(config['configurations']['metron-env']['metron_extension_etc']))
metron_extensions_alt_etc_parsers = format(format(config['configurations']['metron-env']['metron_extension_alt_etc_parsers']))
metron_zookeeper_config_dir = config['configurations']['metron-env']['metron_zookeeper_config_dir']
metron_zookeeper_config_path = format('{metron_home}/{metron_zookeeper_config_dir}')
parsers_configured_flag_file = metron_zookeeper_config_path + '/../metron_parsers_configured'

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

# Enrichment
enrichment_kafka_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_kafka_configured'
enrichment_hbase_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_hbase_configured'
enrichment_geo_configured_flag_file = metron_zookeeper_config_path + '/../metron_enrichment_geo_configured'

# Storm
storm_rest_addr = config['configurations']['metron-env']['storm_rest_addr']

# Zeppelin
zeppelin_server_url = config['configurations']['metron-env']['zeppelin_server_url']
