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
from resource_management.core.resources.system import File
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.script import Script
from resource_management.core.logger import Logger

from enrichment_commands import EnrichmentCommands
from metron_security import storm_security_setup
import metron_service
import metron_security

class Enrichment(Script):

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

        Logger.info("Running enrichment configure")
        File(format("{metron_config_path}/enrichment-splitjoin.properties"),
             content=Template("enrichment-splitjoin.properties.j2"),
             owner=params.metron_user,
             group=params.metron_group)

        File(format("{metron_config_path}/enrichment-unified.properties"),
            content=Template("enrichment-unified.properties.j2"),
            owner=params.metron_user,
            group=params.metron_group)

        if not metron_service.is_zk_configured(params):
            metron_service.init_zk_config(params)
            metron_service.set_zk_configured(params)
        metron_service.refresh_configs(params)

        Logger.info("Calling security setup")
        storm_security_setup(params)

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = EnrichmentCommands(params)

        if params.security_enabled:
            metron_security.kinit(params.kinit_path_local,
                                  params.metron_keytab_path,
                                  params.metron_principal_name,
                                  execute_user=params.metron_user)

        if not commands.is_kafka_configured():
            commands.init_kafka_topics()
        if params.security_enabled and not commands.is_kafka_acl_configured():
            commands.init_kafka_acls()
        if not commands.is_hbase_configured():
            commands.create_hbase_tables()
        if not commands.is_hbase_coprocessor_configured():
            commands.load_enrichment_coprocessor()
        if params.security_enabled and not commands.is_hbase_acl_configured():
            commands.set_hbase_acls()
        if not commands.is_maxmind_configured():
            commands.init_maxmind()

        commands.start_enrichment_topology(env)

    def stop(self, env, upgrade_type=None):
        from params import params

        env.set_params(params)
        commands = EnrichmentCommands(params)

        if params.security_enabled:
            metron_security.kinit(params.kinit_path_local,
                                  params.metron_keytab_path,
                                  params.metron_principal_name,
                                  execute_user=params.metron_user)

        commands.stop_enrichment_topology(env)

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = EnrichmentCommands(status_params)

        if status_params.security_enabled:
            metron_security.kinit(status_params.kinit_path_local,
                                  status_params.metron_keytab_path,
                                  status_params.metron_principal_name,
                                  execute_user=status_params.metron_user)

        if not commands.is_topology_active(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = EnrichmentCommands(params)
        commands.restart_enrichment_topology(env)


if __name__ == "__main__":
    Enrichment().execute()
