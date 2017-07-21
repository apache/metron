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

# Wrap major operations and functionality in this class
class ManagementUICommands:
    __params = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params

    def start_management_ui(self):
        Logger.info('Starting Management UI')
        Execute("service metron-management-ui start")
        Logger.info('Done starting Management UI')

    def stop_management_ui(self):
        Logger.info('Stopping Management UI')
        Execute("service metron-management-ui stop")
        Logger.info('Done stopping Management UI')

    def restart_management_ui(self, env):
        Logger.info('Restarting the Management UI')
        Execute('service metron-management-ui restart')
        Logger.info('Done restarting the Management UI')
