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
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import ExecutionFailed
from resource_management.libraries.functions.get_user_call_output import get_user_call_output

import metron_service

# Wrap major operations and functionality in this class
class ManagementUICommands:
    __params = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params

    def start_management_ui(self):
        """
        Starts the Management UI
        :param env: Environment
        """
        Logger.info('Starting Management UI')
        start_cmd = ('service', 'metron-management-ui', 'start')
        Execute(start_cmd, sudo=True)
        Logger.info('Done starting Management UI')

    def stop_management_ui(self):
        """
        Stops the Management UI
        :param env: Environment
        """
        Logger.info('Stopping Management UI')
        stop_cmd = ('service', 'metron-management-ui', 'stop')
        Execute(stop_cmd, sudo=True)
        Logger.info('Done stopping Management UI')

    def restart_management_ui(self, env):
        """
        Restarts the Management UI
        :param env: Environment
        """
        Logger.info('Restarting the Management UI')
        restart_cmd = ('service', 'metron-management-ui', 'restart')
        Execute(restart_cmd, sudo=True)
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
