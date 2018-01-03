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
from __future__ import print_function

import subprocess
import sys

from resource_management.core.resources.system import Execute
from resource_management.libraries.script import Script
from resource_management.core.logger import Logger

class ServiceCheck(Script):

    def service_check(self, env):
        import params
        env.set_params(params)
        Logger.info("Running Elasticsearch service check")

        port = self.get_port_from_range(params.http_port)
        self.check_cluster_health(params.hostname, port)

        Logger.info("Elasticsearch service check successful")
        exit(0)

    def check_cluster_health(self, host, port, status="green", timeout="120s"):
        """
        Checks Elasticsearch cluster health.  Will wait for a given health
        state to be reached.

        :param host: The name of a host running Elasticsearch.
        :param port: The Elasticsearch HTTP port.
        :param status: The expected cluster health state.  By default, green.
        :param timeout: How long to wait for the cluster.  By default, 120 seconds.
        """
        Logger.info("Checking cluster health")

        cmd = "curl -sS -XGET 'http://{0}:{1}/_cluster/health?wait_for_status={2}&timeout={3}' | grep '\"status\":\"{2}\"'"
        Execute(cmd.format(host, port, status, timeout), logoutput=True, tries=5, try_sleep=10)

    def get_port_from_range(self, port_range, delimiter="-", default="9200"):
        """
        Elasticsearch is configured with a range of ports to bind to, such as
        9200-9300.  This function identifies a single port within the given range.

        :param port_range: A range of ports that Elasticsearch binds to.
        :param delimiter: The port range delimiter, by default "-".
        :param default: If no port can be identified in the port_range, the default is returned.
        :return A single port within the given range.
        """
        port = default
        if delimiter in port_range:
            ports = port_range.split(delimiter)
            if len(ports) > 0:
                port = ports[0]

        return port


if __name__ == "__main__":
    ServiceCheck().execute()
