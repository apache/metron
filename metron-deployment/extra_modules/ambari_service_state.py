#!/usr/bin/python
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

DOCUMENTATION = '''
---
module: ambari_service_state
version_added: "2.1"
author: Apache Metron (https://metron.apache.org)
short_description: Start/Stop/Change Service or Component State
description:
    - Start/Stop/Change Service or Component State
options:
  host:
    description:
      The hostname for the ambari web server
  port:
    description:
      The port for the ambari web server
  username:
    description:
      The username for the ambari web server
  password:
    description:
      The name of the cluster in web server
    required: yes
  cluster_name:
    description:
      The name of the cluster in ambari
    required: yes
  service_name:
    description:
      The name of the service to alter
    required: no
  component_name:
    description:
      The name of the component to alter
    required: no
  component_host:
    description:
      The host running the targeted component. Required when component_name is used.
    required: no
  state:
    description:
      The desired service/component state.
  wait_for_complete:
    description:
      Whether to wait for the request to complete before returning. Default is False.
    required: no
  requirements: [ 'requests']
'''

EXAMPLES = '''
# must use full relative path to any files in stored in roles/role_name/files/
- name: Create a new ambari cluster
    ambari_cluster_state:
      host: localhost
      port: 8080
      username: admin
      password: admin
      cluster_name: my_cluster
      cluster_state: present
      blueprint_var: roles/my_role/files/blueprint.yml
      blueprint_name: hadoop
      wait_for_complete: True
- name: Start the ambari cluster
  ambari_cluster_state:
    host: localhost
    port: 8080
    username: admin
    password: admin
    cluster_name: my_cluster
    cluster_state: started
    wait_for_complete: True
- name: Stop the ambari cluster
  ambari_cluster_state:
    host: localhost
    port: 8080
    username: admin
    password: admin
    cluster_name: my_cluster
    cluster_state: stopped
    wait_for_complete: True
- name: Delete the ambari cluster
  ambari_cluster_state:
    host: localhost
    port: 8080
    username: admin
    password: admin
    cluster_name: my_cluster
    cluster_state: absent
'''

RETURN = '''
results:
    description: The content of the requests object returned from the RESTful call
    returned: success
    type: string
'''

__author__ = 'apachemetron'

import json

try:
    import requests
except ImportError:
    REQUESTS_FOUND = False
else:
    REQUESTS_FOUND = True


def main():

    argument_spec = dict(
        host=dict(type='str', default=None, required=True),
        port=dict(type='int', default=None, required=True),
        username=dict(type='str', default=None, required=True),
        password=dict(type='str', default=None, required=True),
        cluster_name=dict(type='str', default=None, required=True),
        state=dict(type='str', default=None, required=True,
                           choices=['started', 'stopped', 'deleted']),
        service_name=dict(type='str', required=False),
        component_name=dict(type='str', default=None, required=False),
        component_host=dict(type='str', default=None, required=False),
        wait_for_complete=dict(default=False, required=False, type='bool'),
    )

    required_together = ['component_name', 'component_host']

    module = AnsibleModule(
        argument_spec=argument_spec,
        required_together=required_together
    )

    if not REQUESTS_FOUND:
        module.fail_json(
            msg='requests library is required for this module')

    p = module.params

    host = p.get('host')
    port = p.get('port')
    username = p.get('username')
    password = p.get('password')
    cluster_name = p.get('cluster_name')
    state = p.get('state')
    service_name = p.get('service_name')
    component_name = p.get('component_name')
    component_host = p.get('component_host')
    wait_for_complete = p.get('wait_for_complete')
    component_mode = False
    ambari_url = 'http://{0}:{1}'.format(host, port)

    if component_name:
        component_mode = True

    try:
        if not cluster_exists(ambari_url, username, password, cluster_name):
            module.fail_json(msg="Cluster name {0} does not exist".format(cluster_name))

        if state in ['started', 'stopped', 'installed']:
            desired_state = ''

            if state == 'started':
                desired_state = 'STARTED'
            elif state in ['stopped','installed']:
                desired_state = 'INSTALLED'

            if component_mode:
                if desired_state == 'INSTALLED':
                    if(can_add_component(ambari_url, username, password, cluster_name, component_name, component_host)):
                        add_component_to_host(ambari_url, username, password, cluster_name, component_name, component_host)
                request = set_component_state(ambari_url, username, password, cluster_name, component_name, component_host, desired_state)
            else:
                request = set_service_state(ambari_url,username,password,cluster_name,service_name, desired_state)
            if wait_for_complete:
                try:
                    request_id = json.loads(request.content)['Requests']['id']
                except ValueError:
                    module.exit_json(changed=True, results=request.content)
                status = wait_for_request_complete(ambari_url, username, password, cluster_name, request_id, 2)
                if status != 'COMPLETED':
                    module.fail_json(msg="Request failed with status {0}".format(status))
            module.exit_json(changed=True, results=request.content)

        elif state == 'deleted':
            if component_mode:
                request = delete_component(ambari_url, username, password, cluster_name, component_name, component_host)
            else:
                request = delete_service(ambari_url,username,password,cluster_name,service_name)
            module.exit_json(changed=True, results=request.content)

    except requests.ConnectionError, e:
        module.fail_json(msg="Could not connect to Ambari client: " + str(e.message))
    except Exception, e:
        module.fail_json(msg="Ambari client exception occurred: " + str(e.message))


def get_clusters(ambari_url, user, password):
    r = get(ambari_url, user, password, '/api/v1/clusters')
    if r.status_code != 200:
        msg = 'Could not get cluster list: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    clusters = json.loads(r.content)
    return clusters['items']


def cluster_exists(ambari_url, user, password, cluster_name):
    clusters = get_clusters(ambari_url, user, password)
    return cluster_name in [item['Clusters']['cluster_name'] for item in clusters]


def get_request_status(ambari_url, user, password, cluster_name, request_id):
    path = '/api/v1/clusters/{0}/requests/{1}'.format(cluster_name, request_id)
    r = get(ambari_url, user, password, path)
    if r.status_code != 200:
        msg = 'Could not get cluster request status: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    service = json.loads(r.content)
    return service['Requests']['request_status']


def wait_for_request_complete(ambari_url, user, password, cluster_name, request_id, sleep_time):
    while True:
        status = get_request_status(ambari_url, user, password, cluster_name, request_id)
        if status == 'COMPLETED':
            return status
        elif status in ['FAILED', 'TIMEDOUT', 'ABORTED', 'SKIPPED_FAILED']:
            return status
        else:
            time.sleep(sleep_time)


def set_service_state(ambari_url, user, password, cluster_name, service_name, desired_state):
    path = '/api/v1/clusters/{0}/services/{1}'.format(cluster_name,service_name)
    request = {"RequestInfo": {"context": "Setting {0} to {1} via REST".format(service_name,desired_state)},
               "Body": {"ServiceInfo": {"state": "{0}".format(desired_state)}}}
    payload = json.dumps(request)
    r = put(ambari_url, user, password, path, payload)
    if r.status_code not in [202, 200]:
        msg = 'Could not set service state: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    return r


def set_component_state(ambari_url, user, password, cluster_name, component_name, component_host, desired_state):
    path = '/api/v1/clusters/{0}/hosts/{1}/host_components/{2}'.format(cluster_name,component_host,component_name)
    request = {"RequestInfo": {"context": "Setting {0} to {1} via REST".format(component_name,desired_state)},
               "Body": {"HostRoles": {"state": "{0}".format(desired_state)}}}
    payload = json.dumps(request)
    r = put(ambari_url, user, password, path, payload)
    if r.status_code not in [202, 200]:
        msg = 'Could not set component state: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    return r


def delete_component(ambari_url, user, password, cluster_name, component_name, component_host):
    enable_maint_mode(ambari_url, user, password, cluster_name, component_name, component_host)
    path = '/api/v1/clusters/{0}/hosts/{1}/host_components/{2}'.format(cluster_name,component_host,component_name)
    r = delete(ambari_url,user,password,path)
    if r.status_code not in [202, 200]:
        msg = 'Could not set service state: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    return r


def enable_maint_mode(ambari_url, user, password, cluster_name, component_name, component_host):
    path = '/api/v1/clusters/{0}/hosts/{1}/host_components/{2}'.format(cluster_name,component_host,component_name)
    request = {"RequestInfo":{"context":"Turn On Maintenance Mode for {0}".format(component_name)},
               "Body":{"HostRoles":{"maintenance_state":"ON"}}}
    payload = json.dumps(request)
    r = put(ambari_url, user, password, path, payload)
    if r.status_code not in [202, 200]:
        msg = 'Could not set maintenance mode: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    return r


def delete_service(ambari_url, user, password, cluster_name, service_name):
    path = '/api/v1/clusters/{0}/services/{1}'.format(cluster_name,service_name)
    r = delete(ambari_url,user,password,path)
    if r.status_code not in [202, 200]:
        msg = 'Could not delete service: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise Exception(msg)
    return r


def add_component_to_host(ambari_url, user, password, cluster_name, component_name, component_host):
    path = '/api/v1/clusters/{0}/hosts/{1}/host_components/{2}'.format(cluster_name,component_host,component_name)
    r = post(ambari_url, user, password, path,'')
    if r.status_code not in [202,201,200]:
        msg = 'Could not add {0} to host {1}: request code {2}, \
                    request message {3}'.format(component_name,component_host,r.status_code, r.content)
        raise Exception(msg)
    return r


def can_add_component(ambari_url, user, password, cluster_name, component_name, component_host):
    path = '/api/v1/clusters/{0}/hosts/{1}/host_components/{2}'.format(cluster_name,component_host,component_name)
    r = get(ambari_url, user, password, path)
    return r.status_code == 404


def get(ambari_url, user, password, path):
    r = requests.get(ambari_url + path, auth=(user, password))
    return r


def put(ambari_url, user, password, path, data):
    headers = {'X-Requested-By': 'ambari'}
    r = requests.put(ambari_url + path, data=data, auth=(user, password), headers=headers)
    return r


def post(ambari_url, user, password, path, data):
    headers = {'X-Requested-By': 'ambari'}
    r = requests.post(ambari_url + path, data=data, auth=(user, password), headers=headers)
    return r


def delete(ambari_url, user, password, path):
    headers = {'X-Requested-By': 'ambari'}
    r = requests.delete(ambari_url + path, auth=(user, password), headers=headers)
    return r


from ansible.module_utils.basic import *
if __name__ == '__main__':
    main()
