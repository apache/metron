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

This provides a Docker Container containing all of the prerequisites required to build Metron.  This allows you to easily build Metron without installing all of the build dependencies manually.

### Prerequisites

* Docker: Please ensure that Docker is installed and that the daemon is running.

### Quick Start

1. Build the Docker container.
    ```
    cd metron-deployment/packaging/docker/ansible-docker
    docker build -t ansible-docker:latest .
    ```

1. Launch the container.
    ```
    docker run -it \
      -v `pwd`/../../../..:/root/metron \
      -v ~/.m2:/root/.m2 \
      ansible-docker:latest bash
    ```

    This maps the Metron source code along with your local Maven repository to the container.  This will prevent you from having to re-download all of these dependencies each time you build Metron.

1. Execute the following inside the Docker container to build Metron.

    ```
    cd /root/metron
    mvn clean package -DskipTests
    ```

### Notes

If you wish to use this build with a vagrant instance, then after building with rpms as above, modify
your usual vagrant up command to skip the build role, as so:
  ```
  vagrant --ansible-skip-tags="build" up
  ```
