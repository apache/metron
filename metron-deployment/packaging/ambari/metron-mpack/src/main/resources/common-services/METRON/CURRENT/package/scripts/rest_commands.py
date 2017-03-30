#!/usr/bin/env python
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
from resource_management.core.resources.system import Execute, File

# Wrap major operations and functionality in this class
class RestCommands:
    __params = None

    def __init__(self, params):
        if params is None:
            raise ValueError("params argument is required for initialization")
        self.__params = params

    def setup_repo(self):
        def local_repo():
            Logger.info("Setting up local repo")
            Execute("yum -y install createrepo")
            Execute("createrepo /localrepo")
            Execute("chmod -R o-w+r /localrepo")
            Execute("echo \"[METRON-${metron.version}]\n"
                    "name=Metron ${metron.version} packages\n"
                    "baseurl=file:///localrepo\n"
                    "gpgcheck=0\n"
                    "enabled=1\" > /etc/yum.repos.d/local.repo")

        def remote_repo():
            print('Using remote repo')

        yum_repo_types = {
            'local': local_repo,
            'remote': remote_repo
        }
        repo_type = self.__params.yum_repo_type
        if repo_type in yum_repo_types:
            yum_repo_types[repo_type]()
        else:
            raise ValueError("Unsupported repo type '{0}'".format(repo_type))

    def start_rest_application(self):
        Logger.info('Starting REST application')
        Execute("service metron-rest start")
        Logger.info('Done starting REST application')

    def stop_rest_application(self):
        Logger.info('Stopping REST application')
        Execute("service metron-rest stop")
        Logger.info('Done stopping REST application')

    def restart_rest_application(self, env):
        Logger.info('Restarting the REST application')
        Execute('service metron-rest restart')
        Logger.info('Done restarting the REST application')
