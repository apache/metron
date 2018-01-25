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

kibana_master

"""

import errno
import os

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core import shell
from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions.format import format as ambari_format
from resource_management.libraries.script import Script


class Kibana(Script):
    def install(self, env):
        import params
        env.set_params(params)
        Logger.info("Install Kibana Master")
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)

        Logger.info("Configure Kibana for Metron")

        directories = [params.log_dir, params.pid_dir, params.conf_dir]
        Directory(directories,
                  create_parents=True,
                  mode=0755,
                  owner=params.kibana_user,
                  group=params.kibana_user
                  )

        File("{0}/kibana.yml".format(params.conf_dir),
             owner=params.kibana_user,
             content=InlineTemplate(params.kibana_yml_template)
             )

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)

        Logger.info("Stop Kibana Master")

        Execute("service kibana stop")

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)

        self.configure(env)

        Logger.info("Start the Master")


        Execute("service kibana start")

    def restart(self, env):
        import params
        env.set_params(params)

        self.configure(env)

        Logger.info("Restarting the Master")

        Execute("service kibana restart")

    def status(self, env):
        import params
        env.set_params(params)

        Logger.info("Status of the Master")

        # return codes defined by LSB
        # http://refspecs.linuxbase.org/LSB_3.0.0/LSB-PDA/LSB-PDA/iniscrptact.html
        cmd = ('service', 'kibana', 'status')
        rc, out = shell.call(cmd, sudo=True, quiet=False)

        if rc in [1, 2, 3]:
          # if return code = 1, 2, or 3, then 'program is not running' or 'dead'
          # Ambari's resource_management/libraries/script/script.py handles
          # this specific exception as OK
          Logger.info("Kibana is not running")
          raise ComponentIsNotRunning()

        elif rc == 0:
          # if return code = 0, then 'program is running or service is OK'
          Logger.info("Kibana is running")

        else:
          # else, program is dead or service state is unknown
          err_msg = "Execution of '{0}' returned {1}".format(" ".join(cmd), rc)
          raise ExecutionFailed(err_msg, rc, out)

    @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
    def load_template(self, env):
        import params
        env.set_params(params)

        hostname = ambari_format("{es_host}")
        port = int(ambari_format("{es_port}"))

        Logger.info("Connecting to Elasticsearch on host: %s, port: %s" % (hostname, port))

        kibanaTemplate = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dashboard', 'kibana.template')
        if not os.path.isfile(kibanaTemplate):
          raise IOError(
              errno.ENOENT, os.strerror(errno.ENOENT), kibanaTemplate)

        Logger.info("Loading .kibana index template from %s" % kibanaTemplate)
        template_cmd = ambari_format(
            'curl -s -XPOST http://{es_host}:{es_port}/_template/.kibana -d @%s' % kibanaTemplate)
        Execute(template_cmd, logoutput=True)

        kibanaDashboardLoad = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dashboard', 'dashboard-bulkload.json')
        if not os.path.isfile(kibanaDashboardLoad):
          raise IOError(
              errno.ENOENT, os.strerror(errno.ENOENT), kibanaDashboardLoad)

        Logger.info("Loading .kibana dashboard from %s" % kibanaDashboardLoad)

        kibana_cmd = ambari_format(
            'curl -s -H "Content-Type: application/x-ndjson" -XPOST http://{es_host}:{es_port}/.kibana/_bulk --data-binary @%s' % kibanaDashboardLoad)
        Execute(kibana_cmd, logoutput=True)


if __name__ == "__main__":
    Kibana().execute()
