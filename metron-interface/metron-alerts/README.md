- [Caveats](#caveats)
- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [MPack Integration](#mpack-integration)
- [Installing on an existing Cluster](#installing-on-an-existing-cluster)

## Caveats
* UI doesn't have an authentication module yet
* UI uses local storage to save all the data.  A middleware needs to be designed and developed for persisting the data
* We need some good automation to test the UI any help in this regard is most welcome :)
* The UI has a left panel to show filters this is just UI mock for now the functionality is not yet done

## Prerequisites
* Elastic search should be up and running and should have some alerts populated by metron topologies
* The alerts can be populated using Quick Dev, Full Dev  or any other setup
* UI is developed using angular4 and uses angular-cli
* node.JS >= 7.8.0

## Development Setup

Install all the dependent node_modules using the following command
```
cd incubator-metron/metron-interface/metron-alerts
npm install
```
UI can be run by using the following command
```
./scripts/start-dev.sh
```
**NOTE**: *In the development mode ui by default connects to ES at http://node1:9200 for fetching data. If you wish to change it you can change the ES url at incubator-metron/metron-interface/metron-alerts/proxy.conf.json*

## Mpack Integration
Yet to come

## Installing on an existing Cluster
1. Build Metron:
    ```
    mvn clean package -DskipTests
    ```

1. Copy `incubator-metron/metron-interface/metron-alerts/target/metron-alerts-METRON_VERSION-archive.tar.gz` to the desired host.

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
