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
import subprocess
import time

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File


# Wrap major operations and functionality in this class
class EnrichmentCommands:
    __params = None
    __enrichment = None
    __configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__enrichment = params.metron_enrichment_topology
        self.__configured = os.path.isfile(self.__params.enrichment_configured_flag_file)

    def is_configured(self):
        return self.__configured

    def set_configured(self):
        File(self.__params.enrichment_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

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
            raise ValueError("Unsupported repo type '{0}'".format(repo_type))

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
        Logger.info("Creating topics for enrichment")

        Logger.info("Creating topic'{0}'".format(self.__enrichment))
        Execute(command_template.format(self.__params.kafka_bin_dir,
                                        self.__params.zookeeper_quorum,
                                        self.__enrichment,
                                        num_partitions,
                                        replication_factor,
                                        retention_bytes))
        Logger.info("Done creating Kafka topics")

    def start_enrichment_topology(self):
        Logger.info("Starting Metron enrichment topology: {0}".format(self.__enrichment))
        start_cmd_template = """{0}/bin/start_enrichment_topology.sh \
                                    -s {1} \
                                    -z {2}"""
        Logger.info('Starting ' + self.__enrichment)
        Execute(start_cmd_template.format(self.__params.metron_home, self.__enrichment, self.__params.zookeeper_quorum))

        Logger.info('Finished starting enrichment topology')

    def stop_enrichment_topology(self):
        Logger.info('Stopping ' + self.__enrichment)
        stop_cmd = 'storm kill ' + self.__enrichment
        Execute(stop_cmd)
        Logger.info('Done stopping enrichment topologies')

    def restart_enrichment_topology(self, env):
        Logger.info('Restarting the enrichment topologies')
        self.stop_enrichment_topology()

        # Wait for old topology to be cleaned up by Storm, before starting again.
        retries = 0
        topology_active = self.is_topology_active(env)
        while topology_active and retries < 3:
            Logger.info('Existing topology still active. Will wait and retry')
            time.sleep(40)
            topology_active = self.is_topology_active(env)
            retries += 1

        if not topology_active:
            self.start_enrichment_topology()
            Logger.info('Done restarting the enrichment topology')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_topology_active(self, env):
        env.set_params(self.__params)
        cmd_retrieve = "storm list | grep 'enrichment'"
        proc = subprocess.Popen(cmd_retrieve, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (stdout, stderr) = proc.communicate()
        Logger.info("Retrieval response is: %s" % stdout)
        Logger.warning("Error response is: %s" % stderr)

        fields = stdout.split()
        if len(fields) < 2:
            Logger.warning("Enrichment topology is not running")
            return False

        # Get the second column, which is status. We already know first column is enrichment)
        status = stdout.split()[1]
        running_status_set = ['ACTIVE', 'REBALANCING']
        return status in running_status_set

    def create_hbase_tables(self):
        add_enrichment_cmd = format("echo \"create 'enrichment','t'\" | hbase shell -n")
        Execute(add_enrichment_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
                )

        add_threatintel_cmd = format("echo \"create 'threatintel','t'\" | hbase shell -n")
        Execute(add_threatintel_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
                )
