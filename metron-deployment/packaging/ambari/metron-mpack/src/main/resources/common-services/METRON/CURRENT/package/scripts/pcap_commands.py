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
import re
import subprocess
import time

from datetime import datetime
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File

import metron_service
import metron_security


# Wrap major operations and functionality in this class
class PcapCommands:
    __params = None
    __configured = False
    __acl_configured = False
    __pcap_topology = None
    __pcap_perm_configured = False
    __pcap_topic = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__configured = os.path.isfile(self.__params.pcap_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.pcap_acl_configured_flag_file)
        self.__pcap_perm_configured = os.path.isfile(self.__params.pcap_perm_configured_flag_file)
        self.__pcap_topology = params.metron_pcap_topology
        self.__pcap_topic = params.pcap_input_topic

    def __get_topics(self):
        return [self.__pcap_topic]

    def __get_kafka_acl_groups(self):
        return ['pcap']

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def is_hdfs_perm_configured(self):
        return self.__pcap_perm_configured

    def set_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.pcap_configured_flag_file, "Setting PCAP configured to True")

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.pcap_acl_configured_flag_file, "Setting PCAP ACL configured to true")

    def set_hdfs_perm_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.pcap_perm_configured_flag_file, "Setting PCAP HDFS perm configured to True")

    def init_pcap(self):
        self.init_kafka_topics()
        self.init_hdfs_dir()
        Logger.info("Done initializing PCAP configuration")

    def init_hdfs_dir(self):
        Logger.info("Creating HDFS locations for PCAP")
        # Non Kerberized Metron runs under 'storm', requiring write under the 'hadoop' group.
        # Kerberized Metron runs under it's own user.
        ownership = 0755 if self.__params.security_enabled else 0775
        self.__params.HdfsResource(self.__params.pcap_base_path,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=ownership,
                                   )
        self.__params.HdfsResource(self.__params.pcap_base_interim_result_path,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=ownership,
                                   )
        self.__params.HdfsResource(self.__params.pcap_final_output_path,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=ownership,
                                   )

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topic for PCAP')
        metron_service.init_kafka_topics(self.__params, self.__get_topics())

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for PCAP')
        metron_service.init_kafka_acls(self.__params, self.__get_topics())
        metron_service.init_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

    def is_topology_active(self, env):
        env.set_params(self.__params)
        active = True
        topologies = metron_service.get_running_topologies(self.__params)
        is_running = False
        if self.__pcap_topology in topologies:
            is_running = topologies[self.__pcap_topology] in ['ACTIVE', 'REBALANCING']
        active &= is_running
        return active

    def start_pcap_topology(self, env):
        Logger.info('Starting Metron PCAP topology')
        start_cmd_template = """{0}/bin/start_pcap_topology.sh"""
        if not self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            start_cmd = start_cmd_template.format(self.__params.metron_home)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)
        else :
            Logger.info('PCAP topology already started')

        Logger.info('Finished starting pcap topologies')

    def stop_pcap_topology(self, env):
        Logger.info('Stopping Metron PCAP topology')
        if self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            stop_cmd = 'storm kill ' + self.__pcap_topology
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else :
            Logger.info('PCAP topology already stopped')

        Logger.info('Finished starting PCAP topologies')

    def restart_pcap_topology(self, env):
        Logger.info('Restarting the PCAP topology')
        self.stop_pcap_topology(env)

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
            self.start_pcap_topology(env)
            Logger.info('Done restarting the PCAP topologies')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def service_check(self, env):
        """
        Performs a service check for the PCAP.
        :param env: Environment
        """
        Logger.info('Checking Kafka topic for PCAP')
        metron_service.check_kafka_topics(self.__params, self.__get_topics())

        Logger.info("Checking for PCAP sequence files directory in HDFS for PCAP")
        metron_service.check_hdfs_dir_exists(self.__params, self.__params.hdfs_pcap_sequencefiles_dir)

        if self.__params.security_enabled:
            Logger.info('Checking Kafka ACLs for PCAP')
            metron_service.check_kafka_acls(self.__params, self.__get_topics())
            metron_service.check_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

        Logger.info("Checking for PCAP topologies")
        if not self.is_topology_active(env):
            raise Fail("PCAP topologies not running")

        Logger.info("PCAP service check completed successfully")
