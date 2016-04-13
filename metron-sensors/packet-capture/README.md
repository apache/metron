Metron Packet Capture
=====================

A sensor that performs network packet capture leveraging the Data Plane Development Kit ([DPDK](http://dpdk.org/)).  DPDK is a set of libraries and drivers to perform fast packet processing in Linux user space.  The packet capture process will bind to a DPDK-compatible network interface, capture network packets, and send the raw packet data to a Kafka Broker.

Getting Started
---------------

```
cd deployment/vagrant/packet-capture
vagrant up
```
