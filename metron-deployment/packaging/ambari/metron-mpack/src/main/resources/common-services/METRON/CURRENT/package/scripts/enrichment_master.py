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
from resource_management.core.resources.system import File
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.script import Script

from enrichment_commands import EnrichmentCommands
import metron_service


class Enrichment(Script):
    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)
        self.configure(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

        File(format("{metron_config_path}/enrichment.properties"),
             content=Template("enrichment.properties.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = EnrichmentCommands(params)
        metron_service.load_global_config(params)

        if not commands.is_kafka_configured():
            commands.init_kafka_topics()
        if not commands.is_hbase_configured():
            commands.create_hbase_tables()
        if not commands.is_geo_configured():
            commands.init_geo()

        commands.start_enrichment_topology()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = EnrichmentCommands(params)
        commands.stop_enrichment_topology()

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = EnrichmentCommands(status_params)

        if not commands.is_topology_active(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        commands = EnrichmentCommands(params)
        commands.restart_enrichment_topology(env)


if __name__ == "__main__":
    Enrichment().execute()
