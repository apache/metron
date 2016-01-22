# Overview
This set of playbooks will deploy the full Metron stack using ansible 
playbooks. These playbooks currently only target RHEL/CentOS 6.x operating
systems. 

## Setting up your inventory
Make sure to update the hosts file in `inventory/hosts` or provide an 
alternate inventory file when you launch the playbooks, including the 
ssh user(s) and ssh keyfile location(s). These playbooks expect three 
host groups:

- ambari_master
- hadoop_master
- hadoop_slaves

## Running the playbooks
#### Ambari Install:
This will install the Ambari server on one machine the ambari agent on 
the hadoop master/slaves.

Run this playbook with: `ansible-playbook ambari_install.yml`

Some of the code for installing/configuring Ambari comes from here: 
https://github.com/seanorama/ansible-ambari

#### Ambari Config:
This will create a cluster in Ambari with a blueprint for the required 
Metron components. This uses a custom ansible module for managing Ambari 
clusters using a yaml configuration. The cluster blueprint is defined in 
this file: `roles/ambari_config/vars/main.yml`

To create a Metron cluster in Ambari, run this playbook with the command: 
`ansible-playbook ambari_config.yml --tags "create_cluster"`

To delete a Metron cluster in Ambari, run this playbook with the command: 
`ansible-playbook ambari_config.yml --tags "delete_cluster"`  
*NOTE: The bug reported in [AMBARI-13906](https://issues.apache.org/jira/browse/AMBARI-13906) 
prevents cluster delete via the API. To remove the cluster, you must perform
a hard reset of the ambari server to delete the cluster.*

## TODO
- Automatically create and populate HBase tables from files
- migrate existing elasticsearch playbook
- migrate existing OpenSOC UI playbook
- migrate existing MySQL/GeoLite playbook
- migrate existing OpenSOC Storm client playbook
- Update all playbooks to baseline Metron version
- Support Ubuntu deployments
