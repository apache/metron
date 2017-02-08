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
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.source import StaticFile
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.script import Script

import metron_service
from error_commands import ErrorCommands


class Error(Script):
    __configured = False

    def install(self, env):
        from params import params
        env.set_params(params)
        commands = ErrorCommands(params)
        commands.setup_repo()
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

        commands = ErrorCommands(params)
        metron_service.load_global_config(params)

        if not commands.is_configured():
            commands.init_kafka_topics()
            commands.init_hdfs_dir()
            commands.set_configured()

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ErrorCommands(params)
        commands.start_error_topology()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = ErrorCommands(params)
        commands.stop_error_topology()

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = ErrorCommands(status_params)
        if not commands.is_topology_active(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ErrorCommands(params)
        commands.restart_error_topology(env)

    def elasticsearch_error_template_install(self, env):
        from params import params
        env.set_params(params)

        File(params.error_index_path,
             mode=0755,
             content=StaticFile('error_index.template')
             )

        error_cmd = ambari_format(
            'curl -s -XPOST http://{es_http_url}/_template/error_index -d @{error_index_path}')
        Execute(error_cmd, logoutput=True)

    def elasticsearch_template_delete(self, env):
        from params import params
        env.set_params(params)

        error_cmd = ambari_format('curl -s -XDELETE "http://{es_http_url}/error_index*"')
        Execute(error_cmd, logoutput=True)

if __name__ == "__main__":
    Error().execute()
