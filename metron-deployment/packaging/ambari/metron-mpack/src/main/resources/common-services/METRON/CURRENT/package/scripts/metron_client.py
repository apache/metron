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

from resource_management.libraries.script.script import Script
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory
from metron_security import storm_security_setup
from metron_service import install_metron_knox
from metron_service import metron_knox_topology_setup
from metron_service import is_metron_knox_installed
from metron_service import set_metron_knox_installed

class MetronClient(Script):

    def install(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)

    def configure(self, env):
        from params import params
        env.set_params(params)
        storm_security_setup(params)

        if params.metron_knox_enabled and not params.metron_ldap_enabled:
            raise Fail("Enabling Metron with Knox requires LDAP authentication.  Please set 'LDAP Enabled' to true in the Metron Security tab.")

        if params.metron_knox_enabled:
            if not is_metron_knox_installed(params):
                install_metron_knox(params)
                set_metron_knox_installed(params)
            metron_knox_topology_setup(params)

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)


    def status(self, env):
        raise ClientComponentHasNoStatus()

if __name__ == "__main__":
    MetronClient().execute()
