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
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.exceptions import ExecutionFailed
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.format import format

import metron_service

# Wrap major operations and functionality in this class
class ManagementUICommands:
    __params = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params

    def script(self, action): 
        Directory(self.__params.metron_config_pid_dir,
                  mode=0755,
                  owner=self.__params.metron_user,
                  group=self.__params.metron_group,
                  create_parents=True
                  )
        Directory(self.__params.metron_log_dir,
                  mode=0755,
                  owner=self.__params.metron_user,
                  group=self.__params.metron_group,
                  create_parents=True
                  )
        password = self.__params.metron_config_ssl_password
        metron_home = self.__params.metron_home
        pid_dir = self.__params.metron_config_pid_dir
        return format(("export METRON_SSL_PASSWORD={password!p};"
                       "export MODE=service;"
                       "export PID_FOLDER={pid_dir};"
                       "export JAVA_OPTS={metron_config_jvmopts};"
                       "{metron_home}/bin/metron-config.sh {action}"))
    
    def start_management_ui(self):
        """
        Starts the Management UI
        :param env: Environment
        """
        Logger.info('Starting Management UI')
        Execute(self.script("start"), user=self.__params.metron_user)
        Logger.info('Done starting Management UI')

    def stop_management_ui(self):
        """
        Stops the Management UI
        :param env: Environment
        """
        Logger.info('Stopping Management UI')
        Execute(self.script("stop"), user=self.__params.metron_user)
        Logger.info('Done stopping Management UI')

    def restart_management_ui(self):
        """
        Restarts the Management UI
        :param env: Environment
        """
        Logger.info('Restarting the Management UI')
        Execute(self.script("restart"), user=self.__params.metron_user)
        Logger.info('Done restarting the Management UI')

    def status_management_ui(self, env):
        """
        Performs a status check for the Management UI
        :param env: Environment
        """
        Logger.info('Status check the Management UI')
        metron_service.check_http(
          self.__params.metron_management_ui_host,
          self.__params.metron_management_ui_port,
          self.__params.metron_user)

    def service_check(self, env):
        """
        Performs a service check for the Management UI
        :param env: Environment
        """
        Logger.info('Checking connectivity to Management UI')
        metron_service.check_http(
          self.__params.metron_management_ui_host,
          self.__params.metron_management_ui_port,
          self.__params.metron_user)

        Logger.info("Management UI service check completed successfully")
