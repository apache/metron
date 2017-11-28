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

`vagrant --ansible-skip-tags="build,quick_dev" up`


## Using the Container for deployment

> Note these instructions are outdated

Full instructions are found on the wiki at https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=65144361

