#!/usr/bin/env ambari-python-wrap
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
import imp
import os
import traceback

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
    with open(PARENT_FILE, 'rb') as fp:
        service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
    traceback.print_exc()
    print("Failed to load parent service_advisor file '{}'".format(PARENT_FILE))


class PARSERS020BETAServiceAdvisor(service_advisor.ServiceAdvisor):
    # colocate Metron Parser Master with KAFKA_BROKERs
    def TODO_colocateService(self, hostsComponentsMap, serviceComponents):
        parsersMasterComponent = [component for component in serviceComponents if
                                  component["StackServiceComponents"]["component_name"] == "PARSER_MASTER"][0]
        if not self.isComponentHostsPopulated(parsersMasterComponent):
            for hostName in hostsComponentsMap.keys():
                hostComponents = hostsComponentsMap[hostName]
                if ({"name": "KAFKA_BROKER"} in hostComponents) and {"name": "PARSER_MASTER"} not in hostComponents:
                    hostsComponentsMap[hostName].append({"name": "PARSER_MASTER"})
                if ({"name": "KAFKA_BROKER"} not in hostComponents) and {"name": "PARSER_MASTER"} in hostComponents:
                    hostsComponentsMap[hostName].remove({"name": "PARSER_MASTER"})

    def TODO_getServiceComponentLayoutValidations(self, services, hosts):
        componentsListList = [service["components"] for service in services["services"]]
        componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
        hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
        hostsCount = len(hostsList)

        masterHosts = self.getHosts(componentsList, "PARSER_MASTER")
        expectedMasterHosts = set(self.getHosts(componentsList, "KAFKA_BROKER"))

        items = []

        mismatchHosts = sorted(expectedMasterHosts.symmetric_difference(set(masterHosts)))
        if len(mismatchHosts) > 0:
            hostsString = ', '.join(mismatchHosts)
            message = "Metron Parsers Master must be installed on Kafka Brokers. " \
                      "The following {0} host(s) do not satisfy the colocation recommendation: {1}".format(
                len(mismatchHosts), hostsString)
            items.append(
                {"type": 'host-component', "level": 'WARN', "message": message, "component-name": 'PARSER_MASTER'})

        return items
