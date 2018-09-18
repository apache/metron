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

import os.path
from resource_management.core.source import Template
from resource_management.core.source import InlineTemplate
from resource_management.core.resources.system import Directory, File
from resource_management.core import global_lock
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import format as ambari_format


# Convenience function for ensuring home dirs are setup consistently.
def storm_security_setup(params):
    if params.security_enabled:
        # I don't think there's an Ambari way to get a user's local home dir , so have Python perform tilde expansion.
        # Ambari's Directory doesn't do tilde expansion.
        metron_storm_dir_tilde = '~' + params.metron_user + '/.storm'
        metron_storm_dir = os.path.expanduser(metron_storm_dir_tilde)


        Directory(params.metron_home,
                  mode=0755,
                  owner=params.metron_user,
                  group=params.metron_group,
                  create_parents=True
                  )

        Directory(metron_storm_dir,
                  mode=0755,
                  owner=params.metron_user,
                  group=params.metron_group
                  )

        File(ambari_format('{client_jaas_path}'),
             content=InlineTemplate(params.metron_client_jaas_conf_template),
             owner=params.metron_user,
             group=params.metron_group,
             mode=0755
             )

        File(metron_storm_dir + '/storm.yaml',
             content=Template('storm.yaml.j2'),
             owner=params.metron_user,
             group=params.metron_group,
             mode=0755
             )

        File(metron_storm_dir + '/storm.config',
             content=Template('storm.config.j2'),
             owner=params.metron_user,
             group=params.metron_group,
             mode=0755
             )


def kinit(kinit_path_local, keytab_path, principal_name, execute_user=None):
    # prevent concurrent kinit
    kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    kinit_lock.acquire()
    kinitcmd = "{0} -kt {1} {2}; ".format(kinit_path_local, keytab_path, principal_name)
    Logger.info("kinit command: " + kinitcmd + " as user: " + str(execute_user))
    try:
        if execute_user is None:
            Execute(kinitcmd)
        else:
            Execute(kinitcmd, user=execute_user)
    finally:
        kinit_lock.release()
