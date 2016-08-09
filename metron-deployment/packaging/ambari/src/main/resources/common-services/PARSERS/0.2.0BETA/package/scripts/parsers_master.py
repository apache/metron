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
from resource_management.libraries.functions import format
from resource_management.libraries.script import Script
from parsers import parsers_init


class Parsers(Script):

    def install(self, env):
        import params
        env.set_params(params)
        parser_list = params.parsers.replace(' ', '').split(',')

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

        print 'Install RPM packages'
        self.install_packages(env)

        print 'Upload grok patterns'
        parsers_init()

        print 'Setup Kafka topics'
        for parser in parser_list:
            Execute(format("""/usr/hdp/current/kafka-broker/bin/kafka-topics.sh \
                        --zookeeper {zookeeper_url} \
                        --create \
                        --topic {} \
                        --partitions {} \
                        --replication-factor {} \
                        --config retention.bytes={}""").format(parser,1,1,10 * 1024 * 1024 * 1024))
#        {{ kafka_home }}/bin/kafka-topics.sh \
#            --zookeeper {{ zookeeper_url }} \
#            --create \
#            --topic {{ item.topic }} \
#            --partitions {{ item.num_partitions }} \
#            --replication-factor {{ item.replication_factor }} \
#            --config retention.bytes={{ item.retention_gb * 1024 * 1024 * 1024 }}

        print 'Load parser config'
        #Execute(format("{metron_home + "/bin/zk_load_configs.sh --mode PUSH -i {{ zookeeper_config_path }} -z {zookeeper_url}")
        Execute(format("{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_url}"))

    def start(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        parser_list = params.parsers.replace(' ', '').split(',')
        print 'Start the Master'
        for parser in parser_list:
            print 'Starting ' + parser
            start_cmd = params.metron_home + '/bin/start_parser_topology.sh -s ' + parser + ' -z localhost:2181'
            Execute(start_cmd)
        print 'Finished starting parser topologies'

    def stop(self, env, upgrade_type=None):
        import params
        env.set_params(params)
        parser_list = params.parsers.replace(' ', '').split(',')
        print 'Stopping parsers'
        for parser in parser_list:
            print 'Stopping ' + parser
            stop_cmd = 'storm kill ' + parser
            Execute(stop_cmd)
        print 'Done'

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
