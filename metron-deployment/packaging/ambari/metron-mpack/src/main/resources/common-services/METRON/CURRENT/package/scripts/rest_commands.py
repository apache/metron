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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions import get_user_call_output
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs

import metron_service
from metron_security import kinit

# Wrap major operations and functionality in this class
class RestCommands:
    __params = None
    __acl_configured = False

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params
        self.__acl_configured = os.path.isfile(self.__params.rest_acl_configured_flag_file)

    def is_acl_configured(self):
        return self.__acl_configured

    def set_acl_configured(self):
        File(self.__params.rest_acl_configured_flag_file,
             content="",
             owner=self.__params.metron_user,
             mode=0755)

    def init_kafka_topics(self):
      Logger.info('Creating Kafka topics for rest')
      topics = [self.__params.metron_escalation_topic]
      metron_service.init_kafka_topics(self.__params, topics)

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for rest')
        # The following topics must be permissioned for the rest application list operation
        topics = [self.__params.ambari_kafka_service_check_topic, self.__params.consumer_offsets_topic, self.__params.metron_escalation_topic]
        metron_service.init_kafka_acls(self.__params, topics, ['metron-rest'])

    def start_rest_application(self):
        Logger.info('Starting REST application')

        pid_file = format("{metron_rest_pid_dir}/{metron_rest_pid}")
        cmd = format("{start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.err {pid_file} {hive_server_conf_dir} {hive_log_dir}")

        if self.__params.security_enabled:
            kinit(self.__params.kinit_path_local,
            self.__params.metron_keytab_path,
            self.__params.metron_principal_name,
            execute_user=self.__params.metron_user)

        pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=self.__params.metron_user, is_checked_call=False)[1]
        process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

        daemon_cmd = cmd
        hadoop_home = self.__params.hadoop_home
        hive_bin = "hive"

        Execute(daemon_cmd,
                user = self.__params.metron_user,
                environment = { 'HADOOP_HOME': hadoop_home, 'JAVA_HOME': self.__params.java64_home, 'HIVE_BIN': hive_bin },
                path = self.__params.execute_path,
                not_if = process_id_exists_command)
        Logger.info('Done starting REST application')

    def stop_rest_application(self):
        Logger.info('Stopping REST application')
        # Execute("service metron-rest stop")

        pid_file = format("{metron_rest_pid_dir}/{metron_rest_pid}")
        cmd = format("{start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.err {pid_file} {hive_server_conf_dir} {hive_log_dir}")

        pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=self.__params.metron_user, is_checked_call=False)[1]
        process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

        daemon_kill_cmd = format("{sudo} kill {pid}")
        daemon_hard_kill_cmd = format("{sudo} kill -9 {pid}")

        if self.__params.security_enabled:
            kinit(self.__params.kinit_path_local,
            self.__params.metron_keytab_path,
            self.__params.metron_principal_name,
            execute_user=self.__params.metron_user)

        Execute(daemon_kill_cmd,
                not_if = format("! ({process_id_exists_command})")
                )

        wait_time = 5
        Execute(daemon_hard_kill_cmd,
                not_if = format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )"),
                ignore_failures = True
                )

        try:
            # check if stopped the process, else fail the task
            Execute(format("! ({process_id_exists_command})"),
                tries=20,
                try_sleep=3,
                  )
        except:
            show_logs(self.__params.hive_log_dir, self.__params.metron_user)
            raise

        File(pid_file, action = "delete")
        Logger.info('Done stopping REST application')

    def restart_rest_application(self):
        Logger.info('Restarting the REST application')
        # command = format("service metron-rest restart {metron_jdbc_password!p}")
        # Execute(command)
        self.stop_rest_application()
        self.start_rest_application()
        Logger.info('Done restarting the REST application')






