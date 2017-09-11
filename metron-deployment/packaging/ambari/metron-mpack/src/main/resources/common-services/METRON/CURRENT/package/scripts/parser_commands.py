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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File

import metron_service
import metron_security


# Wrap major operations and functionality in this class
class ParserCommands:
    __params = None
    __parser_list = None
    __configured = False
    __acl_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__parser_list = self.__get_parsers(params)
        self.__configured = os.path.isfile(self.__params.parsers_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.parsers_acl_configured_flag_file)

    # get list of parsers
    def __get_parsers(self, params):
        return params.parsers.replace(' ', '').split(',')

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def set_configured(self):
        File(self.__params.parsers_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def set_acl_configured(self):
        File(self.__params.parsers_acl_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def init_parsers(self):
        Logger.info(
            "Copying grok patterns from local directory '{0}' to HDFS '{1}'".format(self.__params.local_grok_patterns_dir,
                                                                                    self.__params.hdfs_grok_patterns_dir))

        self.__params.HdfsResource(self.__params.hdfs_grok_patterns_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   mode=0755,
                                   source=self.__params.local_grok_patterns_dir,
                                   recursive_chown = True)

        Logger.info("Done initializing parser configuration")

    def get_parser_list(self):
        return self.__parser_list

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for parsers')
        # All errors go to indexing topics, so create it here if it's not already
        # Getting topics this way is a bit awkward, but I don't want to append to actual list, so copy it
        topics = list(self.get_parser_list())
        topics.append(self.__params.enrichment_error_topic)
        metron_service.init_kafka_topics(self.__params, topics)

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for parsers')

        # Getting topics this way is a bit awkward, but I don't want to modify the actual list, so copy it
        topics = list(self.get_parser_list())
        topics.append(self.__params.enrichment_error_topic)
        # Parser group is the parser name + '_parser'
        metron_service.init_kafka_acls(self.__params,
                                       topics,
                                       [parser + '_parser' for parser in self.get_parser_list()])

    def start_parser_topologies(self, env):
        Logger.info("Starting Metron parser topologies: {0}".format(self.get_parser_list()))
        start_cmd_template = """{0}/bin/start_parser_topology.sh \
                                    -k {1} \
                                    -z {2} \
                                    -s {3} \
                                    -ksp {4}"""
        if self.__params.security_enabled:
            # Append the extra configs needed for secured cluster.
            start_cmd_template = start_cmd_template + ' -e ~' + self.__params.metron_user + '/.storm/storm.config'
            metron_security.kinit(self.__params.kinit_path_local,
                                  self.__params.metron_keytab_path,
                                  self.__params.metron_principal_name,
                                  execute_user=self.__params.metron_user)

        stopped_parsers = set(self.get_parser_list()) - self.get_running_topology_names(env)
        Logger.info('Parsers that need started: ' + str(stopped_parsers))

        for parser in stopped_parsers:
            Logger.info('Starting ' + parser)
            Execute(start_cmd_template.format(self.__params.metron_home,
                                              self.__params.kafka_brokers,
                                              self.__params.zookeeper_quorum,
                                              parser,
                                              self.__params.kafka_security_protocol),
                    user=self.__params.metron_user)

        Logger.info('Finished starting parser topologies')

    def stop_parser_topologies(self, env):
        Logger.info('Stopping parsers')

        running_parsers = set(self.get_parser_list()) & self.get_running_topology_names(env)
        Logger.info('Parsers that need stopped: ' + str(running_parsers))

        for parser in running_parsers:
            Logger.info('Stopping ' + parser)
            stop_cmd = 'storm kill ' + parser
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            Execute(stop_cmd, user=self.__params.metron_user)
        Logger.info('Done stopping parser topologies')

    def restart_parser_topologies(self, env):
        Logger.info('Restarting the parser topologies')
        self.stop_parser_topologies(env)

        attempt_count = 0
        while self.topologies_running(env):
            if attempt_count > 2:
                raise Exception("Unable to kill topologies")
            attempt_count += 1
            time.sleep(10)
        self.start_parser_topologies(env)

        Logger.info('Done restarting the parser topologies')

    def topologies_exist(self):
        cmd_open = subprocess.Popen(["storm", "list"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = cmd_open.communicate()
        stdout_lines = stdout.splitlines()
        if stdout_lines:
            status_lines = self.__get_status_lines(stdout_lines)
            for parser in self.get_parser_list():
                for line in status_lines:
                    items = re.sub('[\s]+', ' ', line).split()
                    if items and items[0] == parser:
                        return True
        return False

    def get_running_topology_names(self, env):
        """
        Returns the names of all 'running' topologies.  A running topology
        is one that is either active or rebalancing.
        :param env: Environment
        :return: Set containing the names of all running topologies.
        """
        env.set_params(self.__params)
        topology_status = metron_service.get_running_topologies(self.__params)
        topology_names = ([name for name in topology_status if topology_status[name] in ['ACTIVE', 'REBALANCING']])
        return set(topology_names)

    def topologies_running(self, env):
        env.set_params(self.__params)
        all_running = True
        topologies = metron_service.get_running_topologies(self.__params)
        for parser in self.get_parser_list():
            parser_found = False
            is_running = False
            if parser in topologies:
                parser_found = True
                is_running = topologies[parser] in ['ACTIVE', 'REBALANCING']
            all_running &= parser_found and is_running
        return all_running

    def __get_status_lines(self, lines):
        status_lines = []
        do_stat = False
        skipped = 0
        for line in lines:
            if line.startswith("Topology_name"):
                do_stat = True
            if do_stat and skipped == 2:
                status_lines += [line]
            elif do_stat:
                skipped += 1
        return status_lines

    def __is_running(self, status):
        return status in ['ACTIVE', 'REBALANCING']
