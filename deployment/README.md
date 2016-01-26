# Overview
This set of playbooks can be used to deploy an Ambari-managed Hadoop cluster, Metron services, or both using ansible
playbooks. These playbooks currently only target RHEL/CentOS 6.x operating
systems. 

## Prerequisites
The following tools are required to run these scripts:

- Maven - https://maven.apache.org/
- Git - https://git-scm.com/
- Ansible - http://www.ansible.com/ (version 2.0 or greater)

Currently Metron must be built from source.  Before running these scripts perform the following steps:

1. Clone the Metron git repository with `git clone git@github.com:apache/incubator-metron.git`
2. Navigate to `incubator-metron/metron-streaming` and run `mvn clean package`

These scripts depend on two files for configuration:
  
- hosts - declares which Ansible roles will be run on which hosts
- group_vars/all - various configuration settings needed to install Metron

Examples can be found in the
`incubator-metron/deployment/inventory/metron_example` directory and are a good starting point.  Copy this directory 
into `incubator-metron/deployment/inventory/` and rename it to your `project_name`.  More information about Ansible files and directory 
structure can be found at http://docs.ansible.com/ansible/playbooks_best_practices.html.

## Ambari
The Ambari playbook will install a Hadoop cluster with all the services and configuration required by Metron.  This
section can be skipped if installing Metron on a pre-existing cluster.  

Currently, this playbook supports building a local development cluster running on one node but options for other types
 of clusters will be added in the future.

### Setting up your inventory
Make sure to update the hosts file in `incubator-metron/deployment/inventory/project_name/hosts` or provide an 
alternate inventory file when you launch the playbooks, including the 
ssh user(s) and ssh keyfile location(s). These playbooks expect two 
host groups:

- ambari_master
- ambari_slaves

### Running the playbook
This playbook will install the Ambari server on the ambari_master, install the ambari agents on 
the ambari_slaves, and create a cluster in Ambari with a blueprint for the required 
Metron components.

Navigate to `incubator-metron/deployment/playbooks` and run: 
`ansible-playbook -i ../inventory/project_name ambari_install.yml`

## Metron
The Metron playbook will gather the necessary cluster settings from Ambari and install the Metron services.

### Setting up your inventory
Edit the hosts file at `incubator-metron/deployment/inventory/project_name/hosts`.  Declare where which hosts the 
Metron services will be installed on by updating these groups:

- enrichment - submits the topology code to Storm and requires a storm client
- search - host where Elasticsearch will be run
- web - host where the Metron UI and underlying services will run
- sensors - host where network data will be collected and published to Kafka

The Metron topologies depend on Kafka topics and HBase tables being created beforehand.  Declare a host that has Kafka
 and HBase clients installed by updating this group:

- hadoop_client

If only installing Metron, these groups can be ignored:

- ambari_master
- ambari_slaves

### Configuring group variables
The Metron Ansible scripts depend on a set of variables.  These variables can be found in the file at 
`incubator-metron/deployment/inventory/project_name/group_vars/all`.  Edit the ambari* variables to match your Ambari
instance and update the java_home variable to match the java path on your hosts.

### Running the playbook
Navigate to `incubator-metron/deployment/playbooks` and run: 
`ansible-playbook -i ../inventory/project_name metron_install.yml`

## Vagrant
A VagrantFile is included and will install a working version of the entire Metron stack.  The following is required to
run this:

- Vagrant - https://www.vagrantup.com/
- Hostmanager plugin for vagrant - Run `vagrant install plugin vagrant-hostmanager` on the machine where Vagrant is
installed

Navigate to `incubator-metron/deployment/vagrant/singlenode-vagrant` and run `vagrant up`.  This also provides a good
example of how to run a full end-to-end Metron install.


## TODO
- migrate existing MySQL/GeoLite playbook
- Support Ubuntu deployments
