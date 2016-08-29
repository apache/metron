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

import subprocess
import time

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions import format

# Wrap major operations and functionality in this class
class IndexingCommands:
    __params = None
    __indexing = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__indexing = params.metron_indexing_topology

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
        command_template = """{}/kafka-topics.sh \
                                --zookeeper {} \
                                --create \
                                --topic {} \
                                --partitions {} \
                                --replication-factor {} \
                                --config retention.bytes={}"""
        num_partitions = 1
        replication_factor = 1
        retention_gigabytes = int(self.__params.metron_topic_retention)
        retention_bytes = retention_gigabytes * 1024 * 1024 * 1024
        Logger.info("Creating topics for indexing")

        Logger.info("Creating topic'{}'".format(self.__indexing))
        Execute(command_template.format(self.__params.kafka_bin_dir,
                                        self.__params.zookeeper_quorum,
                                        self.__indexing,
                                        num_partitions,
                                        replication_factor,
                                        retention_bytes))
        Logger.info("Done creating Kafka topics")

    def start_indexing_topology(self):
        Logger.info("Starting Metron indexing topology: {}".format(self.__indexing))
        start_cmd_template = """{}/bin/start_elasticsearch_topology.sh \
                                    -s {} \
                                    -z {}"""
        Logger.info('Starting ' + self.__indexing)
        Execute(start_cmd_template.format(self.__params.metron_home, self.__indexing, self.__params.zookeeper_quorum))

        Logger.info('Finished starting indexing topology')

    def stop_indexing_topology(self):
        Logger.info('Stopping ' + self.__indexing)
        stop_cmd = 'storm kill ' + self.__indexing
        Execute(stop_cmd)
        Logger.info('Done stopping indexing topologies')

    def restart_indexing_topology(self):
        Logger.info('Restarting the indexing topologies')
        self.stop_indexing_topology()

        # Wait for old topology to be cleaned up by Storm, before starting again.
        retries = 0
        topology_active = self.is_topology_active()
        while topology_active and retries < 3:
            Logger.info('Existing topology still active. Will wait and retry')
            time.sleep(40)
            topology_active = self.is_topology_active()
            retries += 1

        if not topology_active:
            self.start_indexing_topology()
            Logger.info('Done restarting the indexing topologies')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_configured(self):
        return self.__configured

    def set_configured(self):
        File(self.__params.configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def init_config(self):
        Logger.info('Loading config into ZooKeeper')
        Execute(format(
            "{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_quorum}"),
            path=format("{java_home}/bin")
        )

    def is_topology_active(self):
        # cmd_retrieve = "storm list | grep 'indexing'"
        # proc = subprocess.Popen(cmd_retrieve, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        # (stdout, stderr) = proc.communicate()
        # Logger.info("Retrieval response is: %s" % stdout)
        # Logger.warning("Error response is: %s" % stderr)
        #
        # fields = stdout.split()
        # if len(fields) < 2:
        #     Logger.warning("Indexing topology is not running")
        #     return False
        #
        # # Get the second column, which is status. We already know first column is indexing)
        # status = stdout.split()[1]
        # running_status_set = {'ACTIVE', 'REBALANCING'}
        # return status in running_status_set
        #
        return True
