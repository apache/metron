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
import re

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
        self.index_document(params.hostname, port)

        Logger.info("Elasticsearch service check successful")
        exit(0)

    def index_document(self, host, port, doc='{"name": "Ambari Service Check"}', index="ambari_service_check"):
        """
        Tests the health of Elasticsearch by indexing a document.

        :param host: The name of a host running Elasticsearch.
        :param port: The Elasticsearch HTTP port.
        :param doc: The test document to put.
        :param index: The name of the test index.
        """
        # put a document into a new index
        Execute("curl -XPUT 'http://%s:%s/%s/test/1' -d '%s'" % (host, port, index, doc), logoutput=True)

        # retrieve the document...  use subprocess because we actually need the results here.
        cmd_retrieve = "curl -XGET 'http://%s:%s/%s/test/1'" % (host, port, index)
        proc = subprocess.Popen(cmd_retrieve, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (stdout, stderr) = proc.communicate()
        response_retrieve = stdout
        Logger.info("Retrieval response is: %s" % response_retrieve)
        expected_retrieve = '{"_index":"%s","_type":"test","_id":"1","_version":1,"found":true,"_source":%s}' \
            % (index, doc)

        # delete the test index
        cmd_delete = "curl -XDELETE 'http://%s:%s/%s'" % (host, port, index)
        proc = subprocess.Popen(cmd_delete, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (stdout, stderr) = proc.communicate()
        response_delete = stdout
        Logger.info("Delete index response is: %s" % response_retrieve)
        expected_delete = '{"acknowledged":true}'

        if (expected_retrieve == response_retrieve) and (expected_delete == response_delete):
            Logger.info("Successfully indexed document in Elasticsearch")
        else:
            Logger.info("Unable to retrieve document from Elasticsearch")
            sys.exit(1)

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
