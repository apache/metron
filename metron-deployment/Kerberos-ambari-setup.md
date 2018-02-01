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
# Setting Up Kerberos in Vagrant Full Dev
**Note:** These are instructions for Kerberizing Metron Storm topologies from Kafka to Kafka. This does not cover the sensor connections or MAAS.
General Kerberization notes can be found in the metron-deployment [README.md](../README.md)

## Setup a KDC
See [Setup a KDC](Kerberos-manual-setup.md#setup-a-kdc) and [Verify KDC](Kerberos-manual-setup.md#verify-kdc)

## Ambari Setup
1. Kerberize the cluster via Ambari. More detailed documentation can be found [here](http://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.5.3/bk_security/content/_enabling_kerberos_security_in_ambari.html).

    a. For this exercise, choose existing MIT KDC (this is what we setup and installed in the previous steps.)

    ![enable keberos](readme-images/enable-kerberos.png)

    ![enable keberos get started](readme-images/enable-kerberos-started.png)

    b. Setup Kerberos configuration. Realm is EXAMPLE.COM. The admin principal will end up as admin/admin@EXAMPLE.COM when testing the KDC. Use the password you entered during the step for adding the admin principal.

    ![enable keberos configure](readme-images/enable-kerberos-configure-kerberos.png)

    c. Click through to “Start and Test Services.” Let the cluster spin up.

## Push Data
1. Kinit with the metron user
    ```
    kinit -kt /etc/security/keytabs/metron.headless.keytab metron@EXAMPLE.COM
    ```

See [Push Data](Kerberos-manual-setup.md#push-data)

### More Information

See [More Information](Kerberos-manual-setup.md#more-information)
