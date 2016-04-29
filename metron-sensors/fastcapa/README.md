Fastcapa
========

Fastcapa is an Apache Metron sensor that performs fast network packet capture by leveraging Linux kernel-bypass and user space networking technology.  

The sensor will bind to a network interface, capture network packets, and send the raw packet data to Kafka.  This provides a scalable mechanism for ingesting high-volumes of network packet data into a Hadoop-y cluster.

Fastcapa leverages the Data Plane Development Kit ([DPDK](http://dpdk.org/)).  DPDK is a set of libraries and drivers to perform fast packet processing in Linux user space.  

Getting Started
---------------

The quickest way to get up and running is to use a Virtualbox environment on your local machine.  The necessary files and instructions to do this are located at [`metron-deployment/vagrant/fastcapa-vagrant`](../../metron-deployment/vagrant/fastcapa-vagrant).  

Installation
------------

The process of installing Fastcapa has a fair number of steps and involves building DPDK, loading specific kernel modules, enabling huge page memory, and binding compatible network interface cards.

The best documentation is code that actually does this for you.  An Ansible role that performs the entire installation procedure can be found at [`metron-deployment/roles/fastcapa`](../../metron-deployment/roles/fastcapa).  Use this to install Fastcapa or as a guide for manual installation.
