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

from resource_management.core import shell
from resource_management.core.resources.system import Execute
from resource_management.libraries.script import Script
from resource_management.core.logger import Logger
from elastic_commands import service_check
from elastic_commands import configure_master

class Elasticsearch(Script):

    def install(self, env):
        import params
        env.set_params(params)
        Logger.info('Install Elasticsearch master node')
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)
        Logger.info('Configure Elasticsearch master node')
        configure_master()

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        Logger.info('Stop Elasticsearch master node')
        Execute("service elasticsearch stop")

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        Logger.info('Start Elasticsearch master node')
        self.configure(env)
        Execute("service elasticsearch start")

    def status(self, env):
        import params
        env.set_params(params)
        Logger.info('Status check Elasticsearch master node')
        service_check(
          cmd="service elasticsearch status",
          user=params.elastic_status_check_user,
          label="Elasticsearch Master")

    def restart(self, env):
        import params
        env.set_params(params)
        Logger.info('Restart Elasticsearch master node')
        self.configure(env)
        Execute("service elasticsearch restart")


if __name__ == "__main__":
    Elasticsearch().execute()
