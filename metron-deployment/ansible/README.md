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
# Ansible Assets

This directory contains all of the shared Ansible assets used to deploy Metron in a [development environment](../development/README.md).  The scope of what Ansible is used for is intentionally limited.  The recommended means of deploying Metron is to use the [Metron MPack](../packaging/ambari/metron-mpack/README.md) for [Apache Ambari](https://ambari.apache.org/).

Ansible is only used primarily to prepare the development environment for Ambari and for deploying a suite of test sensors to drive telemetry through Metron.  The Ansible assets contained here are of limited use outside of this scenario.  

**Warning** It is not recommended that you use these assets for deploying Metron in your environment for either production or testing.  Support for this use case cannot be provided.
