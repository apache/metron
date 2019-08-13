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
# HBase Server

# Introduction

This project provides HBase server-side code such as coprocessors.

# Coprocessors

## Enrichment Coprocessor

### Properties

Below is the list of properties to configure the HBase enrichment coprocessor `org.apache.metron.hbase.coprocessor.EnrichmentCoprocessor`
for writing to HBase. These configuration properties all pulled from the global config.

#### `enrichment.list.hhase.provider.impl`

Provider to use for obtaining the HBase table. This class implementation implements `TableProvider` and provides access to an `HTableInterface`.
Defaults to `org.apache.metron.hbase.HTableProvider`.

#### `enrichment.list.hbase.table`

HBase table name for the enrichments list. Defaults to `enrichment_list`.

#### `enrichment.list.hbase.cf`

HBase table column family for the enrichments list. Defaults to `t`.

## Debugging

If you have trouble with a RegionServer failing to start due to a coprocessor problem, e.g. 
```
2019-08-13 14:37:40,793 ERROR [RS_OPEN_REGION-regionserver/node1:16020-0] regionserver.HRegionServer: ***** ABORTING region server node1,16020,1565707051425: The coprocessor org.apache.metron.hbase.coprocessor.EnrichmentCoprocessor threw...
```

you may need to temporarily disable coprocessor loading while you fix the issue.

### Disabling coprocessor loading

* Navigate to HBase > Config in Ambari
* Expand the `Custom hbase-site` subpanel
* Add the property "`hbase.coprocessor.enabled`" and set it to `false`. **Note:** you can also use the property `hbase.coprocessor.user.enabled` instead. From the HBase documentation:
    > Enables or disables user (aka. table) coprocessor loading. If 'false' (disabled), any table coprocessor attributes in table descriptors will be ignored. If "hbase.coprocessor.enabled" is 'false' this setting has no effect.
* Restart the HBase regionservers. You should notice a similar message to the following in your regionserver logs.
    ```
    2019-08-13 15:49:18,859 INFO  [regionserver/node1:16020] regionserver.RegionServerCoprocessorHost: System coprocessor loading is disabled
    2019-08-13 15:49:18,859 INFO  [regionserver/node1:16020] regionserver.RegionServerCoprocessorHost: Table coprocessor loading is disabled
    ```
* HBase should now start successfully

#### Reference

* https://hbase.apache.org/1.1/book.html#load_coprocessor_in_shell
* https://hbase.apache.org/1.1/book.html#hbase_default_configurations
