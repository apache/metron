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
# Upgrade Steps
General guidance for upgrading Metron

1. Cut-off all inputs to Metron
1. Monitor processing until all in-flight data is indexed and stored
1. Stop Metron service
1. Run the [Metron Upgrade Helper](./metron-platform/metron-common#metron-upgrade-helper) script in backup mode - does these 2 tasks
    1. Export zookeeper based metron configs
    1. Export ambari-based metron configs
1. Delete the metron service via Ambari
1. Upgrade OS, if applicable
1. Major Hadoop platform upgrade steps (example. using HDP 2.6 to 3.x) - https://docs.cloudera.com/HDPDocuments/Ambari-2.7.3.0/bk_ambari-upgrade-major/content/ambari_upgrade_guide.html
   1. Update Ambari to latest version
   1. Update to HDP 3.1 using existing Ambari/HDP update documentation (including Solr/ES etc).
1. Reinstall Metron mpack using "--force" option
1. Reinstall Metron service
1. Turn off Metron service
1. Tweak any configs required to support new Metron version
   1. Minimally, you should update `metron.home` in `Ambari -> Metron -> Configs -> Advanced metron-env -> Metron home` to point to the new Metron home dir.
1. Run upgrade helper script in restore mode - does these 2 tasks
   1. Re-import zookeeper based metron configs
   1. Re-import ambari-based zookeeper configs
1. Turn on Metron service
