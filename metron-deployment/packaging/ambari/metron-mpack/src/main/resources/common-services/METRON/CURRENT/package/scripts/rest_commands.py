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

from datetime import datetime
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
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
        Directory(params.metron_rest_pid_dir,
                  mode=0755,
                  owner=params.metron_user,
                  group=params.metron_group,
                  create_parents=True
                  )
        Directory(params.metron_log_dir,
                  mode=0755,
                  owner=params.metron_user,
                  group=params.metron_group,
                  create_parents=True
                  )

    def is_acl_configured(self):
        return self.__acl_configured

    def set_acl_configured(self):
        metron_service.set_configured(self.__params.metron_user, self.__params.rest_acl_configured_flag_file, "Setting REST ACL configured to true")

    def __get_topics(self):
        return [self.__params.metron_escalation_topic]

    def init_kafka_topics(self):
        Logger.info('Creating Kafka topics for rest')
        metron_service.init_kafka_topics(self.__params, self.__get_topics())

    def init_kafka_acls(self):
        Logger.info('Creating Kafka ACLs for rest')

        # The following topics must be permissioned for the rest application list operation
        topics = self.__get_topics() + [self.__params.ambari_kafka_service_check_topic, self.__params.consumer_offsets_topic]
        metron_service.init_kafka_acls(self.__params, topics)

        groups = ['metron-rest']
        metron_service.init_kafka_acl_groups(self.__params, groups)

    def start_rest_application(self):
        """
        Start the REST application
        """
        Logger.info('Starting REST application')

        if self.__params.security_enabled:
            kinit(self.__params.kinit_path_local,
            self.__params.metron_keytab_path,
            self.__params.metron_principal_name,
            execute_user=self.__params.metron_user)

        # Get the PID associated with the service
        pid_file = format("{metron_rest_pid_dir}/{metron_rest_pid}")
        pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=self.__params.metron_user, is_checked_call=False)[1]
        process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

        # Set the password with env variable instead of param to avoid it showing in ps
        cmd = format((
          "export METRON_JDBC_PASSWORD={metron_jdbc_password!p};"
          "export JAVA_HOME={java_home};"
          "export METRON_REST_CLASSPATH={metron_rest_classpath};"
          "export METRON_INDEX_CP={metron_indexing_classpath};"
          "export METRON_LOG_DIR={metron_log_dir};"
          "export METRON_PID_FILE={pid_file};"
          "{metron_home}/bin/metron-rest.sh;"
          "unset METRON_JDBC_PASSWORD;"
        ))

        Execute(cmd,
                user = self.__params.metron_user,
                logoutput=True,
                not_if = process_id_exists_command,
                timeout=60)
        Logger.info('Done starting REST application')

    def stop_rest_application(self):
        """
        Stop the REST application
        """
        Logger.info('Stopping REST application')

        # Get the pid associated with the service
        pid_file = format("{metron_rest_pid_dir}/{metron_rest_pid}")
        pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=self.__params.metron_user, is_checked_call=False)[1]
        process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

        if self.__params.security_enabled:
            kinit(self.__params.kinit_path_local,
            self.__params.metron_keytab_path,
            self.__params.metron_principal_name,
            execute_user=self.__params.metron_user)

        # Politely kill
        kill_cmd = ('kill', format("{pid}"))
        Execute(kill_cmd,
                sudo=True,
                not_if = format("! ({process_id_exists_command})")
                )

        # Violently kill
        hard_kill_cmd = ('kill', '-9', format("{pid}"))
        wait_time = 5
        Execute(hard_kill_cmd,
                not_if = format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )"),
                sudo=True,
                ignore_failures = True
                )

        try:
            # check if stopped the process, else fail the task
            Execute(format("! ({process_id_exists_command})"),
                tries=20,
                try_sleep=3,
                  )
        except:
            show_logs(self.__params.metron_log_dir, self.__params.metron_user)
            raise

        File(pid_file, action = "delete")
        Logger.info('Done stopping REST application')

    def restart_rest_application(self, env):
        """
        Restart the REST application
        :param env: Environment
        """
        Logger.info('Restarting the REST application')
        self.stop_rest_application()
        self.start_rest_application()
        Logger.info('Done restarting the REST application')

    def status_rest_application(self, env):
        """
        Performs a status check for the REST application
        :param env: Environment
        """
        Logger.info('Status check the REST application')
        metron_service.check_http(
            self.__params.metron_rest_host,
            self.__params.metron_rest_port,
            self.__params.metron_user)

    def service_check(self, env):
        """
        Performs a service check for the REST application
        :param env: Environment
        """
        Logger.info('Checking connectivity to REST application')
        metron_service.check_http(
            self.__params.metron_rest_host,
            self.__params.metron_rest_port,
            self.__params.metron_user)

        Logger.info('Checking Kafka topics for the REST application')
        metron_service.check_kafka_topics(self.__params, self.__get_topics())

        if self.__params.security_enabled:
            Logger.info('Checking Kafka topic ACL for the REST application')
            metron_service.check_kafka_acls(self.__params, self.__get_topics())

        Logger.info("REST application service check completed successfully")
