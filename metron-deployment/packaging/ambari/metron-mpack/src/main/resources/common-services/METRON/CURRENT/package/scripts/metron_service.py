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
import subprocess

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, File
from resource_management.core.resources.system import Execute
from resource_management.core.source import Template
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from metron_security import kinit

def init_config():
    Logger.info('Loading config into ZooKeeper')
    Execute(ambari_format(
        "{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_quorum}"),
        path=ambari_format("{java_home}/bin")
    )


def get_running_topologies(params):
    Logger.info('Getting Running Storm Topologies from Storm REST Server')

    Logger.info('Security enabled? ' + str(params.security_enabled))

    # Want to sudo to the metron user and kinit as them so we aren't polluting root with Metron's Kerberos tickets.
    # This is becuase we need to run a command with a return as the metron user. Sigh
    negotiate = '--negotiate -u : ' if params.security_enabled else ''
    cmd = ambari_format('curl --max-time 3 ' + negotiate + '{storm_rest_addr}/api/v1/topology/summary')

    if params.security_enabled:
        kinit(params.kinit_path_local,
              params.metron_keytab_path,
              params.metron_principal_name,
              execute_user=params.metron_user)

    Logger.info('Running cmd: ' + cmd)
    return_code, stdout, sdterr = get_user_call_output(cmd, user=params.metron_user)

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


def load_global_config(params):
    Logger.info('Create Metron Local Config Directory')
    Logger.info("Configure Metron global.json")

    directories = [params.metron_zookeeper_config_path]
    Directory(directories,
              mode=0755,
              owner=params.metron_user,
              group=params.metron_group
              )

    File(ambari_format("{metron_zookeeper_config_path}/global.json"),
         content=Template("global.json.j2"),
         owner=params.metron_user,
         group=params.metron_group
         )

    init_config()


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
                user=params.kafka_user)
    Logger.info("Done creating Kafka topics")


def init_kafka_acls(params, topics, groups):
    Logger.info('Creating Kafka ACLs')

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
                user=params.kafka_user)

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
                user=params.kafka_user)
    Logger.info("Done creating Kafka ACLs")
