Pcap Replay
===========

This will install components necessary to easily replay packet capture data to support functional performance, and load testing of Metron.

Getting Started
---------------

Pcap Replay is installed as a System V service at `/etc/init.d/pcap-replay`.  To start the service, run the following command.

```
service pcap-replay start
```

An example packet capture file has been installed at `/opt/pcap-replay/example.pcap`.  By default, the network traffic contained within this file is continually replayed.  That's right, it will never, ever stop.  

To replay your own packet capture data, simply add any number of files containing `libpcap` formatted packet capture data to `/opt/pcap-replay`.  The files must end with the `.pcap` file extension.  To pick up newly installed files, simply restart the service.

```
service pcap-replay restart
```

All nodes on the same subnet with their network interface set to promiscuous mode will then be able to capture the network traffic being replayed.  To validate, simply run something like the following.

```
tcpdump -i eth1
```
