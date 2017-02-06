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

### [METRON-283: Migrate Geo Enrichment outside of MySQL](https://issues.apache.org/jira/browse/METRON-283)

#### Description

As of 0.3.0, a MySQL database was used for storage and retrieval of
GeoIP information during enrichment.
As of 0.3.1, the MySQL database is removed in favor of using MaxMind's
binary GeoIP files and stored on HDFS

After initial setup, this change is transparent and existing enrichment
definitions will run as-is.

#### Migration

While new installs will not require any additional steps, in an existing
install a script must be run to retrieve and load the initial data.

The shell script `geo_enrichment_load.sh` will retrieve MaxMind GeoLite2
data and load data into HDFS, and update the configuration to point to
this data.
In most cases the following usage will grab the data appropriately:

```
$METRON_HOME/bin/geo_enrichment_load.sh -z <zk_server>:<zk_port>
```

Additional options, including changing the source file location (which
can be a file:// location if the GeoIP data is already downloaded), are
available with the
-h flag and are also detailed in the metron-data-management README.me
 file.

One caveat is that this script will NOT update on disk config files. It
is recommended to retrieve the configuration using

```
$METRON_HOME/bin/zk_load_configs.sh -z <zk_server>:<zk_port> -m DUMP
```

The new config will be `geo.hdfs.file` in the global section of the
configuration. Append this key-value into the global.json in the config
directory. A PUSH is unnecessary

### [METRON-684: Decouple Timestamp calculation from PROFILE_GET](https://issues.apache.org/jira/browse/METRON-684)

#### Description

During 0.3.1 we decoupled specifying durations for calls to the profiler
into a separate function.  The consequence is that existing calls to
`PROFILE_GET` will need to migrate.

#### Migration

Existing calls to `PROFILE_GET` will need to change from `PROFILE_GET('profile', 'entity', duration, 'durationUnits')` to `PROFILE_GET('profile', 'entity', PROFILE_FIXED(duration, 'durationUnits'))`

