Developer Image for Apache Metron on Virtualbox
===============================================

This image is a fully functional Metron installation that has been pre-loaded with Ambari, HDP and Metron.

Getting Started
---------------

### Prerequisites

As with the Singlenode Full Image, the computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) 2.0.0.2
 - [Vagrant](https://www.vagrantup.com) 1.8.1
 - [Virtualbox](virtualbox.org) 5.0.16
 - Python 2.7.11
 - Maven 3.3.9


### Launch the Metron Development Image

Start the image with the following commands:

  ```
  cd metron-deployment/vagrant/codelab-platform
  ./launch_dev_image.sh
  ```

### Work with Metron

As you build out new capabilities for Metron, you will need to re-deploy the Storm topologies. To do so, first HALT the running Storm topologies and then run:

```
./run_enrichment_role.sh
```

Remember Navigate to the following resources to explore your newly minted Apache Metron environment.

 - [Metron](http://node1:8080)
 - [Ambari](http://node1:5000)

Connecting to the host through SSH is as simple as running the following command.

   ```
   vagrant ssh
   ```