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
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute

from resource_management.core.logger import Logger

from alerts_ui_commands import AlertsUICommands


class AlertsUIMaster(Script):

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        print 'configure alerts_ui'
        from params import params
        env.set_params(params)

        File(format("{metron_config_path}/alerts_ui.yml"),
             mode=0755,
             content=Template("alerts_ui.yml.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )

        File(format("{metron_alerts_ui_path}/assets/app-config.json"),
             content=Template("alerts-ui-app-config.json.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )

        if params.metron_knox_enabled and not params.metron_ldap_enabled:
            raise Fail("Enabling Metron with Knox requires LDAP authentication.  Please set 'LDAP Enabled' to true in the Metron Security tab.")

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = AlertsUICommands(params)
        commands.start_alerts_ui()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = AlertsUICommands(params)
        commands.stop_alerts_ui()

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = AlertsUICommands(status_params)
        commands.status_alerts_ui(env)

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = AlertsUICommands(params)
        commands.restart_alerts_ui(env)


if __name__ == "__main__":
    AlertsUIMaster().execute()
