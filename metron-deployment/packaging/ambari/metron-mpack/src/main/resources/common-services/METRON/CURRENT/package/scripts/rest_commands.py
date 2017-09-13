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
import string

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

        # Build the spring options
        # the vagrant Spring profile provides configuration values, otherwise configuration is provided by rest_application.yml
        metron_spring_options = format(" --server.port={metron_rest_port}")
        if not "vagrant" in self.__params.metron_spring_profiles_active:
            metron_spring_options += format(" --spring.config.location={metron_home}/config/rest_application.yml")
        if self.__params.metron_spring_profiles_active:
            metron_spring_options += format(" --spring.profiles.active={metron_spring_profiles_active}")

        # if self.__params.metron_jdbc_password:
        #     metron_spring_options += format(" --spring.datasource.password={metron_jdbc_password!p}")

        metron_rest_classpath = format("{hadoop_conf_dir}:{hbase_conf_dir}:{metron_home}/lib/metron-rest-{metron_version}.jar")
        if self.__params.metron_jdbc_client_path:
            metron_rest_classpath += format(":{metron_jdbc_client_path}")


        if self.__params.metron_indexing_classpath:
            metron_rest_classpath += format(":{metron_indexing_classpath}")
        else:
            metron_rest_classpath += format(":{metron_home}/lib/metron-elasticsearch-{metron_version}-uber.jar")

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
        cmd = format(("set -o allexport; source {metron_sysconfig}; set +o allexport;"
                     "export METRON_JDBC_PASSWORD={metron_jdbc_password!p};"
                     "{java64_home}/bin/java -cp {metron_rest_classpath} org.apache.metron.rest.MetronRestApplication {metron_spring_options} >> {metron_log_dir}/metron-rest.log 2>&1 & echo $! > {pid_file};"
                     "unset METRON_JDBC_PASSWORD;"))
        daemon_cmd = cmd

        Execute(daemon_cmd,
                user = self.__params.metron_user,
                logoutput=True,
                not_if = process_id_exists_command)
        Logger.info('Done starting REST application')

    def stop_rest_application(self):
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
        daemon_kill_cmd = ('kill', format("{pid}"))
        Execute(daemon_kill_cmd,
                sudo=True,
                not_if = format("! ({process_id_exists_command})")
                )

        # Violently kill
        daemon_hard_kill_cmd = ('kill', '-9', format("{pid}"))
        wait_time = 5
        Execute(daemon_hard_kill_cmd,
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
        Logger.info('Restarting the REST application')
        self.stop_rest_application()
        self.start_rest_application()
        Logger.info('Done restarting the REST application')
