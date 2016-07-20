# Overview
The Metron ansible-docker container is provided in an effort reduce the installation burden of deploying Metron in a live envirionment.
It is provisioned with software required to sucessfully run the deployment scripts.

## Building the Container
1. Install Docker [https://www.docker.com/products/overview]
2. Navigate to <project-directory>/metron-deployment/packaging/docker/ansible-docker
3. Build the container `docker build -t ansible-docker:2.0.0.2 .`

## Using the Container
Full instructions are found on the wiki [https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=65144361].

tl;dr

1. docker run -it -v <project-directory>:/root/incubator-metron ansible-docker:2.0.0.2 bash
2. cd /root/incubator-metron
3. mvn clean package -DskipTests
