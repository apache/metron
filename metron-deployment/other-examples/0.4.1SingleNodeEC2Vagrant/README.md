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
Metron 0.4.1 - AWS EC2 Single Node Deployment with Vagrant 
==================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized node in AWS EC2. 

Warning: Amazon will charge for the use of their resources when running Apache Metron.  


### AWS Single Node Cluster Deployment Using Vagrant
This will deploy Metron and all of its dependencies as a single node in Amazon Web Service's EC2 platform using Vagrant. 

#### What is this good for?

* If you are new to Metron and want to explore the functionality that it offers, this is good place to start.  

* If you are a developer contributing to the Apache Metron project, this is also a great way to test your changes.  

* The single node will survive a reboot.

* The single node can use a pre-existing AWS elastic ip, security group id, and subnet id.

#### What is this **not** good for?

* This single node is **not** intended for processing anything beyond the most basic, low volume work loads.

* Additional services should **not** be installed along side Metron in this VM.

* This single node should **not** be used to run a proof-of-concept for Apache Metron within your organization.

* You might need to run the Vagrant file in Mac OS and have install prerequisites installed properly 

Running Metron within the resource constraints of a single VM is incredibly challenging. Failing to respect this warning, will cause various services to fail mysteriously as the system runs into memory and processing limits.





Getting Started
---------------
### Amazon Web Services

If you already have an Amazon Web Services account that you have used to deploy EC2 hosts, then you should be able to skip the next few steps.

1. Head over to [Amazon Web Services](http://aws.amazon.com/) and create an account.  As part of the account creation process you will need to provide a credit card to cover any charges that may apply.

2. Create a set of user credentials through [Amazon's Identity and Access Management (IAM) ](https://console.aws.amazon.com/iam/) dashboard.  On the IAM dashboard menu click "Users" and then "Create New User". Provide a name and ensure that "Generate an access key for each user" remains checked.  Download the credentials and keep them for later use.

3.  While still in [Amazon's Identity and Access Management (IAM) ](https://console.aws.amazon.com/iam/) dashboard, click on the user that was previously created.  Click the "Permissions" tab and then the "Attach Policy" button.  Attach the following policies to the user.

  - AmazonEC2FullAccess
  - AmazonVPCFullAccess



### Prerequisites
Apache Metron uses the [official, open source CentOS 6](https://aws.amazon.com/marketplace/pp/B00NQAYLWO) Amazon Machine Image (AMI).  If you have never used this AMI before then you will need to accept Amazon's terms and conditions. Navigate to the [web page for this AMI](https://aws.amazon.com/marketplace/pp/B00NQAYLWO) and "Accept Software Terms" for the "Manual Launch" tab.

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) (2.2.2.0)
 - [Docker](https://www.docker.com/community-edition)
 - [Vagrant](https://www.vagrantup.com) 1.8+
 - [Vagrant Hostmanager Plugin](https://github.com/devopsgroup-io/vagrant-hostmanager)
 - [Virtualbox](https://virtualbox.org) 5.0+
 - Python 2.7
 - Maven 3.3.9
 - C++11 compliant compiler, like [GCC](https://gcc.gnu.org/projects/cxx-status.html#cxx11)

Download the 0.4.1 build from [here]: 

Running the following script can help validate whether you have all the prerequisites installed and running correctly.

  ```
  metron-deployment/scripts/platform-info.sh
  ```

#### How do I install these on MacOS?

Any platform that supports these tools is suitable, but the following instructions cover installation on macOS.  The easiest means of installing these tools on a Mac is to use the excellent [Homebrew](http://brew.sh/) project.

1. Install Homebrew by following the instructions at [Homebrew](http://brew.sh/).

2. Run the following commands in a terminal to install all of the required tools.

    ```  
    brew cask install vagrant virtualbox docker
    brew cask install caskroom/versions/java8
    brew install maven@3.3 git
    pip install ansible==2.2.2.0
    vagrant plugin install vagrant-hostmanager
    vagrant plugin install vagrant-aws
    vagrant plugin install vagrant-reload
    pip install --upgrade setuptools --user python
    open /Applications/Docker.app
    ```
3. In your AWS console you need to reserve/create an AWS Elastic IP, a Subnet id, Security Group id, and a key pair (key pair name & *.pem file[remember to set permissions to chmod 400]).

4. The following will clear existing Vagrant, Docker & Maven builds and deployments. WARNING - THESE STEPS WILL DISTROY ALL LOCAL DOCKER CONTAINERS AND VAGRANT BOXES. It will also create a new Vagrant box.
```
vagrant halt node1 -f
vagrant halt default -f
vagrant destroy node1 -f
vagrant destroy default -f
for i in `vagrant global-status | grep virtualbox | awk '{print $1 }'` ; do vagrant destroy $i  ; done
vagrant global-status --prune
docker rm $(docker ps -aq)
osascript -e 'quit app "Docker"'
open -a Docker
rm -rf /../.m2/repository/*
rm -rf /../.vagrant.d/boxes/*
vagrant box add dummy --force https://github.com/mitchellh/vagrant-aws/raw/master/dummy.box
```
5. Associate your AWS ids and key information to the following enviroment variables (enter values inside the quotes).
```
export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_KEYNAME=''
export AWS_KEYPATH='../..*.pem'
export AWS_ELASTIC_IP=""
export AWS_SECURITYGROUP_ID=""
export AWS_SUBNET_ID=""
```
6. Update your local /etc/hosts file to include AWS_ELASTIC_IP which will be tied to "node1" (do only once).
```
sed -i "$AWS_ELASTIC_IP  node1" /etc/hosts
```

### Deploy Metron

1. Ensure that the Docker service is running.

2. Deploy Metron

    ```
    cd metron-deployment/development/aws-centos6
    vagrant up --provider=aws
    ```

### Explore Metron

Navigate to the following resources to explore your newly minted Apache Metron environment.

* [Metron Alerts](http://<elasticip>:4201)
* [Ambari](http://<elasticip>:8080)
