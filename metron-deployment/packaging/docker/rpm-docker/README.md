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
# Overview
The Metron ansible-docker container is provided in an effort reduce the installation burden of deploying Metron in a live envirionment.
It is provisioned with software required to sucessfully run the deployment scripts.

## Building the Container
1. Install Docker ( https://www.docker.com/products/overview )
2. Navigate to \<project-directory\>/metron-deployment/packaging/rpm-docker
3. Build the container `docker build -t rpm-docker .`
