# Pycapa

## Overview

Pycapa performs network packet capture, both off-the-wire and from Kafka, which is useful for the testing and development of [Apache Metron](https://github.com/apache/incubator-metron).  It is not intended for production use. The tool will capture packets from a specified interface and push them into a Kafka Topic.

## Installation

```
pip install -r requirements.txt
python setup.py install
```

## Usage

```
$ pycapa --help
usage: pycapa [-h] [-p] [-c] [-k KAFKA_BROKERS] [-t TOPIC] [-n PACKET_COUNT]
              [-d DEBUG] [-i INTERFACE]

optional arguments:
  -h, --help            show this help message and exit
  -p, --producer        sniff packets and send to kafka
  -c, --consumer        read packets from kafka
  -k KAFKA_BROKERS, --kafka KAFKA_BROKERS
                        kafka broker(s)
  -t TOPIC, --topic TOPIC
                        kafka topic
  -n PACKET_COUNT, --number PACKET_COUNT
                        number of packets to consume
  -d DEBUG, --debug DEBUG
                        debug every X packets
  -i INTERFACE, --interface INTERFACE
                        interface to listen on
```

Pycapa has two primary runtime modes.

### Producer Mode

Pycapa can be configured to capture packets from a network interface and then forward those packets to a Kafka topic.  The following example will capture packets from the `eth0` network interface and forward those to a Kafka topic called `pcap` running on `localhost`.

```
pycapa --producer --kafka localhost:9092 --topic pcap -i eth0
```

To output debug messages every 100 captured packets, run the following.

```
pycapa --producer --kafka localhost:9092 --topic pcap -i eth0 --debug 100
```

### Consumer Mode

Pycapa can be configured to consume packets from a Kafka topic and then write those packets to a [libpcap-compliant file](https://wiki.wireshark.org/Development/LibpcapFileFormat).  To read 100 packets from a kafka topic and then write those to a [libpcap-compliant file](https://wiki.wireshark.org/Development/LibpcapFileFormat), run the following command.  The file `out.pcap` can then be opened with a tool such as Wireshark for further validation.

```
pycapa --consumer --kafka localhost:9092 --topic pcap --n 100 > out.pcap
```

To consume packets from Kafka continuously and print debug messages every 10 packets, run the following command.  

```
pycapa --consumer --kafka localhost:9092 --topic pcap --debug 10
```

## Dependencies

* [kafka-python](https://github.com/dpkp/kafka-python)
* [pcapy](https://github.com/CoreSecurity/pcapy)

## Implementation

When run in Producer Mode, Pycapa embeds the raw network packet data in the Kafka message.  The message key contains the timestamp indicating when the packet was captured in microseconds from the epoch.  This value is in network byte order.
