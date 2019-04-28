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

import metron_service
from rest_commands import RestCommands

class RestMaster(Script):

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)
        File(format("/etc/default/metron"),
             content=Template("metron.j2")
             )

        metron_service.refresh_configs(params)

        commands = RestCommands(params)

        if not commands.is_kafka_configured():
            commands.init_kafka_topics()
        if not commands.is_hbase_configured():
            commands.create_hbase_tables()
        if not commands.is_metron_user_hdfs_dir_configured():
            commands.create_metron_user_hdfs_dir()
        if params.security_enabled and not commands.is_hbase_acl_configured():
            commands.set_hbase_acls()
        if params.security_enabled and not commands.is_kafka_acl_configured():
            commands.init_kafka_acls()
            commands.set_kafka_acl_configured()

        if params.metron_knox_enabled and not params.metron_ldap_enabled:
            raise Fail("Enabling Metron with Knox requires LDAP authentication.  Please set 'LDAP Enabled' to true in the Metron Security tab.")

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = RestCommands(params)
        commands.start_rest_application()

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = RestCommands(params)
        commands.stop_rest_application()

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        cmd = format('curl --max-time 3 {hostname}:{metron_rest_port}')
        try:
            get_user_call_output(cmd, user=status_params.metron_user)
        except ExecutionFailed:
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = RestCommands(params)
        commands.restart_rest_application(env)


if __name__ == "__main__":
    RestMaster().execute()
