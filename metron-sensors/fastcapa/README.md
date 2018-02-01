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
Fastcapa
========

Fastcapa is a probe that performs fast network packet capture by leveraging Linux kernel-bypass and user space networking technology.  The probe will bind to a network interface, capture network packets, and send the raw packet data to Kafka.  This provides a scalable mechanism for ingesting high-volumes of network packet data into a Hadoop-y cluster.

Fastcapa leverages the Data Plane Development Kit ([DPDK](http://dpdk.org/)).  DPDK is a set of libraries and drivers to perform fast packet processing in Linux user space.  

* [Quick Start](#quick-start)
* [Requirements](#requirements)
* [Installation](#installation)
* [Usage](#usage)
    * [Parameters](#parameters)
    * [Output](#output)
    * [Kerberos](#kerberos)
* [How It Works](#how-it-works)
* [Performance](#performance)
* [FAQs](#faqs)

Quick Start
-----------

The quickest way to see Fastcapa in action is to use a Virtualbox environment on your local machine.  The necessary files and instructions to do this are located at [`metron-deployment/vagrant/fastcapa-vagrant`](../../metron-deployment/vagrant/fastcapa-test-platform). All you need to do is execute the following.
```
cd metron-deployment/vagrant/fastcapa-test-platform
vagrant up
```

This environment sets up two nodes.  One node produces network packets over a network interface.  The second node uses Fastcapa to capture those packets and write them to a Kafka broker running on the first node.  Basic validation is performed to ensure that Fastcapa is able to land packet data in Kafka.

Requirements
------------

The following system requirements must be met to run the Fastcapa probe.

* Linux kernel >= 2.6.34
* A [DPDK supported ethernet device; NIC](http://dpdk.org/doc/nics).
* Port(s) on the ethernet device that can be dedicated for exclusive use by Fastcapa

Installation
------------

The process of installing Fastcapa has a fair number of steps and involves building DPDK, loading specific kernel modules, enabling huge page memory, and binding compatible network interface cards.

### Automated Installation

The best documentation is code that actually does this for you.  An Ansible role that performs the entire installation procedure can be found at [`metron-deployment/roles/fastcapa`](../../metron-deployment/roles/fastcapa).  Use this to install Fastcapa or as a guide for manual installation.  The automated installation assumes CentOS 7.1 and is directly tested against [bento/centos-7.1](https://atlas.hashicorp.com/bento/boxes/centos-7.1).

### Manual Installation

The following manual installation steps assume that they are executed on CentOS 7.1.  Some minor differences may result if you use a different Linux distribution.

* [Enable Transparent Huge Pages](#enable-transparent-huge-pages)
* [Install DPDK](#install-dpdk)
* [Install Librdkafka](#install-librdkafka)
* [Install Fastcapa](#install-fastcapa)

#### Enable Transparent Huge Pages

The probe performs its own memory management by leveraging transparent huge pages.  In Linux, Transparent Huge Pages (THP) can be enabled either dynamically or on boot.  It is recommended that these be allocated on boot to increase the chance that a larger, physically contiguous chunk of memory can be allocated.

The size of THPs that are supported will vary based on your CPU.  These typically include 2 MB and 1 GB THPs.  For better performance, allocate 1 GB THPs if supported by your CPU.

1. Ensure that your CPU supports 1 GB THPs.  A CPU flag `pdpe1gb` indicates whether or not the CPU supports 1 GB THPs.
    ```
    grep --color=always pdpe1gb /proc/cpuinfo | uniq
    ```

2. Add the following boot parameters to the Linux kernel.  Edit `/etc/default/grub` and add the additional kernel parameters to the line starting with `GRUB_CMDLINE_LINUX`.
    ```
    GRUB_CMDLINE_LINUX=... default_hugepagesz=1G hugepagesz=1G hugepages=16
    ```

3. Rebuild the grub configuration then reboot.  The location of the Grub configuration file will differ across Linux distributions.
    ```
    cp /etc/grub2-efi.cfg /etc/grub2-efi.cfg.orig
    /sbin/grub2-mkconfig -o /etc/grub2-efi.cfg
    ```

4. Once the host has been rebooted, ensure that the THPs were successfully allocated.
    ```
    $ grep HugePage /proc/meminfo
    AnonHugePages:    933888 kB
    HugePages_Total:      16
    HugePages_Free:        0
    HugePages_Rsvd:        0
    HugePages_Surp:        0
    ```

    The total number of huge pages that you have been allocated should be distributed fairly evenly across each NUMA node.  In this example, a total of 16 were requested and 8 have been assigned on each of the 2 NUMA nodes.
    ```
    $ cat /sys/devices/system/node/node*/hugepages/hugepages-1048576kB/nr_hugepages
    8
    8
    ```

5. Once the THPs have been reserved, they need to be mounted to make them available to the probe.
    ```
    cp /etc/fstab /etc/fstab.orig
    mkdir -p /mnt/huge_1GB
    echo "nodev /mnt/huge_1GB hugetlbfs pagesize=1GB 0 0" >> /etc/fstab
    mount -fav
    ```

#### Install DPDK

1. Install the required dependencies.
    ```
    yum -y install "@Development tools"
    yum -y install pciutils net-tools glib2 glib2-devel git
    yum -y install kernel kernel-devel kernel-headers
    ```

2. Decide where DPDK will be installed.
    ```
    export DPDK_HOME=/usr/local/dpdk/
    ```

3. Download, build and install DPDK.
    ```
    wget http://fast.dpdk.org/rel/dpdk-16.11.1.tar.xz -O - | tar -xJ
    cd dpdk-stable-16.11.1/
    make config install T=x86_64-native-linuxapp-gcc DESTDIR=$DPDK_HOME
    ```

4. Find the PCI address of the ethernet device that you plan on using to capture network packets.  In the following example I plan on binding `enp9s0f0` which has a PCI address of `09:00.0`.
    ```
    $ lspci | grep "VIC Ethernet"
    09:00.0 Ethernet controller: Cisco Systems Inc VIC Ethernet NIC (rev a2)
    0a:00.0 Ethernet controller: Cisco Systems Inc VIC Ethernet NIC (rev a2)
    ```

5. Bind the device.  Replace the device name and PCI address with what is appropriate for your environment.
    ```
    ifdown enp9s0f0
    modprobe uio_pci_generic
    $DPDK_HOME/sbin/dpdk-devbind --bind=uio_pci_generic "09:00.0"
    ```

6. Ensure that the device was bound. It should be shown as a 'network device using DPDK-compatible driver.'
    ```
    $ dpdk-devbind --status
    Network devices using DPDK-compatible driver
    ============================================
    0000:09:00.0 'VIC Ethernet NIC' drv=uio_pci_generic unused=enic
    Network devices using kernel driver
    ===================================
    0000:01:00.0 'I350 Gigabit Network Connection' if=eno1 drv=igb unused=uio_pci_generic
    ```

#### Install Librdkafka

The probe has been tested with [Librdkafka 0.9.4](https://github.com/edenhill/librdkafka/releases/tag/v0.9.4).

1. Choose an installation path.  In this example, the libs will actually be installed at `/usr/local/lib`; note that `lib` is appended to the prefix.
    ```
    export RDK_PREFIX=/usr/local
    ```

2. Download, build and install.
    ```
    wget https://github.com/edenhill/librdkafka/archive/v0.9.4.tar.gz  -O - | tar -xz
    cd librdkafka-0.9.4/
    ./configure --prefix=$RDK_PREFIX
    make
    make install
    ```

3. Ensure that the installation location is on the search path for the runtime shared library loader.
    ```
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$RDK_PREFIX/lib
    ```

#### Install Fastcapa

1. Set the required environment variables.
    ```
    export RTE_SDK=$DPDK_HOME/share/dpdk/
    export RTE_TARGET=x86_64-native-linuxapp-gcc
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$RDK_HOME
    ```

2. Build Fastcapa.  The resulting binary will be placed at `build/app/fastcapa`.
    ```
    cd metron/metron-sensors/fastcapa
    make
    ```


Usage
-----

Follow these steps to run Fastcapa.

1. Create a configuration file that at a minimum specifies your Kafka broker.  An example configuration file, `conf/fastcapa.conf`, is available that documents other useful parameters.
    ```
    [kafka-global]
    metadata.broker.list = kafka-broker1:9092
    ```

2. Bind the capture device.  This is only needed if the device is not already bound.  In this example, the device `enp9s0f0` with a PCI address of `09:00:0` is bound.  Use values specific to your environment.
    ```
    ifdown enp9s0f0
    modprobe uio_pci_generic
    $DPDK_HOME/sbin/dpdk-devbind --bind=uio_pci_generic "09:00.0"
    ```

3. Run Fastcapa.
    ```
    fastcapa -c 0x03 --huge-dir /mnt/huge_1GB -- -p 0x01 -t pcap -c /etc/fastcapa.conf
    ```

4. Terminate Fastcapa with `SIGINT` or by entering `CTRL-C`.  The probe will cleanly shut down all of the workers and allow the backlog of packets to drain.  To terminate the process without clearing the queue, send a `SIGKILL` or be entering `killall -9 fastcapa`.

### Parameters

Fastcapa accepts three sets of parameters.  

1. Command-line parameters passed directly to DPDK's Environmental Abstraction Layer (EAL)
2. Command-line parameters that define how Fastcapa will interact with DPDK.  These parametera are separated on the command line by a  double-dash (`--`).
3. A configuration file that define how Fastcapa interacts with Librdkafka.

#### Environmental Abstraction Layer Parameters

The most commonly used EAL parameter involves specifying which logical CPU cores should be used for processing.  This can be specified in any of the following ways.
```
  -c COREMASK         Hexadecimal bitmask of cores to run on
  -l CORELIST         List of cores to run on
                      The argument format is <c1>[-c2][,c3[-c4],...]
                      where c1, c2, etc are core indexes between 0 and 128
  --lcores COREMAP    Map lcore set to physical cpu set
                      The argument format is
                            '<lcores[@cpus]>[<,lcores[@cpus]>...]'
                      lcores and cpus list are grouped by '(' and ')'
                      Within the group, '-' is used for range separator,
                      ',' is used for single number separator.
                      '( )' can be omitted for single element group,
                      '@' can be omitted if cpus and lcores have the same value                     
```

To get more information about other EAL parameters, run the following.
```
fastcapa -h
```

#### Fastcapa-Core Parameters

| Name | Command | Description | Default |
|--------------------------|-----------------|---------------------------------------------------------------------------------------------------------------------------|---------|
| Port Mask | -p PORT_MASK | A bit mask identifying which ports to bind. | 0x01 |
| Receive Burst Size | -b RX_BURST_SIZE | The max number of packets processed by a receive worker. | 32 |
| Transmit Burst Size | -w TX_BURST_SIZE | The max number of packets processed by a transmit worker.  | 256 |
| Receive Descriptors | -d NB_RX_DESC | The number of descriptors for each receive queue (the size of the receive queue.)  Limited by the ethernet device in use. | 1024 |
| Transmission Ring Size | -x TX_RING_SIZE | The size of each transmission ring.  This must be a power of 2. | 2048 |
| Number Receive Queues | -q NB_RX_QUEUE | Number of receive queues to use for each port.  Limited by the ethernet device in use. | 2 |
| Kafka Topic | -t KAFKA_TOPIC | The name of the Kafka topic. | pcap |
| Configuration File | -c KAFKA_CONF | Path to a file containing configuration values. |  |
| Stats | -s KAFKA_STATS | Appends performance metrics in the form of JSON strings to the specified file. |  |

To get more information about the Fastcapa specific parameters, run the following.  Note that this puts the `-h` after the double-dash `--`.
```
fastcapa -- -h

fastcapa [EAL options] -- [APP options]
  -p PORT_MASK        bitmask of ports to bind                     [0x01]
  -b RX_BURST_SIZE    burst size of receive worker                 [32]
  -w TX_BURST_SIZE    burst size of transmit worker                [256]
  -d NB_RX_DESC       num of descriptors for receive ring          [1024]
  -x TX_RING_SIZE     size of tx rings (must be a power of 2)      [2048]
  -q NB_RX_QUEUE      num of receive queues for each device        [1]
  -t KAFKA_TOPIC      name of the kafka topic                      [pcap]
  -c KAFKA_CONF       file containing configs for kafka client
  -s KAFKA_STATS      append kafka client stats to a file
  -h                  print this help message
```

#### Fastcapa-Kafka Configuration File

The path to the configuration file is specified with the `-c` command line argument.  The file can contain any global or topic-specific, producer-focused [configuration values accepted by Librdkafka](https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md).  

The configuration file is a `.ini`-like Glib configuration file.  The global configuration values should be placed under a `[kafka-global]` header and topic-specific values should be placed under `[kafka-topic]`.

A minimally viable configuration file would only need to include the Kafka broker to connect to.
```
[kafka-global]
metadata.broker.list = kafka-broker1:9092, kafka-broker2:9092
```

The configuration parameters that are important for either basic functioning or performance tuning of Fastcapa include the following.

Global configuration values that should be located under the `[kafka-global]` header.

| *Name* | *Description* | *Default* |
|--------|---------------|-----------|
| metadata.broker.list | Initial list of brokers as a CSV list of broker host or host:port |  |
| client.id | Client identifier. |  |
| queue.buffering.max.messages | Maximum number of messages allowed on the producer queue | 100000 |
| queue.buffering.max.ms | Maximum time, in milliseconds, for buffering data on the producer queue | 1000 |
| message.copy.max.bytes | Maximum size for message to be copied to buffer. Messages larger than this will be passed by reference (zero-copy) at the expense of larger iovecs. | 65535 |
| batch.num.messages | Maximum number of messages batched in one MessageSet | 10000 |
| statistics.interval.ms | How often statistics are emitted; 0 = never | 0 |
| compression.codec | Compression codec to use for compressing message sets; {none, gzip, snappy, lz4 } | none |


Topic configuration values that should be located under the `[kafka-topic]` header.

| *Name* | *Description* | *Default* |
|--------|---------------|-----------|
| compression.codec | Compression codec to use for compressing message sets; {none, gzip, snappy, lz4 } | none |
| request.required.acks | How many acknowledgements the leader broker must receive from ISR brokers before responding to the request; { 0 = no ack, 1 = leader ack, -1 = all ISRs } | 1 |
| message.timeout.ms | Local message timeout. This value is only enforced locally and limits the time a produced message waits for successful delivery. A time of 0 is infinite. | 300000 |
| queue.buffering.max.kbytes | Maximum total message size sum allowed on the producer queue |  |

### Output

When running the probe some basic counters are output to stdout.  Of course during normal operation these values will be much larger.

```
     ------ in ------  --- queued --- ----- out ----- ---- drops ----
[nic]               8               -               -               -
[rx]                8               0                8                0
[tx]                8               0                8                0
[kaf]               8               1                7                0
```

* `[nic]` +  `in`:  The ethernet device is reporting that it has seen 8 packets.
* `[rx]` + `in`: The receive workers have consumed 8 packets from the device.
* `[rx]` + `out`: The receive workers have enqueued 8 packets onto the transmission rings.
* `[rx]` + `drops`: If the transmission rings become full it will prevent the receive workers from enqueuing additional packets.  The excess packets are dropped.  This value will never decrease.
* `[tx]` + `in`: The transmission workers have consumed 8 packets.
* `[tx]` + `out`: The transmission workers have packaged 8 packets into Kafka messages.
* `[tx]` + `drops`: If the Kafka client library accepted fewer packets than expected.  This value can increase or decrease over time as additional packets are acknowledged by the Kafka client library at a later point in time.
* `[kaf]` + `in`: The Kafka client library has received 8 packets.
* `[kaf]` + `out`: A total of 7 packets has successfully reached Kafka.
* `[kaf]` + `queued`: There is 1 packet within the `rdkafka` queue waiting to be sent.

### Kerberos

The probe can be used in a Kerberized environment.  Follow these additional steps to use Fastcapa with Kerberos.  The following assumptions have been made.  These may need altered to fit your environment.

* The Kafka broker is at `kafka1:6667`
* Zookeeper is at `zookeeper1:2181`
* The Kafka security protocol is `SASL_PLAINTEXT`
* The keytab used is located at `/etc/security/keytabs/metron.headless.keytab`
* The service principal is `metron@EXAMPLE.COM`

1. Build Librdkafka with SASL support (` --enable-sasl`).
    ```
    wget https://github.com/edenhill/librdkafka/archive/v0.9.4.tar.gz  -O - | tar -xz
    cd librdkafka-0.9.4/
    ./configure --prefix=$RDK_PREFIX --enable-sasl
    make
    make install
    ```

1. Validate Librdkafka does indeed support SASL.  Run the following command and ensure that `sasl` is returned as a built-in feature.
    ```
    $ examples/rdkafka_example -X builtin.features
    builtin.features = gzip,snappy,ssl,sasl,regex
    ```

   If it is not, ensure that you have `libsasl` or `libsasl2` installed.  On CentOS, this can be installed with the following command.
    ```
    yum install -y cyrus-sasl cyrus-sasl-devel cyrus-sasl-gssapi
    ```

1. Grant access to your Kafka topic.  In this example, it is simply named `pcap`.
    ```
    $KAFKA_HOME/bin/kafka-acls.sh --authorizer kafka.security.auth.SimpleAclAuthorizer \
      --authorizer-properties zookeeper.connect=zookeeper1:2181 \
      --add --allow-principal User:metron --topic pcap
    ```

1. Obtain a Kerberos ticket.
    ```
    kinit -kt /etc/security/keytabs/metron.headless.keytab metron@EXAMPLE.COM
    ```

1. Add the following additional configuration values to your Fastcapa configuration file.
    ```
    security.protocol = SASL_PLAINTEXT
    sasl.kerberos.keytab = /etc/security/keytabs/metron.headless.keytab
    sasl.kerberos.principal = metron@EXAMPLE.COM
    ```

1. Now run Fastcapa as you normally would.  It should have no problem landing packets in your kerberized Kafka broker.

How It Works
------

The probe leverages a poll-mode, burst-oriented mechanism to capture packets from a network interface and transmit them efficiently to a Kafka topic.  Each packet is wrapped within a single Kafka message and the current timestamp, as epoch microseconds in network byte order, is attached as the message's key.

The probe leverages Receive Side Scaling (RSS), a feature provided by some ethernet devices that allows processing of received data to occur across multiple processes and logical cores.  It does this by running a hash function on each packet, whose value assigns the packet to one, of possibly many, receive queues.  The total number and size of these receive queues are limited by the ethernet device in use.  More capable ethernet devices will offer a greater number and greater sized receive queues.  

 * Increasing the number of receive queues allows for greater parallelization of receive side processing.  
 * Increasing the size of each receive queue can allow the probe to handle larger, temporary spikes of network packets that can often occur.

A set of receive workers, each assigned to a unique logical core, are responsible for fetching packets from the receive queues.  There can only be one receive worker for each receive queue.  The receive workers continually poll the receive queues and attempt to fetch multiple packets on each request.  The maximum number of packets fetched in one request is known as the 'burst size'.  If the receive worker actually receives 'burst size' packets, then it is likely that the queue is under pressure and more packets are available.  In this case the worker immediately fetches another 'burst size' set of packets.  It repeats this process up to a fixed number of times while the queue is under pressure.

The receive workers then enqueue the received packets into a fixed size ring buffer known as a transmit ring.  There is always one transmit ring for each receive queue.  A set of transmit workers then dequeue packets from the transmit rings.  There can be one or more transmit workers assigned to any single transmit ring.  Each transmit worker has its own unique connection to Kafka.

* Increasing the number of transmit workers allows for greater parallelization when writing data to Kafka.
* Increasing the size of the transmit rings allows the probe to better handle temporary interruptions and latency when writing to Kafka.

After receiving the network packets from the transmit worker, the Kafka client library internally maintains its own send queue of messages.  Multiple threads are then responsible for managing this queue and creating batches of messages that are sent in bulk to a Kafka broker.  No control is exercised over this additional send queue and its worker threads, which can be an impediment to performance.  This is an opportunity for improvement that can be addressed as follow-on work.

Performance
-----------------
Beyond tuning the parameters previously described, the following should be carefully considered to achieve maximum performance.

### Kafka Partitions

Parallelizing access to a topic in Kafka is achieved by defining multiple partitions.  The greater the number of partitions, the more parallelizable access to that topic becomes.  To achieve high throughput it is important to ensure that the Kafka topic in use has a large number of partitions, evenly distributed across each of the nodes in your Kafka cluster.

The specific number of partitions needed will differ for each environment, but at least 128 partitions has been shown to significantly increase performance in some environments.

### Physical Layout

It is important to note the physical layout of the hardware when assigning worker cores to the probe.  The worker cores should be on the same NUMA node or socket as the ethernet device itself.  Assigning logical cores across NUMA boundaries can significantly impede performance.

The following commands can help identify logical cores that are located on the same NUMA node or socket as the ethernet device itself.  These commands should be run when the device is still managed by the kernel itself; before binding the interface.
```
$ cat /sys/class/net/enp9s0f0/device/local_cpulist
0-7,16-23
```

The following command can be used to better understand the physical layout of the CPU in relation to NUMA nodes.
```
$ lscpu
...
NUMA node0 CPU(s):     0-7,16-23
NUMA node1 CPU(s):     8-15,24-31
```

In this example `enp9s0f0` is located on NUMA node 0 and is local to the logical cores 0-7 and 16-23.  You should choose worker cores from this list.

### CPU Isolation

Once you have chosen the logical cores to use that are local to the ethernet device, it also beneficial to isolate those cores so that the Linux kernel scheduler does not attempt to run tasks on them.  This can be done using the `isolcpus` kernel boot parameter.
```
isolcpus=0,1,2,3,4,5,6,7
```

### Device Limitations

Check the output of running the probe to ensure that there are no device limitations that you are not aware of.  While you may have specified 16 receive queues on the command line, your device may not support that number.  This is especially true for the number of receive queues and descriptors.

The following example shows the output when the number of receive descriptors requested is greater than what can be supported by the device.  In many cases the probe will not terminate, but will choose the maximum allowable value and continue.  This behavior is specific to the underlying device driver in use.
```
PMD: rte_enic_pmd: Rq 0 Scatter rx mode enabled
PMD: rte_enic_pmd: Rq 0 Scatter rx mode not being used
PMD: rte_enic_pmd: Number of rx_descs too high, adjusting to maximum
PMD: rte_enic_pmd: Using 512 rx descriptors (sop 512, data 0)
PMD: rte_enic_pmd: Rq 1 Scatter rx mode enabled
PMD: rte_enic_pmd: Rq 1 Scatter rx mode not being used
PMD: rte_enic_pmd: Number of rx_descs too high, adjusting to maximum
PMD: rte_enic_pmd: Using 512 rx descriptors (sop 512, data 0)
PMD: rte_enic_pmd: TX Queues - effective number of descs:32
PMD: rte_enic_pmd: vNIC resources used:  wq 1 rq 4 cq 3 intr 0
```

### More Information

More information on this topic can be found in [DPDK's Getting Started Guide](http://dpdk.org/doc/guides/linux_gsg/nic_perf_intel_platform.html).

FAQs
----

### No free hugepages reported

Problem: When executing `fastcapa` it fails with the following error message.
```
EAL: No free hugepages reported in hugepages-1048576kB
PANIC in rte_eal_init():
Cannot get hugepage information
```

Solution: This can occur if any process that has been allocated THPs crashes and fails to return the resources.

* Delete the THP files that are not in use.
    ```
    rm -f /mnt/huge_1GB/rtemap_*
    ```

* If the first option does not work, re-mount the `hugetlbfs` file system.
    ```
    umount -a -t hugetlbfs
    mount -a
    ```

### No ethernet ports detected

Problem: When executing `fastcapa` it fails with the following error message.
```
EAL: Error - exiting with code: 1
  Cause: No ethernet ports detected.
```

* Solution 1: The `uio_pci_generic` kernel module has not been loaded.
```
modprobe uio_pci_generic
```

* Solution 2: Ensure that the ethernet device is bound. Re-bind if necessary.
```
 dpdk-devbind --unbind "09:00.0"
 dpdk-devbind --bind=uio_pci_generic "09:00.0"
 dpdk-devbind --status
```
