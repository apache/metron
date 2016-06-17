Full Development Platform
=========================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized host running on Virtualbox.  

Metron is composed of many components and installing all of these on a single host, especially a virtualized one, will greatly stress the resources of the host.   The host will require at least 8 GB of RAM and a fair amount of patience.  It is highly recommended that you shut down all unnecessary services.

Getting Started
---------------

### Prerequisites

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) 2.0.0.2
 - [Vagrant](https://www.vagrantup.com) 1.8.1
 - [Virtualbox](virtualbox.org) 5.0.16
 - Python 2.7.11
 - Maven 3.3.9

#### OS X

Any platform that supports these tools is suitable, but the following instructions cover installation on Mac OS X.  The easiest means of installing these tools on a Mac is to use the excellent [Homebrew](http://brew.sh/) project.

1. Install Homebrew by following the instructions at [Homebrew](http://brew.sh/).

2. Run the following command in a terminal to install all of the required tools.

  ```  
  brew cask install vagrant virtualbox
  brew install brew-pip maven git
  pip install ansible==2.0.0.2
  ```

### Deploy Metron

1. Build Metron

  ```
  cd incubator-metron
  mvn clean package -DskipTests
  ```

2. Install Vagrant Hostmanager.

  ```
  vagrant plugin install vagrant-hostmanager
  ```

3. Deploy Metron

  ```
  cd metron-deployment/vagrant/full-dev-platform
  vagrant up
  ```

  Should the process fail before completing the deployment, the following command will continue the deployment process without re-instantiating the host.

  ```
  vagrant provision
  ```

### Explore Metron

Navigate to the following resources to explore your newly minted Apache Metron environment.

 - [Metron](http://node1:8080)
 - [Ambari](http://node1:5000)
 - [Services](http://node1:2812)

Connecting to the host through SSH is as simple as running the following command.

```
vagrant ssh
```

### Working with Metron

In addition to re-running the entire provisioning play book, you may now re-run an individual Ansible tag or a collection of tags in the following ways.  The following commands will re-run the `web` role on the Vagrant image. This will install components (if necessary) and start the UI.

```
./run_ansible_role.sh web
```
or

```
vagrant --ansible-tags="web" provision
```

#### Using Tags

A collection of tags is specified as a comma separated list.

```
./run_ansible_role.sh "sensors,enrichment"

```

Tags are listed in the playbooks, some frequently used tags:
+ `hdp-install` - Install HDP
+ `hdp-deploy` - Deploy and Start HDP Services (will start all Hadoop Services)
+ `sensors` - Deploy and Start Sensors.
+ `enrichment` - Deploy and Start Enrichment Topology.

Note also that there is a convenience script `./run_enrichment_role.sh`  which executes Vagrant with the `enrichment` tag.
