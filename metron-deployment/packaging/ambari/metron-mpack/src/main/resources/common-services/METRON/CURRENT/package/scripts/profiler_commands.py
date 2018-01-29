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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute

import metron_security
import metron_service


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

    def __get_topics(self):
        return [self.__profiler_topic]

    def __get_kafka_acl_groups(self):
        return ['profiler']

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def is_hbase_configured(self):
        return self.__hbase_configured

    def is_hbase_acl_configured(self):
        return self.__hbase_acl_configured

    def set_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.profiler_configured_flag_file, "Setting Profiler configured flag to true")

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.profiler_acl_configured_flag_file, "Setting Profiler acl configured flag to true")

    def set_hbase_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.profiler_hbase_configured_flag_file, "Setting HBase configured to True for profiler")

    def set_hbase_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.profiler_hbase_acl_configured_flag_file, "Setting HBase ACL configured to True for profiler")

    def create_hbase_tables(self):
        Logger.info("Creating HBase table '{0}' for profiler".format(
            self.__params.profiler_hbase_table))
        metron_service.create_hbase_table(self.__params,
                                          self.__params.profiler_hbase_table,
                                          self.__params.profiler_hbase_cf)
        self.set_hbase_configured()
        Logger.info("Done creating HBase Tables for profiler")

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACls for profiler')
        metron_service.init_kafka_acls(self.__params, self.__get_topics())
        metron_service.init_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

    def set_hbase_acls(self):
        Logger.info("Setting HBase ACLs for profiler")
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

        self.set_hbase_acl_configured()
        Logger.info("Done setting HBase ACLs for profiler")

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
            start_cmd = start_cmd_template.format(self.__params.metron_home,
                                                  self.__profiler_topology,
                                                  self.__params.zookeeper_quorum)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)
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
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

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

    def service_check(self, env):
        """
        Performs a service check for the Profiler.
        :param env: Environment
        """
        Logger.info('Checking Kafka topics for Profiler')
        metron_service.check_kafka_topics(self.__params, [self.__params.profiler_input_topic])

        Logger.info("Checking HBase table for profiler")
        metron_service.check_hbase_table(self.__params, self.__params.profiler_hbase_table)
        metron_service.check_hbase_column_family(self.__params, self.__params.profiler_hbase_table, self.__params.profiler_hbase_cf)

        if self.__params.security_enabled:

            Logger.info('Checking Kafka ACLs for Profiler')
            metron_service.check_kafka_acls(self.__params, self.__get_topics())
            metron_service.check_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

            Logger.info('Checking Kafka ACLs for Profiler')
            metron_service.check_hbase_acls(self.__params, self.__params.profiler_hbase_table)

        Logger.info("Checking for Profiler topology")
        if not self.is_topology_active(env):
            raise Fail("Profiler topology not running")

        Logger.info("Profiler service check completed successfully")
