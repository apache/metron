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

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.libraries.script import Script
from indexing_commands import IndexingCommands
import metron_service

class Indexing(Script):

    __configured = False

    def install(self, env):
        from params import params
        env.set_params(params)
        commands = IndexingCommands(params)
        commands.setup_repo()
        Logger.info('Install RPM packages')
        self.install_packages(env)

    def configure(self, env):
        from params import params
        env.set_params(params)

        commands = IndexingCommands(params)
        metron_service.load_global_config(params)

        if not commands.is_configured():
            commands.init_kafka_topics()
            commands.set_configured()

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = IndexingCommands(params)
        commands.start_indexing_topology()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = IndexingCommands(params)
        commands.stop_indexing_topology()

    def status(self, env):
        import status_params
        env.set_params(status_params)
        commands = IndexingCommands(status_params)
        if not commands.is_topology_active(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = IndexingCommands(params)
        commands.restart_indexing_topology(env)


if __name__ == "__main__":
    Indexing().execute()
