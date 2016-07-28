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


from kibana import kibana
from resource_management.core.resources.system import Execute
from resource_management.libraries.script import Script


class Kibana(Script):
    def install(self, env):
        import params
        env.set_params(params)

        print 'Install the Master'
        #TODO: Figure this out for all supported OSes
        Execute('rpm --import https://packages.elastic.co/GPG-KEY-elasticsearch')
        Execute("echo \"[kibana-4.x]\n"
                "name=Kibana repository for 4.5.x packages\n"
                "baseurl=http://packages.elastic.co/kibana/4.5/centos\n"
                "gpgcheck=1\n"
                "gpgkey=https://packages.elastic.co/GPG-KEY-elasticsearch\n"
                "enabled=1\" > /etc/yum.repos.d/kibana.repo")

        self.install_packages(env)

    def configure(self, env):
        import params
        env.set_params(params)

        print 'Install plugins'
        kibana()

    def stop(self, env):
        import params
        env.set_params(params)
        stop_cmd = format("service kibana stop")
        print 'Stop the Master'
        Execute(stop_cmd)

    def start(self, env):
        import params
        env.set_params(params)

        self.configure(env)
        start_cmd = format("service kibana start")
        print 'Start the Master'
        Execute(start_cmd)

    def restart(self,env):
        import params
        env.set_params(params)

        self.configure(env)
        restart_cmd = format("service kibana restart")
        print 'Restarting the Master'
        Execute(restart_cmd)

    def status(self, env):
        import params
        env.set_params(params)
        status_cmd = format("service kibana status")
        print 'Status of the Master'
        Execute(status_cmd)


if __name__ == "__main__":
    Kibana().execute()
