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

import os

from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.script import Script

import status_params

# Server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hostname = config['hostname']
metron_home = config['configurations']['metron-enrichment']['metron_home']
metron_ddl_dir = metron_home + '/ddl'
geo_ip_ddl = metron_ddl_dir + '/geoip_ddl.sql'
metron_enrichment_topology = status_params.metron_enrichment_topology
yum_repo_type = 'local'

metron_config_path = metron_home + '/config'
configured_flag_file = metron_config_path + '/metron_enrichment_is_configured'

# Hadoop params
hadoop_home_dir = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

# Zookeeper
zk_hosts = default("/clusterHostInfo/zookeeper_hosts", [])
has_zk_host = not len(zk_hosts) == 0
zookeeper_quorum = None
if has_zk_host:
    if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
        zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
    else:
        zookeeper_clientPort = '2181'
    zookeeper_quorum = (':' + zookeeper_clientPort + ',').join(config['clusterHostInfo']['zookeeper_hosts'])
    # last port config
    zookeeper_quorum += ':' + zookeeper_clientPort

# Kafka
stack_root = Script.get_stack_root()
kafka_home = os.path.join(stack_root, "current", "kafka-broker")
kafka_bin_dir = os.path.join(kafka_home, "bin")
metron_enrichment_topic_retention = config['configurations']['metron-enrichment']['metron_enrichment_topic_retention']

kafka_hosts = default("/clusterHostInfo/kafka_broker_hosts", [])
has_kafka_host = not len(kafka_hosts) == 0
kafka_brokers = None
if has_kafka_host:
    if 'port' in config['configurations']['kafka-broker']:
        kafka_broker_port = config['configurations']['kafka-broker']['port']
    else:
        kafka_broker_port = '6667'
    kafka_brokers = (':' + kafka_broker_port + ',').join(config['clusterHostInfo']['kafka_broker_hosts'])
    kafka_brokers += ':' + kafka_broker_port

# MYSQL
if OSCheck.is_ubuntu_family():
    mysql_configname = '/etc/mysql/my.cnf'
else:
    mysql_configname = '/etc/my.cnf'

daemon_name = status_params.daemon_name
# There will always be exactly one mysql_host
mysql_host = config['clusterHostInfo']['enrichment_mysql_server_hosts'][0]
mysql_port = config['configurations']['metron-enrichment']['metron_enrichment_db_port']

mysql_adduser_path = tmp_dir + "/addMysqlUser.sh"
mysql_deluser_path = tmp_dir + "/removeMysqlUser.sh"
mysql_create_geoip_path = tmp_dir + "/createMysqlGeoIp.sh"

enrichment_hosts = default("/clusterHostInfo/enrichment_host", [])
enrichment_host = enrichment_hosts[0] if len(enrichment_hosts) > 0 else None

metron_user = config['configurations']['metron-enrichment']['metron_user']
user_group = config['configurations']['cluster-env']['user_group']

enrichment_metron_user = config['configurations']['metron-enrichment']['metron_enrichment_db_user']
enrichment_metron_user_passwd = config['configurations']['metron-enrichment']['metron_enrichment_db_password']
enrichment_metron_user_passwd = unicode(enrichment_metron_user_passwd) if not is_empty(enrichment_metron_user_passwd) else enrichment_metron_user_passwd
process_name = status_params.process_name
