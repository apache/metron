# Upgrading
This document constitutes a per-version listing of changes of
configuration which are non-backwards compatible.

## 0.2.0BETA to 0.3.0
### [METRON-447: Monit fails to reload when upgrading from 0.2.0BETA to master](https://issues.apache.org/jira/browse/METRON-447)

#### Description

`/etc/monit.d/enrichment-elasticsearch.monit` was renamed to
`/etc/monit.d/indexing-elasticsearch.monit`, however the old file isn't
removed via ansible, which causes the below error during an upgrade:
`Starting monit: /etc/monit.d/enrichment-elasticsearch.monit:18: Service
name conflict, enrichment already defined
'/usr/local/monit/status_enrichment_topology.sh'`

### [METRON-448:Upgrading via Ansible deployment does not add topology.classpath ](https://issues.apache.org/jira/browse/METRON-448)

#### Description
When using Ansible to deploy the latest Metron bits to an existing installation, storm-site is not being updated with the new 0.2.1BETA parameter `topology.classpath`. Topologies are unable to find the client configs as a result.

#### Workaround
Set the `topology.classpath` property for storm in Ambari to `/etc/hbase/conf:/etc/hadoop/conf`

## 0.3.0 to 0.3.1

### [METRON-664: Make the index configuration per-writer with enabled/disabled](https://issues.apache.org/jira/browse/METRON-664)

#### Description

As of 0.3.0 the indexing configuration
* Is held in the enrichment configuration for a sensor 
* Has properties which control every writers (i.e. HDFS, solr or elasticsearch).

In the 0.3.1 release, this configuration has been broken out
and control for individual writers are separated.

Please see the description of the configurations in the indexing [README](https://github.com/apache/incubator-metron/tree/Metron_0.3.1/metron-platform/metron-indexing#sensor-indexing-configuration)

#### Migration

Migrate the configurations from each sensor enrichment configuration and create appropriate configurations for indexing.

For instance, if a sensor enrichment config for sensor `foo`
is in `$METRON_HOME/config/zookeeper/enrichments/foo.json` and looks like
```
{
  "index" : "foo",
  "batchSize" : 100
}
```

You would create a file to configure each writer for sensor `foo` called `$METRON_HOME/config/zookeeper/indexing/foo.json` with the contents
```
{
  "elasticsearch" : {
    "index" : "foo",
    "batchSize" : 100,
    "enabled" : true
  },
  "hdfs" : { 
    "index" : "foo",
    "batchSize" : 100,
    "enabled" : true
  }
}
```

### [METRON-675: Make Threat Triage rules able to be assigned names and comments](https://issues.apache.org/jira/browse/METRON-675)

#### Description

As of 0.3.0, threat triage rules were defined as a simple Map associating a Stellar expression with a score.
As of 0.3.1, due to the fact that there may be many threat triage rules, we have made the rules more complex.
To help organize these, we have made the threat triage objects in their own right that contain optional name and optional comment fields.
   
This essentially makes the risk level rules slightly more complex.  The format goes from:
```
"riskLevelRules" : {
    "stellar expression" : numeric score
}
```
to:
```
"riskLevelRules" : [
     {
        "name" : "optional name",
        "comment" : "optional comment",
        "rule" : "stellar expression",
        "score" : numeric score
     }
]
```
   
#### Migration

For every sensor enrichment configuration, you will need to migrate the `riskLevelRules` section
to move from a map to a list of risk level rule objects.
