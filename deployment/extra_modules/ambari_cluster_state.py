#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Author: Mark Bittmann (https://github.com/mbittmann)
# Documentation section
DOCUMENTATION = '''
---
module: ambari_cluster_state
version_added: "1.0"
author: Mark Bittmann (https://github.com/mbittmann)
short_description: Create, delete, start or stop an ambari cluster
  - Create, delete, start or stop an ambari cluster
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
  cluster_state:
    description:
      The desired state for the ambari cluster ['present', 'absent', 'started', 'stopped']. Setting the cluster
      state to absent will first stop the cluster.
    required: yes
  blueprint_yml:
    description:
      The path to the file defining the cluster blueprint and host mapping. Required when state == 'present'
    required: no
  blueprint_name:
    description:
      The name of the blueprint. Required when state == 'present'
    required: no
  wait_for_complete:
    description:
      Whether to wait for the request to complete before returning. Default is False.
    required: no
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
      blueprint_yml: roles/my_role/files/blueprint.yml
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
__author__ = 'mbittmann'
from ansible.module_utils.basic import *
import json
import os
try:
    import requests
except ImportError:
    REQUESTS_FOUND = False
else:
    REQUESTS_FOUND = True

try:
    import yaml
except ImportError:
    YAML_FOUND = False
else:
    YAML_FOUND = True

def main():

    argument_spec = dict(
        host=dict(type='str', default=None, required=True),
        port=dict(type='int', default=None, required=True),
        username=dict(type='str', default=None, required=True),
        password=dict(type='str', default=None, required=True),
        cluster_name=dict(type='str', default=None, required=True),
        cluster_state=dict(type='str', default=None, required=True,
                           choices=['present', 'absent', 'started', 'stopped']),
        blueprint_var=dict(type='dict', required=False),
        blueprint_name=dict(type='str', default=None, required=False),
        wait_for_complete=dict(default=False, required=False, choices=BOOLEANS),
    )

    module = AnsibleModule(
        argument_spec=argument_spec
    )

    if not REQUESTS_FOUND:
        module.fail_json(
            msg='requests library is required for this module')

    if not YAML_FOUND:
        module.fail_json(
            msg='pyYaml library is required for this module')

    p = module.params

    host = p.get('host')
    port = p.get('port')
    username = p.get('password')
    password = p.get('password')
    cluster_name = p.get('cluster_name')
    cluster_state = p.get('cluster_state')
    blueprint_name = p.get('blueprint_name')
    wait_for_complete = p.get('wait_for_complete')

    ambari_url = 'http://{0}:{1}'.format(host, port)

    try:
        if cluster_state in ['started', 'stopped']:
            if not cluster_exists(ambari_url, username, password, cluster_name):
                module.fail_json(msg="Cluster name {0} does not exist".format(cluster_name))
            state = ''
            if cluster_state == 'started':
                state = 'STARTED'
            elif cluster_state == 'stopped':
                state = 'INSTALLED'

            request = set_cluster_state(ambari_url, username, password, cluster_name, state)
            if wait_for_complete:
                request_id = json.loads(request.content)['Requests']['id']
                status = wait_for_request_complete(ambari_url, username, password, cluster_name, request_id, 2)
                if status != 'COMPLETED':
                    module.fail_json(msg="Request failed with status {0}".format(status))
            module.exit_json(changed=True, results=request.content)
        elif cluster_state == 'absent':
            if not cluster_exists(ambari_url, username, password, cluster_name):
                module.exit_json(changed=False, msg='Skipping. Cluster does not exist')
            if not can_delete_cluster(ambari_url, username, password, cluster_name):
                request = set_cluster_state(ambari_url, username, password, cluster_name, 'INSTALLED')
                request_id = json.loads(request.content)['Requests']['id']
                status = wait_for_request_complete(ambari_url, username, password, cluster_name, request_id, 2)
                if status != 'COMPLETED':
                    module.fail_json(msg="Request failed with status {0}".format(status))
            request = delete_cluster(ambari_url, username, password, cluster_name)
            module.exit_json(changed=True, results=request.content)
        elif cluster_state == 'present':
            if not p.get('blueprint_var') or not blueprint_name:  # have neither name nor file
                module.fail_json(msg="Must provide blueprint_var and blueprint_name when cluster_state=='present'")

            blueprint_var = p.get('blueprint_var')
            blueprint, host_map = blueprint_var_to_ambari_converter(blueprint_var)
            created_blueprint = False

            if not blueprint_exists(ambari_url, username, password, blueprint_name):
                create_blueprint(ambari_url, username, password, blueprint_name, blueprint)
                created_blueprint = True

            if cluster_exists(ambari_url, username, password, cluster_name):
                module.exit_json(changed=False, msg='Cluster {0} already exists'.format(cluster_name),
                                 created_blueprint=created_blueprint)

            request = create_cluster(ambari_url, username, password, cluster_name, blueprint_name, host_map)
            request_id = json.loads(request.content)['Requests']['id']
            if wait_for_complete:
                status = wait_for_request_complete(ambari_url, username, password, cluster_name, request_id, 2)
                if status != 'COMPLETED':
                    module.fail_json(msg="Request failed with status {0}".format(status))
            request_status = get_request_status(ambari_url, username, password, cluster_name, request_id)
            module.exit_json(changed=True, results=request.content,
                             created_blueprint=created_blueprint, status=request_status)


    except requests.ConnectionError as e:
        module.fail_json(msg="Could not connect to Ambari client: " + str(e.message))
    except AssertionError as e:
        module.fail_json(msg=e.message)
    except Exception as e:
        module.fail_json(msg="Ambari client exception occurred: " + str(e.message))


def get_clusters(ambari_url, user, password):
    r = get(ambari_url, user, password, '/api/v1/clusters')
    try:
        assert r.status_code == 200
    except AssertionError as e:
        e.message = 'Coud not get cluster list: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    clusters = json.loads(r.content)
    return clusters['items']


def cluster_exists(ambari_url, user, password, cluster_name):
    clusters = get_clusters(ambari_url, user, password)
    if cluster_name in [item['Clusters']['cluster_name'] for item in clusters]:
        return True
    else:
        return False


def set_cluster_state(ambari_url, user, password, cluster_name, cluster_state):
    path = '/api/v1/clusters/{0}/services'.format(cluster_name)
    request = {"RequestInfo": {"context": "Setting cluster state"},
               "Body": {"ServiceInfo": {"state": "{0}".format(cluster_state)}}}
    payload = json.dumps(request)
    r = put(ambari_url, user, password, path, payload)
    try:
        assert r.status_code == 202 or r.status_code == 200
    except AssertionError as e:
        e.message = 'Coud not set cluster state: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    return r


def create_cluster(ambari_url, user, password, cluster_name, blueprint_name, hosts_json):
    path = '/api/v1/clusters/{0}'.format(cluster_name)
    data = json.dumps({'blueprint': blueprint_name, 'host_groups': hosts_json})
    r = post(ambari_url, user, password, path, data)
    try:
        assert r.status_code == 202
    except AssertionError as e:
        e.message = 'Coud not create cluster: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    return r


def get_request_status(ambari_url, user, password, cluster_name, request_id):
    path = '/api/v1/clusters/{0}/requests/{1}'.format(cluster_name, request_id)
    r = get(ambari_url, user, password, path)
    try:
        assert r.status_code == 200
    except AssertionError as e:
        e.message = 'Coud not get cluster request status: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
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


def can_delete_cluster(ambari_url, user, password, cluster_name):
    path = '/api/v1/clusters/{0}/services?ServiceInfo/state=STARTED'.format(cluster_name)
    r = get(ambari_url, user, password, path)
    items = json.loads(r.content)['items']
    if len(items) > 0:
        return False
    else:
        return True


def get_blueprints(ambari_url, user, password):
    path = '/api/v1/blueprints'
    r = get(ambari_url, user, password, path)
    try:
        assert r.status_code == 200
    except AssertionError as e:
        e.message = 'Coud not get blueprint list: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    services = json.loads(r.content)
    return services['items']


def create_blueprint(ambari_url, user, password, blueprint_name, blueprint_data):
    data = json.dumps(blueprint_data)
    path = "/api/v1/blueprints/" + blueprint_name
    r = post(ambari_url, user, password, path, data)
    try:
        assert r.status_code == 201
    except AssertionError as e:
        e.message = 'Coud not create blueprint: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    return r


def blueprint_exists(ambari_url, user, password, blueprint_name):
    blueprints = get_blueprints(ambari_url, user, password)
    if blueprint_name in [item['Blueprints']['blueprint_name'] for item in blueprints]:
        return True
    else:
        return False


def delete_cluster(ambari_url, user, password, cluster_name):
    path = '/api/v1/clusters/{0}'.format(cluster_name)
    r = delete(ambari_url, user, password, path)
    try:
        assert r.status_code == 200
    except AssertionError as e:
        e.message = 'Coud not delete cluster: request code {0}, \
                    request message {1}'.format(r.status_code, r.content)
        raise
    return r


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


def blueprint_var_to_ambari_converter(blueprint_var):
    groups = blueprint_var['groups']
    new_groups = []
    host_map = []
    for group in groups:
        components = []
        for component in group['components']:
            components.append({'name': component})
        group['components'] = components
        hosts = group.pop('hosts')
        new_groups.append(group)
        this_host_map = dict()
        this_host_map['name'] = group['name']
        this_host_list = [{'fqdn': host} for host in hosts]
        this_host_map['hosts'] = this_host_list
        host_map.append(this_host_map)
    blueprint = dict()
    blueprint['host_groups'] = new_groups
    blueprint['Blueprints'] = {'stack_name': blueprint_var['stack_name'], 'stack_version': blueprint_var['stack_version']}

    return blueprint, host_map


if __name__ == '__main__':
    main()
