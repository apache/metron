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

from resource_management.core.resources.system import Execute
from resource_management.libraries.script import Script

from slave import slave


class Elasticsearch(Script):
    def install(self, env):
        import params
        env.set_params(params)
        print 'Install the Slave'
        Execute('rpm --import https://packages.elastic.co/GPG-KEY-elasticsearch')
        Execute("echo \"[elasticsearch-2.x]\n"
                "name=Elasticsearch repository for 2.x packages\n"
                "baseurl=https://packages.elastic.co/elasticsearch/2.x/centos\n"
                "gpgcheck=1\n"
                "gpgkey=https://packages.elastic.co/GPG-KEY-elasticsearch\n"
                "enabled=1\" > /etc/yum.repos.d/elasticsearch.repo")
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        import params
        env.set_params(params)
        slave()

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        stop_cmd = format("service elasticsearch stop")
        print 'Stop the Slave'
        Execute(stop_cmd)

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        self.configure(env)
        start_cmd = format("service elasticsearch start")
        print 'Start the Slave'
        Execute(start_cmd)

    def status(self, env):
        import params
        env.set_params(params)
        status_cmd = format("service elasticsearch status")
        print 'Status of the Slave'
        Execute(status_cmd)

    def restart(self, env):
        import params
        env.set_params(params)
        self.configure(env)
        restart_cmd = format("service elasticsearch restart")
        print 'Restarting the Slave'
        Execute(restart_cmd)


if __name__ == "__main__":
    Elasticsearch().execute()
