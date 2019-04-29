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
# Enrichment

## Introduction

The `enrichment` module is a module dedicated to taking the data
from the parsers that have been normalized into the Metron
data format (e.g. a JSON Map structure with `original_message` and
`timestamp`) and
* Enriching messages with external data from data stores (e.g. hbase) by
  adding new fields based on existing fields in the messages.
* Marking messages as threats based on data in external data stores
* Marking threat alerts with a numeric triage level based on a set of
  Stellar rules.

## Deployment Options

There is currently one option for running enrichments in Metron, which is as a Storm topology.

## Submodules

* metron-enrichment-common - this module houses the prepackaged enrichment configuration by sensor. It also contains the core enrichment and threat intelligence processing functionality.
* metron-common-storm - this module is home to Storm-specific code such as Flux files and Storm Bolts.

## Enrichments List

Metron provides an HBase table for storing enrichments. The rowkeys are a combination of a salt (for managing
RegionServer hotspotting), indicator (this would be the search value, e.g. "192.168.1.1"), and type (whois, geoip, etc.).

This approach performs well for both inserts and lookups, but poses a challenge when looking to get an
up-to-date list of all the current enrichments. This is of particular concern for the Management UI where
it's desirable to provide an accurate list of all available enrichment types.
A table scan is undesirable because it results in a performance hit for inserts and reads.
The alternative approach that mitigates these performance bottlenecks is to leverage a custom HBase
Coprocessor which will listen to postPut calls from the RegionServer, extract the enrichment type from the rowkey, and
perform an insert into a separate `enrichment_list` HBase table.

See more about configuring the coprocessor here [Enrichment Coprocessor](../metron-hbase-server/#enrichment-coprocessor)
