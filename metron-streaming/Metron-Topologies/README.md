#Metron-Topologies

#Module Description

This module provides example topologies that show how to drive Metron modules and components.  The sample topologies provided are to process PCAP, Ise, Lancope, and Bro telemetries

##Launching Topologies

We use Storm Flux to launch topologies, which are each described in a YAML file.

```
storm jar target/Metron-Topologies-0.1BETA.jar org.apache.storm.flux.Flux --local src/main/resources/Metron_Configs/topologies/bro/local.yaml --filter src/main/resources/Metron_Configs/etc/env/config.properties

storm jar target/Metron-Topologies-0.1BETA.jar org.apache.storm.flux.Flux --remote src/main/resources/Metron_Configs/topologies/bro/remote.yaml --filter src/main/resources/Metron_Configs/etc/env/config.properties
```

Note that if you use `--local` it will run the topology in local mode, using test data. If you use `--remote` it will attempt to connect to and deploy to Storm Nimbus.

Each topology's YAML files are responsible for either connecting to a real spout or enabling their own testing spout. This is the primary reason different `local.yaml` and `remote.yaml` files are provided for each topology.

##Topology Configs

The sample topologies provided use a specific directory structure.  The example directory structure was checked in here:

```
https://github.com/apache/incubator-metron/tree/master/metron-streaming/Metron-Topologies/src/main/resources/Metron_Configs
```

Each topology has a `local.yaml` and a `remote.yaml` file to support local mode and remote mode, respectively.

These topology configurations have variables that can be replaced by the `--filter` option to Flux. These variables are in `src/main/resources/OpenSOC_Configs/etc/env/config.properties`, and apply to:

- Kafka
- Elasticsearch
- MySQL
- Metrics
- Bolt acks/emits/fails
- Host enrichment
- HDFS
