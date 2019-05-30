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
# Indexing

## Introduction

The `indexing` module is a module dedicated to indexing data from
enrichment (or possible directly from parsers) into various data stores
including Elasticsearch, Solr and HDFS.

## Deployment Options

There is currently one option for running indexing in Metron, which is as a Storm topology.

## Submodules

* metron-indexing-common - this module contains common indexing code including IndexDao related-classes.
* metron-indexing-storm - this module is home to Storm-specific code such as Flux files and Storm indexing integration tests.