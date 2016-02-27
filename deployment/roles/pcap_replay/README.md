Pcap Replay
===========

This project enables packet capture data to be replayed through a network interface to simulate live network traffic.  This can be used to support functional, performance, and load testing of Apache Metron.

Getting Started
---------------

To replay packet capture data, simply start the `pcap-replay` SysV service.  To do this run the following command.

```
service pcap-replay start
```

All additional options accepted by `tcpreplay` can be passed to the service script to modify how the network data is replayed.  For example, this makes it simple to control the amount and rate of data replayed during functional, performance and load testing.

Example: Replay data at a rate of 10 mbps.

```
service pcap-replay start --mbps 10
```

Example: Replay data at a rate of 10 packets per second.

```
service pcap-replay start --pps 10
```

All nodes on the same subnet with their network interface set to promiscuous mode will then be able to capture the network traffic being replayed.  To validate, simply run something like the following.

```
tcpdump -i eth1
```

Data
----

An example packet capture file has been installed at `/opt/pcap-replay/example.pcap`.  By default, the network traffic contained within this file is continually replayed.   

To replay your own packet capture data, simply add any number of files containing `libpcap` formatted packet capture data to `/opt/pcap-replay`.  The files must end with the `.pcap` extension.  To pick up newly installed files, simply restart the service.

```
service pcap-replay restart
```
