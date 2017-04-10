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

import json
import subprocess

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, File
from resource_management.core.resources.system import Execute
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions import format as ambari_format

def init_config():
    Logger.info('Loading config into ZooKeeper')
    Execute(ambari_format(
        "{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_quorum}"),
        path=ambari_format("{java_home}/bin")
    )


def get_running_topologies():
    Logger.info('Getting Running Storm Topologies from Storm REST Server')

    cmd = ambari_format('curl --max-time 3 {storm_rest_addr}/api/v1/topology/summary')
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    (stdout, stderr) = proc.communicate()

    try:
        stormjson = json.loads(stdout)
    except ValueError:
        return {}

    topologiesDict = {}

    for topology in stormjson['topologies']:
        topologiesDict[topology['name']] = topology['status']

    Logger.info("Topologies: " + str(topologiesDict))
    return topologiesDict


def load_global_config(params):
    Logger.info('Create Metron Local Config Directory')
    Logger.info("Configure Metron global.json")

    directories = [params.metron_zookeeper_config_path]
    Directory(directories,
              mode=0755,
              owner=params.metron_user,
              group=params.metron_group
              )

    File("{0}/global.json".format(params.metron_zookeeper_config_path),
         owner=params.metron_user,
         content=InlineTemplate(params.global_json_template)
         )

    File("{0}/elasticsearch.properties".format(params.metron_zookeeper_config_path + '/..'),
         owner=params.metron_user,
         content=InlineTemplate(params.global_properties_template))

    init_config()
