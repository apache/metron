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

import os

from ambari_commons.os_check import OSCheck
from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.core.resources import User
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format as ambari_format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output


def service_check(cmd, user, label):
    """
    Executes a SysV service check command that adheres to LSB-compliant
    return codes.  The return codes are interpreted as defined
    by the LSB.

    See http://refspecs.linuxbase.org/LSB_3.0.0/LSB-PDA/LSB-PDA/iniscrptact.html
    for more information.

    :param cmd: The service check command to execute.
    :param label: The name of the service.
    """
    Logger.info("Performing service check; cmd={0}, user={1}, label={2}".format(cmd, user, label))
    rc, out, err = get_user_call_output(cmd, user, is_checked_call=False)

    if rc in [1, 2, 3]:
      # if return code in [1, 2, 3], then 'program is not running' or 'program is dead'
      Logger.info("{0} is not running".format(label))
      raise ComponentIsNotRunning()

    elif rc == 0:
      # if return code = 0, then 'program is running or service is OK'
      Logger.info("{0} is running".format(label))

    else:
      # else service state is unknown
      err_msg = "{0} service check failed; cmd '{1}' returned {2}".format(label, cmd, rc)
      Logger.error(err_msg)
      raise ExecutionFailed(err_msg, rc, out, err)

def is_systemd_running():
    """
    Determines if the platform is running Systemd.
    :return True, if the platform is running Systemd.  False, otherwise.
    """
    Logger.info("Is the platform running Systemd?")
    rc, out, err = get_user_call_output("pidof systemd", "root", is_checked_call=False)
    if rc == 0:
        Logger.info("Systemd was found")
        return True
    else:
        Logger.info("Systemd was NOT found")
        return False

def configure_systemd(params):
    """
    Configure Systemd for Elasticsearch.
    """
    Logger.info("Configuring Systemd for Elasticsearch");

    # ensure the systemd directory for elasticsearch overrides exists
    Logger.info("Create Systemd directory for Elasticsearch: {0}".format(params.systemd_elasticsearch_dir))
    Directory(params.systemd_elasticsearch_dir,
              create_parents=True,
              owner='root',
              group='root')

    # when using Elasticsearch packages on systems that use systemd, system
    # limits must also be specified via systemd.
    # see https://www.elastic.co/guide/en/elasticsearch/reference/5.6/setting-system-settings.html#systemd
    Logger.info("Elasticsearch systemd limits: {0}".format(params.systemd_override_file))
    File(params.systemd_override_file,
         content=InlineTemplate(params.systemd_override_template),
         owner="root",
         group="root")

    # reload the configuration
    Execute("systemctl daemon-reload")

def create_user(params):
    """
    Creates the user required for Elasticsearch.
    """
    Logger.info("Creating user={0} in group={1}".format(params.elastic_user, params.elastic_group))
    User(params.elastic_user, action = "create", groups = params.elastic_group)

def create_directories(params, directories):
    """
    Creates one or more directories.
    """
    Logger.info("Creating directories: {0}".format(directories))
    Directory(directories,
              create_parents=True,
              mode=0755,
              owner=params.elastic_user,
              group=params.elastic_group
              )

def create_elastic_env(params):
    """
    Creates the Elasticsearch environment file.
    """
    Logger.info("Create Elasticsearch environment file.")
    File("{0}/elastic-env.sh".format(params.conf_dir),
         owner=params.elastic_user,
         group=params.elastic_group,
         content=InlineTemplate(params.elastic_env_sh_template))

def create_elastic_site(params, template_name):
    """
    Creates the Elasticsearch site file.
    """
    Logger.info("Creating Elasticsearch site file; template={0}".format(template_name))

    elastic_site = params.config['configurations']['elastic-site']
    path = "{0}/elasticsearch.yml".format(params.conf_dir)
    template = Template(template_name, configurations=elastic_site)
    File(path,
         content=template,
         owner=params.elastic_user,
         group=params.elastic_group)

def get_elastic_config_path(default="/etc/default/elasticsearch"):
    """
    Defines the path to the Elasticsearch environment file.  This path will
    differ based on the OS family.
    :param default: The path used if the OS family is not recognized.
    """
    path = default
    if OSCheck.is_redhat_family():
      path = "/etc/sysconfig/elasticsearch"
    elif OSCheck.is_ubuntu_family():
      path = "/etc/default/elasticsearch"
    else:
      Logger.error("Unexpected OS family; using default path={0}".format(path))

    return path

def create_elastic_config(params):
    """
    Creates the Elasticsearch system config file.  Usually lands at either
    /etc/sysconfig/elasticsearch or /etc/default/elasticsearch.
    """
    path = get_elastic_config_path()
    Logger.info("Creating the Elasticsearch system config; path={0}".format(path))
    File(path, owner="root", group="root", content=InlineTemplate(params.sysconfig_template))

def create_elastic_pam_limits(params):
    """
    Creates the PAM limits for Elasticsearch.
    """
    Logger.info("Creating Elasticsearch PAM limits.")

    # in some OS this folder may not exist, so create it
    Logger.info("Ensure PAM limits directory exists: {0}".format(params.limits_conf_dir))
    Directory(params.limits_conf_dir,
              create_parents=True,
              owner='root',
              group='root')

    Logger.info("Creating Elasticsearch PAM limits; file={0}".format(params.limits_conf_file))
    File(params.limits_conf_file,
         content=Template('elasticsearch_limits.conf.j2'),
         owner="root",
         group="root")

def create_elastic_jvm_options(params):
    """
    Defines the jvm.options file used to specify JVM options.
    """
    path = "{0}/jvm.options".format(params.conf_dir)
    Logger.info("Creating Elasticsearch JVM Options; file={0}".format(path))
    File(path,
         content=InlineTemplate(params.jvm_options_template),
         owner=params.elastic_user,
         group=params.elastic_group)

def get_data_directories(params):
    """
    Returns the directories to use for storing Elasticsearch data.
    """
    path = params.path_data
    path = path.replace('"', '')
    path = path.replace(' ', '')
    path = path.split(',')
    dirs = [p.replace('"', '') for p in path]

    Logger.info("Elasticsearch data directories: dirs={0}".format(dirs))
    return dirs

def configure_master():
    """
    Configures the Elasticsearch master node.
    """
    import params

    # define the directories required
    dirs = [
      params.log_dir,
      params.pid_dir,
      params.conf_dir,
      "{0}/scripts".format(params.conf_dir)
    ]
    dirs += get_data_directories(params)

    # configure the elasticsearch master
    create_user(params)
    create_directories(params, dirs)
    create_elastic_env(params)
    create_elastic_site(params,  "elasticsearch.master.yaml.j2")
    create_elastic_config(params)
    create_elastic_pam_limits(params)
    create_elastic_jvm_options(params)
    if is_systemd_running():
        configure_systemd(params)

def configure_slave():
    """
    Configures the Elasticsearch slave node.
    """
    import params

    # define the directories required
    dirs = [
      params.log_dir,
      params.pid_dir,
      params.conf_dir,
    ]
    dirs += get_data_directories(params)

    # configure the elasticsearch slave
    create_user(params)
    create_directories(params, dirs)
    create_elastic_env(params)
    create_elastic_site(params, "elasticsearch.slave.yaml.j2")
    create_elastic_config(params)
    create_elastic_pam_limits(params)
    create_elastic_jvm_options(params)
    if is_systemd_running():
        configure_systemd(params)
