#Metron-Topologies

#Module Description

This module provides example topologies that show how to drive Metron modules and components.  The sample topologies provided are to process PCAP, Ise, Lancope, and Bro telemetries

##Launching Topologies


```

storm jar Metron-Topologies-0.6BETA.jar com.apache.metron.topology.Pcap
storm jar Metron-Topologies-0.6BETA.jar com.apache.metron.topology.Sourcefire
storm jar Metron-Topologies-0.6BETA.jar com.apache.metron.topology.Lancope
storm jar Metron-Topologies-0.6BETA.jar com.apache.metron.topology.Ise

Topology Options:
-config_path <arg>       OPTIONAL ARGUMENT [/path/to/configs] Path to
configuration folder. If not provided topology
will initialize with default configs
-debug <arg>             OPTIONAL ARGUMENT [true|false] Storm debugging
enabled.  Default value is true
-generator_spout <arg>   REQUIRED ARGUMENT [true|false] Turn on test
generator spout.  Default is set to false.  If
test generator spout is turned on then kafka
spout is turned off.  Instead the generator
spout will read telemetry from file and ingest
it into a topology
-h                       Display help menue
-local_mode <arg>        REQUIRED ARGUMENT [true|false] Local mode or
cluster mode.  If set to true the topology will
run in local mode.  If set to false the topology
will be deployed to Storm nimbus
```

##Topology Configs

The sample topologies provided use a specific directory structure.  The example directory structure was checked in here:

```
https://github.com/apache/incubator-metron-streaming/tree/master/Metron-Topologies/src/main/resources/Metron_Configs
```

topology.conf - settings specific to each topology
features_enabled.conf - turn on and off features for each topology and control parallelism
metrics.conf - export definitions for metrics to Graphite 
topology_dentifier.conf - customer-specific tag (since we deploy to multiple data centers we need to identify where the alerts are coming from and what topologies we are looking at when we need to debug)
