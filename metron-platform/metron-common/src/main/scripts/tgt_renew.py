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

from requests_kerberos import HTTPKerberosAuth
from contextlib import closing
import json
import requests
import subprocess
import sys

INDENT_SIZE = 2

class ShellHandler(object):

    def __init__(self):
        pass

    # returns full stdout of process call
    def call(self, command):
        try:
            return subprocess.call(command, shell=True)
        except OSError as e:
            print >> sys.stderr, "Execution failed:", e
    
    # partly hijacked from Python 2.7+ check_output for use in 2.6
    def ret_output(self, cmd):
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
        output, unused_err = process.communicate()
        retcode = process.poll()
        if retcode:
            raise subprocess.CalledProcessError(retcode, cmd, output=output)
        return output

def get_topologies(storm_ui_host, requested_owner):
    print "Connecting to Storm via " + storm_ui_host
    response = requests.get(storm_ui_host, auth=HTTPKerberosAuth())
    topology_ids = []
    if response.status_code == 200:
        print "Connected to server ok, parsing results"
        for feature, value in response.json().iteritems():
            if feature == 'topologies':
                for topology in value:
                    t_id = ''
                    t_owner = ''
                    for k, v in topology.iteritems():
                        if k == 'name':
                            t_id = v
                        if k == 'owner':
                            t_owner = v
                    if t_owner == requested_owner:
                        topology_ids.append(t_id)
    else:
        print "Request for topologies failed with status code: " + str(response.status_code)
    print "Finished. Found {0} topologies for user '{1}'".format(len(topology_ids), requested_owner)
    return topology_ids

def upload_credentials(topologies):
    if len(topologies) == 0:
        print "No topologies running for specified username/owner. Exiting"
        return
    storm_cmd_template = "/usr/hdp/current/storm-client/bin/storm -c topology.auto-credentials=\'[\"org.apache.storm.security.auth.kerberos.AutoTGT\"]\' upload-credentials $topology_name"
    for t in topologies:
        cmd = storm_cmd_template.replace("$topology_name", t)
        print "Running renew-credentials command: " + cmd
        ShellHandler().call(cmd)

if len(sys.argv) != 3:
    raise ValueError("Need host info 'host:port' for Storm and topology owner name")

host_info = sys.argv[1]
requested_owner = sys.argv[2]
storm_ui_host = 'http://{0}/api/v1/topology/summary'.format(host_info)
topologies = get_topologies(storm_ui_host, requested_owner)
upload_credentials(topologies)

