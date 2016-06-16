Quick Development Platform
==========================

This project fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized host running on Virtualbox.  

This image is designed for quick deployment of a single node Metron cluster running on Virtualbox.  This platform is ideal for use by Metron developers.  It uses a base image that has been pre-loaded with Ambari and HDP.

Metron is composed of many components and installing all of these on a single host, especially a virtualized one, will greatly stress the resources of the host. The host will require at least 8 GB of RAM and a fair amount of patience. It is highly recommended that you shut down all unnecessary services.

Getting Started
---------------

### Prerequisites

As with the Full Development Platform (`metron-deployment/vagrant/full-dev-platform`), the computer used to deploy Apache Metron will need the following components installed.

 - [Ansible](https://github.com/ansible/ansible) 2.0.0.2
 - [Vagrant](https://www.vagrantup.com) 1.8.1
 - [Virtualbox](virtualbox.org) 5.0.16
 - Python 2.7.11
 - Maven 3.3.9

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
  cd metron-deployment/vagrant/quick-dev-platform
  ./run.sh
  ```

  Should the process fail before completing the deployment, the following command will continue the deployment process without re-instantiating the host.

  ```
  vagrant --ansible-tags="hdp-deploy,metron" provision
  ```

### Explore Metron

Navigate to the following resources to explore your newly minted Apache Metron environment.

 - [Metron](http://node1:8080)
 - [Ambari](http://node1:5000)
 - [Services](http://node1:2812)

### Working with Metron

As you build out new capabilities for Metron, you will need to re-deploy the Storm topologies. To do so, first HALT the running Storm topologies and then run the following command.

```
./run_enrichment_role.sh
```

Connecting to the host through SSH is as simple as running the following command.

```
vagrant ssh
```
