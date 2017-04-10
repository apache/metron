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


# Wrap major operations and functionality in this class
class ParserCommands:
    __params = None
    __parser_list = None
    __configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__parser_list = self.__get_parsers(params)
        self.__configured = os.path.isfile(self.__params.parsers_configured_flag_file)

    # get list of parsers
    def __get_parsers(self, params):
        return params.parsers.replace(' ', '').split(',')

    def is_configured(self):
        return self.__configured

    def set_configured(self):
        File(self.__params.parsers_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def init_parsers(self):
        Logger.info(
            "Copying grok patterns from local directory '{0}' to HDFS '{1}'".format(self.__params.local_grok_patterns_dir,
                                                                                    self.__params.hdfs_grok_patterns_dir))
        self.__params.HdfsResource(self.__params.hdfs_grok_patterns_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   mode=0775,
                                   source=self.__params.local_grok_patterns_dir)

        Logger.info("Done initializing parser configuration")

    def get_parser_list(self):
        return self.__parser_list

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics')
        command_template = """{0}/kafka-topics.sh \
                                --zookeeper {1} \
                                --create \
                                --topic {2} \
                                --partitions {3} \
                                --replication-factor {4} \
                                --config retention.bytes={5}"""
        num_partitions = 1
        replication_factor = 1
        retention_gigabytes = int(self.__params.metron_topic_retention)
        retention_bytes = retention_gigabytes * 1024 * 1024 * 1024
        Logger.info("Creating main topics for parsers")
        for parser_name in self.get_parser_list():
            Logger.info("Creating topic'{0}'".format(parser_name))
            Execute(command_template.format(self.__params.kafka_bin_dir,
                                            self.__params.zookeeper_quorum,
                                            parser_name,
                                            num_partitions,
                                            replication_factor,
                                            retention_bytes))
        Logger.info("Done creating Kafka topics")

    def start_parser_topologies(self):
        Logger.info("Starting Metron parser topologies: {0}".format(self.get_parser_list()))
        start_cmd_template = """{0}/bin/start_parser_topology.sh \
                                    -k {1} \
                                    -z {2} \
                                    -s {3}"""
        for parser in self.get_parser_list():
            Logger.info('Starting ' + parser)
            Execute(start_cmd_template.format(self.__params.metron_home, self.__params.kafka_brokers,
                                              self.__params.zookeeper_quorum, parser))

        Logger.info('Finished starting parser topologies')

    def stop_parser_topologies(self):
        Logger.info('Stopping parsers')
        for parser in self.get_parser_list():
            Logger.info('Stopping ' + parser)
            stop_cmd = 'storm kill ' + parser
            Execute(stop_cmd)
        Logger.info('Done stopping parser topologies')

    def restart_parser_topologies(self, env):
        Logger.info('Restarting the parser topologies')
        self.stop_parser_topologies()
        attempt_count = 0
        while self.topologies_running(env):
            if attempt_count > 2:
                raise Exception("Unable to kill topologies")
            attempt_count += 1
            time.sleep(10)
        self.start_parser_topologies()
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

    def topologies_running(self, env):
        env.set_params(self.__params)
        all_running = True
        topologies = metron_service.get_running_topologies()
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
