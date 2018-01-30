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
Metron in AWS as a Single Node with CentOS 6 using Vagrant
==================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized node in AWS EC2. 

Getting Started
---------------

### Prerequisites

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) (2.2.2.0)
 - [Docker](https://www.docker.com/community-edition)
 - [Vagrant](https://www.vagrantup.com) 1.8+
 - [Vagrant Hostmanager Plugin](https://github.com/devopsgroup-io/vagrant-hostmanager)
 - [Virtualbox](https://virtualbox.org) 5.0+
 - Python 2.7
 - Maven 3.3.9
 - C++11 compliant compiler, like [GCC](https://gcc.gnu.org/projects/cxx-status.html#cxx11)

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

4. The following will clear existing Vagrant, Docker & Maven deployments. WARNING - THESE STEPS WILL DISTROY ALL LOCAL DOCKER CONTAINERS AND VAGRANT BOXES
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
5. Associate your AWS ids to the following enviroment variables names
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
