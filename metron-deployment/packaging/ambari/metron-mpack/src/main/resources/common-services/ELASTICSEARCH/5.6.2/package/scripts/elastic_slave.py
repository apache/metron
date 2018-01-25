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
from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.script import Script

from slave import slave


class Elasticsearch(Script):
    def install(self, env):
        import params
        env.set_params(params)
        Logger.info('Install Elasticsearch data node')
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)
        Logger.info('Configure Elasticsearch data node')
        slave()

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        Logger.info('Stop Elasticsearch data node')
        stop_cmd = "service elasticsearch stop"
        Execute(stop_cmd)

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        self.configure(env)
        Logger.info('Start Elasticsearch data node')
        start_cmd = "service elasticsearch start"
        Execute(start_cmd)

    def status(self, env):
        import params
        env.set_params(params)
        Logger.info('Check status of Elasticsearch data node')

        # return codes defined by LSB
        # http://refspecs.linuxbase.org/LSB_3.0.0/LSB-PDA/LSB-PDA/iniscrptact.html
        cmd = ('service', 'elasticsearch', 'status')
        rc, out = shell.call(cmd, sudo=True, quiet=False)

        if rc in [1, 2, 3]:
          # if return code = 1, 2, or 3, then 'program is not running' or 'dead'
          # Ambari's resource_management/libraries/script/script.py handles
          # this specific exception as OK
          Logger.info("Elasticsearch slave is not running")
          raise ComponentIsNotRunning()

        elif rc == 0:
          # if return code = 0, then 'program is running or service is OK'
          Logger.info("Elasticsearch slave is running")

        else:
          # else, program is dead or service state is unknown
          err_msg = "Execution of '{0}' returned {1}".format(" ".join(cmd), rc)
          raise ExecutionFailed(err_msg, rc, out)

    def restart(self, env):
        import params
        env.set_params(params)
        self.configure(env)
        Logger.info('Restart Elasticsearch data node')
        restart_cmd = "service elasticsearch restart"
        Execute(restart_cmd)


if __name__ == "__main__":
    Elasticsearch().execute()
