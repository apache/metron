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

import errno
import os
import requests

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

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
        metron_service.check_indexer_parameters()
        File(format("{metron_config_path}/elasticsearch.properties"),
             content=Template("elasticsearch.properties.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )
        File(format("{metron_config_path}/solr.properties"),
             content=Template("solr.properties.j2"),
             owner=params.metron_user,
             group=params.metron_group
             )
        File(format("{metron_config_path}/hdfs.properties"),
             content=Template("hdfs.properties.j2"),
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
        if params.ra_indexing_writer == 'Solr':
            # Install Solr schemas
            if not commands.is_solr_schema_installed():
                if commands.solr_schema_install(env):
                    commands.set_solr_schema_installed()

        elif params.ra_indexing_writer == 'Elasticsearch':
            # Install elasticsearch templates
            if not commands.is_elasticsearch_template_installed():
                if self.elasticsearch_template_install(env):
                    commands.set_elasticsearch_template_installed()

        else :
            msg = "WARNING:  index schemas/templates could not be installed.  " \
                  "Is Indexing server configured properly ?  Will reattempt install on next start.  index server configured={0}"
            Logger.warning(msg.format(params.ra_indexing_writer))

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

        if params.ra_indexing_writer == 'Solr':
            # Install Solr schemas
            if not commands.is_solr_schema_installed():
                if commands.solr_schema_install(env):
                    commands.set_solr_schema_installed()

        elif params.ra_indexing_writer == 'Elasticsearch':
            # Install elasticsearch templates
            if not commands.is_elasticsearch_template_installed():
                if self.elasticsearch_template_install(env):
                    commands.set_elasticsearch_template_installed()

        else :
            msg = "WARNING:  index schemas/templates could not be installed.  " \
                  "Is Indexing server configured properly ?  Will reattempt install on next start.  index server configured={0}"
            Logger.warning(msg.format(params.ra_indexing_writer))

        commands.restart_indexing_topology(env)

    def elasticsearch_template_install(self, env):
        from params import params
        env.set_params(params)
        Logger.info("Installing Elasticsearch index templates")

        try:
            metron_service.check_indexer_parameters()
            commands = IndexingCommands(params)
            for template_name, template_path in commands.get_templates().iteritems():
                # install the index template
                File(template_path, mode=0755, content=StaticFile("{0}.template".format(template_name)))
                cmd = "curl -s -XPOST http://{0}/_template/{1} -d @{2}"
                Execute(
                  cmd.format(params.es_http_url, template_name, template_path),
                  logoutput=True)
            return True

        except Exception as e:
            msg = "WARNING: Elasticsearch index templates could not be installed.  " \
                  "Is Elasticsearch running?  Will reattempt install on next start.  error={0}"
            Logger.warning(msg.format(e))
            return False

    def elasticsearch_template_delete(self, env):
        from params import params
        env.set_params(params)
        Logger.info("Deleting Elasticsearch index templates")
        metron_service.check_indexer_parameters()

        commands = IndexingCommands(params)
        for template_name in commands.get_templates():

            # delete the index template
            cmd = "curl -s -XDELETE \"http://{0}/_template/{1}\""
            Execute(
              cmd.format(params.es_http_url, template_name),
              logoutput=True)

    @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
    def kibana_dashboard_install(self, env):
      from params import params
      env.set_params(params)
      metron_service.check_indexer_parameters()

      Logger.info("Connecting to Elasticsearch on: %s" % (params.es_http_url))
      kibanaTemplate = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dashboard', 'kibana.template')
      if not os.path.isfile(kibanaTemplate):
        raise IOError(
            errno.ENOENT, os.strerror(errno.ENOENT), kibanaTemplate)

      Logger.info("Loading .kibana index template from %s" % kibanaTemplate)
      template_cmd = ambari_format(
          'curl -s -XPOST http://{es_http_url}/_template/.kibana -d @%s' % kibanaTemplate)
      Execute(template_cmd, logoutput=True)

      kibanaDashboardLoad = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dashboard', 'dashboard-bulkload.json')
      if not os.path.isfile(kibanaDashboardLoad):
        raise IOError(
            errno.ENOENT, os.strerror(errno.ENOENT), kibanaDashboardLoad)

      Logger.info("Loading .kibana dashboard from %s" % kibanaDashboardLoad)

      kibana_cmd = ambari_format(
          'curl -s -H "Content-Type: application/x-ndjson" -XPOST http://{es_http_url}/.kibana/_bulk --data-binary @%s' % kibanaDashboardLoad)
      Execute(kibana_cmd, logoutput=True)

    def zeppelin_notebook_import(self, env):
        from params import params
        env.set_params(params)
        metron_service.check_indexer_parameters()

        commands = IndexingCommands(params)
        Logger.info(ambari_format('Searching for Zeppelin Notebooks in {metron_config_zeppelin_path}'))

        # Check if authentication is configured on Zeppelin server, and fetch details if enabled.
        ses = requests.session()
        ses = commands.get_zeppelin_auth_details(ses, params.zeppelin_server_url, env)
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
