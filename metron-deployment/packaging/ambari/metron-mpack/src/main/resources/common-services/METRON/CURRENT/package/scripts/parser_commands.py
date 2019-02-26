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
import shlex
import subprocess
import time

from datetime import datetime
from resource_management.core.exceptions import Fail
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
        """
        Combines the list of parser topics and sends a unique list to be used for
        Kafka topic creation and the like.
        :param params:
        :return: List containing the names of unique parsers
        """
        parserBatches = list(self.__get_aggr_parsers(params))
        parsers = ','.join(s.translate(None, '"') for s in parserBatches)
        # Get only the unique list of parser names
        parsers = list(set(parsers.split(',')))
        return parsers

    def __get_aggr_parsers(self, params):
        """
        Fetches the list of aggregated (and regular) parsers and returns a list.
        If the input list of parsers were "bro,yaf,snort", "bro,snort" and yaf, for example,
        then this method will return ["bro,snort,yaf", "bro,snort", "yaf"].  Sensors within
        a group are sorted alphabetically.
        :param params:
        :return: List containing the names of parsers
        """
        parserList = []
        parsers = shlex.shlex(params.parsers)
        for name in parsers:
            sensors = name.strip('",').split(",")
            # if name contains multiple sensors, sort them alphabetically
            if len(sensors) > 1:
                sensors.sort()
                name = '"' + ",".join(sensors) + '"'
            parserList.append(name.strip(','))
        return [s.translate(None, "'[]") for s in filter(None, parserList)]

    def get_parser_aggr_topology_names(self, params):
        """
        Returns the names of regular and aggregated topologies as they would run in storm
        An aggregated topology has the naming convention of 'parserA__parserB'.
        For example, a list of parsers like ["bro,snort", yaf] will be returned as ["bro__snort", "yaf"]
        :param params:
        :return: List containing the names of parser topologies
        """
        topologyName = []
        for parser in self.__get_aggr_parsers(params):
            parser = parser.replace(",", "__").strip('"')
            topologyName.append(parser)
        return topologyName

    def __get_topics(self):
        # All errors go to indexing topics, so create it here if it's not already
        # Getting topics this way is a bit awkward, but I don't want to append to actual list, so copy it
        topics = list(self.get_parser_list())
        topics.append(self.__params.enrichment_error_topic)
        return topics

    def __get_kafka_acl_groups(self):
        # Parser group is the parser name + '_parser'
        return [parser + '_parser' for parser in self.get_parser_list()]

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def set_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.parsers_configured_flag_file, "Setting Parsers configured to true")

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.parsers_acl_configured_flag_file, "Setting Parsers ACL configured to true")

    def init_parsers(self):
        self.init_grok_patterns()
        Logger.info("Done initializing parser configuration")

    def init_grok_patterns(self):
        Logger.info("Copying grok patterns from local directory '{0}' to HDFS '{1}'"
            .format(self.__params.local_grok_patterns_dir,
                    self.__params.hdfs_grok_patterns_dir))

        self.__params.HdfsResource(self.__params.hdfs_grok_patterns_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   mode=0755,
                                   source=self.__params.local_grok_patterns_dir,
                                   recursive_chown = True)

    def get_parser_list(self):
        return self.__parser_list

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for Parsers')
        metron_service.init_kafka_topics(self.__params, self.__get_topics())

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for Parsers')
        metron_service.init_kafka_acls(self.__params, self.__get_topics())
        metron_service.init_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

    def start_parser_topologies(self, env):
        Logger.info("Starting Metron parser topologies: {0}".format(self.__get_aggr_parsers(self.__params)))
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

        stopped_parsers = set(self.__get_aggr_parsers(self.__params)) - self.get_running_topology_names(env)
        Logger.info('Parsers that need started: ' + str(stopped_parsers))

        for parser in stopped_parsers:
            Logger.info('Starting ' + parser)
            start_cmd = start_cmd_template.format(self.__params.metron_home,
                                                  self.__params.kafka_brokers,
                                                  self.__params.zookeeper_quorum,
                                                  parser,
                                                  self.__params.kafka_security_protocol)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        Logger.info('Finished starting parser topologies')

    def stop_parser_topologies(self, env):
        Logger.info('Stopping parsers')

        running_parsers = set(self.get_parser_aggr_topology_names(self.__params)) & self.get_running_topology_names(env)
        Logger.info('Parsers that need stopped: ' + str(running_parsers))

        for parser in running_parsers:
            Logger.info('Stopping ' + parser)
            stop_cmd = 'storm kill ' + parser
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)
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
        for parser in self.get_parser_aggr_topology_names(self.__params):
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

    def service_check(self, env):
        """
        Performs a service check for the Parsers.
        :param env: Environment
        """
        Logger.info("Checking for grok patterns in HDFS for Parsers")
        metron_service.check_hdfs_dir_exists(self.__params, self.__params.hdfs_grok_patterns_dir)

        Logger.info('Checking Kafka topics for Parsers')
        metron_service.check_kafka_topics(self.__params, self.__get_topics())

        if self.__params.security_enabled:
            Logger.info('Checking Kafka ACLs for Parsers')
            metron_service.check_kafka_acls(self.__params, self.__get_topics())
            metron_service.check_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

        Logger.info("Checking for Parser topologies")
        if not self.topologies_running(env):
            raise Fail("Parser topologies not running")

        Logger.info("Parser service check completed successfully")
