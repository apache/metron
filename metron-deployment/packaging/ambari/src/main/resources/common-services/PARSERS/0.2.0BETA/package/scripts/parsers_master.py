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


class Parsers(Script):

    parser_list = None

    def __init__(self):
        import params
        self.parser_list = params.parsers.replace(' ', '').split(',')

    def install(self, env):
        import params
        env.set_params(params)

        print 'Install the Master'
        ## TODO - this will become a remote yum repo instead of local
        Execute("yum -y install createrepo")
        Execute("createrepo /localrepo")
        Execute("chmod -R o-w+r /localrepo")
        Execute("echo \"[METRON-0.2.0BETA]\n"
                "name=Metron 0.2.0BETA packages\n"
                "baseurl=file:///localrepo\n"
                "gpgcheck=0\n"
                "enabled=1\" > /etc/yum.repos.d/local.repo")

        print 'Install packages'
        self.install_packages(env)

        print 'Load parser config'
        ## TODO find where to get/share the ZK configs
        # Execute(params.metron_home + "/bin/zk_load_configs.sh --mode PUSH -i {{ zookeeper_config_path }} -z {{ zookeeper_url }}")

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        print 'Start the Master'
        for parser in self.parser_list:
            ## TODO
            # start_cmd = params.metron_home + "/bin/start_parser_topology.sh " + parser
            print 'Starting ' + parser
            # Execute(start_cmd)
        print 'Finished starting parser topologies'

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        parser_list = params.parsers.replace(' ', '').split('')
        stop_cmd = format("storm kill ")
        print 'Stop the Master'
        Execute(stop_cmd)

    def status(self, env):
        import params
        env.set_params(params)
        print 'Status of the Master'

    def restart(self, env):
        import params
        env.set_params(params)
        print 'Restarting the Master'

if __name__ == "__main__":
    Parsers().execute()
