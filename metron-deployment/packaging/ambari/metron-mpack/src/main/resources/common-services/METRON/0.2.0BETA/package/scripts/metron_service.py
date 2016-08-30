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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import format
from resource_management.libraries.script import Script

import subprocess
import json

def init_config():
    Logger.info('Loading config into ZooKeeper')
    Execute(format(
        "{metron_home}/bin/zk_load_configs.sh --mode PUSH -i {metron_zookeeper_config_path} -z {zookeeper_quorum}"),
        path=format("{java_home}/bin")
    )

def get_running_topologies():
    Logger.info('Getting Running Storm Topologies from Storm REST Server')

    cmd = format('curl {storm_rest_addr}/api/v1/topology/summary')
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    (stdout, stderr) = proc.communicate()

    stormjson = json.loads(stdout)
    topologiesDict = {}

    for topology in stormjson['topologies']:
        topologiesDict[topology['name']] = topology['status']

    return topologiesDict
