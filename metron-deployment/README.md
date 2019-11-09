<!--
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
-->

This project contains tools for building, packaging, and deploying Apache Metron.  Please refer to the following sections for more information on how to get Apache Metron running in your environment.

 - [How do I deploy Metron with Ambari?](#how-do-i-deploy-metron-with-ambari)
 - [How do I deploy Metron on a single VM?](#how-do-i-deploy-metron-on-a-single-vm)
 - [How do I build RPM packages?](#how-do-i-build-rpm-packages)
 - [How do I build DEB packages?](#how-do-i-build-deb-packages)
 - [How do I deploy Metron within AWS?](#how-do-i-deploy-metron-within-aws)
   - [AWS Single Node Cluster Deployment Using Vagrant](#aws-single-node-cluster-deployment-using-vagrant)
   - [AWS Single Node Cluster Deployment Using an AMI](#aws-single-node-cluster-deployment-using-an-ami)
   - [AWS 10 Node Cluster Deployment](#aws-10-node-cluster-deployment)
 - [How do I build Metron with Docker?](#how-do-i-build-metron-with-docker)


How do I deploy Metron with Ambari?
-----------------------------------

This provides a Management Pack (MPack) extension for [Apache Ambari](https://ambari.apache.org/) that simplifies the provisioning, management and monitoring of Metron on clusters of any size.

This allows you to easily install Metron using a simple, guided process.  This also allows you to monitor cluster health and even secure your cluster with kerberos.

#### What is this good for?

* If you want to see how Metron can really scale by deploying it on your own hardware, or even in the cloud, this is the best option for you.

* If you want to run a proof-of-concept to see how Apache Metron can benefit your organization, then this is the way to do it.

#### How?

To deploy Apache Metron using Ambari, follow the instructions at [packaging/ambari/metron-mpack](packaging/ambari/metron-mpack).


How do I deploy Metron on a single VM?
--------------------------------------

This will deploy Metron and all of its dependencies on a virtual machine running on your computer.

#### What is this good for?

* If you are new to Metron and want to explore the functionality that it offers, this is good place to start.

* If you are a developer contributing to the Apache Metron project, this is also a great way to test your changes.

#### What is this **not** good for?

* This VM is **not** intended for processing anything beyond the most basic, low volume work loads.

* Additional services should **not** be installed along side Metron in this VM.

* This VM should **not** be used to run a proof-of-concept for Apache Metron within your organization.

Running Metron within the resource constraints of a single VM is incredibly challenging. Failing to respect this warning, will cause various services to fail mysteriously as the system runs into memory and processing limits.

#### How?

To deploy Metron in a VM running on your computer, follow the instructions at [development/centos6](development/centos6).

#### How do I address services crashing when running Metron on a single VM?

We recommend looking at Ambari and shutting down any services you may not be using. For example, we recommend turning off Metron Profiler, as this commonly causes REST services to crash when running on a single VM.

### Parser Aggregation Feature

The [Parser Aggregation](../metron-platform/metron-parsing/metron-parsing-storm#parser-aggregation) feature does not currently exist in the management UI. In order to address resource limitations in the full dev development environments, bro, yaf, and snort have been aggregated into a single parser
topology. However, the Management UI is not currently able to display its status until the feature is added. Aggregated parsers can still be created via Ambari and the command line scripts.

Here are some tips for working with parser aggregation while the UI feature is being developed.

* **How are parsers picked up by the UI?:** This is based entirely on what is currently stored in the Zookeeper configs. See [Management Utility](../metron-platform/metron-common#management-utility) "DUMP" option with "-c PARSER" to see all of what is currently loaded. The management UI does not
update the configurations stored locally on disk, so Zookeeper is the source of truth.

* **Removing an existing aggregation:** In the [Ambari UI](http://node1:8080) click on the Metron serice and select "Metron Parsers." Select "stop" from the dropdown for the parser component. Click "back," "configs," and then navigate to "Parsers." In the text field option labeled "parsers".
Remove the double quotes from around the listed parsers. Save and choose "Restart" when prompted. This will deploy three individual parsers rather than a single aggregated parser: bro, snort, and yaf. Be aware, you may need to shut down other topologies to free up resources so that you can
run the parsers without aggregation. Stopping the profiler, pcap, or batch_indexing are a few options that will still allow data to pass through the system end-to-end.

* **Managing parser lifecycle:** Starting and stopping parsers in the management UI will in no way affect a parser running as aggregated. The exception to this is if you create a parser via the management UI that has the same name as the
aggregation, e.g. "bro__snort__yaf." We recommend against this. It will appear as thought you now have the ability to manage the aggregated parser now, but you will only be able to start/stop it.

* **Editing parser configuration:** In order to modify the aggregated parsers' configurations, you will need to first pull all of the configuration from Zookeeper to the local configuration directory by executing the following
commands

    ```
    source /etc/default/metron
    $METRON_HOME/bin/zk_load_configs.sh -m PULL -o ${METRON_HOME}/config/zookeeper -z $ZOOKEEPER -f
    ```

    Make your changes to an individual parser's configuration json, e.g. `${METRON_HOME}/config/zookeeper/parsers/bro.json`, save locally, and then push them back up to Zookeeper

    ```
    $METRON_HOME/bin/zk_load_configs.sh -m PUSH -i $METRON_HOME/config/zookeeper/ -z $ZOOKEEPER
    ```

    See [Management Utility](../metron-platform/metron-common#management-utility) for more detail.

* **Other gotchas:** Stop the aggregated parsers in Ambari before removing or adding grouping quotes. Otherwise, you will end up with both the individual parsers and the aggregated topology running concurrently. Ambari only manages the parser topology lifecycle via the current parser name list provided,
so changing that list removes Ambari's ability to reference the old topology names.

How do I build RPM packages?
----------------------------

This provides RPM packages that allow you to install Metron on an RPM-based operating system like CentOS.

#### What is this good for?

* If you want to manually install Apache Metron on an RPM-based system like CentOS, installation can be simplified by using these packages.

* If you want a guided installation process using Ambari on an RPM-based system, then these RPMs are a necessary prerequisite.

#### What is this **not** good for?

* If you want a complete, guided installation process, use Ambari rather than just these packages.  Installing Metron using **only** these RPMs still leaves a considerable amount of configuration necessary to get Metron running.  Installing with Ambari automates these additional steps.

#### How?

To build the RPM packages, follow the instructions at [packaging/docker/rpm-docker](packaging/docker/rpm-docker).


How do I build DEB packages?
-------------------------------

This builds installable DEB packages that allow you to install Metron on an APT-based operating system like Ubuntu.

#### What is this good for?

* If you want to manually install Metron on a APT-based system like Ubuntu, installation can be simplified by using these packages.

* If you want a guided installation process using Ambari on an APT-based system, then these DEBs are a necessary prerequisite.

#### What is this **not** good for?

* If you want a complete, guided installation process, use Ambari rather than just these packages.  Installing Metron using **only** these RPMs still leaves a considerable amount of configuration necessary to get Metron running.  Installing with Ambari automates these additional steps.

#### How?

To build the DEB packages, follow the instructions at [packaging/docker/deb-docker](packaging/docker/deb-docker).


How do I deploy Metron within AWS?
----------------------------------
You can deploy Metron into Amazon Web Service(AWS) in three ways:
i) [As a single node using Vagrant](#aws-single-node-cluster-deployment-using-vagrant)
ii) [As single node using an AMI from the AWS Community AMI Marketplace](#aws-single-node-cluster-deployment-using-an-ami)
iii) [As a 10-node cluster](#aws-10-node-cluster-deployment)

Below will provide more information on the three different deployment methods. 
Warning: Amazon will charge for the use of their resources when running Apache Metron.  The amount will vary based on the number and size of hosts, along with current Amazon pricing structure.  Be sure to stop or terminate all of the hosts instantiated by Apache Metron when not in use to avoid unnecessary charges.


### AWS Single Node Cluster Deployment Using Vagrant
This will deploy Metron and all of its dependencies as a single node in Amazon Web Service's EC2 platform using Vagrant. 

#### What is this good for?

* If you are new to Metron and want to explore the functionality that it offers, this is good place to start.  

* If you are a developer contributing to the Apache Metron project, this is also a great way to test your changes.  

* The single node will survive a reboot.

* The single node can use a pre-existing AWS elastic ip, security group id, and subnet id.

#### What is this **not** good for?

* This single node is **not** intended for processing anything beyond the most basic, low volume work loads.

* This single node is **not** intended for processing anything beyond the most basic, low volume work loads.

* Additional services should **not** be installed along side Metron in this VM.

* This single node should **not** be used to run a proof-of-concept for Apache Metron within your organization.

* You might need to run the Vagrant file in Mac OS and have install prerequisites installed properly 

Running Metron within the resource constraints of a single VM is incredibly challenging. Failing to respect this warning, will cause various services to fail mysteriously as the system runs into memory and processing limits.

#### How?

To deploy Metron in EC2 as a single node using Vagrant, follow the instructions at [development/aws-centos6](development/aws-centos6).


### AWS Single Node Cluster Deployment Using an AMI
This will deploy Metron as a single node in Amazon Web Service's EC2 platform by using existing Amazon Machine Image (AMI) that can be found in the AWS Community AMI Marketplace. 

#### What is this good for?

* This is intended to be the simplest EC2 AWS deployment option

* No need for preinstall requriments on a seperate machine

* If you are new to Metron and want to explore the functionality that it offers, this is good place to start.  

* The single node will survive a reboot.

* The single node can use your pre-existing AWS infrastructure settings (example: security group, keys ext..)

* If you are a developer contributing to the Apache Metron project, you will see your changes if making modifications after deployment

#### What is this **not** good for?

* This single node is **not** intended for processing anything beyond the most basic, low volume work loads.

* Additional services should **not** be installed along side Metron in this VM.

* This single node should **not** be used to run a proof-of-concept for Apache Metron within your organization.

* You might need to run the Vagrant file in Mac OS and have install prerequisites installed properly 

Running Metron within the resource constraints of a single VM is incredibly challenging. Failing to respect this warning, will cause various services to fail mysteriously as the system runs into memory and processing limits.

#### How?
1) Navigate to the [web page for this AMI](https://aws.amazon.com/marketplace/pp/B00NQAYLWO) and "Accept Software Terms" for the "Manual Launch" tab.
2) In the "EC2 Dashboard" click on "Launch Instance" in the "Canada (Central)" region
3) Search for "GCR-Xetron Demo" or "ami-93cb4ff7" in the "Community AMIs" and click on "Select"
4) Manually choose the following mandatory/non-default option and then "Launch Instance"

- t2.t2xlarge [32 GB Memory, 8 Cores, 64 bit]

5) Change security group setting to only allow traffic to what is necessary. By default a new security group might block all inbound traffic except SSH. Inbound and outbound ports 8080, 5000, 4200 ext.. will need to be allowed for your web client.
6) (optional)Associate the newly launched instance to an elastic IP
7) After the instance is launched you will need to change the hostname to node1. 

SSH into the machine using your \*.pem key
```
ssh -i "<file>.pem" centos@<elastic_ip>
```

Update the /etc/sysconfig/network file to look like the following
```
sudo sed -i "s/^HOSTNAME=.*/HOSTNAME=node1/g" /etc/sysconfig/network
```
8) Reboot the instance
9) Go to the Ambari dashboard (http://<elasticip>:8080) and select "Start All" services in the left dropdown menu

### AWS Single Node Cluster Deployment
This will deploy Metron and all of its dependencies on a single node in Amazon Web Service's EC2 platform. 

#### What is this good for?

* If you are new to Metron and want to explore the functionality that it offers, this is good place to start.  

* If you are a developer contributing to the Apache Metron project, this is also a great way to test your changes.  

* The single node will survive a reboot.

* The single node can use a pre-existing AWS elastic ip, security group id, and subnet id.

#### What is this **not** good for?

* This VM is **not** intended for processing anything beyond the most basic, low volume work loads.

* Additional services should **not** be installed along side Metron in this VM.

* This VM should **not** be used to run a proof-of-concept for Apache Metron within your organization.

Running Metron within the resource constraints of a single VM is incredibly challenging. Failing to respect this warning, will cause various services to fail mysteriously as the system runs into memory and processing limits.

#### How?

To deploy Metron in a VM running on your computer, follow the instructions at [development/aws-centos6](development/aws-centos6).



### AWS 10 Node Cluster Deployment

This deploys Apache Metron on an automatically provisioned 10-node cluster running in Amazon Web Service's EC2 platform.

This installs real sources of telemetry like Bro, Snort, and YAF, but feeds those sensors with canned pcap data.

#### What is this good for?

* If you are a Metron developer wanting to test at-scale on a multi-node cluster, then this is the right option for you.

#### What is this **not** good for?

* If you want to run Metron in AWS with real data for either testing or production, then this is NOT the right option for you.

* **WARNING** This is only intended for creating an ephemeral cluster for brief periods of testing.  This deployment method has the following severe limitations.
    * The cluster is not secured in any way. It is up to you to manually secure it.
    * The cluster will not survive a reboot.

#### How?

Follow the instructions available at [amazon-ec2](amazon-ec2).


How do I build Metron with Docker?
----------------------------------

This provides a Docker containing all of the prerequisites required to build Metron.  This allows you to easily build Metron without installing all of the build dependencies manually.

#### What is this good for?

* If you want to build Metron, but do not want to manually install all of the build dependencies, then this is a good option.

#### How?

Follow the instructions available at [packaging/docker/ansible-docker](packaging/docker/ansible-docker).
