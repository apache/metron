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
class EnrichmentCommands:
    __params = None
    __enrichment_topology = None
    __enrichment_topic = None
    __enrichment_error_topic = None
    __threat_intel_error_topic = None
    __kafka_configured = False
    __hbase_configured = False
    __geo_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__enrichment_topology = params.metron_enrichment_topology
        self.__enrichment_topic = params.metron_enrichment_topic
        self.__kafka_configured = os.path.isfile(self.__params.enrichment_kafka_configured_flag_file)
        self.__hbase_configured = os.path.isfile(self.__params.enrichment_hbase_configured_flag_file)
        self.__geo_configured = os.path.isfile(self.__params.enrichment_geo_configured_flag_file)

    def is_kafka_configured(self):
        return self.__kafka_configured

    def set_kafka_configured(self):
        Logger.info("Setting Kafka Configured to True")
        File(self.__params.enrichment_kafka_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def is_hbase_configured(self):
        return self.__hbase_configured

    def set_hbase_configured(self):
        Logger.info("Setting HBase Configured to True")
        File(self.__params.enrichment_hbase_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def is_geo_configured(self):
        return self.__geo_configured

    def set_geo_configured(self):
        Logger.info("Setting GEO Configured to True")
        File(self.__params.enrichment_geo_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0775)

    def init_geo(self):
        Logger.info("Creating HDFS location for GeoIP database")
        self.__params.HdfsResource(self.__params.geoip_hdfs_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=0775,
                                   )

        Logger.info("Creating and loading GeoIp database")
        command_template = """{0}/bin/geo_enrichment_load.sh \
                                -g {1} \
                                -r {2} \
                                -z {3}"""
        command = command_template.format(self.__params.metron_home,
                                          self.__params.geoip_url,
                                          self.__params.geoip_hdfs_dir,
                                          self.__params.zookeeper_quorum
                                          )
        Logger.info("Executing command " + command)
        Execute(command, user=self.__params.metron_user, tries=1, logoutput=True)
        Logger.info("Done intializing GeoIP data")
        self.set_geo_configured()

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
        topics = [self.__enrichment_topic]
        for topic in topics:
            Logger.info("Creating topic'{0}'".format(topic))
            Execute(command_template.format(self.__params.kafka_bin_dir,
                                            self.__params.zookeeper_quorum,
                                            topic,
                                            num_partitions,
                                            replication_factor,
                                            retention_bytes))

        Logger.info("Done creating Kafka topics")
        self.set_kafka_configured()

    def start_enrichment_topology(self):
        Logger.info("Starting Metron enrichment topology: {0}".format(self.__enrichment_topology))
        start_cmd_template = """{0}/bin/start_enrichment_topology.sh \
                                    -s {1} \
                                    -z {2}"""
        Logger.info('Starting ' + self.__enrichment_topology)
        Execute(start_cmd_template.format(self.__params.metron_home, self.__enrichment_topology, self.__params.zookeeper_quorum))

        Logger.info('Finished starting enrichment topology')

    def stop_enrichment_topology(self):
        Logger.info('Stopping ' + self.__enrichment_topology)
        stop_cmd = 'storm kill ' + self.__enrichment_topology
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

        active = True
        topologies = metron_service.get_running_topologies()
        is_running = False
        if self.__enrichment_topology in topologies:
            is_running = topologies[self.__enrichment_topology] in ['ACTIVE', 'REBALANCING']
        active &= is_running
        return active

    def create_hbase_tables(self):
        Logger.info("Creating HBase Tables")
        add_enrichment_cmd = "echo \"create '{0}','{1}'\" | hbase shell -n".format(self.__params.enrichment_table, self.__params.enrichment_cf)
        Execute(add_enrichment_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
                )

        add_threatintel_cmd = "echo \"create '{0}','{1}'\" | hbase shell -n".format(self.__params.threatintel_table, self.__params.threatintel_cf)
        Execute(add_threatintel_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
                )
        Logger.info("Done creating HBase Tables")
        self.set_hbase_configured()
