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

import os
import re
import requests
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.core.source import StaticFile
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.script import Script

from metron_security import storm_security_setup
import metron_service
from indexing_commands import IndexingCommands


class Indexing(Script):
    __configured = False

    def install(self, env):
        from params import params
        env.set_params(params)
        self.install_packages(env)

    def configure(self, env, upgrade_type=None, config_dir=None):
        from params import params
        env.set_params(params)

        Logger.info("Running indexing configure")
        File(format("{metron_config_path}/elasticsearch.properties"),
             content=Template("elasticsearch.properties.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )

        if not metron_service.is_zk_configured(params):
            metron_service.init_zk_config(params)
            metron_service.set_zk_configured(params)
        metron_service.refresh_configs(params)

        commands = IndexingCommands(params)
        if not commands.is_configured():
            commands.init_kafka_topics()
            commands.init_hdfs_dir()
            commands.set_configured()
        if params.security_enabled and not commands.is_hdfs_perm_configured():
            # If we Kerberize the cluster, we need to call this again, to remove write perms from hadoop group
            # If we start off Kerberized, it just does the same thing twice.
            commands.init_hdfs_dir()
            commands.set_hdfs_perm_configured()
        if params.security_enabled and not commands.is_acl_configured():
            commands.init_kafka_acls()
            commands.set_acl_configured()

        if not commands.is_hbase_configured():
            commands.create_hbase_tables()
        if params.security_enabled and not commands.is_hbase_acl_configured():
            commands.set_hbase_acls()

        Logger.info("Calling security setup")
        storm_security_setup(params)

    def start(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = IndexingCommands(params)

        # Install elasticsearch templates
        try:
            if not commands.is_elasticsearch_template_installed():
                self.elasticsearch_template_install(env)
                commands.set_elasticsearch_template_installed()

        except Exception as e:
            msg = "WARNING: Elasticsearch index templates could not be installed.  " \
                  "Is Elasticsearch running?  error={0}"
            Logger.warning(msg.format(e))
            raise

        commands.start_indexing_topology(env)

    def stop(self, env, upgrade_type=None):
        from params import params
        env.set_params(params)
        commands = IndexingCommands(params)
        commands.stop_indexing_topology(env)

    def status(self, env):
        from params import status_params
        env.set_params(status_params)
        commands = IndexingCommands(status_params)
        if not commands.is_topology_active(env):
            raise ComponentIsNotRunning()

    def restart(self, env):
        from params import params
        env.set_params(params)
        self.configure(env)
        commands = IndexingCommands(params)
        commands.restart_indexing_topology(env)

    def elasticsearch_template_install(self, env):
        from params import params
        env.set_params(params)
        Logger.info("Installing Elasticsearch index templates")

        commands = IndexingCommands(params)
        for template_name, template_path in commands.get_templates().iteritems():

            # install the index template
            File(template_path, mode=0755, content=StaticFile("{0}.template".format(template_name)))
            cmd = "curl -s -XPOST http://{0}/_template/{1} -d @{2}"
            Execute(
              cmd.format(params.es_http_url, template_name, template_path),
              logoutput=True)

    def elasticsearch_template_delete(self, env):
        from params import params
        env.set_params(params)
        Logger.info("Deleting Elasticsearch index templates")

        commands = IndexingCommands(params)
        for template_name in commands.get_templates():

            # delete the index template
            cmd = "curl -s -XDELETE \"http://{0}/_template/{1}\""
            Execute(
              cmd.format(params.es_http_url, template_name),
              logoutput=True)

    def zeppelin_notebook_import(self, env):
        from params import params
        env.set_params(params)

        Logger.info(ambari_format('Searching for Zeppelin Notebooks in {metron_config_zeppelin_path}'))

        # With Ambari 2.5+, Zeppelin server is enabled to work with Shiro authentication, which requires user/password
        # for authentication (see https://zeppelin.apache.org/docs/0.6.0/security/shiroauthentication.html for details).
        ses = requests.session()

        # Check if authentication is enabled on the Zeppelin server
        try:
            conn = ses.get(ambari_format('http://{zeppelin_server_url}/api/login'))

            # Establish connection if authentication is enabled
            try:
                Logger.info("Shiro authentication is found to be enabled on the Zeppelin server.")
                # Read the Shiro admin user credentials from Zeppelin config in Ambari
                seen_users = False
                username = None
                password = None
                if re.search(r'^\[users\]', params.zeppelin_shiro_ini_content, re.MULTILINE):
                    seen_users = True
                    tokens = re.search(r'^admin\ =.*', params.zeppelin_shiro_ini_content, re.MULTILINE).group()
                    userpassword = tokens.split(',')[0].strip()
                    username = userpassword.split('=')[0].strip()
                    password = userpassword.split('=')[1].strip()
                else:
                    Logger.error("ERROR: Admin credentials config was not found in shiro.ini. Notebook import may fail.")

                zeppelin_payload = {'userName': username, 'password' : password}
                conn = ses.post(ambari_format('http://{zeppelin_server_url}/api/login'), data=zeppelin_payload)
            except:
                pass

        # If authentication is not enabled, fall back to default method of imporing notebooks
        except requests.exceptions.RequestException:
            conn = ses.get(ambari_format('http://{zeppelin_server_url}/api/notebook'))

        for dirName, subdirList, files in os.walk(params.metron_config_zeppelin_path):
            for fileName in files:
                if fileName.endswith(".json"):
                    Logger.info("Importing notebook: " + fileName)
                    zeppelin_import_url = ambari_format('http://{zeppelin_server_url}/api/notebook/import')
                    zeppelin_notebook = {'file' : open(os.path.join(dirName, fileName), 'rb')}
                    res = ses.post(zeppelin_import_url, files=zeppelin_notebook)
                    Logger.info("Result: " + res.text)

if __name__ == "__main__":
    Indexing().execute()
