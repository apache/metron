This project contains a variety of tools for deploying Apache Metron.  Please refer to the following sections for more information on the best way to deploy Metron in your environment.

 * [How do I deploy Metron on a single VM?](#how-do-i-deploy-metron-on-a-single-vm)
 * [How do I deploy Metron on a large cluster with Ambari?](#how-do-i-deploy-metron-on-a-large-cluster-with-ambari)
 * [How do I build RPM packages?](#how-do-i-build-rpm-packages)
 * [How do I build DEB packages?](#how-do-i-build-deb-packages)
 * [How do I deploy Metron within AWS?](#how-do-i-deploy-metron-within-aws)

## How do I deploy Metron on a single VM?

#### Who is this for?

If you are new to Metron and just want to explore the functionality that it offers, this is good place to start.  If you are a developer building on Metron, then this is also a great way to test out your changes.

#### What does this do?

This option will deploy Metron on a virtual machine running on your computer.  This is often referred to as the "Full Dev" environment.  

#### How do I do it?
Follow these [instructions to deploy Metron on a single VM](vagrant/full-dev-platform/README.md).

#### Where can I get more information?
* [Full Dev Platform](vagrant/full-dev-platform)

## How do I deploy Metron on a large cluster with Ambari?

#### Who is this for?

If you want to see how Metron can really scale by deploying it on your own hardware, or even in your own cloud-based environment, this is the best option for you.

#### What does this do?

This creates a Management Pack (MPack) extension for [Apache Ambari](https://ambari.apache.org/) that simplifies the provisioning, managing and monitoring of Metron on large clusters.  This allows you to easily install Metron on a large cluster using a simple, guided process.  This also allows you to monitor cluster health and even secure your cluster with kerberos.

#### How do I do it?

##### Prerequisites

- A cluster managed by Ambari 2.4.2+
- Installable Metron packages (either RPMs or DEBs) available on the cluster, by default, in the `/localrepo` directory.  
- A [Node.js](https://nodejs.org/en/download/package-manager/) repository installed on the host running the Management and Alarm UI.

##### How do I build the Mpack?

```
cd metron-deployment
mvn clean package -Pmpack
```

This results in the Mpack being produced at the following location.
```
metron-deployment/packaging/ambari/metron-mpack/target/metron_mpack-0.4.2.0.tar.gz
```

##### How do I install the MPack?

Copy the tarball to the host where Ambari is installed and run the following command.

```
ambari-server install-mpack --mpack=metron_mpack-0.4.2.0.tar.gz --verbose
```

This will make the Metron available as an installable service within Ambari.

#### Where can I get more information?

The MPack can allow Metron to be installed and then kerberized, or installed on top of an already kerberized cluster.  This is done through Ambari's standard kerberization setup.  Using the MPack is preferred, but instructions for Kerberizing manually can be found at [Kerberos-manual-setup.md](Kerberos-manual-setup.md). These instructions are reference by the Ambari Kerberos install instructions and include commands for setting up a KDC.

* [Metron Mpack](packaging/ambari/metron-mpack)

## How do I build RPM packages?

#### Who is this for?
If you want to manually install Metron on an RPM-based system like CentOS or if you want a guided installation process using the Ambari Mpack on an RPM-based system, then this is the right option for you.

#### What does this do?
This builds installable RPM packages that allow you to install Metron on an RPM-based operating system like CentOS.

#### How do I do it?
```
cd metron-deployment
mvn clean package -Pbuild-rpms
```

The RPM packages will land in `metron-deployment/packaging/docker/rpm-docker/RPMS/noarch`.   

#### Where can I get more information?

* [RPM Docker](packaging/docker/rpm-docker)

## How do I build DEB packages?

**WARNING**: The DEB packages are a recent addition to Metron.  These packages have not undergone the same level of testing as the RPM packages.  Improvements and more rigerous testing of these packages is underway and will improve in future releases.  Until then, use these at your own risk.

#### Who is this for?
If you want to manually install Metron on a APT-based system like Ubuntu or if you want a guided installation process using the Ambari Mpack on an APT-based system, then this is the right option for you.

#### What does this do?
This builds installable DEB packages that allow you to install Metron on an APT-based operating system like Ubuntu.

#### How do I do it?
```
cd metron-deployment
mvn clean package -Pbuild-debs
```

The DEB packages will land in `metron-deployment/packaging/docker/deb-docker/target`.  

#### Where can I get more information?
* [DEB Docker](packaging/docker/deb-docker)


## How do I deploy Metron within AWS?

**WARNING**: This is only intended for short-lived testing and development.  This deployment method has the following severe limitations.
* The cluster is not secured in any way. It is up to you to manually secure it.  
* The cluster will not survive a reboot.

#### Who is this for?
If you are a developer wanting to test Metron at-scale on a multi-node cluster, then this is the right option for you.  If you want to run Metron in AWS with real data for either testing or production, then this is NOT the right option for you.

#### What does this do?
This deploys Metron on an automatically provisioned 10-node cluster.  This installs real sources of telemetry like Bro, Snort, and YAF, but feeds those sensors with canned pcap data.

#### How do I do it?
Follow the [instructions available here](amazon-ec2/README.md).  

#### Where can I get more information?
* [Amazon EC2](amazon-ec2)
