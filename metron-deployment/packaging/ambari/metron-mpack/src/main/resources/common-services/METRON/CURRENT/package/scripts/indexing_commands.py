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
import time

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File

import metron_service
import metron_security


# Wrap major operations and functionality in this class
class IndexingCommands:
    __params = None
    __indexing_topic = None
    __indexing_topology = None
    __configured = False
    __acl_configured = False
    __hdfs_perm_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__indexing_topology = params.metron_indexing_topology
        self.__indexing_topic = params.indexing_input_topic
        self.__configured = os.path.isfile(self.__params.indexing_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.indexing_acl_configured_flag_file)

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def is_hdfs_perm_configured(self):
        return self.__hdfs_perm_configured

    def set_configured(self):
        File(self.__params.indexing_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def set_acl_configured(self):
        File(self.__params.indexing_acl_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def set_hdfs_perm_configured(self):
        File(self.__params.indexing_hdfs_perm_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for indexing')
        metron_service.init_kafka_topics(self.__params, [self.__indexing_topic])

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs')
        # Indexed topic names matches the group
        metron_service.init_kafka_acls(self.__params, [self.__indexing_topic], [self.__indexing_topic])

    def init_hdfs_dir(self):
        Logger.info('Setting up HDFS indexing directory')

        # Non Kerberized Metron runs under 'storm', requiring write under the 'hadoop' group.
        # Kerberized Metron runs under it's own user.
        ownership = 0755 if self.__params.security_enabled else 0775
        Logger.info('HDFS indexing directory ownership is: ' + str(ownership))
        self.__params.HdfsResource(self.__params.metron_apps_indexed_hdfs_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=ownership,
                                   )
        Logger.info('Done creating HDFS indexing directory')

    def start_indexing_topology(self):
        Logger.info("Starting Metron indexing topology: {0}".format(self.__indexing_topology))
        start_cmd_template = """{0}/bin/start_elasticsearch_topology.sh \
                                    -s {1} \
                                    -z {2}"""
        Logger.info('Starting ' + self.__indexing_topology)
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                                  self.__params.metron_keytab_path,
                                  self.__params.metron_principal_name,
                                  execute_user=self.__params.metron_user)
        Execute(start_cmd_template.format(self.__params.metron_home, self.__indexing_topology, self.__params.zookeeper_quorum),
                user=self.__params.metron_user)

        Logger.info('Finished starting indexing topology')

    def stop_indexing_topology(self):
        Logger.info('Stopping ' + self.__indexing_topology)
        stop_cmd = 'storm kill ' + self.__indexing_topology
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                                  self.__params.metron_keytab_path,
                                  self.__params.metron_principal_name,
                                  execute_user=self.__params.metron_user)
        Execute(stop_cmd,
                user=self.__params.metron_user)
        Logger.info('Done stopping indexing topologies')

    def restart_indexing_topology(self, env):
        Logger.info('Restarting the indexing topologies')
        self.stop_indexing_topology()

        # Wait for old topology to be cleaned up by Storm, before starting again.
        retries = 0
        topology_active = self.is_topology_active(env)
        while self.is_topology_active(env) and retries < 3:
            Logger.info('Existing topology still active. Will wait and retry')
            time.sleep(10)
            retries += 1

        if not topology_active:
            Logger.info('Waiting for storm kill to complete')
            time.sleep(30)
            self.start_indexing_topology()
            Logger.info('Done restarting the indexing topologies')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_topology_active(self, env):
        env.set_params(self.__params)
        active = True
        topologies = metron_service.get_running_topologies(self.__params)
        is_running = False
        if self.__indexing_topology in topologies:
            is_running = topologies[self.__indexing_topology] in ['ACTIVE', 'REBALANCING']
        active &= is_running
        return active
