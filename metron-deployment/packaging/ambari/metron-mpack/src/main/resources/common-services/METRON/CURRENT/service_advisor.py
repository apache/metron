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
import os
import traceback
import sys
import imp

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
    with open(PARENT_FILE, 'rb') as fp:
        service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
    traceback.print_exc()
    print "Failed to load parent"

class METRON${metron.short.version}ServiceAdvisor(service_advisor.ServiceAdvisor):

    def getServiceComponentLayoutValidations(self, services, hosts):

        componentsListList = [service["components"] for service in services["services"]]
        componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]

        metronParsersHost = self.getHosts(componentsList, "METRON_PARSERS")[0]
        metronEnrichmentMaster = self.getHosts(componentsList, "METRON_ENRICHMENT_MASTER")[0]
        metronProfilerHost = self.getHosts(componentsList, "METRON_PROFILER")[0]
        metronPcapHost = self.getHosts(componentsList, "METRON_PCAP")[0]
        metronIndexingHost = self.getHosts(componentsList, "METRON_INDEXING")[0]
        metronRESTHost = self.getHosts(componentsList, "METRON_REST")[0]
        metronManagementUIHost = self.getHosts(componentsList, "METRON_MANAGEMENT_UI")[0]
        metronAlertsUIHost = self.getHosts(componentsList, "METRON_ALERTS_UI")[0]

        hbaseClientHosts = self.getHosts(componentsList, "HBASE_CLIENT")
        hdfsClientHosts = self.getHosts(componentsList, "HDFS_CLIENT")
        zookeeperClientHosts = self.getHosts(componentsList, "ZOOKEEPER_CLIENT")

        kafkaBrokers = self.getHosts(componentsList, "KAFKA_BROKER")
        stormSupervisors = self.getHosts(componentsList, "SUPERVISOR")

        items = []

        # Metron Must Co-locate with KAFKA_BROKER and STORM_SUPERVISOR
        if metronParsersHost not in kafkaBrokers:
            message = "Metron must be colocated with an instance of KAFKA BROKER"
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_PARSERS', "host": metronParsersHost })

        if metronParsersHost not in stormSupervisors:
            message = "Metron must be colocated with an instance of STORM SUPERVISOR"
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_PARSERS', "host": metronParsersHost })

        if metronRESTHost not in stormSupervisors:
            message = "Metron REST must be colocated with an instance of STORM SUPERVISOR"
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_REST', "host": metronRESTHost })

        if metronParsersHost !=  metronRESTHost:
            message = "Metron REST must be co-located with Metron Parsers on {0}".format(metronParsersHost)
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_REST', "host": metronRESTHost })

        if metronParsersHost != metronEnrichmentMaster:
            message = "Metron Enrichment Master must be co-located with Metron Parsers on {0}".format(metronParsersHost)
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_ENRICHMENT_MASTER', "host": metronEnrichmentMaster })

        if metronParsersHost != metronIndexingHost:
            message = "Metron Indexing must be co-located with Metron Parsers on {0}".format(metronParsersHost)
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_INDEXING', "host": metronIndexingHost })

        if metronParsersHost != metronPcapHost:
            message = "Metron PCAP must be co-located with Metron Parsers on {0}".format(metronParsersHost)
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_PCAP', "host": metronPcapHost })

        if metronParsersHost != metronProfilerHost:
            message = "Metron Profiler must be co-located with Metron Parsers on {0}".format(metronParsersHost)
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_PROFILER', "host": metronProfilerHost })

        # Enrichment Master also needs ZK Client, but this is already guaranteed by being colocated with Parsers Master
        if metronParsersHost not in zookeeperClientHosts:
            message = "Metron must be co-located with an instance of Zookeeper Client"
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_PARSERS', "host": metronParsersHost })

            # Enrichment Master also needs HDFS clients, but this is already guaranteed by being colocated with Parsers Master
        if metronParsersHost not in hdfsClientHosts:
            message = "Metron must be co-located with an instance of HDFS Client"
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_PARSERS', "host": metronParsersHost })

        if metronEnrichmentMaster not in hbaseClientHosts:
            message = "Metron Enrichment Master must be co-located with an instance of HBase Client"
            items.append({ "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'METRON_ENRICHMENT_MASTER', "host": metronEnrichmentMaster })

        if metronManagementUIHost != metronAlertsUIHost:
            message = "Metron Alerts UI must be co-located with Metron Management UI on {0}".format(metronManagementUIHost)
            items.append({ "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'METRON_ALERTS_UI', "host": metronAlertsUIHost })

        return items

    def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):

        # validate recommended properties in storm-site
        siteName = "storm-site"
        method = self.validateSTORMSiteConfigurations
        items = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)

        return items

    def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
        is_secured = self.isSecurityEnabled(services)

        #Suggest Storm Rest URL
        if "storm-site" in services["configurations"]:
            stormUIServerHost = self.getComponentHostNames(services, "STORM", "STORM_UI_SERVER")[0]
            stormUIServerPort = services["configurations"]["storm-site"]["properties"]["ui.port"]
            stormUIProtocol = "http://"
            if "ui.https.port" in services["configurations"]["storm-site"]["properties"]:
                stormUIServerPort = services["configurations"]["storm-site"]["properties"]["ui.https.port"]
                stormUIProtocol = "https://"
            stormUIServerURL = stormUIProtocol + stormUIServerHost + ":" + stormUIServerPort
            putMetronEnvProperty = self.putProperty(configurations, "metron-env", services)
            putMetronEnvProperty("storm_rest_addr",stormUIServerURL)

            storm_site = services["configurations"]["storm-site"]["properties"]
            putStormSiteProperty = self.putProperty(configurations, "storm-site", services)

            for property, desired_value in self.getSTORMSiteDesiredValues(is_secured).iteritems():
                if property not in storm_site:
                    putStormSiteProperty(property, desired_value)
                elif  property == "topology.classpath" and storm_site[property] != desired_value:
                    topologyClasspath = storm_site[property]
                    #check that desired values exist in topology.classpath. append them if they do not
                    for path in desired_value.split(':'):
                        if path not in topologyClasspath:
                            topologyClasspath += ":" + path
                    putStormSiteProperty(property,topologyClasspath)

        #Suggest Zeppelin Server URL
        if "zeppelin-config" in services["configurations"]:
            zeppelinServerHost = self.getComponentHostNames(services, "ZEPPELIN", "ZEPPELIN_MASTER")[0]
            zeppelinServerPort = services["configurations"]["zeppelin-config"]["properties"]["zeppelin.server.port"]
            zeppelinServerUrl = zeppelinServerHost + ":" + zeppelinServerPort
            putMetronEnvProperty = self.putProperty(configurations, "metron-env", services)
            putMetronEnvProperty("zeppelin_server_url", zeppelinServerUrl)

        #Suggest Zookeeper quorum
        if "solr-cloud" in services["configurations"]:
            zookeeperHost = self.getComponentHostNames(services, "ZOOKEEPER", "ZOOKEEPER_SERVER")[0]
            zookeeperClientPort = services["configurations"]["zoo.cfg"]["properties"]["clientPort"]
            solrZkDir = services["configurations"]["solr-cloud"]["properties"]["solr_cloud_zk_directory"]
            solrZookeeperUrl = zookeeperHost + ":" + zookeeperClientPort + solrZkDir
            putMetronEnvProperty = self.putProperty(configurations, "metron-env", services)
            putMetronEnvProperty("solr_zookeeper_url", solrZookeeperUrl)


    def validateSTORMSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
        # Determine if the cluster is secured
        is_secured = self.isSecurityEnabled(services)

        storm_site = properties
        validationItems = []

        for property, desired_value in self.getSTORMSiteDesiredValues(is_secured).iteritems():
            if property not in storm_site :
                message = "Metron requires this property to be set to the recommended value of " + desired_value
                item = self.getErrorItem(message) if property == "topology.classpath" else self.getWarnItem(message)
                validationItems.append({"config-name": property, "item": item})
            elif  storm_site[property] != desired_value:
                topologyClasspath = storm_site[property]
                for path in desired_value.split(':'):
                    if path not in topologyClasspath:
                        message = "Metron requires this property to contain " + desired_value
                        item = self.getErrorItem(message)
                        validationItems.append({"config-name": property, "item": item})

        return self.toConfigurationValidationProblems(validationItems, "storm-site")

    def getSTORMSiteDesiredValues(self, is_secured):

        storm_site_desired_values = {
            "topology.classpath" : "/etc/hbase/conf:/etc/hadoop/conf"
        }

        return storm_site_desired_values

    # need to override this method from ServiceAdvisor to work around https://issues.apache.org/jira/browse/AMBARI-25375
    def colocateService(self, hostsComponentsMap, serviceComponents):
        self.validateMetronComponentLayout(hostsComponentsMap, serviceComponents)


    # This method is added as work around for https://issues.apache.org/jira/browse/AMBARI-25375
    # Note: This only affects the recommendations in Ambari -
    # users can still modify the hosts for the components as they see fit
    def validateMetronComponentLayout(self, hostsComponentsMap, serviceComponents):
        metronComponents = [
            {'name': 'METRON_ALERTS_UI'},
            {'name': 'METRON_CLIENT'},
            {'name': 'METRON_ENRICHMENT_MASTER'},
            {'name': 'METRON_INDEXING'},
            {'name': 'METRON_MANAGEMENT_UI'},
            {'name': 'METRON_PARSERS'},
            {'name': 'METRON_PCAP'},
            {'name': 'METRON_PROFILER'},
            {'name': 'METRON_REST'}
        ]

        # find any metron components that have not been assigned to a host
        unassignedMetronComponents = []
        for component in metronComponents:
          if (component not in hostsComponentsMap.values()):
            unassignedMetronComponents.append(component)

        if len(unassignedMetronComponents) > 0:
          # assign each unassigned metron component to a host
          default_host = self.getDefaultHost(metronComponents, hostsComponentsMap)
          for unassigned in unassignedMetronComponents:
              self.logger.info("Anassigned component {0} to host {1}".format(unassigned, default_host))
              hostsComponentsMap[default_host].append(unassigned)


    def getDefaultHost(self, metronComponents, hostsComponentsMap):
        # fist, attempt to colocate metron; suggest a host where metron is already assigned
        default_host = None
        for component in metronComponents:
            if default_host is None:
                for host, hostComponents in hostsComponentsMap.items():
                    if component in hostComponents:
                      default_host = host
                      break

        # if there are no assigned metron components, just choose the first known host
        if default_host is None:
            default_host = hostsComponentsMap.keys()[0]
            self.logger.info("No hosts found with Metron components. Using first known host: host={0}".format(default_host))

        return default_host

