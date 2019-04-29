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
Metron on CentOS 6
==================================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized host running CentOS 6.
It utilizes Vagrant for the virtual machine, and Docker for the build and deployment.  Therefore lessens the burden on the user to have the correct versions of the build and deployment tools in order to try Metron.

Metron is composed of many components and installing all of these on a single host, especially a virtualized one, will greatly stress the resources of the host.   The host will require at least 8 GB of RAM and a fair amount of patience.  It is highly recommended that you shut down all unnecessary services.

Getting Started
---------------

### Prerequisites

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Docker](https://www.docker.com/community-edition)
 - [Vagrant](https://www.vagrantup.com) 2.0+
 - [Vagrant Hostmanager Plugin](https://github.com/devopsgroup-io/vagrant-hostmanager)
 - [Virtualbox](https://virtualbox.org) 5.0+

Running the following script can help validate whether you have all the prerequisites installed and running correctly.

  ```
  metron-deployment/scripts/platform-info.sh
  ```

#### How do I install these on MacOS?

Any platform that supports these tools is suitable, but the following instructions cover installation on macOS.  The easiest means of installing these tools on a Mac is to use the excellent [Homebrew](http://brew.sh/) project.

1. Install Homebrew by following the instructions at [Homebrew](http://brew.sh/).

1. Run the following command in a terminal to install all of the required tools.

    ```
    brew cask install vagrant virtualbox docker 
    vagrant plugin install vagrant-hostmanager
    open /Applications/Docker.app
    ```

### Deploy Metron

1. Ensure that the Docker service is running.

1. Deploy Metron

 ```bash
    cd metron-deployment/development/centos6
    ./metron-up.sh -h
 ```
 ```bash   
    usage: ./metron-up.sh
        --force-docker-build            force build docker machine
        --skip-tags='tag,tag2,tag3'     the ansible skip tags
        --skip-vagrant-up               skip vagrant up
        -h/--help                       Usage information.

    example: to skip vagrant up and force docker build with two tags
        metron-up.sh -skip-vagrant-up --force-docker-build --skip-tags='solr,sensors'
 ```
    
### Explore Metron

Navigate to the following resources to explore your newly minted Apache Metron environment.

* [Metron Alerts](http://node1:4201) credentials: user/password
* [Ambari](http://node1:8080) credentials: admin/admin

Connecting to the host through SSH is as simple as running the following command.
```
vagrant ssh
```

### Working with Metron

In addition to running the entire provisioning play book,you may run an individual Ansible tag or a collection of tags.

```
vagrant --ansible-tags="sensor-stubs" provision
```

Tags are listed in the playbooks.  Here are some frequently used tags:
+ `hdp-install` - Install HDP
+ `hdp-deploy` - Deploy and Start HDP Services (will start all Hadoop Services)
+ `sensors` - Deploy the sensors (see [Sensors](#sensors) for more details regarding this tag)
+ `sensor-stubs` - Deploy and start the sensor stubs.

#### Sensors

By default, the Metron development environment uses sensor stubs to mimic the behavior of the full sensors.  This is done because the full sensors take a significant amount of time and CPU to build, install, and run.

From time to time you may want to install the full sensors for testing (see the specifics of what that means [here](../../ansible/playbooks/sensor_install.yml)).  This can be done by running the following command:

```
metron-up.sh --skip-tags='sensor-stubs,solr'
```

This will skip only the `sensor-stubs` and solr tags, allowing the ansible roles with the `sensors` tag to be run.  This provisions the full sensors in a 'testing mode' so that they are more active, and thus more useful for testing (more details on that [here](../../ansible/roles/sensor-test-mode/)).  **However**, when vagrant completes the sensors will NOT be running.  In order to start the sensors and simulate traffic through them (which will create a fair amount of load on your test system), complete the below steps:

```
vagrant ssh
sudo su -
service pcap-replay restart
service yaf restart
service snortd restart
service snort-producer restart
```

