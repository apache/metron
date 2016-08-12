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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import format


# Wrap major operations and functionality in this class
class Commands:
    __params = None
    __parser_list = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__parser_list = self.__get_parsers(params)

    # get list of parsers
    def __get_parsers(self, params):
        return params.parsers.replace(' ', '').split(',')

    def init_parsers(self):
        Logger.info(
            "Copying grok patterns from local directory '{}' to HDFS '{}'".format(self.__params.local_grok_patterns_dir,
                                                                                  self.__params.metron_apps_dir))
        self.__params.HdfsResource(self.__params.metron_apps_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   mode=0775,
                                   source=self.__params.local_grok_patterns_dir)
        Logger.info("Done initializing parser configuration")

    def get_parser_list(self):
        return self.__parser_list

    def setup_repo(self):
        def local_repo():
            Logger.info("Setting up local repo")
            Execute("yum -y install createrepo")
            Execute("createrepo /localrepo")
            Execute("chmod -R o-w+r /localrepo")
            Execute("echo \"[METRON-0.2.0BETA]\n"
                    "name=Metron 0.2.0BETA packages\n"
                    "baseurl=file:///localrepo\n"
                    "gpgcheck=0\n"
                    "enabled=1\" > /etc/yum.repos.d/local.repo")

        def remote_repo():
            print('Using remote repo')

        yum_repo_types = {
            'local': local_repo,
            'remote': remote_repo
        }
        repo_type = self.__params.yum_repo_type
        if repo_type in yum_repo_types:
            yum_repo_types[repo_type]()
        else:
            raise ValueError("Unsupported repo type '{}'".format(repo_type))

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics')
        # TODO get HDP home from params
        command_template = format("""{hadoop_home_dir}/kafka-broker/bin/kafka-topics.sh \
                                --zookeeper {zookeeper_quorum} \
                                --create \
                                --topic {} \
                                --partitions {} \
                                --replication-factor {} \
                                --config retention.bytes={}""")
        num_partitions = 1
        replication_factor = 1
        retention_gigabytes = 10
        retention_bytes = retention_gigabytes * 1024 * 1024 * 1024
        Logger.info("Creating main topics for parsers")
        for parser_name in self.get_parser_list():
            Logger.info("Creating topic'{}'".format(parser_name))
            Execute(command_template.format(parser_name, num_partitions, replication_factor, retention_bytes))
        Logger.info("Creating topics for error handling")
        Execute(command_template.format("parser_invalid", num_partitions, replication_factor, retention_bytes))
        Execute(command_template.format("parser_error", num_partitions, replication_factor, retention_bytes))
        Logger.info("Done creating Kafka topics")

    def init_parser_config(self):
        Logger.info('Loading parser config into ZooKeeper')
        Execute(format(
            "{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_quorum}"))

    def start_parser_topologies(self):
        Logger.info("Starting Metron parser topologies: {}".format(self.get_parser_list()))
        start_cmd_template = format("""{metron_home}/bin/start_parser_topology.sh \
                                    -s {} \
                                    -z {zookeeper_quorum}""")
        for parser in self.get_parser_list():
            Logger.info('Starting ' + parser)
            Execute(start_cmd_template.format(parser))

        Logger.info('Finished starting parser topologies')

    def stop_parser_topologies(self):
        Logger.info('Stopping parsers')
        for parser in self.get_parser_list():
            Logger.info('Stopping ' + parser)
            stop_cmd = 'storm kill ' + parser
            Execute(stop_cmd)
        Logger.info('Done stopping parser topologies')

    def restart_parser_topologies(self):
        Logger.info('Restarting the parser topologies')
        self.stop_parser_topologies()
        self.start_parser_topologies()
        Logger.info('Done restarting the parser topologies')
