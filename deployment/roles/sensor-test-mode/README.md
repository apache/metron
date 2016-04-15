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
