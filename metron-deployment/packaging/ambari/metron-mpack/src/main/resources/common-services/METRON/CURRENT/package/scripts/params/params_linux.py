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

import functools
import os

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hdp_version = default("/commandParams/version", None)

hostname = config['hostname']
metron_home = status_params.metron_home

parsers = status_params.parsers
parser_error_topic = config['configurations']['metron-parsers-env']['parser_error_topic']
geoip_hdfs_dir = "/apps/metron/geo/default/"
asn_hdfs_dir = "/apps/metron/asn/default/"
metron_user = status_params.metron_user
metron_group = config['configurations']['metron-env']['metron_group']
metron_log_dir = config['configurations']['metron-env']['metron_log_dir']
metron_pid_dir = config['configurations']['metron-env']['metron_pid_dir']
metron_rest_host = status_params.metron_rest_host
metron_rest_port = status_params.metron_rest_port
metron_management_ui_host = status_params.metron_management_ui_host
metron_management_ui_port = status_params.metron_management_ui_port
metron_management_ui_path = metron_home + '/web/management-ui/'
metron_alerts_ui_host = status_params.metron_alerts_ui_host
metron_alerts_ui_port = status_params.metron_alerts_ui_port
metron_alerts_ui_path = metron_home + '/web/alerts-ui/'
metron_jvm_flags = config['configurations']['metron-rest-env']['metron_jvm_flags']

# Construct the profiles as a temp variable first. Only the first time it's set will carry through
metron_spring_profiles_active = config['configurations']['metron-rest-env']['metron_spring_profiles_active']
metron_ldap_enabled = config['configurations']['metron-security-env']['metron.ldap.enabled']
if metron_ldap_enabled:
    if not len(metron_spring_profiles_active) == 0:
        metron_spring_profiles_active += ',ldap'
    else:
        metron_spring_profiles_active = 'ldap'

metron_jdbc_driver = config['configurations']['metron-rest-env']['metron_jdbc_driver']
metron_jdbc_url = config['configurations']['metron-rest-env']['metron_jdbc_url']
metron_jdbc_username = config['configurations']['metron-rest-env']['metron_jdbc_username']
metron_jdbc_password = config['configurations']['metron-rest-env']['metron_jdbc_password']
metron_jdbc_platform = config['configurations']['metron-rest-env']['metron_jdbc_platform']
metron_jdbc_client_path = config['configurations']['metron-rest-env']['metron_jdbc_client_path']
metron_spring_options = config['configurations']['metron-rest-env']['metron_spring_options']
metron_escalation_topic = config['configurations']['metron-rest-env']['metron_escalation_topic']
metron_config_path = metron_home + '/config'
metron_zookeeper_config_dir = status_params.metron_zookeeper_config_dir
metron_zookeeper_config_path = status_params.metron_zookeeper_config_path
# indicates if zk_load_configs.sh --mode PUSH has been executed
zk_configured_flag_file = status_params.zk_configured_flag_file
parsers_configured_flag_file = status_params.parsers_configured_flag_file
parsers_acl_configured_flag_file = status_params.parsers_acl_configured_flag_file
enrichment_kafka_configured_flag_file = status_params.enrichment_kafka_configured_flag_file
enrichment_kafka_acl_configured_flag_file = status_params.enrichment_kafka_acl_configured_flag_file
enrichment_hbase_configured_flag_file = status_params.enrichment_hbase_configured_flag_file
enrichment_hbase_acl_configured_flag_file = status_params.enrichment_hbase_acl_configured_flag_file
enrichment_maxmind_configured_flag_file = status_params.enrichment_maxmind_configured_flag_file
indexing_configured_flag_file = status_params.indexing_configured_flag_file
indexing_acl_configured_flag_file = status_params.indexing_acl_configured_flag_file
indexing_hbase_configured_flag_file = status_params.indexing_hbase_configured_flag_file
indexing_hbase_acl_configured_flag_file = status_params.indexing_hbase_acl_configured_flag_file
indexing_hdfs_perm_configured_flag_file = status_params.indexing_hdfs_perm_configured_flag_file
elasticsearch_template_installed_flag_file = status_params.elasticsearch_template_installed_flag_file
solr_schema_installed_flag_file = status_params.solr_schema_installed_flag_file
rest_kafka_configured_flag_file = status_params.rest_kafka_configured_flag_file
rest_kafka_acl_configured_flag_file = status_params.rest_kafka_acl_configured_flag_file
rest_hbase_configured_flag_file = status_params.rest_hbase_configured_flag_file
rest_hbase_acl_configured_flag_file = status_params.rest_hbase_acl_configured_flag_file
metron_knox_installed_flag_file = status_params.metron_knox_installed_flag_file
global_properties_template = config['configurations']['metron-env']['elasticsearch-properties']

# Elasticsearch hosts and port management
es_cluster_name = config['configurations']['metron-env']['es_cluster_name']
es_hosts = config['configurations']['metron-env']['es_hosts']
es_host_list = es_hosts.split(",")
es_http_port = config['configurations']['metron-env']['es_http_port']
es_url = ",".join([host + ":" + es_http_port for host in es_host_list])
es_http_url = es_host_list[0] + ":" + es_http_port
es_date_format = config['configurations']['metron-env']['es_date_format']

# hadoop params
stack_root = Script.get_stack_root()
# This is the cluster group named 'hadoop'. Its membership is the stack process user ids not individual users.
# The config name 'user_group' is out of our control and a bit misleading, so it is renamed to 'hadoop_group'.
hadoop_group = config['configurations']['cluster-env']['user_group']
hadoop_home_dir = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
kafka_home = os.path.join(stack_root, "current", "kafka-broker")
kafka_bin_dir = os.path.join(kafka_home, "bin")

# zookeeper
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

# Solr params
solr_version = '6.6.2'
solr_home = '/var/solr/solr-' + solr_version
solr_zookeeper_url = format(format(config['configurations']['metron-env']['solr_zookeeper_url']))
solr_user = config['configurations']['solr-config-env']['solr_config_user']
solr_principal_name = config['configurations']['solr-config-env']['solr_principal_name']
solr_keytab_path = config['configurations']['solr-config-env']['solr_keytab_path']

# Storm
storm_rest_addr = status_params.storm_rest_addr

# Zeppelin
zeppelin_server_url = status_params.zeppelin_server_url

# Kafka
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

metron_apps_hdfs_dir = config['configurations']['metron-env']['metron_apps_hdfs_dir']

# the double "format" is not an error - we are pulling in a jinja-templated param. This is a bit of a hack, but works
# well enough until we find a better way via Ambari
metron_temp_grok_path = format(format(config['configurations']['metron-rest-env']['metron_temp_grok_path']))

metron_topic_retention = config['configurations']['metron-env']['metron_topic_retention']

local_grok_patterns_dir = format("{metron_home}/patterns")
hdfs_grok_patterns_dir = format("{metron_apps_hdfs_dir}/patterns")

# for create_hdfs_directory
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
dfs_type = default("/commandParams/dfs_type", "")

# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
    HdfsResource,
    user=hdfs_user,
    hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
    security_enabled=security_enabled,
    keytab=hdfs_user_keytab,
    kinit_path_local=kinit_path_local,
    hadoop_bin_dir=hadoop_bin_dir,
    hadoop_conf_dir=hadoop_conf_dir,
    principal_name=hdfs_principal_name,
    hdfs_site=hdfs_site,
    default_fs=default_fs,
    immutable_paths=get_not_managed_resources(),
    dfs_type=dfs_type
)

# Metron HBase configuration
enrichment_hbase_provider_impl = 'org.apache.metron.hbase.HTableProvider'
enrichment_hbase_table = status_params.enrichment_hbase_table
enrichment_hbase_cf = status_params.enrichment_hbase_cf
update_hbase_table = status_params.update_hbase_table
update_hbase_cf = status_params.update_hbase_cf


threatintel_hbase_table = status_params.threatintel_hbase_table
threatintel_hbase_cf = status_params.threatintel_hbase_cf

# Kafka Topics
ambari_kafka_service_check_topic = 'ambari_kafka_service_check'
consumer_offsets_topic = '__consumer_offsets'

# ES Templates
bro_index_path = tmp_dir + "/bro_index.template"
snort_index_path = tmp_dir + "/snort_index.template"
yaf_index_path = tmp_dir + "/yaf_index.template"
error_index_path = tmp_dir + "/error_index.template"
meta_index_path = tmp_dir + "/metaalert_index.template"

# Solr Schemas
bro_schema_path = metron_home + "/config/schema/bro"
snort_schema_path = metron_home + "/config/schema/snort"
yaf_schema_path = metron_home + "/config/schema/yaf"
error_schema_path = metron_home + "/config/schema/error"
meta_schema_path = metron_home + "/config/schema/metaalert"

# Zeppelin Notebooks
metron_config_zeppelin_path = format("{metron_config_path}/zeppelin")
zeppelin_shiro_ini_content = status_params.zeppelin_shiro_ini_content

# kafka_security
kafka_security_protocol = config['configurations']['kafka-broker'].get('security.inter.broker.protocol', 'PLAINTEXT')

kafka_user = config['configurations']['kafka-env']['kafka_user']
storm_user = config['configurations']['storm-env']['storm_user']

# HBase user table creation and ACLs
hbase_user = config['configurations']['hbase-env']['hbase_user']

# Security
security_enabled = status_params.security_enabled
client_jaas_path = metron_home + '/client_jaas.conf'
client_jaas_arg = '-Djava.security.auth.login.config=' + metron_home + '/client_jaas.conf'
enrichment_topology_worker_childopts = client_jaas_arg if security_enabled else ''
profiler_topology_worker_childopts = client_jaas_arg if security_enabled else ''
indexing_topology_worker_childopts = client_jaas_arg if security_enabled else ''
pcap_topology_worker_childopts = client_jaas_arg if security_enabled else ''
metron_jvm_flags += (' ' + client_jaas_arg) if security_enabled else ''
topology_auto_credentials = config['configurations']['storm-site'].get('nimbus.credential.renewers.classes', [])
# Needed for storm.config, because it needs Java String
topology_auto_credentials_double_quotes = str(topology_auto_credentials).replace("'", '"')

if security_enabled:
    hostname_lowercase = config['hostname'].lower()
    metron_principal_name = status_params.metron_principal_name
    metron_keytab_path = status_params.metron_keytab_path
    kinit_path_local = status_params.kinit_path_local

    hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
    hbase_keytab_path = config['configurations']['hbase-env']['hbase_user_keytab']

    kafka_principal_raw = config['configurations']['kafka-env']['kafka_principal_name']
    kafka_principal_name = kafka_principal_raw.replace('_HOST', hostname_lowercase)
    kafka_keytab_path = config['configurations']['kafka-env']['kafka_keytab']

    metron_client_jaas_conf_template = config['configurations']['metron-client-jaas-conf']['content']

    nimbus_seeds = config['configurations']['storm-site']['nimbus.seeds']
    # Check wether Solr mpack is installed
    if 'solr-config-env' in config['configurations']:
        solr_principal_name = solr_principal_name.replace('_HOST', hostname_lowercase)

# LDAP
metron_ldap_url = config['configurations']['metron-security-env']['metron.ldap.url']
metron_ldap_userdn = config['configurations']['metron-security-env']['metron.ldap.bind.dn']
metron_ldap_password = config['configurations']['metron-security-env']['metron.ldap.bind.password']
metron_ldap_user_pattern = config['configurations']['metron-security-env']['metron.ldap.user.dnpattern']
metron_ldap_user_password = config['configurations']['metron-security-env']['metron.ldap.user.password']
metron_ldap_user_dnbase = config['configurations']['metron-security-env']['metron.ldap.user.basedn']
metron_ldap_user_searchbase = config['configurations']['metron-security-env']['metron.ldap.user.searchbase']
metron_ldap_user_searchfilter = config['configurations']['metron-security-env']['metron.ldap.user.searchfilter']
metron_ldap_group_searchbase = config['configurations']['metron-security-env']['metron.ldap.group.searchbase']
metron_ldap_group_searchfilter = config['configurations']['metron-security-env']['metron.ldap.group.searchfilter']
metron_ldap_group_role = config['configurations']['metron-security-env']['metron.ldap.group.roleattribute']
metron_ldap_ssl_truststore = config['configurations']['metron-security-env']['metron.ldap.ssl.truststore']
metron_ldap_ssl_truststore_password = config['configurations']['metron-security-env']['metron.ldap.ssl.truststore.password']

# Roles
metron_user_role = config['configurations']['metron-security-env']['metron_user_role']
metron_admin_role = config['configurations']['metron-security-env']['metron_admin_role']

# REST
metron_rest_pid_dir = config['configurations']['metron-rest-env']['metron_rest_pid_dir']
metron_rest_pid = 'metron-rest.pid'
metron_indexing_classpath = config['configurations']['metron-rest-env']['metron_indexing_classpath']
metron_rest_classpath = config['configurations']['metron-rest-env']['metron_rest_classpath']
metron_sysconfig = config['configurations']['metron-rest-env']['metron_sysconfig']
user_settings_hbase_table = status_params.user_settings_hbase_table
user_settings_hbase_cf = status_params.user_settings_hbase_cf
source_type_field = config['configurations']['metron-rest-env']['source_type_field']
threat_triage_score_field = config['configurations']['metron-rest-env']['threat_triage_score_field']

# Enrichment
metron_enrichment_topology = status_params.metron_enrichment_topology
geoip_url = config['configurations']['metron-enrichment-env']['geoip_url']
asn_url = config['configurations']['metron-enrichment-env']['asn_url']
enrichment_host_known_hosts = config['configurations']['metron-enrichment-env']['enrichment_host_known_hosts']

# Enrichment - Kafka
enrichment_kafka_start = config['configurations']['metron-enrichment-env']['enrichment_kafka_start']
enrichment_input_topic = status_params.enrichment_input_topic
enrichment_output_topic = config['configurations']['metron-enrichment-env']['enrichment_output_topic']
enrichment_error_topic = config['configurations']['metron-enrichment-env']['enrichment_error_topic']
threatintel_error_topic = config['configurations']['metron-enrichment-env']['threatintel_error_topic']
enrichment_kafka_writer_batch_size = config['configurations']['metron-enrichment-env']['enrichment_kafka_writer_batch_size']
enrichment_kafka_writer_batch_timeout = config['configurations']['metron-enrichment-env']['enrichment_kafka_writer_batch_timeout']

# Enrichment - Storm common parameters
enrichment_workers = config['configurations']['metron-enrichment-env']['enrichment_workers']
enrichment_acker_executors = config['configurations']['metron-enrichment-env']['enrichment_acker_executors']
if not len(enrichment_topology_worker_childopts) == 0:
    enrichment_topology_worker_childopts += ' '
enrichment_topology_worker_childopts += config['configurations']['metron-enrichment-env']['enrichment_topology_worker_childopts']
enrichment_topology_max_spout_pending = config['configurations']['metron-enrichment-env']['enrichment_topology_max_spout_pending']
enrichment_topology = config['configurations']['metron-enrichment-env']['enrichment_topology']

# Enrichment - Split Join topology
enrichment_join_cache_size = config['configurations']['metron-enrichment-env']['enrichment_join_cache_size']
threatintel_join_cache_size = config['configurations']['metron-enrichment-env']['threatintel_join_cache_size']
enrichment_kafka_spout_parallelism = config['configurations']['metron-enrichment-env']['enrichment_kafka_spout_parallelism']
enrichment_split_parallelism = config['configurations']['metron-enrichment-env']['enrichment_split_parallelism']
enrichment_stellar_parallelism = config['configurations']['metron-enrichment-env']['enrichment_stellar_parallelism']
enrichment_join_parallelism = config['configurations']['metron-enrichment-env']['enrichment_join_parallelism']
threat_intel_split_parallelism = config['configurations']['metron-enrichment-env']['threat_intel_split_parallelism']
threat_intel_stellar_parallelism = config['configurations']['metron-enrichment-env']['threat_intel_stellar_parallelism']
threat_intel_join_parallelism = config['configurations']['metron-enrichment-env']['threat_intel_join_parallelism']
kafka_writer_parallelism = config['configurations']['metron-enrichment-env']['kafka_writer_parallelism']

# Enrichment - Unified topology
unified_kafka_spout_parallelism = config['configurations']['metron-enrichment-env']['unified_kafka_spout_parallelism']
unified_enrichment_parallelism = config['configurations']['metron-enrichment-env']['unified_enrichment_parallelism']
unified_threat_intel_parallelism = config['configurations']['metron-enrichment-env']['unified_threat_intel_parallelism']
unified_kafka_writer_parallelism = config['configurations']['metron-enrichment-env']['unified_kafka_writer_parallelism']
unified_enrichment_cache_size = config['configurations']['metron-enrichment-env']['unified_enrichment_cache_size']
unified_threat_intel_cache_size = config['configurations']['metron-enrichment-env']['unified_threat_intel_cache_size']
unified_enrichment_threadpool_size = config['configurations']['metron-enrichment-env']['unified_enrichment_threadpool_size']
unified_enrichment_threadpool_type = config['configurations']['metron-enrichment-env']['unified_enrichment_threadpool_type']

# Profiler
metron_profiler_topology = 'profiler'
profiler_input_topic = config['configurations']['metron-enrichment-env']['enrichment_output_topic']
profiler_kafka_start = config['configurations']['metron-profiler-env']['profiler_kafka_start']
profiler_period_duration = config['configurations']['metron-profiler-env']['profiler_period_duration']
profiler_period_units = config['configurations']['metron-profiler-env']['profiler_period_units']
profiler_window_duration = config['configurations']['metron-profiler-env']['profiler_window_duration']
profiler_window_units = config['configurations']['metron-profiler-env']['profiler_window_units']
profiler_ttl = config['configurations']['metron-profiler-env']['profiler_ttl']
profiler_ttl_units = config['configurations']['metron-profiler-env']['profiler_ttl_units']
profiler_hbase_batch = config['configurations']['metron-profiler-env']['profiler_hbase_batch']
profiler_hbase_flush_interval = config['configurations']['metron-profiler-env']['profiler_hbase_flush_interval']
profiler_topology_workers = config['configurations']['metron-profiler-env']['profiler_topology_workers']
profiler_acker_executors = config['configurations']['metron-profiler-env']['profiler_acker_executors']
profiler_hbase_table = config['configurations']['metron-profiler-env']['profiler_hbase_table']
profiler_hbase_cf = config['configurations']['metron-profiler-env']['profiler_hbase_cf']
profiler_configured_flag_file = status_params.profiler_configured_flag_file
profiler_acl_configured_flag_file = status_params.profiler_acl_configured_flag_file
profiler_hbase_configured_flag_file = status_params.profiler_hbase_configured_flag_file
profiler_hbase_acl_configured_flag_file = status_params.profiler_hbase_acl_configured_flag_file
if not len(profiler_topology_worker_childopts) == 0:
    profiler_topology_worker_childopts += ' '
profiler_topology_worker_childopts += config['configurations']['metron-profiler-env']['profiler_topology_worker_childopts']
profiler_max_routes_per_bolt=config['configurations']['metron-profiler-env']['profiler_max_routes_per_bolt']
profiler_window_lag=config['configurations']['metron-profiler-env']['profiler_window_lag']
profiler_window_lag_units=config['configurations']['metron-profiler-env']['profiler_window_lag_units']
profiler_topology_message_timeout_secs=config['configurations']['metron-profiler-env']['profiler_topology_message_timeout_secs']
profiler_topology_max_spout_pending=config['configurations']['metron-profiler-env']['profiler_topology_max_spout_pending']
profiler_kafka_writer_batch_size = config['configurations']['metron-profiler-env']['profiler_kafka_writer_batch_size']
profiler_kafka_writer_batch_timeout = config['configurations']['metron-profiler-env']['profiler_kafka_writer_batch_timeout']

# Indexing
ra_indexing_kafka_start = config['configurations']['metron-indexing-env']['ra_indexing_kafka_start']
batch_indexing_kafka_start = config['configurations']['metron-indexing-env']['batch_indexing_kafka_start']
indexing_input_topic = status_params.indexing_input_topic
indexing_error_topic = config['configurations']['metron-indexing-env']['indexing_error_topic']
metron_random_access_indexing_topology = status_params.metron_random_access_indexing_topology
metron_batch_indexing_topology = status_params.metron_batch_indexing_topology
ra_indexing_writer = config['configurations']['metron-indexing-env']['ra_indexing_writer']
batch_indexing_writer_class_name = config['configurations']['metron-indexing-env']['batch_indexing_writer_class_name']
ra_indexing_workers = config['configurations']['metron-indexing-env']['ra_indexing_workers']
batch_indexing_workers = config['configurations']['metron-indexing-env']['batch_indexing_workers']
ra_indexing_acker_executors = config['configurations']['metron-indexing-env']['ra_indexing_acker_executors']
batch_indexing_acker_executors = config['configurations']['metron-indexing-env']['batch_indexing_acker_executors']
if not len(indexing_topology_worker_childopts) == 0:
    indexing_topology_worker_childopts += ' '
indexing_topology_worker_childopts += config['configurations']['metron-indexing-env']['indexing_topology_worker_childopts']
ra_indexing_topology_max_spout_pending = config['configurations']['metron-indexing-env']['ra_indexing_topology_max_spout_pending']
batch_indexing_topology_max_spout_pending = config['configurations']['metron-indexing-env']['batch_indexing_topology_max_spout_pending']
ra_indexing_kafka_spout_parallelism = config['configurations']['metron-indexing-env']['ra_indexing_kafka_spout_parallelism']
batch_indexing_kafka_spout_parallelism = config['configurations']['metron-indexing-env']['batch_indexing_kafka_spout_parallelism']
ra_indexing_writer_parallelism = config['configurations']['metron-indexing-env']['ra_indexing_writer_parallelism']
hdfs_writer_parallelism = config['configurations']['metron-indexing-env']['hdfs_writer_parallelism']

# the double "format" is not an error - we are pulling in a jinja-templated param. This is a bit of a hack, but works
# well enough until we find a better way via Ambari
metron_apps_indexed_hdfs_dir = format(format(config['configurations']['metron-indexing-env']['metron_apps_indexed_hdfs_dir']))

bolt_hdfs_rotation_policy = config['configurations']['metron-indexing-env']['bolt_hdfs_rotation_policy']
bolt_hdfs_rotation_policy_units = config['configurations']['metron-indexing-env']['bolt_hdfs_rotation_policy_units']
bolt_hdfs_rotation_policy_count = config['configurations']['metron-indexing-env']['bolt_hdfs_rotation_policy_count']

# PCAP
metron_pcap_topology = status_params.metron_pcap_topology
pcap_input_topic = status_params.pcap_input_topic
pcap_base_path = config['configurations']['metron-pcap-env']['pcap_base_path']
pcap_base_interim_result_path = config['configurations']['metron-pcap-env']['pcap_base_interim_result_path']
pcap_final_output_path = config['configurations']['metron-pcap-env']['pcap_final_output_path']
pcap_page_size = config['configurations']['metron-pcap-env']['pcap_page_size']
pcap_yarn_queue = config['configurations']['metron-pcap-env']['pcap_yarn_queue']
pcap_finalizer_threadpool_size= config['configurations']['metron-pcap-env']['pcap_finalizer_threadpool_size']
pcap_configured_flag_file = status_params.pcap_configured_flag_file
pcap_perm_configured_flag_file = status_params.pcap_perm_configured_flag_file
pcap_acl_configured_flag_file = status_params.pcap_acl_configured_flag_file
pcap_topology_workers = config['configurations']['metron-pcap-env']['pcap_topology_workers']
if not len(pcap_topology_worker_childopts) == 0:
    pcap_topology_worker_childopts += ' '
pcap_topology_worker_childopts += config['configurations']['metron-pcap-env']['pcap_topology_worker_childopts']
spout_kafka_topic_pcap = config['configurations']['metron-pcap-env']['spout_kafka_topic_pcap']
hdfs_sync_every = config['configurations']['metron-pcap-env']['hdfs_sync_every']
hdfs_replication_factor = config['configurations']['metron-pcap-env']['hdfs_replication_factor']
kafka_pcap_start = config['configurations']['metron-pcap-env']['kafka_pcap_start']
kafka_pcap_numpackets = config['configurations']['metron-pcap-env']['kafka_pcap_numpackets']
kafka_pcap_maxtimems = config['configurations']['metron-pcap-env']['kafka_pcap_maxtimems']
kafka_pcap_tsscheme = config['configurations']['metron-pcap-env']['kafka_pcap_tsscheme']
kafka_pcap_out = config['configurations']['metron-pcap-env']['kafka_pcap_out']
kafka_pcap_ts_granularity = config['configurations']['metron-pcap-env']['kafka_pcap_ts_granularity']
kafka_spout_parallelism = config['configurations']['metron-pcap-env']['kafka_spout_parallelism']


# MapReduce
metron_user_hdfs_dir = '/user/' + metron_user
metron_user_hdfs_dir_configured_flag_file = status_params.metron_user_hdfs_dir_configured_flag_file

# Knox
knox_user = config['configurations']['knox-env']['knox_user']
knox_group = config['configurations']['knox-env']['knox_group']
metron_knox_root_path = '/gateway/metron'
metron_rest_path = '/api/v1'
metron_alerts_ui_login_path = '/login'
metron_management_ui_login_path = '/login'
metron_knox_enabled = config['configurations']['metron-security-env']['metron.knox.enabled']
metron_knox_sso_pubkey = config['configurations']['metron-security-env']['metron.knox.sso.pubkey']
metron_knox_sso_token_ttl = config['configurations']['metron-security-env']['metron.knox.sso.token.ttl']
if metron_knox_enabled:
    metron_rest_path = metron_knox_root_path + '/metron-rest' + metron_rest_path
    metron_alerts_ui_login_path = metron_knox_root_path + '/metron-alerts/'
    metron_management_ui_login_path = metron_knox_root_path + '/metron-management/sensors'
    if not len(metron_spring_options) == 0:
        metron_spring_options += ' '
    metron_spring_options += '--knox.root=' + metron_knox_root_path + '/metron-rest'
    metron_spring_options += ' --knox.sso.pubkey=' + metron_knox_sso_pubkey
    if not len(metron_spring_profiles_active) == 0:
        metron_spring_profiles_active += ','
    metron_spring_profiles_active += 'knox'

knox_home = os.path.join(stack_root, "current", "knox-server")
knox_hosts = default("/clusterHostInfo/knox_gateway_hosts", [])
knox_host = ''
if not len(knox_hosts) == 0:
    knox_host = knox_hosts[0]