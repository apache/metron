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


class ENRICHMENT020BETAServiceAdvisor(service_advisor.ServiceAdvisor):
    # colocate Metron Parser Master with KAFKA_BROKERs
    def colocateService(self, hostsComponentsMap, serviceComponents):
        enrichmentMasterComponent = [component for component in serviceComponents if
                                   component["StackServiceComponents"]["component_name"] == "ENRICHMENT_MASTER"]
        enrichmentMasterComponent = enrichmentMasterComponent[0]
        if not self.isComponentHostsPopulated(enrichmentMasterComponent):
            for hostName in hostsComponentsMap.keys():
                hostComponents = hostsComponentsMap[hostName]
                if ({"name": "KAFKA_BROKER"} in hostComponents) and {"name": "ENRICHMENT_MASTER"} not in hostComponents:
                    hostsComponentsMap[hostName].append({"name": "ENRICHMENT_MASTER"})
                if ({"name": "KAFKA_BROKER"} not in hostComponents) and {"name": "ENRICHMENT_MASTER"} in hostComponents:
                    hostsComponentsMap[hostName].remove({"name": "ENRICHMENT_MASTER"})

    def getServiceComponentLayoutValidations(self, services, hosts):
        componentsListList = [service["components"] for service in services["services"]]
        componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]

        enrichmentHosts = self.getHosts(componentsList, "ENRICHMENT_MASTER")
        brokerHosts = self.getHosts(componentsList, "KAFKA_BROKER")

        items = []

        mismatchHosts = sorted(set(enrichmentHosts).symmetric_difference(set(brokerHosts)))
        if len(mismatchHosts) > 0:
            hostsString = ', '.join(mismatchHosts)
            message = "Metron Enrichment Master must be installed on Kafka Brokers. " \
                      "The following {0} host(s) do not satisfy the colocation recommendation: {1}".format(len(mismatchHosts), hostsString)
            items.append(
                {"type": 'host-component', "level": 'WARN', "message": message, "component-name": 'ENRICHMENT_MASTER'})

        return items
