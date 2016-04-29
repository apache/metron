Apache Metron on Virtualbox
===========================

This project automates the provisioning of Apache Metron on a single, virtualized node within Virtualbox.  This is the easiest, most cost-effective way to get started with Apache Metron.

Be forewarned that Metron leverages numerous components and it will greatly tax even the most capable laptops and computers.


Getting Started
---------------

### Prerequisites

- Vagrant 1.8.1
- Ansible 2.0.2.0
- Virtualbox 5.0.18-106667
- Maven 3.3.9

This guide will use [Homebrew](brew.sh) to install the prerequisites required on your local machine to deploy Metron.  You are welcome to use any means of installing these software packages.

1. Install [Homebrew](brew.sh) if you have not already done so.

2. Install the Apache Metron dependencies.

```
brew install ansible maven
brew cask install vagrant virtualbox java
```

```
$ brew list --versions
ansible 2.0.2.0
maven 3.3.9

$ brew cask list --versions
java 1.8.0_92-b14
vagrant 1.8.1
virtualbox 5.0.18-106667

```

### Deploy Metron

1. Ensure that Metron's platform uber-jar has been built.

  ```
  cd metron-platform
  mvn clean package -DskipTests
  ```

2. Start the Metron deployment.

  ```
  cd metron-deployment/vagrant/full-dev-platform
  vagrant up
  ```

  Should the process fail before completing the deployment, the following command will continue the deployment process without re-instantiating the host.

  ```
  vagrant provision
  ```
  
  In addition to re-running the entire provisioning play book, you may now re-run an individual Ansible tag or a collection of tags in the following ways.
  
  ```
  ./run_ansible_role.sh web
  ```
  or
  ```
  vagrant --ansible-tags="web" provision
  ```
  Will re-run the web role on the Vagrant image. This will re-install (if necessary) and start the UI.
   
  A collection of tags is specified as a comma separated list.
  
  ```
  ./run_ansbile_role.sh "sensors,enrichment"
  
  ```
  
  Tags are listed in the playbooks, some frequently used tags:
  + hdp-install - Install HDP
  + hdp-deploy - Deploy and Start HDP Services (will start all Hadoop Services)
  + sensors - Deploy and Start Sensors.
  + enrichment - Deploy and Start Enrichment Topology.
  
  Note: there is a convienence script, ```./run_enrichment_role.sh```,  which runs the enrichment tag.
  
### Explore Metron

1. After the deployment has completed successfully, a message like the following will be displayed.  Navigate to the specified resources to explore your newly minted Apache Metron environment.

  ```
  TASK [debug] *******************************************************************
  ok: [localhost] => {
      "Success": [
          "Apache Metron deployed successfully",
          "   Metron  @  http://ec2-52-37-255-142.us-west-2.compute.amazonaws.com:5000",
          "   Ambari  @  http://ec2-52-37-225-202.us-west-2.compute.amazonaws.com:8080",
          "   Sensors @  ec2-52-37-225-202.us-west-2.compute.amazonaws.com on tap0",
          "For additional information, see https://metron.incubator.apache.org/'"
      ]
  }
  ```

2. Each of the provisioned hosts will be accessible from the internet. Connecting to one over SSH as the user `centos` will not require a password as it will authenticate with the pre-defined SSH key.  

  ```
  ssh centos@ec2-52-91-215-174.compute-1.amazonaws.com
  ```
