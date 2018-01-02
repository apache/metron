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
The Metron ansible-docker container is provided in an effort reduce the installation burden of building Metron.
It may also be used to deploy Metron in a live environment.
It is provisioned with software required to sucessfully build metron and run the deployment scripts.

## Building the Container
1. Install Docker ( https://www.docker.com/products/overview )
2. Navigate to \<project-directory\>/metron-deployment/packaging/docker/ansible-docker
3. Build the container `docker build -t ansible-docker:2.0.0.2 .`

## Using the Container to build metron
anytime after building the container you can run it with the following command

`docker run -it -v \<project-directory\>:/root/metron ansible-docker:2.0.0.2 bash`

If you are going to build metron multiple times, you may want to map the /root/.m2 maven
repo from outside of the container so that you don't start with an empty repo every build and have to download
the world.

`docker run -it -v \<project-directory\>:/root/metron -v \<your .m2 directory\>:/root/.m2 ansible-docker:2.0.0.2 bash`

After running the container:

1. cd /root/metron
2. run build commands, for example:
  - build metron without tests : `mvn clean package -DskipTests`
  - build metron and build the rpms as well : `mvn clean install && cd metron-deployment && mvn package -P build-rpms`

If you wish to use this build with a vagrant instance, then after building with rpms as above, modify
your usual vagrant up command to skip the build role, as so:

`vagrant --ansible-skip-tags="build" up`


## Using the Container for deployment

> Note these instructions are outdated

Full instructions are found on the wiki at https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=65144361
