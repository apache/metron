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

from datetime import datetime
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
    __hbase_configured = False
    __hbase_acl_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__indexing_topology = params.metron_indexing_topology
        self.__indexing_topic = params.indexing_input_topic
        self.__configured = os.path.isfile(self.__params.indexing_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.indexing_acl_configured_flag_file)
        self.__hbase_configured = os.path.isfile(self.__params.indexing_hbase_configured_flag_file)
        self.__hbase_acl_configured = os.path.isfile(self.__params.indexing_hbase_acl_configured_flag_file)
        self.__hdfs_perm_configured = os.path.isfile(self.__params.indexing_hdfs_perm_configured_flag_file)

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def is_hdfs_perm_configured(self):
        return self.__hdfs_perm_configured

    def is_hbase_configured(self):
        return self.__hbase_configured

    def is_hbase_acl_configured(self):
        return self.__hbase_acl_configured

    def set_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_configured_flag_file, "Setting Indexing configured to True")

    def set_hbase_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hbase_configured_flag_file, "Setting HBase configured to True for indexing")

    def set_hbase_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hbase_acl_configured_flag_file, "Setting HBase ACL configured to True for indexing")

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_acl_configured_flag_file, "Setting Indexing ACL configured to True")

    def set_hdfs_perm_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hdfs_perm_configured_flag_file, "Setting HDFS perm configured to True")

    def create_hbase_tables(self):
        Logger.info("Creating HBase Tables for indexing")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"create '{0}','{1}'\" | hbase shell -n"
        add_update_cmd = cmd.format(self.__params.update_hbase_table, self.__params.update_hbase_cf)
        Execute(add_update_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done creating HBase Tables for indexing")
        self.set_hbase_configured()

    def set_hbase_acls(self):
        Logger.info("Setting HBase ACLs for indexing")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"grant '{0}', 'RW', '{1}'\" | hbase shell -n"
        add_update_acl_cmd = cmd.format(self.__params.metron_user, self.__params.update_hbase_table)
        Execute(add_update_acl_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done setting HBase ACLs for indexing")
        self.set_hbase_acl_configured()

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for indexing')
        metron_service.init_kafka_topics(self.__params, [self.__indexing_topic])

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for indexing')
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

    def start_indexing_topology(self, env):
        Logger.info('Starting ' + self.__indexing_topology)

        if not self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)

            start_cmd_template = """{0}/bin/start_elasticsearch_topology.sh \
                                        -s {1} \
                                        -z {2}"""
            start_cmd = start_cmd_template.format(self.__params.metron_home,
                                                  self.__indexing_topology,
                                                  self.__params.zookeeper_quorum)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info('Indexing topology already running')

        Logger.info('Finished starting indexing topology')

    def stop_indexing_topology(self, env):
        Logger.info('Stopping ' + self.__indexing_topology)

        if self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            stop_cmd = 'storm kill ' + self.__indexing_topology
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info("Indexing topology already stopped")

        Logger.info('Done stopping indexing topologies')

    def restart_indexing_topology(self, env):
        Logger.info('Restarting the indexing topologies')
        self.stop_indexing_topology(env)

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
            self.start_indexing_topology(env)
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
