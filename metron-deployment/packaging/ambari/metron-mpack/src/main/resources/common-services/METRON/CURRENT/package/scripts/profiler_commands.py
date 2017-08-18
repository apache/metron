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
class ProfilerCommands:
    __params = None
    __profiler_topic = None
    __profiler_topology = None
    __configured = False
    __acl_configured = False
    __hbase_configured = False
    __hbase_acl_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__profiler_topology = params.metron_profiler_topology
        self.__profiler_topic = params.profiler_input_topic
        self.__configured = os.path.isfile(self.__params.profiler_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.profiler_acl_configured_flag_file)
        self.__hbase_configured = os.path.isfile(self.__params.profiler_hbase_configured_flag_file)
        self.__hbase_acl_configured = os.path.isfile(self.__params.profiler_hbase_acl_configured_flag_file)

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def set_configured(self):
        File(self.__params.profiler_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def is_hbase_configured(self):
        return self.__hbase_configured

    def is_hbase_acl_configured(self):
        return self.__hbase_acl_configured

    def set_hbase_configured(self):
        Logger.info("Setting HBase Configured to True")
        File(self.__params.profiler_hbase_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def set_hbase_acl_configured(self):
        Logger.info("Setting HBase ACL Configured to True")
        File(self.__params.profiler_hbase_acl_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def create_hbase_tables(self):
        Logger.info("Creating HBase Tables")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"create '{0}','{1}'\" | hbase shell -n"
        add_table_cmd = cmd.format(self.__params.profiler_hbase_table, self.__params.profiler_hbase_cf)
        Execute(add_table_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done creating HBase Tables")
        self.set_hbase_configured()

    def set_hbase_acls(self):
        Logger.info("Setting HBase ACLs")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"grant '{0}', 'RW', '{1}'\" | hbase shell -n"
        add_table_acl_cmd = cmd.format(self.__params.metron_user, self.__params.profiler_hbase_table)
        Execute(add_table_acl_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done setting HBase ACLs")
        self.set_hbase_acl_configured()

    def set_acl_configured(self):
        File(self.__params.profiler_acl_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def start_profiler_topology(self, env):
        Logger.info('Starting ' + self.__profiler_topology)

        if not self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            start_cmd_template = """{0}/bin/start_profiler_topology.sh \
                                    -s {1} \
                                    -z {2}"""
            Execute(start_cmd_template.format(self.__params.metron_home,
                                              self.__profiler_topology,
                                              self.__params.zookeeper_quorum),
                    user=self.__params.metron_user)

        else:
            Logger.info('Profiler topology already running')

        Logger.info('Finished starting profiler topology')

    def stop_profiler_topology(self, env):
        Logger.info('Stopping ' + self.__profiler_topology)

        if self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            stop_cmd = 'storm kill ' + self.__profiler_topology
            Execute(stop_cmd, user=self.__params.metron_user)

        else:
            Logger.info("Profiler topology already stopped")

        Logger.info('Done stopping profiler topologies')

    def restart_profiler_topology(self, env):
        Logger.info('Restarting the profiler topologies')
        self.stop_profiler_topology(env)

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
            self.start_profiler_topology(env)
            Logger.info('Done restarting the profiler topologies')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_topology_active(self, env):
        env.set_params(self.__params)
        active = True
        topologies = metron_service.get_running_topologies(self.__params)
        is_running = False
        if self.__profiler_topology in topologies:
            is_running = topologies[self.__profiler_topology] in ['ACTIVE', 'REBALANCING']
        active &= is_running
        return active
