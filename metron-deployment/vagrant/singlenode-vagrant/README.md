Apache Metron on Virtualbox
===========================

This project fully automates the provisioning of Apache Metron on single, virtualized host running on Virtualbox.  Metron is composed of many components and installing all of these on a single host, especially a virtualized one, will greatly stress your computer.   To work sufficiently this will require at least 8 GB of RAM and a fair amount of patience.

Getting Started
---------------

### Prerequisites

The computer used to deploy Apache Metron will need to have the following components installed.

 - [Ansible](https://github.com/ansible/ansible) 2.0.0.2
 - [Vagrant](https://www.vagrantup.com) 1.8.1
 - [Virtualbox](virtualbox.org) 5.0.16
 - Python 2.7.11
 - Maven 3.3.9

Any platform that supports these tools is suitable, but the following instructions cover installation on Mac OS X only.  The easiest means of installing these tools on a Mac is to use the excellent [Homebrew](http://brew.sh/) project.

1. Install Homebrew by running the following command in a terminal.  Refer to the  [Homebrew](http://brew.sh/) home page for the latest installation instructions.

  ```
  /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
  ```

2. With Homebrew installed, run the following command in a terminal to install all of the required tools.

  ```  
  brew cask install vagrant virtualbox
  brew install brew-pip maven git
  pip install ansible==2.0.0.2
  ```

3. Install Vagrant Hostmanager.

  ```
  vagrant plugin install vagrant-hostmanager
  ```

### Metron

Now that the hard part is done, start the Metron deployment process.

1. Build Metron

  ```
  cd metron-platform
  mvn clean package -DskipTests
  ```

2. Deploy Metron

  ```
  cd metron-deployment/vagrant/singlenode-vagrant
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

Connecting to the host through SSH is as simple as running the following command.

   ```
   vagrant ssh
   ```
