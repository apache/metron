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

import metron_service

# Wrap major operations and functionality in this class
class AlertsUICommands:
    __params = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params

    def start_alerts_ui(self):
        """
        Start the Alerts UI
        :param env: Environment
        """
        Logger.info('Starting Alerts UI')
        start_cmd = ('service', 'metron-alerts-ui', 'start')
        Execute(start_cmd, sudo=True)
        Logger.info('Done starting Alerts UI')

    def stop_alerts_ui(self):
        """
        Stop the Alerts UI
        :param env: Environment
        """
        Logger.info('Stopping Alerts UI')
        stop_cmd = ('service', 'metron-alerts-ui', 'stop')
        Execute(stop_cmd, sudo=True)
        Logger.info('Done stopping Alerts UI')

    def restart_alerts_ui(self, env):
        """
        Restart the Alerts UI
        :param env: Environment
        """
        Logger.info('Restarting the Alerts UI')
        restart_cmd = ('service', 'metron-alerts-ui', 'restart')
        Execute(restart_cmd, sudo=True)
        Logger.info('Done restarting the Alerts UI')

    def status_alerts_ui(self, env):
        """
        Performs a status check for the Alerts UI
        :param env: Environment
        """
        Logger.info('Status check the Alerts UI')
        metron_service.check_http(
          self.__params.metron_alerts_ui_host,
          self.__params.metron_alerts_ui_port,
          self.__params.metron_user)

    def service_check(self, env):
        """
        Performs a service check for the Alerts UI
        :param env: Environment
        """
        Logger.info('Checking connectivity to Alerts UI')
        metron_service.check_http(
          self.__params.metron_alerts_ui_host,
          self.__params.metron_alerts_ui_port,
          self.__params.metron_user)

        Logger.info("Alerts UI service check completed successfully")
