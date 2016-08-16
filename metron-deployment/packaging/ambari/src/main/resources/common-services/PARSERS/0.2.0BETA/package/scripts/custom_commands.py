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

from resource_management.libraries.script import Script

from commands import Commands


class CustomCommands(Script):
    def kafkabuild(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = Commands(params)
        commands.init_kafka_topics()

    def zookeeperbuild(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = Commands(params)
        commands.init_parser_config()


if __name__ == "__main__":
    CustomCommands().execute()
