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

from metron_security import storm_security_setup
import metron_service
from parser_commands import ParserCommands


class ParserMaster(Script):
    def get_component_name(self):
        # TODO add this at some point - currently will cause problems with hdp-select
        # return "parser-master"
        pass

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

        if not metron_service.is_zk_configured(params):
            metron_service.init_zk_config(params)
            metron_service.set_zk_configured(params)
        metron_service.refresh_configs(params)
        commands = ParserCommands(params)
        if not commands.is_configured():
            commands.init_parsers()
            commands.init_kafka_topics()
            commands.set_configured()
        if params.security_enabled and not commands.is_acl_configured():
            commands.init_kafka_acls()
            commands.set_acl_configured()

        Logger.info("Calling security setup")
        storm_security_setup(params)

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ParserCommands(params)
        commands.start_parser_topologies(env)

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = ParserCommands(params)
        commands.stop_parser_topologies(env)

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = ParserCommands(status_params)
        if not commands.topologies_running(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ParserCommands(params)
        commands.restart_parser_topologies(env)

    def servicechecktest(self, env):
        from params import params
        env.set_params(params)
        from service_check import ServiceCheck
        service_check = ServiceCheck()
        Logger.info('Service Check Test')
        service_check.service_check(env)


if __name__ == "__main__":
    ParserMaster().execute()
