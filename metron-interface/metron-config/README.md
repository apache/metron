# Metron Management UI

This module provides a user interface for management functions in Metron.

## Prerequisites

* A network accessible Metron REST application
* nodejs v6.9+ (nodejs can be installed on quick dev with `curl --silent --location https://rpm.nodesource.com/setup_6.x | bash - && yum install -y nodejs`)

## Installation

### From Source

1. Package the application with Maven:

    ```
    cd metron-interface/metron-config
    mvn clean package
    ```

1. Untar the archive in the $METRON_HOME directory.  The directory structure will look like:

    ```
    bin
      metron-management-ui
    web
      expressjs
        package.json
        server.js
      management-ui
        web assets (html, css, js, ...)
    ```

1. Copy the `$METRON_HOME/bin/metron-management-ui` script to `/etc/init.d/metron-management-ui`

1. Install the [Express](https://expressjs.com/) web framework from the `package.json` file in `$METRON_HOME/web/expressjs`:

    ```
    npm --prefix $METRON_HOME/web/expressjs/ install
    ```

### From Package Manager

1. Deploy the RPM at `/metron/metron-deployment/packaging/docker/rpm-docker/target/RPMS/noarch/metron-config-$METRON_VERSION-*.noarch.rpm`

1. Install the RPM with:

    ```
    rpm -ih metron-config-$METRON_VERSION-*.noarch.rpm
    ```

1. Install the [Express](https://expressjs.com/) web framework from the `package.json` file in `$METRON_HOME/web/expressjs`:

    ```
    npm --prefix $METRON_HOME/web/expressjs/ install
    ```

## Configuration

The Managment UI is configured in the `$METRON_HOME/config/management_ui.yml` file.  Create this file and set the values to match your environment:

```
port: port the managment UI will run on

rest:
  host: REST application host
  port: REST applciation port
```

## Usage

After configuration is complete, the Management UI can be managed as a service:

```
service metron-management-ui start
```

The application will be available at http://host:4200 assuming the port is set to `4200`.  Logs can be found at `/var/log/metron/metron-management-ui.log`.

## Development

The Management UI can also be started in development mode.  This allows changes to web assets to be seen interactively.

1. Install the application with dev dependencies:

    ```
    cd metron-interface/metron-config
    npm install
    ```
  
1. Start the application:

    ```
    ./scripts/start_dev.sh
    ```

The application will be available at http://localhost:4200/.  The REST application url defaults to `http://localhost:8080` but can be changed in the `proxy.conf.json` file.

## Testing

1. Install the application with dev dependencies:

    ```
    cd metron-interface/metron-config
    npm install
    ```

1. Unit tests can be run with:

    ```
    npm test
    ```

## E2E Testing
### Prerequisites

- quick-dev-platfom should be up and running. For more information on running quick-dev click [here](https://github.com/apache/incubator-metron/tree/master/metron-deployment/vagrant/quick-dev-platform).
- quick-dev-platfom should have snort and bro parser topologies deployed and they should be in a running state. ( This should be available by default unless they are stopped manually ).
- Metron rest application should be up and running on quick-dev-platfom.  For more information on running metron rest on quick-dev-platfom click [here](https://github.com/apache/incubator-metron/blob/master/metron-interface/metron-rest/README.md#quick-dev).
- Metron rest application should be running as hdfs user. Once logged in as root you can run 'sudo su - hdfs' to become a hdfs user.
- Port 4200 should be available on the machine running management ui.

### Running E2E tests
1. Execute the script 'start_server_for_e2e.sh' by passing the port to run the config UI and metron rest URL as arguments. The script perform two tasks:
    - Build the UI
    - Start a expressjs server for the UI on the given port and proxy the rest calls to metron rest on quick-dev-platform

    Ex: If metron rest on quick-dev-platform is available on node1:8081 and you wish to start UI on port 4200 the command would look like
    ```
      sh ./scripts/start_server_for_e2e.sh  -p 4200 -r http://node1:8081

       -p  Port to run metron management ui                [required]
       -r  Url where metron rest application is available  [required]
    ```

1. You can execute all the test cases by issuing the following command.

    Note: Running all the test cases can be a time-consuming process since it involves starting and stopping parsers multiple times
    ```
    npm run e2e-all
    ```

1. You can also execute subset of test cases that would test all the functionality that does not involve starting of parsers. The command used for this as follows
   ```
    npm run e2e
   ```

NOTE: Automated UI test cases can be [flaky at times](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html). To rerun potentially flakey protractor tests before failing we are using [protractor-flake](https://www.npmjs.com/package/protractor-flake).

## License

This projects bundles Font Awesome which is available under the SIL Open Font License.  See http://fontawesome.io/license/ for more details.