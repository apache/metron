
This project builds DEB packages for installing Apache Metron on Ubuntu.

**WARNING**: The DEB packages are a recent addition to Metron.  These packages have not undergone the same level of testing as the RPM packages.  Improvements and more rigerous testing of these packages is underway and will improve in future releases.  Until then, use these at your own risk.

### Prerequisites

Building these packages requires Docker.  Please ensure that Docker is installed and that the daemon is running.

### How do I create packages for Ubuntu?

Running the following from Metron's top-level source directory will build Metron along with the DEB packages.  Using the `package` or `install` target are both sufficient.

  ```
  mvn clean package -DskipTests -Pbuild-debs
  ```

If Metron has already been built, just the DEB packages can be built by doing the following.

  ```
  cd metron-deployment
  mvn clean package -Pbuild-debs
  ```

### How does this really work?

Using the `build-debs` profile as shown above effectively automates the following steps.

1. Copy the tarball for each Metron sub-project to the `target` working directory.

1. Build a Docker image of a Ubuntu Trusty host called `docker-deb` that contains all of the tools needed to build the packages.

    ```
    docker build -t deb-docker .
    ```

1. Execute the `build.sh` script within a Docker container.  The argument passed to the build script is the current version of Metron.

    ```
    docker run -v `pwd`:/root deb-docker:latest /bin/bash -c ./build.sh 0.4.2
    ```

1. This results in the DEBs being generated within the `target` directory.
