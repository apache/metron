Fastcapa Test Environment
=========================

Provides a test environment for the development and testing of Fastcapa.  The environment is automatically validated after it is created to ensure that Fastcapa is behaving correctly.

Two virtualized nodes are launched with Vagrant that can communicate with one another over a private network.  
- The `source` node uses Metron's `pcap_replay` functionality to transmit raw network packet data over a private network.
- The `sink` node is running `fastcapa` and is capturing these network packets.
- Fastcapa then transforms and bundles the packets into a message.
- The message is sent to a Kafka broker running on the `source` node.

Getting Started
---------------

Simply run `vagrant up` to launch the environment.  Automated tests are executed after provisioning completes to ensure that Fastcapa and the rest of the environment is functioning properly.

```
$ vagrant up
==> source: Running provisioner: ansible...
    source: Running ansible-playbook...
...
TASK [debug] *******************************************************************
ok: [source] => {
    "msg": "Successfully received packets sent from pcap-replay!"
}
...
TASK [debug] *******************************************************************
ok: [source] => {
    "msg": "Successfully received a Kafka message from fastcapa!"
}
```

Going Deeper
------------

This section will outline in more detail the environment and how to interact with it.

### `source`

To validate that the `source` node is functioning properly, run the following commands.

First, ensure that the `pcap-replay` service is running.

```
vagrant ssh source
sudo service pcap-replay status
```

Use `tcpdump` to ensure that the raw packet data is being sent over the private network.  Enter 'CTRL-C' to kill the `tcpdump` process once you are able to see that packets are being sent.

```
sudo yum -y install tcpdump
sudo tcpdump -i enp0s8
```

### `sink`

Next validate that the `sink` is functioning properly. Run the following commands starting from the host operating system.  

First, ensure that the `fastcapa` service is running.

```
vagrant ssh sink
service fastcapa status
```

Ensure that the raw network packet data is being received by Kafka.

```
/usr/hdp/current/kafka-broker/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic pcap
```

Enter 'CTRL-C' to kill the `kafka-console-consumer` process once you are able to see that packets are being sent.  These packets will appear to be gibberish in the console.  This is the raw binary network packet data after all.
