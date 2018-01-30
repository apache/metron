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
ii) [As single node using an AMI from the AWS Marketplace](#aws-single-node-cluster-deployment-using-an-ami)
iii) [As a 10-node cluster](#aws-10-node-cluster-deployment)

Below will provide more information on the three different deployment methods

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
This will deploy Metron as a single node in Amazon Web Service's EC2 platform by using existing Amazon Machine Image (AMI) that can be found in the AWS Marketplace. 

#### What is this good for?

* This is intended to be the simplest EC2 AWS deployment option

* No need to have a separate machine to deploy

* No need for preinstall requriments

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

1) In the "EC2 Dashboard" click on "Launch Instance" in the "Canada (Central)" region
2) Search for "GCR-Xetron Demo" or "ami-93cb4ff7" in the "AWS Marketplace" and click on "Select"
3) Manually choose the following mandatory non-default options
t2.t2xlarge
4) Launch the instance
6) Change security group setting to only allow traffic to what is necessary
5) Associate the newly launched instance to an elastic IP(optional)
6) After the image is launched you will need to change the /etc/hosts file. 

SSH into the machine using your \*.pem key
```
ssh -i "<file>.pem" centos@<elastic_ip>
```

Update the /etc/hosts file to look like the following
```
127.0.0.1 localhost node
```
7) Restart the instance
8) Go to the following to see the Metron dashboard 
http://<elasticip>:5000

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
