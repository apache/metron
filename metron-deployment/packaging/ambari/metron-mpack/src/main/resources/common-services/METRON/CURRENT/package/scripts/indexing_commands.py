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

from datetime import datetime
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File

import metron_service
import metron_security


# Wrap major operations and functionality in this class
class IndexingCommands:
    __params = None
    __indexing_topic = None
    __random_access_indexing_topology = None
    __batch_indexing_topology = None
    __configured = False
    __acl_configured = False
    __hdfs_perm_configured = False
    __hbase_configured = False
    __hbase_acl_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__random_access_indexing_topology = params.metron_random_access_indexing_topology
        self.__batch_indexing_topology = params.metron_batch_indexing_topology
        self.__indexing_topic = params.indexing_input_topic
        self.__configured = os.path.isfile(self.__params.indexing_configured_flag_file)
        self.__acl_configured = os.path.isfile(self.__params.indexing_acl_configured_flag_file)
        self.__hbase_configured = os.path.isfile(self.__params.indexing_hbase_configured_flag_file)
        self.__hbase_acl_configured = os.path.isfile(self.__params.indexing_hbase_acl_configured_flag_file)
        self.__elasticsearch_template_installed = os.path.isfile(self.__params.elasticsearch_template_installed_flag_file)
        self.__hdfs_perm_configured = os.path.isfile(self.__params.indexing_hdfs_perm_configured_flag_file)

    def __get_topics(self):
        return [self.__indexing_topic]

    def __get_kafka_acl_groups(self):
        # Indexed topic names matches the group
        return ['indexing-batch', 'indexing-ra']

    def get_templates(self):
        """
        Defines the Elasticsearch index templates.
        :return: Dict where key is the name of an index template and the
          value is a path to file containing the index template definition.
        """
        from params import params
        return {
            "bro_index": params.bro_index_path,
            "yaf_index": params.yaf_index_path,
            "snort_index": params.snort_index_path,
            "error_index": params.error_index_path,
            "metaalert_index": params.meta_index_path
        }

    def is_configured(self):
        return self.__configured

    def is_acl_configured(self):
        return self.__acl_configured

    def is_hdfs_perm_configured(self):
        return self.__hdfs_perm_configured

    def is_hbase_configured(self):
        return self.__hbase_configured

    def is_hbase_acl_configured(self):
        return self.__hbase_acl_configured

    def is_elasticsearch_template_installed(self):
        return self.__elasticsearch_template_installed

    def set_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_configured_flag_file, "Setting Indexing configured to True")

    def set_hbase_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hbase_configured_flag_file, "Setting HBase configured to True for indexing")

    def set_hbase_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hbase_acl_configured_flag_file, "Setting HBase ACL configured to True for indexing")

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_acl_configured_flag_file, "Setting Indexing ACL configured to True")

    def set_hdfs_perm_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.indexing_hdfs_perm_configured_flag_file, "Setting HDFS perm configured to True")

    def set_elasticsearch_template_installed(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.elasticsearch_template_installed_flag_file, "Setting Elasticsearch template installed to True")

    def create_hbase_tables(self):
        Logger.info("Creating HBase Tables for indexing")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"create '{0}','{1}'\" | hbase shell -n"
        add_update_cmd = cmd.format(self.__params.update_hbase_table, self.__params.update_hbase_cf)
        Execute(add_update_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done creating HBase Tables for indexing")
        self.set_hbase_configured()

    def set_hbase_acls(self):
        Logger.info("Setting HBase ACLs for indexing")
        if self.__params.security_enabled:
            metron_security.kinit(self.__params.kinit_path_local,
                  self.__params.hbase_keytab_path,
                  self.__params.hbase_principal_name,
                  execute_user=self.__params.hbase_user)
        cmd = "echo \"grant '{0}', 'RW', '{1}'\" | hbase shell -n"
        add_update_acl_cmd = cmd.format(self.__params.metron_user, self.__params.update_hbase_table)
        Execute(add_update_acl_cmd,
                tries=3,
                try_sleep=5,
                logoutput=False,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                user=self.__params.hbase_user
                )

        Logger.info("Done setting HBase ACLs for indexing")
        self.set_hbase_acl_configured()

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for indexing')
        metron_service.init_kafka_topics(self.__params, self.__get_topics())

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for indexing')
        metron_service.init_kafka_acls(self.__params, self.__get_topics())
        metron_service.init_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

    def init_hdfs_dir(self):
        Logger.info('Setting up HDFS indexing directory')

        # Non Kerberized Metron runs under 'storm', requiring write under the 'hadoop' group.
        # Kerberized Metron runs under it's own user.
        ownership = 0755 if self.__params.security_enabled else 0775
        Logger.info('HDFS indexing directory ownership is: ' + str(ownership))
        self.__params.HdfsResource(self.__params.metron_apps_indexed_hdfs_dir,
                                   type="directory",
                                   action="create_on_execute",
                                   owner=self.__params.metron_user,
                                   group=self.__params.hadoop_group,
                                   mode=ownership,
                                   )
        Logger.info('Done creating HDFS indexing directory')

    def check_elasticsearch_templates(self):
        for template_name in self.get_templates():

            # check for the index template
            cmd = "curl -s -XGET \"http://{0}/_template/{1}\" | grep -o {1}"
            err_msg="Missing Elasticsearch index template: name={0}"
            metron_service.execute(
              cmd=cmd.format(self.__params.es_http_url, template_name),
              user=self.__params.metron_user,
              err_msg=err_msg.format(template_name))

    def start_batch_indexing_topology(self, env):
        Logger.info('Starting ' + self.__batch_indexing_topology)

        if not self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)

            start_cmd_template = """{0}/bin/start_hdfs_topology.sh"""
            start_cmd = start_cmd_template.format(self.__params.metron_home)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info('Batch Indexing topology already running')

        Logger.info('Finished starting batch indexing topology')

    def start_random_access_indexing_topology(self, env):
        Logger.info('Starting ' + self.__random_access_indexing_topology)

        if not self.is_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)

            start_cmd_template = """{0}/bin/start_elasticsearch_topology.sh"""
            start_cmd = start_cmd_template.format(self.__params.metron_home)
            Execute(start_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info('Random Access Indexing topology already running')

        Logger.info('Finished starting random access indexing topology')


    def start_indexing_topology(self, env):
        self.start_batch_indexing_topology(env)
        self.start_random_access_indexing_topology(env)
        Logger.info('Finished starting indexing topologies')

    def stop_batch_indexing_topology(self, env):
        Logger.info('Stopping ' + self.__batch_indexing_topology)

        if self.is_batch_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            stop_cmd = 'storm kill ' + self.__batch_indexing_topology
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info("Batch Indexing topology already stopped")

        Logger.info('Done stopping batch indexing topologies')

    def stop_random_access_indexing_topology(self, env):
        Logger.info('Stopping ' + self.__random_access_indexing_topology)

        if self.is_random_access_topology_active(env):
            if self.__params.security_enabled:
                metron_security.kinit(self.__params.kinit_path_local,
                                      self.__params.metron_keytab_path,
                                      self.__params.metron_principal_name,
                                      execute_user=self.__params.metron_user)
            stop_cmd = 'storm kill ' + self.__random_access_indexing_topology
            Execute(stop_cmd, user=self.__params.metron_user, tries=3, try_sleep=5, logoutput=True)

        else:
            Logger.info("Random Access Indexing topology already stopped")

        Logger.info('Done stopping random access indexing topologies')

    def stop_indexing_topology(self, env):
        self.stop_batch_indexing_topology(env)
        self.stop_random_access_indexing_topology(env)
        Logger.info('Done stopping indexing topologies')

    def restart_indexing_topology(self, env):
        Logger.info('Restarting the indexing topologies')
        self.stop_indexing_topology(env)

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
            self.start_indexing_topology(env)
            Logger.info('Done restarting the indexing topologies')
        else:
            Logger.warning('Retries exhausted. Existing topology not cleaned up.  Aborting topology start.')

    def is_batch_topology_active(self, env):
        env.set_params(self.__params)
        topologies = metron_service.get_running_topologies(self.__params)
        is_batch_running = False
        if self.__batch_indexing_topology in topologies:
            is_batch_running = topologies[self.__batch_indexing_topology] in ['ACTIVE', 'REBALANCING']
        return is_batch_running

    def is_random_access_topology_active(self, env):
        env.set_params(self.__params)
        topologies = metron_service.get_running_topologies(self.__params)
        is_random_access_running = False
        if self.__random_access_indexing_topology in topologies:
            is_random_access_running = topologies[self.__random_access_indexing_topology] in ['ACTIVE', 'REBALANCING']
        return is_random_access_running


    def is_topology_active(self, env):
        return self.is_batch_topology_active(env) and self.is_random_access_topology_active(env)

    def service_check(self, env):
        """
        Performs a service check for Indexing.
        :param env: Environment
        """
        Logger.info('Checking Kafka topics for Indexing')
        metron_service.check_kafka_topics(self.__params, self.__get_topics())

        Logger.info("Checking HBase for Indexing")
        metron_service.check_hbase_table(self.__params, self.__params.update_hbase_table)
        metron_service.check_hbase_column_family(self.__params, self.__params.update_hbase_table, self.__params.update_hbase_cf)

        Logger.info('Checking Elasticsearch templates for Indexing')
        self.check_elasticsearch_templates()

        if self.__params.security_enabled:

            Logger.info('Checking Kafka ACLs for Indexing')
            metron_service.check_kafka_acls(self.__params, self.__get_topics())
            metron_service.check_kafka_acl_groups(self.__params, self.__get_kafka_acl_groups())

            Logger.info("Checking HBase ACLs for Indexing")
            metron_service.check_hbase_acls(self.__params, self.__params.update_hbase_table)

        Logger.info("Checking for Indexing topology")
        if not self.is_topology_active(env):
            raise Fail("Indexing topology not running")

        Logger.info("Indexing service check completed successfully")
