Fastcapa
========

Fastcapa is an Apache Metron sensor that performs fast network packet capture by leveraging Linux kernel-bypass and user space networking technology.  

The sensor will bind to a network interface, capture network packets, and send the raw packet data to Kafka.  This provides a scalable mechanism for ingesting high-volumes of network packet data into a Hadoop-y cluster.

Fastcapa leverages the Data Plane Development Kit ([DPDK](http://dpdk.org/)).  DPDK is a set of libraries and drivers to perform fast packet processing in Linux user space.  

Getting Started
---------------

The quickest way to get up and running is to use a Virtualbox environment on your local machine.  The necessary files to do this are located at `deployment/vagrant/fastcapa-vagrant`.  Use the following commands to launch the environment.  

```
cd deployment/vagrant/fastcapa-vagrant
vagrant up
```

Two virtualized nodes will be launched in Virtualbox that can communicate with one another over a private network.  
- The `source` node uses Metron's `pcap_replay` functionality to send raw network packet data to the `sink` node.  
- The `sink` node is running both Fastcapa and a Kafka broker.  Fastcapa is configured to consume the raw packet capture data and send it to Kafka.

Validate that the `source` is functioning properly.  Run the following commands starting from the host operating system.  Ensure that the `pcap-replay` service is running.

```
$ vagrant ssh source
...

[vagrant@source ~]$ sudo service pcap-replay status
Checking pcap-replay...                           Running
```

Use `tcpdump` to ensure that the raw packet data is being sent over the private network.  Enter 'CTRL-C' to kill the `tcpdump` process once you are able to see that packets are being sent.

```
[vagrant@source ~]$ sudo yum -y install tcpdump
...

[vagrant@source ~]$ sudo tcpdump -i enp0s8
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
listening on enp0s8, link-type EN10MB (Ethernet), capture size 65535 bytes
19:14:34.863428 IP 192.168.138.158.49190 > static-ip-62-75-195-236.inaddr.ip-pool.com.http: Flags [S], seq 2883836677, win 8192, options [mss 1460,nop,wscale 2,nop,nop,sackOK], length 0
19:14:34.994785 IP static-ip-62-75-195-236.inaddr.ip-pool.com.http > 192.168.138.158.49190: Flags [S.], seq 561530234, ack 2883836678, win 64240, options [mss 1460], length 0
19:14:34.995120 IP 192.168.138.158.49190 > static-ip-62-75-195-236.inaddr.ip-pool.com.http: Flags [.], ack 1, win 64240, length 0
19:14:34.995412 IP 192.168.138.158.49190 > static-ip-62-75-195-236.inaddr.ip-pool.com.http: Flags [P.], seq 1:314, ack 1, win 64240, length 313
19:14:34.995518 IP static-ip-62-75-195-236.inaddr.ip-pool.com.http > 192.168.138.158.49190: Flags [.], ack 314, win 64240, length 0
...
```

Next valdate that the `sink` is functioning properly. Run the following commands starting from the host operating system.  Ensure that the `fastcapa` service is running.

```
$ vagrant ssh sink
...

[vagrant@sink ~]$ service fastcapa status
Checking fastcapa...                              Running
```

Ensure that the raw network packet data is being received by Kafka. Enter 'CTRL-C' to kill the `kafka-console-consumer` process once you are able to see that packets are being sent.  These packets will appear to be gibberish in the console.  This is the raw binary network packet data after all.

```
[vagrant@sink ~]$ /usr/hdp/current/kafka-broker/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic pcap
33��e߆�`P:�������e����@@@6������@���e�
E(�@TY�&�2�����3�"P9�
C��e(b$@7SD&�2����"P��
^CConsumed 3 messages
```

Installation
------------

The process of installing Fastcapa has a fair number of steps and involves building DPDK, loading specific kernel modules, enabling hugepage memory, and binding compatible network interface cards.

The best documentation is code that actually does this for you.  An Ansible role that performs the entire installation procedure can be found at `deployment/roles/fastcapa`.  Use this to install `fastcapa` or as a guide for manual installation.
