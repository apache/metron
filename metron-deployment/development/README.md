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
# Metron Development Environments

This directory contains environments useful for Metron developers.  These environments are not intended for proof-of-concept, testing, or production use.  These are extremely resource constrained and cannot support anything beyond the most basic work loads.

* Metron running on CentOS 6
* Metron running on Ubuntu 14
* Fastcapa


## Vagrant Cachier recommendations

The development boxes are designed to be spun up and destroyed on a regular basis as part of the development cycle. In order to avoid the overhead of re-downloading many of the heavy platform dependencies, Vagrant can use the [vagrant-cachier](http://fgrehm.viewdocs.io/vagrant-cachier/) plugin to store package caches between builds. If the plugin has been installed to your vagrant it will be used, and packages will be cached in ~/.vagrant/cache.

## Knox Demo LDAP

The development environment can be set up to authenticate against Knox's demo LDAP.

A couple notes
* A custom LDIF file is used to setup users. This is to get the roles and passwords setup correctly.
* The demo LDAP uses plaintext passwords with no encryption prefix (e.g. {SSHA}).
* You may need or want to shut down any or all of the topologies. This is optional, but clears some room

To setup this up, start full dev.
* In Ambari, add the Knox service (Actions -> +Add Service).  Accept all defaults and let it install. The configs that will be set how we need by default are:
  * LDAP URL = ldap://localhost:33389
  * User dn pattern = uid={0},ou=people,dc=hadoop,dc=apache,dc=org
  * LDAP user searchbase = ou=people,dc=hadoop,dc=apache,dc=org
  * Group Search Base = ou=groups,dc=hadoop,dc=apache,dc=org
  * Group Search Filter = member={0}
  * User Base DN = uid=admin,ou=people,dc=hadoop,dc=apache,dc=org
  * User Search Filter is empty
  * User password attribute = userPassword
  * LDAP group role attribute = cn
  * Bind User = uid=admin,ou=people,dc=hadoop,dc=apache,dc=org
  * LDAP Truststore is empty
  * LDAP Truststore Password is empty
  
* In the Knox configuration, go to "Advanced users-ldif". We have a custom ldif file "knox-demo-ldap.ldif" in "metron-deployment/development" that contains a customized variant of the users and groups defined here. Replace the default ldif configuration with the contents of "knox-demo-ldap.ldif"
* Start the Demo LDAP (In Knox, "Service Actions -> Start Demo LDAP)
* In Metron's configs, we're going to make two changes
  * Set "LDAP Enabled" to "On"
  * In Security, set "Bind user password" to match the admin user's password from the ldif file (admin-password).
* Restart the REST application

Now, when you go to Swagger or the UIs, you should be able to give a user and password.
"admin" will have the roles ROLE_ADMIN and ROLE_USER, which can be verified via the "/whoami/roles" endpoint in Swagger. Similarly, there is a user "sam" that only has ROLE_USER. A third user, "tom" has neither role.
