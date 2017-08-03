# Metron Parser Extension Deployment

Metron Parser Extensions have the common extension deployment of the [Metron Bundles](../../bundles-lib) containing the runtime
library and parser dependencies.  On top of this, parser extensions deploy the parser configurations required
to integrate into the Metron system.

## Bundle Deployment

These Bundles are deployed to HDFS under /apps/metron/extension_lib for system parsers ( parsers that are built and deployed with Metron itself).
Parser extensions created and managed outside the project have their bundles deployed to extension_alt_lib.

The bundles are loaded by Apache VFS as a composite file system, at which time they will be cached locally.

> NOTE: Bundles may also be deployed locally on the cluster under /usr/metron/VERSION/

```bash
    drwxrwxr-x   - metron hadoop          0 2017-06-27 16:15 /apps/metron/extension_lib
    drwxrwxr-x   - metron hadoop          0 2017-06-27 16:15 /apps/metron/extension_alt_lib
```
The system parsers bundles being deployed as such:
```bash
  [root@node1 0.4.0]# hadoop fs -ls /apps/metron/extension_lib
Found 11 items
-rwxr-xr-x   1 hdfs hdfs      39809 2017-06-27 16:15 /apps/metron/extension_lib/metron-parser-asa-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      27377 2017-06-27 16:15 /apps/metron/extension_lib/metron-parser-bro-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      26947 2017-06-27 16:15 /apps/metron/extension_lib/metron-parser-cef-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      24758 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-fireeye-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      76150 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-ise-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      21904 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-lancope-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      20624 2017-06-27 16:15 /apps/metron/extension_lib/metron-parser-logstash-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      23546 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-paloalto-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      23972 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-snort-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      24230 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-sourcefire-bundle-0.4.0.bundle
-rwxr-xr-x   1 hdfs hdfs      23790 2017-06-27 16:14 /apps/metron/extension_lib/metron-parser-websphere-bundle-0.4.0.bundle
```

## Configuration Deployment

- System Parser configurations are deployed to disk on Metron Parser cluster nodes, and are then deployed
to Zookeeper by the Ambari service installation.

Configurations for each parser are deployed to /usr/metron/VERSION/extension_etc/PARSERNAME.
The configurations include

- zookeeper
Configurations to be loaded into zookeeper, including configurations for enrichments (if present), indexing and parsers.

- patterns
GROK patterns by the parser

### On disk
```bash
extension_etc
└── parsers
    ├── asa
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── asa.json
    │   │       ├── indexing
    │   │       │   └── asa.json
    │   │       └── parsers
    │   │           └── asa.json
    │   └── patterns
    │       ├── asa
    │       └── common
    ├── bro
    │   ├── config
    │   │   ├── elasticsearch
    │   │   │   └── bro_index.template
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── bro.json
    │   │       ├── indexing
    │   │       │   └── bro.json
    │   │       └── parsers
    │   │           └── bro.json
    ├── cef
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── cef.json
    │   │       ├── indexing
    │   │       │   └── cef.json
    │   │       └── parsers
    │   │           └── cef.json
    ├── fireeye
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── fireeye.json
    │   │       ├── indexing
    │   │       │   └── fireeye.json
    │   │       └── parsers
    │   │           └── fireeye.json
    │   └── patterns
    │       ├── common
    │       └── fireeye
    ├── ise
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── ise.json
    │   │       ├── indexing
    │   │       │   └── ise.json
    │   │       └── parsers
    │   │           └── ise.json
    ├── lancope
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── lancope.json
    │   │       ├── indexing
    │   │       │   └── lancope.json
    │   │       └── parsers
    │   │           └── lancope.json
    ├── logstash
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── logstash.json
    │   │       ├── indexing
    │   │       │   └── logstash.json
    │   │       └── parsers
    │   │           └── logstash.json
    ├── paloalto
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── paloalto.json
    │   │       ├── indexing
    │   │       │   └── paloalto.json
    │   │       └── parsers
    │   │           └── paloalto.json
    ├── snort
    │   ├── config
    │   │   ├── elasticsearch
    │   │   │   └── snort_index.template
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── snort.json
    │   │       ├── indexing
    │   │       │   └── snort.json
    │   │       └── parsers
    │   │           └── snort.json
    ├── sourcefire
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── sourcefire.json
    │   │       ├── indexing
    │   │       │   └── sourcefire.json
    │   │       └── parsers
    │   │           └── sourcefire.json
    │   └── patterns
    │       ├── common
    │       └── sourcefire
    ├── squid
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── squid.json
    │   │       ├── indexing
    │   │       │   └── squid.json
    │   │       └── parsers
    │   │           └── squid.json
    │   └── patterns
    │       ├── common
    │       └── squid
    ├── websphere
    │   ├── config
    │   │   └── zookeeper
    │   │       ├── enrichments
    │   │       │   └── websphere.json
    │   │       ├── indexing
    │   │       │   └── websphere.json
    │   │       └── parsers
    │   │           └── websphere.json
    │   └── patterns
    │       ├── common
    │       └── websphere
    └── yaf
        ├── config
        │   ├── elasticsearch
        │   │   └── yaf_index.template
        │   └── zookeeper
        │       ├── enrichments
        │       │   └── yaf.json
        │       ├── indexing
        │       │   └── yaf.json
        │       └── parsers
        │           └── yaf.json
        └── patterns
            ├── common
            └── yaf
```
### In Zookeeper

```bash

[zk: localhost(CONNECTED) 4] ls /metron/topology/parsers
[websphere, cef, fireeye, asa, paloalto, logstash, jsonMap, lancope, sourcefire, ise, squid, bro, snort, yaf]

[zk: localhost(CONNECTED) 5] ls /metron/topology/indexing
[websphere, cef, fireeye, error, asa, paloalto, logstash, lancope, sourcefire, ise, squid, bro, snort, yaf]

[zk: localhost(CONNECTED) 7] ls /metron/topology/enrichments
[websphere, cef, fireeye, asa, paloalto, logstash, lancope, sourcefire, ise, squid, bro, snort, yaf]
```
Patterns are also deployed to HDFS: 

```bash
[root@node1 0.4.0]# hadoop fs -ls /apps/metron/patterns
Found 14 items
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/asa
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/bro
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/cef
-rwxr-xr-x   1 hdfs   hdfs         5202 2017-06-27 16:14 /apps/metron/patterns/common
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/fireeye
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/ise
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/lancope
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/logstash
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/paloalto
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/snort
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/sourcefire
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/squid
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/websphere
drwxrwxr-x   - metron hadoop          0 2017-06-27 16:14 /apps/metron/patterns/yaf
```

An example for a specific parser being:

```bash
root@node1 0.4.0]# hadoop fs -ls /apps/metron/patterns/asa
Found 2 items
-rwxr-xr-x   1 hdfs hadoop      13748 2017-06-27 16:14 /apps/metron/patterns/asa/asa
-rwxr-xr-x   1 hdfs hadoop       5202 2017-06-27 16:14 /apps/metron/patterns/asa/common

```

