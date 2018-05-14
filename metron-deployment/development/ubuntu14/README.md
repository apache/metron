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
Metron on Ubuntu 14
===================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized host running Ubuntu 14.

Metron is composed of many components and installing all of these on a single host, especially a virtualized one, will greatly stress the resources of the host.   The host will require at least 8 GB of RAM and a fair amount of patience.  It is highly recommended that you shut down all unnecessary services.

Getting Started
---------------

### Prerequisites

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) (2.0.0.2, 2.2.2.0, or 2.5.0)
 - [Docker](https://www.docker.com/community-edition)
 - [Vagrant](https://www.vagrantup.com) 2.0+
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

1. Run the following command in a terminal to install all of the required tools.

    ```
    brew cask install vagrant virtualbox docker
    brew cask install caskroom/versions/java8
    brew install maven@3.3 git
    pip install ansible==2.2.2.0
    vagrant plugin install vagrant-hostmanager
    open /Applications/Docker.app
    ```

### Deploy Metron

1. Ensure that the Docker service is running.

1. Deploy Metron

    ```
    cd metron-deployment/development/ubuntu14
    vagrant up
    ```

    Should the process fail before completing the deployment, the following command will continue the deployment process without re-instantiating the host.

    ```
    vagrant provision
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

In addition to re-running the entire provisioning play book, you may now re-run an individual Ansible tag or a collection of tags in the following ways.  The following commands will re-run the `sensor-stubs` role on the Vagrant image. This will install and start the sensor stub components.

```
vagrant --ansible-tags="sensor-stubs" provision
```

Tags are listed in the playbooks, some frequently used tags:
+ `hdp-install` - Install HDP
+ `hdp-deploy` - Deploy and Start HDP Services (will start all Hadoop Services)
+ `sensors` - Deploy and start the sensors.
+ `sensor-stubs` - Deploy and start the sensor stubs.
