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
