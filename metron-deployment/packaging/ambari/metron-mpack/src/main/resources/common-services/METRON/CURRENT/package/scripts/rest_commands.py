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
from resource_management.libraries.functions.format import format

import metron_service

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
        command = format("service metron-rest start {metron_jdbc_password!p}")
        Execute(command)
        Logger.info('Done starting REST application')

    def stop_rest_application(self):
        Logger.info('Stopping REST application')
        Execute("service metron-rest stop")
        Logger.info('Done stopping REST application')

    def restart_rest_application(self, env):
        Logger.info('Restarting the REST application')
        command = format("service metron-rest restart {metron_jdbc_password!p}")
        Execute(command)
        Logger.info('Done restarting the REST application')
