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
from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute

from resource_management.core.logger import Logger

from management_ui_commands import ManagementUICommands


class ManagementUIMaster(Script):

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)
        Execute('npm --prefix ' + params.metron_home + '/web/expressjs/ install')

    def configure(self, env, upgrade_type=None, config_dir=None):
        print 'configure managment_ui'
        from params import params
        env.set_params(params)

        File(format("{metron_config_path}/management_ui.yml"),
             mode=0755,
             content=Template("management_ui.yml.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )

        Directory('/var/run/metron',
                  create_parents=False,
                  mode=0755,
                  owner=params.metron_user,
                  group=params.metron_group
                  )

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ManagementUICommands(params)
        commands.start_management_ui()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = ManagementUICommands(params)
        commands.stop_management_ui()

    def status(self, env):
        status_cmd = format('service metron-management-ui status')
        try:
            Execute(status_cmd)
        except ExecutionFailed:
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = ManagementUICommands(params)
        commands.restart_management_ui(env)


if __name__ == "__main__":
    ManagementUIMaster().execute()
