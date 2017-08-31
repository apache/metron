- [Caveats](#caveats)
- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [E2E Tests](#e2e-tests)
- [Mpack Integration](#mpack-integration)
- [Installing on an existing Cluster](#installing-on-an-existing-cluster)

## Caveats
* UI doesn't have an authentication module yet
* UI uses local storage to save all the data.  A middleware needs to be designed and developed for persisting the data

## Prerequisites
* Elastic search should be up and running and should have some alerts populated by metron topologies
* The alerts can be populated using Quick Dev, Full Dev  or any other setup
* UI is developed using angular4 and uses angular-cli
* node.JS >= 7.8.0

## Development Setup

1. Install all the dependent node_modules using the following command
    ```
    cd metron/metron-interface/metron-alerts
    npm install
    ```
1. UI can be run by using the following command
    ```
    ./scripts/start-dev.sh
    ```
1. You can view the GUI @http://localhost:4200 . The default credentials for login are admin/password

**NOTE**: *In the development mode ui by default connects to ES at http://node1:9200 for fetching data. If you wish to change it you can change the ES url at metron/metron-interface/metron-alerts/proxy.conf.json*

## E2E Tests

An expressjs server is available for mocking the elastic search api.

1. Run e2e webserver :
    ```
    cd metron/metron-interface/metron-alerts
    sh ./scripts/start-server-for-e2e.sh
    ```

1. run e2e test using the following command
    ```
    cd metron/metron-interface/metron-alerts
    npm run e2e
    ```

1. E2E tests uses data from full-dev wherever applicable. The tests assume rest-api's are available @http://node1:8082

**NOTE**: *e2e tests covers all the general workflows and we will extend them as we need*

## Mpack Integration
Yet to come

## Installing on an existing Cluster
1. Build Metron:
    ```
    mvn clean package -DskipTests
    ```

1. Copy `metron/metron-interface/metron-alerts/target/metron-alerts-METRON_VERSION-archive.tar.gz` to the desired host.

1. Untar the archive in the target directory.  The directory structure will look like:
    ```
    bin
      start_alerts_ui.sh
    web
      alerts-ui
        package.json
        server.js
        web assets (html, css, js, ...)
    ```

1. [Expressjs](https://github.com/expressjs/express) webserver script is included in the build that will serve the application. (The script has few rewrite rules and we can replace expressjs with any other webserver)

1. Then start the application with the script:
    ```
    ./bin/start_alerts_ui.sh
    Usage: server.js -p [port] -r [restUrl]
    Options:
      -p             Port to run metron alerts ui                [required]
      -r, --resturl  Url where elastic search rest api is available  [required]
    ```
