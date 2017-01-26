# Overview
These modules provide three alternatives for assisted deployment of an Ambari-managed Hadoop cluster, Metron services,
or both:
- A set of Ansible playbooks. These playbooks currently only target RHEL/CentOS 6.x/7.x operating systems.
- A Vagrant image for an entire pre-installed stack with Metron, in a single node.
- An Ambari Mpack, which in conjuction with RPMs detailed below can be used to deploy Metron services on an existing Ambari 2.4 managed stack,
or with Ambari to install a new stack plus Metron.

## Prerequisites
The following tools are required to run these scripts:

- Maven - https://maven.apache.org/ (version 3.3.9 recommended)
- Git - https://git-scm.com/
- Python - version 2.7 required; version 2.7.11 strongly recommended
- Ansible - http://www.ansible.com/ (version 2.0 or greater; version 2.0.0.2 strongly recommended) Required only for Ansible deployment.
- Vagrant - https://www.vagrantup.com/ (version 1.8.1 required), and Virtualbox (version 5.0.16 or greater) Required only for Vagrant deployment.

Currently Metron must be built from source.  Before running these scripts perform the following steps:

1. Clone the Metron git repository with `git clone git@github.com:apache/incubator-metron.git`
2. Navigate to `incubator-metron` and run `mvn clean package`

# Using Ansible Playbooks

These scripts depend on two files for configuration:

- hosts - declares which Ansible roles will be run on which hosts
- group_vars/all - various configuration settings needed to install Metron

Examples can be found in the
`incubator-metron/metron-deployment/inventory/metron_example` directory and are a good starting point.  Copy this directory
into `incubator-metron/metron-deployment/inventory/` and rename it to your `project_name`.  More information about Ansible files and directory
structure can be found at http://docs.ansible.com/ansible/playbooks_best_practices.html.

## Ambari
The Ambari playbook will install a Hadoop cluster with all the services and configuration required by Metron.  This
section can be skipped if installing Metron on a pre-existing cluster.

Currently, this playbook supports building a local development cluster running on one node but options for other types
 of clusters will be added in the future.

### Setting up your inventory
Make sure to update the hosts file in `incubator-metron/metron-deployment/inventory/project_name/hosts` or provide an
alternate inventory file when you launch the playbooks, including the
ssh user(s) and ssh keyfile location(s). These playbooks expect two
host groups:

- ambari_master
- ambari_slaves

### Running the playbook
This playbook will install the Ambari server on the ambari_master, install the ambari agents on
the ambari_slaves, and create a cluster in Ambari with a blueprint for the required
Metron components.

Navigate to `incubator-metron/metron-deployment/playbooks` and run:
`ansible-playbook -i ../inventory/project_name ambari_install.yml`

## Metron
The Metron playbook will gather the necessary cluster settings from Ambari and install the Metron services.

### Setting up your inventory
Edit the hosts file at `incubator-metron/metron-deployment/inventory/project_name/hosts`.  Declare where which hosts the
Metron services will be installed on by updating these groups:

- enrichment - submits the topology code to Storm and requires a storm client
- search - host where Elasticsearch will be run
- web - host where the Metron UI and underlying services will run
- sensors - host where network data will be collected and published to Kafka

The Metron topologies depend on Kafka topics and HBase tables being created beforehand.  Declare a host that has Kafka and HBase clients installed by updating these groups:

- metron_kafka_topics
- metron_hbase_tables

If only installing Metron, these groups can be ignored:

- ambari_master
- ambari_slaves

### Configuring group variables
The Metron Ansible scripts depend on a set of variables.  These variables can be found in the file at
`incubator-metron/metron-deployment/inventory/project_name/group_vars/all`.  Edit the ambari* variables to match your Ambari
instance and update the java_home variable to match the java path on your hosts.

### Running the playbook
Navigate to `incubator-metron/metron-deployment/playbooks` and run:
`ansible-playbook -i ../inventory/project_name metron_install.yml`

# Using Vagrant Image
A VagrantFile is included and will install a working version of the entire Metron stack.  The following is required to
run this:

- Hostmanager plugin for vagrant - Run `vagrant plugin install vagrant-hostmanager` on the machine where Vagrant is
installed

Navigate to `incubator-metron/metron-deployment/vagrant/full-dev-platform` and run `vagrant up`.  This also provides a good
example of how to run a full end-to-end Metron install.

# Using Ambari Management Pack

An Ambari Management Pack can be built in order to make the Metron service available on top of an existing Ambari-managed stack,
or install Metron along with a new stack with Ambari.

This will set up
- Metron Parsers
- Enrichment
- Indexing
- GeoIP database on MySQL
- Optional Elasticsearch
- Optional Kibana

## Prerequisites
- A cluster managed (or to be managed) by Ambari 2.4
- Metron RPMs available on the cluster in the /localrepo directory.  See [RPM](#RPM) for further information.

## Building Management Pack
From `metron-deployment` run
```
mvn clean package
```

This builds both `metron-mpack` and `metron-mpack-singlenode`.  The tar.gz files can be found at
`metron-deployment/packaging/ambari/metron-mpack/target/`  The `metron-mpack` requires a minimum of 4 nodes, and
is the only mpack that should be used in production environments.  The `metron-mpack-singlenode` is intended only
for lab or test environments.  It has been tested on 1-node test clusters.  It may also work on 2- or 3-node clusters,
but has not been adequately tested there.

## Installing Management Pack
Before installing the mpack, update Storm's topology.classpath in Ambari to include '/etc/hbase/conf:/etc/hadoop/conf'. Restart Storm service.

Place the mpack's tar.gz onto the node running Ambari Server. From the command line on this node, run
```
ambari-server install-mpack --mpack=<mpack_location> --verbose
```

This will make the services available in Ambari in the same manner as any services in a stack, e.g. through Add Services or during cluster install.
The Indexing / Parsers / Enrichment masters should be colocated with a Kafka Broker (to create topics) and HBase client (to create the enrichment and threatintel tables).
This colocation is currently not enforced by Ambari, and should be managed by either a Service or Stack advisor as an enhancement.

Several configuration parameters will need to be filled in, and should be pretty self explanatory (primarily a couple of Elasticsearch configs, and the Storm REST URL).  Examples are provided in the descriptions on Ambari.
Notably, the URL for the GeoIP database that is preloaded (and is prefilled by default) can be set to use a `file://` location

After installation, a custom Metron action is available in Ambari (in the Metron stop / start services pull-down) to install Elasticsearch templates.  Similar to this, a custom Kibana action to Load Template is available.

### Offline installation
Currently there is only one point that would reach out to the internet during an install.  This is the URL for the GeoIP database information that is preloaded into MySQL.

The RPMs DO NOT reach out to the internet (because there is currently no hosting for them).  They look on the local filesystem in `/localrepo`.

### Current Limitations
There are a set of limitations that should be addressed based to improve the current state of the mpacks.

- There is currently no hosting for RPMs remotely.  They will have to be built locally.
- Colocation of appropriate services should be enforced by Ambari.  See [#Installing Management Pack] for more details.
- Storm's topology.classpath is not updated with the Metron service install and needs to be updated separately.
- Several configuration parameters used when installing the Metron service could (and should) be grabbed from Ambari.  Install will require them to be manually entered.
- Need to handle upgrading Metron

## RPM
RPMs can be built to install the components in metron-platform. These RPMs are built in a Docker container and placed into `target`.

Components in the RPMs:
- metron-common
- metron-data-management
- metron-elasticsearch
- metron-enrichment
- metron-parsers
- metron-pcap
- metron-solr

### Prerequisites
- Docker.  The image detailed in: `metron-deployment/packaging/docker/rpm-docker/README.md` will automatically be built (or rebuilt if necessary).
- Artifacts for metron-platform have been produced.  E.g. `mvn clean package -DskipTests` in `metron-platform`

### Building RPMs
From `metron-deployment` run
```
mvn clean package -Pbuild-rpms
```

The output RPM files will land in `target/RPMS/noarch`.  They can be installed with the standard
```
rpm -i <package>
```

# TODO
- migrate existing MySQL/GeoLite playbook
- Support Ubuntu deployments

