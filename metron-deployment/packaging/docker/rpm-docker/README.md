# Overview
The Metron ansible-docker container is provided in an effort reduce the installation burden of deploying Metron in a live envirionment.
It is provisioned with software required to sucessfully run the deployment scripts.

## Building the Container
1. Install Docker [https://www.docker.com/products/overview]
2. Navigate to <project-directory>/metron-deployment/packaging/rpm-docker
3. Build the container `docker build -t rpm-docker .`
