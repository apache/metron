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
