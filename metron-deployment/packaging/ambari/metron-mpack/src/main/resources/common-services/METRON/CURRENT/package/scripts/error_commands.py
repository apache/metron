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


# Wrap major operations and functionality in this class
class ErrorCommands:
    __params = None
    __error = None
    __configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__error = params.metron_error_topology
        self.__configured = os.path.isfile(self.__params.error_configured_flag_file)

    def is_configured(self):
        return self.__configured

    def set_configured(self):
        File(self.__params.error_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def setup_repo(self):
        def local_repo():
            Logger.info("Setting up local repo")
            Execute("yum -y install createrepo")
            Execute("createrepo /localrepo")
            Execute("chmod -R o-w+r /localrepo")
            Execute("echo \"[METRON-${metron.version}]\n"
                    "name=Metron ${metron.version} packages\n"
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
        Logger.info("Creating topics for error")

        Logger.info("Creating topic'{0}'".format(self.__error))
        Execute(command_template.format(self.__params.kafka_bin_dir,
                                        self.__params.zookeeper_quorum,
                                        self.__error,
                                        num_partitions,
                                        replication_factor,
                                        retention_bytes))
        Logger.info("Done creating Kafka topics")

    def init_hdfs_dir(self):
        Logger.info('Creating HDFS error directory')
        self.__params.HdfsResource(self.__params.metron_apps_error_hdfs_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.user_group,
                                   mode=0775,
                                   )
        Logger.info('Done creating HDFS error directory')


    def start_error_topology(self):
        Logger.info("Starting Metron error topology: {0}".format(self.__error))
        start_cmd_template = """{0}/bin/start_elasticsearch_error_topology.sh \
                                    -s {1} \
                                    -z {2}"""
        Logger.info('Starting ' + self.__error)
        Execute(start_cmd_template.format(self.__params.metron_home, self.__error, self.__params.zookeeper_quorum))

        Logger.info('Finished starting error topology')

    def stop_error_topology(self):
        Logger.info('Stopping ' + self.__error)
        stop_cmd = 'storm kill ' + self.__error
        Execute(stop_cmd)
        Logger.info('Done stopping error topology')

    def restart_error_topology(self, env):
        Logger.info('Restarting the error topology')
        self.stop_error_topology()

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
            self.start_error_topology()
            Logger.info('Done restarting the error topology')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_topology_active(self, env):
        env.set_params(self.__params)
        active = True
        topologies = metron_service.get_running_topologies()
        is_running = False
        if self.__error in topologies:
            is_running = topologies[self.__error] in ['ACTIVE', 'REBALANCING']
        active &= is_running
        return active
