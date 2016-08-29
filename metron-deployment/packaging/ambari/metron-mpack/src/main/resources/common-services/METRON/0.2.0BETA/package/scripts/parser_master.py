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
from resource_management.core.resources.system import Directory, File
from parser_commands import ParserCommands


class ParserMaster(Script):
    def get_component_name(self):
        # TODO add this at some point - currently will cause problems with hdp-select
        # return "parser-master"
        pass

    def install(self, env):
        from params import params
        env.set_params(params)
        commands = ParserCommands(params)
        commands.setup_repo()
        Logger.info('Install RPM packages')
        self.install_packages(env)

        Logger.info('Create Metron Local Config Directory')
        directories = [params.metron_zookeeper_config_path]
        Directory(directories,
              # recursive=True,
              mode=0755,
              owner=params.metron_user,
              group=params.metron_group
              )

        #Default global.json to empty file
        File("{}/global.json".format(params.metron_zookeeper_config_path),
         owner=params.metron_user,
         content="{}"
         )
        commands.init_config()

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = ParserCommands(params)
        commands.init_parsers()
        commands.init_kafka_topics()
        commands.start_parser_topologies()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = ParserCommands(params)
        commands.stop_parser_topologies()

    def status(self, env):
        # from params import params
        # env.set_params(params)
        #
        # commands = ParserCommands(params)
        #
        # if not commands.topologies_running():
        raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        commands = ParserCommands(params)
        commands.restart_parser_topologies()

if __name__ == "__main__":
    ParserMaster().execute()
