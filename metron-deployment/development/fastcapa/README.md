<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
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

The Fastcapa test environment can be executed on different operating systems.  There is a sub-directory for each operating system that Fastcapa can be tested on.  

To run, simply execute `vagrant up` within the appropriate directory.  For example, to run the tests on CentOS 7.1 then execute the following commands.
```
cd centos-7.1
vagrant up
```

Automated tests are executed after provisioning completes to ensure that Fastcapa and the rest of the environment is functioning properly.  If you see something like the following, then the tests have passed.
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

If the deployment process fails mid-course, running `vagrant provision` will continue the process from where it left off.  This can sometimes occur when the VM reboots as part of the deployment process.  The error might look like the following.
```
TASK [fastcapa : Restart for modified kernel params] ***************************
fatal: [sink]: UNREACHABLE! => {"changed": false, "msg": "Failed to connect to the host via ssh: Shared connection to 127.0.0.1 closed.\r\n", "unreachable": true}
	to retry, use: --limit @/Users/nallen/Development/metron/metron-deployment/vagrant/fastcapa-test-platform/playbook.retry

PLAY RECAP *********************************************************************
sink                       : ok=11   changed=9    unreachable=1    failed=0
source                     : ok=29   changed=25   unreachable=0    failed=0

Ansible failed to complete successfully. Any error output should be
visible above. Please fix these errors and try again.
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

FAQ
---

### Error Message: Timed out while waiting for the machine to boot

```
Timed out while waiting for the machine to boot. This means that
Vagrant was unable to communicate with the guest machine within
the configured ("config.vm.boot_timeout" value) time period.
If you look above, you should be able to see the error(s) that
Vagrant had when attempting to connect to the machine. These errors
are usually good hints as to what may be wrong.
If you're using a custom box, make sure that networking is properly
working and you're able to connect to the machine. It is a common
problem that networking isn't setup properly in these boxes.
Verify that authentication configurations are also setup properly,
as well.
If the box appears to be booting properly, you may want to increase
the timeout ("config.vm.boot_timeout") value.
➜  centos-7.4 git:(master) ✗ vagrant status
Current machine states:
source                    running (virtualbox)
sink                      not created (virtualbox)
```

If you are unable to launch any of the Fastcapa test environments, which results in a message like the one above, then you may need to upgrade your version of Virtualbox.  Success has been reported with versions of VirtualBox 5.1.22+.
