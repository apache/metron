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

from ambari_commons.os_check import OSCheck
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
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

        # TODO: Figure this out for all supported OSes
        Execute('rpm --import https://packages.elastic.co/GPG-KEY-elasticsearch')
        Logger.info("Installing Kibana CentOS/RHEL repo")
        Execute("echo \"[kibana-4.x]\n"
                "name=Kibana repository for 4.5.x packages\n"
                "baseurl=http://packages.elastic.co/kibana/4.5/centos\n"
                "gpgcheck=1\n"
                "gpgkey=https://packages.elastic.co/GPG-KEY-elasticsearch\n"
                "enabled=1\" > /etc/yum.repos.d/kibana.repo")

        majorVersion = OSCheck.get_os_major_version()
        Logger.info("CentOS/RHEL major version reported by Ambari: " + majorVersion)
        if majorVersion == "6" or majorVersion == "7":
            repoName = "name=CentOS/RHEL {} repository for Elasticsearch Curator 4.x packages\n".format(majorVersion)
            baseUrl = "baseurl=http://packages.elastic.co/curator/4/centos/{}\n".format(majorVersion)
            Logger.info("Installing Elasticsearch Curator CentOS/RHEL {} repo".format(majorVersion))
            Execute("echo \"[curator-4]\n" +
                    repoName +
                    baseUrl +
                    "gpgcheck=1\n"
                    "gpgkey=http://packages.elastic.co/GPG-KEY-elasticsearch\n"
                    "enabled=1\" > /etc/yum.repos.d/curator.repo")
        else:
            raise Exception("Unsupported CentOS/RHEL version: " + majorVersion)

        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)

        Logger.info("Configure Kibana for Metron")

        directories = [params.log_dir, params.pid_dir, params.conf_dir]
        Directory(directories,
                  # recursive=True,
                  mode=0755,
                  owner=params.kibana_user,
                  group=params.kibana_user
                  )

        File("{}/kibana.yml".format(params.conf_dir),
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

        Execute("service kibana status")

    @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
    def load_template(self, env):
        from dashboard.dashboardindex import DashboardIndex

        import params
        env.set_params(params)

        hostname = ambari_format("{es_host}")
        port = int(ambari_format("{es_port}"))

        Logger.info("Connecting to Elasticsearch on host: %s, port: %s" % (hostname, port))
        di = DashboardIndex(host=hostname, port=port)

        # Loads Kibana Dashboard definition from disk and replaces .kibana on index
        templateFile = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dashboard', 'dashboard.p')
        if not os.path.isfile(templateFile):
            raise IOError(
                errno.ENOENT, os.strerror(errno.ENOENT), templateFile)

        Logger.info("Deleting .kibana index from Elasticsearch")

        di.es.indices.delete(index='.kibana', ignore=[400, 404])

        Logger.info("Loading .kibana index from %s" % templateFile)

        di.put(data=di.load(filespec=templateFile))


if __name__ == "__main__":
    Kibana().execute()
