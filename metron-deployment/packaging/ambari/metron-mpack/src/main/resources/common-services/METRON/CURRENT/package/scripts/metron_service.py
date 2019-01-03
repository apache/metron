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

import json
import os
import subprocess

from datetime import datetime
from resource_management.core.logger import Logger
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, File
from resource_management.core.resources.system import Execute
from resource_management.core.source import Template
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.script import Script

from metron_security import kinit


def is_zk_configured(params):
  return os.path.isfile(params.zk_configured_flag_file)

def init_zk_config(params):
  Logger.info('Loading ALL Metron config into ZooKeeper - this command should ONLY be executed by Ambari on initial install.')
  Execute(ambari_format(
      "{metron_home}/bin/zk_load_configs.sh --zk_quorum {zookeeper_quorum} --mode PUSH --input_dir {metron_zookeeper_config_path}"),
      path=ambari_format("{java_home}/bin")
  )

def set_configured(user, flag_file, log_msg):
  Logger.info(log_msg)
  File(flag_file,
       content="This file created on: " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
       owner=user,
       mode=0755)

def set_zk_configured(params):
  set_configured(params.metron_user, params.zk_configured_flag_file, "Setting Zookeeper configured to true")

def solr_global_config_patches():
  """
  Builds the global configuration patches required for Solr.
  """
  return """
    {
        "op": "add",
        "path": "/solr.zookeeper",
        "value": "{{solr_zookeeper_url}}"
    }
  """

def elasticsearch_global_config_patches():
  """
  Builds the global configuration patches required for Elasticsearch.
  """
  return """
    {
        "op": "add",
        "path": "/es.clustername",
        "value": "{{ es_cluster_name }}"
    },
    {
        "op": "add",
        "path": "/es.ip",
        "value": "{{ es_url }}"
    },
    {
        "op": "add",
        "path": "/es.date.format",
        "value": "{{es_date_format}}"
    }
  """

def build_global_config_patch(params, patch_file):
  """
  Build the file used to patch the global configuration.
  See RFC 6902 at https://tools.ietf.org/html/rfc6902

  :param params:
  :param patch_file: The path where the patch file will be created.
  """
  if params.ra_indexing_writer == 'Solr':
      indexing_patches = solr_global_config_patches()
  else:
      indexing_patches = elasticsearch_global_config_patches()
  other_patches = """
    {
        "op": "add",
        "path": "/profiler.client.period.duration",
        "value": "{{profiler_period_duration}}"
    },
    {
        "op": "add",
        "path": "/profiler.client.period.duration.units",
        "value": "{{profiler_period_units}}"
    },
    {
        "op": "add",
        "path": "/parser.error.topic",
        "value": "{{parser_error_topic}}"
    },
    {
        "op": "add",
        "path": "/update.hbase.table",
        "value": "{{update_hbase_table}}"
    },
    {
        "op": "add",
        "path": "/update.hbase.cf",
        "value": "{{update_hbase_cf}}"
    },
    {
        "op": "add",
        "path": "/user.settings.hbase.table",
        "value": "{{user_settings_hbase_table}}"
    },
    {
        "op": "add",
        "path": "/user.settings.hbase.cf",
        "value": "{{user_settings_hbase_cf}}"
    },
    {
        "op": "add",
        "path": "/bootstrap.servers",
        "value": "{{kafka_brokers}}"
    },
    {
        "op": "add",
        "path": "/source.type.field",
        "value": "{{source_type_field}}"
    },
    {
        "op": "add",
        "path": "/threat.triage.score.field",
        "value": "{{threat_triage_score_field}}"
    },
    {
        "op": "add",
        "path": "/enrichment.writer.batchSize",
        "value": "{{enrichment_kafka_writer_batch_size}}"
    },
    {
        "op": "add",
        "path": "/enrichment.writer.batchTimeout",
        "value": "{{enrichment_kafka_writer_batch_timeout}}"
    },
    {
        "op": "add",
        "path": "/profiler.writer.batchSize",
        "value": "{{profiler_kafka_writer_batch_size}}"
    },
    {
        "op": "add",
        "path": "/profiler.writer.batchTimeout",
        "value": "{{profiler_kafka_writer_batch_timeout}}"
    }
  """
  patch_template = ambari_format(
  """
  [
    {indexing_patches},
    {other_patches}
  ]
  """)
  File(patch_file,
       content=InlineTemplate(patch_template),
       owner=params.metron_user,
       group=params.metron_group)

def patch_global_config(params):
  patch_file = "/tmp/metron-global-config-patch.json"
  Logger.info("Setup temporary global config JSON patch (formatting per RFC6902): " + patch_file)
  build_global_config_patch(params, patch_file)

  Logger.info('Patching global config in ZooKeeper')
  Execute(ambari_format(
      "{metron_home}/bin/zk_load_configs.sh --zk_quorum {zookeeper_quorum} --mode PATCH --config_type GLOBAL --patch_file " + patch_file),
      path=ambari_format("{java_home}/bin")
  )
  Logger.info("Done patching global config")

def pull_config(params):
  Logger.info('Pulling all Metron configs down from ZooKeeper to local file system')
  Logger.info('NOTE - THIS IS OVERWRITING THE LOCAL METRON CONFIG DIR WITH ZOOKEEPER CONTENTS: ' + params.metron_zookeeper_config_path)
  Execute(ambari_format(
      "{metron_home}/bin/zk_load_configs.sh --zk_quorum {zookeeper_quorum} --mode PULL --output_dir {metron_zookeeper_config_path} --force"),
      path=ambari_format("{java_home}/bin")
  )

def refresh_configs(params):
  if not is_zk_configured(params):
    Logger.warning("The expected flag file '" + params.zk_configured_flag_file + "'indicating that Zookeeper has been configured does not exist. Skipping patching. An administrator should look into this.")
    return
  check_indexer_parameters()
  patch_global_config(params)
  pull_config(params)

def get_running_topologies(params):
  Logger.info('Getting Running Storm Topologies from Storm REST Server')
  Logger.info('Security enabled? ' + str(params.security_enabled))

  # Want to sudo to the metron user and kinit as them so we aren't polluting root with Metron's Kerberos tickets.
  # This is becuase we need to run a command with a return as the metron user. Sigh
  negotiate = '--negotiate -u : ' if params.security_enabled else ''
  cmd = ambari_format(
    'curl --max-time 3 ' + negotiate + '{storm_rest_addr}/api/v1/topology/summary')

  if params.security_enabled:
    kinit(params.kinit_path_local,
          params.metron_keytab_path,
          params.metron_principal_name,
          execute_user=params.metron_user)

  Logger.info('Running cmd: ' + cmd)
  return_code, stdout, stderr = get_user_call_output(cmd,
                                                     user=params.metron_user,
                                                     is_checked_call=False)

  if (return_code != 0):
    return {}

  try:
    stormjson = json.loads(stdout)
  except ValueError, e:
    Logger.info('Stdout: ' + str(stdout))
    Logger.info('Stderr: ' + str(stderr))
    Logger.exception(str(e))
    return {}

  topologiesDict = {}

  for topology in stormjson['topologies']:
    topologiesDict[topology['name']] = topology['status']

  Logger.info("Topologies: " + str(topologiesDict))
  return topologiesDict

def init_kafka_topics(params, topics):
  Logger.info('Creating Kafka topics')

  # Create the topics. All the components need indexing (for errors), so we pass '--if-not-exists'.
  command_template = """{0}/kafka-topics.sh \
                            --zookeeper {1} \
                            --create \
                            --if-not-exists \
                            --topic {2} \
                            --partitions {3} \
                            --replication-factor {4} \
                            --config retention.bytes={5}"""

  num_partitions = 1
  replication_factor = 1
  retention_gigabytes = int(params.metron_topic_retention)
  retention_bytes = retention_gigabytes * 1024 * 1024 * 1024
  for topic in topics:
    Logger.info("Creating topic'{0}'".format(topic))
    Execute(command_template.format(params.kafka_bin_dir,
                                    params.zookeeper_quorum,
                                    topic,
                                    num_partitions,
                                    replication_factor,
                                    retention_bytes),
            user=params.kafka_user, tries=3, try_sleep=5, logoutput=True)
  Logger.info("Done creating Kafka topics")

def init_kafka_acls(params, topics):
    Logger.info('Creating Kafka topic ACLs')
    acl_template = """{0}/kafka-acls.sh \
    --authorizer kafka.security.auth.SimpleAclAuthorizer \
    --authorizer-properties zookeeper.connect={1} \
    --add \
    --allow-principal User:{2} \
    --topic {3}"""

    for topic in topics:
        Logger.info("Creating ACL for topic '{0}'".format(topic))
        Execute(acl_template.format(params.kafka_bin_dir,
                                    params.zookeeper_quorum,
                                    params.metron_user,
                                    topic),
                user=params.kafka_user, tries=3, try_sleep=5, logoutput=True)

def init_kafka_acl_groups(params, groups):
    Logger.info('Creating Kafka group ACLs')
    acl_template = """{0}/kafka-acls.sh \
    --authorizer kafka.security.auth.SimpleAclAuthorizer \
    --authorizer-properties zookeeper.connect={1} \
    --add \
    --allow-principal User:{2} \
    --group {3}"""

    for group in groups:
        Logger.info("Creating ACL for group '{0}'".format(group))
        Execute(acl_template.format(params.kafka_bin_dir,
                                    params.zookeeper_quorum,
                                    params.metron_user,
                                    group),
                user=params.kafka_user, tries=3, try_sleep=5, logoutput=True)

def execute(cmd, user, err_msg=None, tries=3, try_sleep=5, logoutput=True, path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'):
    """
    Executes a command and raises an appropriate error message if the command
    fails.
    :param cmd: The command to execute.
    :param user: The user to execute the command as.
    :param err_msg: The error message to display if the command fails.
    :param tries: The number of attempts to execute the command.
    :param try_sleep: The time between attempts.
    :param logoutput: If true, log the command output.
    :param path: The path use when running the command.
    :return:
    """
    try:
        Execute(cmd, tries=tries, try_sleep=try_sleep, logoutput=logoutput, user=user, path=path)
    except:
        if err_msg is None:
            err_msg = "Execution failed: cmd={0}, user={1}".format(cmd, user)
        raise Fail(err_msg)

def check_kafka_topics(params, topics):
    """
    Validates that the Kafka topics exist.  An exception is raised if any of the
    topics do not exist.
    :param params:
    :param topics: A list of topic names.
    """

    # if needed kinit as 'metron'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.metron_keytab_path,
              params.metron_principal_name,
              execute_user=params.metron_user)

    template = """{0}/kafka-topics.sh \
      --zookeeper {1} \
      --list | \
      awk 'BEGIN {{cnt=0;}} /{2}/ {{cnt++}} END {{if (cnt > 0) {{exit 0}} else {{exit 1}}}}'"""

    for topic in topics:
        Logger.info("Checking existence of Kafka topic '{0}'".format(topic))
        cmd = template.format(params.kafka_bin_dir, params.zookeeper_quorum, topic)
        err_msg = "Missing Kafka topic; topic={0}".format(topic)
        execute(cmd, user=params.kafka_user, err_msg=err_msg)


def create_hbase_table(params, table, cf):
    """
    Creates an HBase table, if the table does not currently exist
    :param params:
    :param table: The name of the HBase table.
    :param cf:  The column family
    :param user: The user to execute the command as
    """
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)
    cmd = """if [[ $(echo \"exists '{0}'\" | hbase shell | grep 'not exist') ]]; \
     then echo \"create '{0}','{1}'\" | hbase shell -n; fi"""
    add_update_cmd = cmd.format(table, cf)
    Execute(add_update_cmd,
            tries=3,
            try_sleep=5,
            logoutput=False,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            user=params.hbase_user
            )


def check_hbase_table(params, table):
    """
    Validates that an HBase table exists.  An exception is raised if the table
    does not exist.
    :param params:
    :param table: The name of the HBase table.
    """
    Logger.info("Checking HBase table '{0}'".format(table))

    # if needed kinit as 'hbase'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)

    template = "echo \"exists '{0}'\" | hbase shell -n | grep 'Table {1} does exist'"
    cmd = template.format(table, table)
    err_msg = "Missing HBase table; table={0}".format(table)
    execute(cmd, user=params.hbase_user, err_msg=err_msg)

def check_hbase_column_family(params, table, column_family):
    """
    Validates that an HBase column family exists.  An exception is raised if the
    column family does not exist.
    :param params:
    :param table: The name of the HBase table.
    :param column_family: The name of the HBase column family.
    """
    Logger.info("Checking column family '{0}:{1}'".format(table, column_family))

    # if needed kinit as 'hbase'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)

    template = "echo \"desc '{0}'\" | hbase shell -n | grep \"NAME => '{1}'\""
    cmd = template.format(table, column_family)
    err_msg = "Missing HBase column family; table={0}, cf={1}".format(table, column_family)
    execute(cmd, user=params.hbase_user, err_msg=err_msg)

def check_hbase_acls(params, table, user=None, permissions="READ,WRITE"):
    """
    Validates that HBase table permissions exist for a user. An exception is
    raised if the permissions do not exist.
    :param params:
    :param table: The name of the HBase table.
    :param user: The name of the user.
    :param permissions: The permissions that should exist.
    """
    if user is None:
        user = params.metron_user
    Logger.info("Checking HBase ACLs; table={0}, user={1}, permissions={2}".format(table, user, permissions))

    # if needed kinit as 'hbase'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)



    template = """echo "user_permission '{0}'" | \
      hbase shell -n | \
      grep " {1} " | \
      grep "actions={2}"
    """
    cmd = template.format(table, user, permissions)
    err_msg = "Missing HBase access; table={0}, user={1}, permissions={2}".format(table, user, permissions)
    execute(cmd, user=params.hbase_user, err_msg=err_msg)

def check_hdfs_dir_exists(params, path, user=None):
    """
    Validate that a directory exists in HDFS.
    :param params:
    :param path: The directory path in HDFS.
    :param user: The user to execute the check under.
    """
    if user is None:
        user = params.metron_user
    Logger.info("Checking HDFS; directory={0} user={1}".format(path, user))

    # if needed kinit as 'metron'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.metron_keytab_path,
              params.metron_principal_name,
              execute_user=params.metron_user)

    template = "{0}/hdfs dfs -test -d {1}"
    cmd = template.format(params.hadoop_bin_dir, path)
    err_msg = "Missing directory in HDFS: directory={0} user={1}".format(path, user)
    execute(cmd, user=params.metron_user, err_msg=err_msg)

def check_hdfs_file_exists(params, path, user=None):
    """
    Validate that a file exists in HDFS.
    :param params:
    :param path: The file path in HDFS.
    :param user: The user to execute the check under.
    """
    if user is None:
        user = params.metron_user
    Logger.info("Checking HDFS; file={0}, user={1}".format(path, user))

    # if needed kinit as 'metron'
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.metron_keytab_path,
              params.metron_principal_name,
              execute_user=params.metron_user)

    template = "{0}/hdfs dfs -test -f {1}"
    cmd = template.format(params.hadoop_bin_dir, path)
    err_msg = "Missing file in HDFS; file={0}".format(path)
    execute(cmd, user=user, err_msg=err_msg)

def check_kafka_acls(params, topics, user=None):
    """
    Validate that permissions have been granted for a list of Kakfa topics.
    :param params:
    :param topics: A list of topic names.
    :param user: The user whose access is checked.
    """
    if user is None:
        user = params.metron_user

    template = """{0}/kafka-acls.sh \
        --authorizer kafka.security.auth.SimpleAclAuthorizer \
        --authorizer-properties zookeeper.connect={1} \
        --topic {2} \
        --list | grep 'User:{3}'"""

    for topic in topics:
        Logger.info("Checking ACL; topic={0}, user={1}'".format(topic, user))
        cmd = template.format(params.kafka_bin_dir, params.zookeeper_quorum, topic, user)
        err_msg = "Missing Kafka access; topic={0}, user={1}".format(topic, user)
        execute(cmd, user=params.kafka_user, err_msg=err_msg)

def check_kafka_acl_groups(params, groups, user=None):
    """
    Validate that Kafka group permissions have been granted.
    :param params:
    :param groups: A list of group name.
    :param user: The user whose access is checked.
    """
    if user is None:
        user = params.metron_user

    template = """{0}/kafka-acls.sh \
        --authorizer kafka.security.auth.SimpleAclAuthorizer \
        --authorizer-properties zookeeper.connect={1} \
        --group {2} \
        --list | grep 'User:{3}'"""

    for group in groups:
        Logger.info("Checking group ACL for topic '{0}'".format(group))
        cmd = template.format(params.kafka_bin_dir, params.zookeeper_quorum, group, user)
        err_msg = "Missing Kafka group access; group={0}, user={1}".format(group, user)
        execute(cmd, user=params.kafka_user, err_msg=err_msg)

def check_http(host, port, user):
    """
    Check for a valid HTTP response.
    :param hostname: The hostname.
    :param port: The port number.
    :param user: Execute the HTTP request as.
    """
    cmd = "curl -sS --max-time 3 {0}:{1}".format(host, port)
    Logger.info('Checking HTTP connectivity; host={0}, port={1}, user={2} cmd={3}'.format(host, port, user, cmd))
    try:
      Execute(cmd, tries=3, try_sleep=5, logoutput=False, user=user)
    except:
      raise ComponentIsNotRunning()

def check_indexer_parameters():
    """
    Ensure that all required parameters have been defined for the chosen
    Indexer; either Solr or Elasticsearch.
    """
    missing = []
    config = Script.get_config()
    indexer = config['configurations']['metron-indexing-env']['ra_indexing_writer']
    Logger.info('Checking parameters for indexer = ' + indexer)

    if indexer == 'Solr':
      # check for all required solr parameters
      if not config['configurations']['metron-env']['solr_zookeeper_url']:
        missing.append("metron-env/solr_zookeeper_url")

    else:
      # check for all required elasticsearch parameters
      if not config['configurations']['metron-env']['es_cluster_name']:
        missing.append("metron-env/es_cluster_name")
      if not config['configurations']['metron-env']['es_hosts']:
        missing.append("metron-env/es_hosts")
      if not config['configurations']['metron-env']['es_date_format']:
        missing.append("metron-env/es_date_format")

    if len(missing) > 0:
      raise Fail("Missing required indexing parameters(s): indexer={0}, missing={1}".format(indexer, missing))

def install_metron_knox(params):
    if os.path.exists(params.knox_home):
        template = """export KNOX_HOME={0}; \
            export KNOX_USER={1}; \
            export KNOX_GROUP={2}; \
            {3}/bin/install_metron_knox.sh; \
            unset KNOX_USER; \
            unset KNOX_GROUP; \
            unset KNOX_HOME;"""
        cmd = template.format(params.knox_home, params.knox_user, params.knox_group, params.metron_home)

        Execute(cmd)

def is_metron_knox_installed(params):
    return os.path.isfile(params.metron_knox_installed_flag_file)

def set_metron_knox_installed(params):
    Directory(params.metron_zookeeper_config_path,
              mode=0755,
              owner=params.metron_user,
              group=params.metron_group,
              create_parents=True
              )
    set_configured(params.metron_user, params.metron_knox_installed_flag_file, "Setting Metron Knox installed to true")

def metron_knox_topology_setup(params):
    if os.path.exists(params.knox_home):
        File(ambari_format("{knox_home}/conf/topologies/metron.xml"),
             content=Template("metron.xml.j2"),
             owner=params.knox_user,
             group=params.knox_group
             )
        File(ambari_format("{knox_home}/conf/topologies/metronsso.xml"),
             content=Template("metronsso.xml.j2"),
             owner=params.knox_user,
             group=params.knox_group
             )
