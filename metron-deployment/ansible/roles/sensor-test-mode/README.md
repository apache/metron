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
Sensor Test Mode
================

A role that configures each of the sensors to produce the maximum amount of telemetry data.  This role is useful only for testing.  It can be useful to support functional, performance, and load testing of Apache Metron.

The role does the following to maximize the amount of telemetry data produced by each Metron sensor.

- Plays a packet capture file through a network interface to simulate live network traffic.
- Configures [YAF](https://tools.netsa.cert.org/yaf/yaf.html) with `idle-timeout=0`.  This causes a flow record to be produced for every network packet received.
- Configures [Snort](https://www.snort.org/) to produce an alert for every network packet received.

Getting Started
---------------

To enable the `sensor-test-mode` role apply the role to the `sensors` host group in your Ansible playbook.

```
- hosts: sensors
  roles:
    - role: sensor-test-mode
```

The role has also been added to the default `metron_install.yml` playbook so that it can be turned on/off with a property in both the local Virtualbox and the remote EC2 deployments.

```
sensor_test_mode: True
```
