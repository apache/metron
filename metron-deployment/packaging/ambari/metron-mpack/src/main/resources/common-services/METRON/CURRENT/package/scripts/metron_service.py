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
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, File
from resource_management.core.resources.system import Execute
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
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

def build_global_config_patch(params, patch_file):
  # see RFC 6902 at https://tools.ietf.org/html/rfc6902
  patch_template = """
  [
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
        "path": "/profiler.client.period.duration",
        "value": "{{profiler_period_duration}}"
    },
    {
        "op": "add",
        "path": "/profiler.client.period.duration.units",
        "value": "{{profiler_period_units}}"
    }
  ]
  """
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

def pull_config(params):
  Logger.info('Pulling all Metron configs down from ZooKeeper to local file system')
  Logger.info('NOTE - THIS IS OVERWRITING THE LOCAL METRON CONFIG DIR WITH ZOOKEEPER CONTENTS: ' + params.metron_zookeeper_config_path)
  Execute(ambari_format(
      "{metron_home}/bin/zk_load_configs.sh --zk_quorum {zookeeper_quorum} --mode PULL --output_dir {metron_zookeeper_config_path} --force"),
      path=ambari_format("{java_home}/bin")
  )

# pushes json patches to zookeeper based on Ambari parameters that are configurable by the user
def refresh_configs(params):
  if not is_zk_configured(params):
    Logger.warning("The expected flag file '" + params.zk_configured_flag_file + "'indicating that Zookeeper has been configured does not exist. Skipping patching. An administrator should look into this.")
    return

  Logger.info("Patch global config in Zookeeper")
  patch_global_config(params)
  Logger.info("Done patching global config")

  Logger.info("Pull zookeeper config locally")
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


def check_kafka_topics(params, topics):

  if params.security_enabled:
    kinit(params.kinit_path_local,
          params.metron_keytab_path,
          params.metron_principal_name,
          execute_user=params.metron_user)

  cmd = """{0}/kafka-topics.sh \
    --zookeeper {1} \
    --list | \
    awk 'BEGIN {{cnt=0;}} /{2}/ {{cnt++}} END {{if (cnt > 0) {{exit 0}} else {{exit 1}}}}'"""
  for topic in topics:
    Logger.info("Checking existence of Kafka topic '{0}'".format(topic))
    try:
      Execute(
        cmd.format(params.kafka_bin_dir, params.zookeeper_quorum, topic),
        tries=3,
        try_sleep=5,
        logoutput=False,
        user=params.kafka_user)
    except:
      raise Fail("Missing Kafka topic; topic={0}".format(topic))

def check_hbase_table(params, table):
    Logger.info("Checking HBase table '{0}'".format(table))
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)

    cmd = "echo \"exists '{0}'\" | hbase shell -n | grep 'Table {1} does exist'"
    try:
        Execute(cmd.format(table, table),
                tries=3,
                try_sleep=5,
                logoutput=True,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=params.hbase_user)
    except:
        raise Fail("Missing HBase table; table={0}".format(table))

def check_hbase_column_family(params, table, column_family):
    Logger.info("Checking column family '{0}:{1}'".format(table, column_family))
    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.hbase_keytab_path,
              params.hbase_principal_name,
              execute_user=params.hbase_user)

    cmd = "echo \"desc '{0}'\" | hbase shell -n | grep \"NAME => '{1}'\""
    try:
        Execute(cmd.format(table, column_family),
                tries=3,
                try_sleep=5,
                logoutput=True,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=params.hbase_user)
    except:
        raise Fail("Missing HBase column family; table={0}, cf={1}".format(table, column_family))


def check_hbase_acls(params, table, permissions="READ,WRITE"):
    Logger.info("Checking HBase ACLs; table={0}".format(table))
    cmd = """echo "user_permission '{0}'" | \
      hbase shell -n | \
      grep " {1} " | \
      grep "actions={2}"
    """
    user = params.metron_user
    try:
        Execute(
            cmd.format(table, user, permissions),
            tries=3,
            try_sleep=5,
            logoutput=True,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            user=params.hbase_user)
    except:
        raise Fail("Missing HBase access; table={0}, user={1}, permissions={2}".format(table, user, permissions))

def check_hdfs_dir_exists(params, path):
    Logger.info("Checking HDFS for directory: {0}".format(path))
    cmd = "{0}/hdfs dfs -test -d {1}"
    try:
        Execute(
            cmd.format(params.hadoop_bin_dir, path),
            tries=3,
            try_sleep=5,
            logoutput=True,
            user=params.metron_user)
    except:
        raise Fail("Missing directory in HDFS: directory={0}".format(path))

def check_hdfs_file_exists(params, path):
    Logger.info("Checking HDFS for file: {0}".format(path))
    cmd = "{0}/hdfs dfs -test -f {1}"
    try:
        Execute(
            cmd.format(params.hadoop_bin_dir, path),
            tries=3,
            try_sleep=5,
            logoutput=True,
            user=params.metron_user)
    except:
        raise Fail("Missing file in HDFS; file={0}".format(path))

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

def check_kafka_acls(params, topics):
  cmd = """{0}/kafka-acls.sh \
    --authorizer kafka.security.auth.SimpleAclAuthorizer \
    --authorizer-properties zookeeper.connect={1} \
    --topic {2} \
    --list | grep 'User:{3}'"""

  user = params.metron_user
  for topic in topics:
    Logger.info("Checking ACL for topic '{0}'".format(topic))
    try:
      Execute(
        cmd.format(params.kafka_bin_dir, params.zookeeper_quorum, topic, user),
        tries=3,
        try_sleep=5,
        logoutput=True,
        user=params.kafka_user)
    except:
        raise Fail("Missing Kafka access; topic={0}, user={1}".format(topic, user))

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


def check_kafka_acl_groups(params, groups):
  cmd = """{0}/kafka-acls.sh \
    --authorizer kafka.security.auth.SimpleAclAuthorizer \
    --authorizer-properties zookeeper.connect={1} \
    --group {2} \
    --list | grep 'User:{3}'"""

  user = params.metron_user
  for group in groups:
    Logger.info("Checking group ACL for topic '{0}'".format(group))
    try:
      Execute(
        cmd.format(params.kafka_bin_dir, params.zookeeper_quorum, group, user),
        tries=3,
        try_sleep=5,
        logoutput=True,
        user=params.kafka_user)
    except:
      raise Fail("Missing Kafka group access; group={0}, user={1}".format(group, user))
