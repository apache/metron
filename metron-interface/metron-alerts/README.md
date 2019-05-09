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
- [Caveats](#caveats)
- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [E2E Tests](#e2e-tests)
- [Cypress Tests](#cypress-tests)
- [Mpack Integration](#mpack-integration)
- [Installing on an existing Cluster](#installing-on-an-existing-cluster)

## Caveats
### Local Storage
UI uses local storage to save all the data.  A middleware needs to be designed and developed for persisting the data

### Search for Alert GUIDs
Alert GUIDs must be double-quoted when being searched on to ensure correctness of results, e.g. guid:"id1".

### Search for Comments
Users cannot search for the contents of the comment's in the Alerts-UI

### Meta alerts
Grouping/faceting requests and other aggregations do not return meta alerts.  This is because it's not clear what the intended results should be when there are multiple matching items.

Sorting has a similar caveat, in that if we are matching on multiple alerts, there is no well defined sort.

Alerts that are contained in a a meta alert are generally excluded from search results, because a user has already grouped them in a meaningful way.

## Prerequisites
* The Metron REST application should be up and running
* Elasticsearch or Solr should have some alerts populated by Metron topologies, depending on which real-time store is enabled
* The Management UI should be installed (which includes [Express](https://expressjs.com/))
* The alerts can be populated using Full Dev or any other setup
* UI is developed using Angular 6 and uses Angular CLI.
* nvm (or a similar node verison manager) should be installed. The node version required for this project is listed in the [.nvmrc](https://github.com/creationix/nvm#nvmrc) file.

## Installation

### From Source

1. Package the application with Maven:

    ```
    cd metron-interface/metron-alerts
    mvn clean package
    ```

1. Untar the archive in the $METRON_HOME directory.  The directory structure will look like:

    ```
    bin
      metron-alerts-ui
    web
      expressjs
        alerts-server.js
      alerts-ui
        web assets (html, css, js, ...)
    ```

1. Copy the `$METRON_HOME/bin/metron-alerts-ui` script to `/etc/init.d/metron-alerts-ui`

1. [Express](https://expressjs.com/) is installed at `$METRON_HOME/web/expressjs/` as part of the Management UI installation process.  The Management UI should be installed first on the same host as the Alerts UI.

### From Package Manager

1. Deploy the RPM at `/metron/metron-deployment/packaging/docker/rpm-docker/target/RPMS/noarch/metron-alerts-$METRON_VERSION-*.noarch.rpm`

1. Install the RPM with:

    ```
    rpm -ih metron-alerts-$METRON_VERSION-*.noarch.rpm
    ```

### From Ambari MPack

The Alerts UI is included in the Metron Ambari MPack.  It can be accessed through the Quick Links in the Metron service.

## Configuration

The Alerts UI is configured in the `$METRON_HOME/config/alerts_ui.yml` file.  Create this file and set the values to match your environment:

```
port: port the alerts UI will run on

rest:
  host: REST application host
  port: REST applciation port
```

## Global Configuration Properties

### `source.type.field`

The source type field name used in the real-time store. Defaults to `source:type`.

### `threat.triage.score.field`

The threat triage score field name used in the real-time store. Defaults to `threat:triage:score`.

## Usage

After configuration is complete, the Management UI can be managed as a service:

```
service metron-alerts-ui start
```

The application will be available at http://host:4201 assuming the port is set to `4201`.  Logs can be found at `/var/log/metron/metron-alerts-ui.log`.

## Development Setup

1. Switch to the correct node version and install all the dependent node_modules using the following commands
    ```
    cd metron/metron-interface/metron-alerts
    nvm use
    npm ci
    ```

    You're probably wondering why we use the `ci` command instead of `install`. By design, `npm install` will change the lock file every time it is ran. This happens whether or not dependencies have a new release or not because `npm install` still updates a unique identifier within the lock file.

    To prevent the lock file from being changed, run the `ci` command. This installs the modules listed in the lock file without updating it. The only case when you should run `npm install` is when you want to add a new dependency to the application. You can update dependencies with the `npm update` command.

    `nvm use` will ensure your local node version matches the one specified in the `.nvmrc` file. It doesn't necessarily mean that you'll have an npm version installed which includes the `ci` command. Make sure you have the latest npm version which comes with the `ci` command.

1. UI can be run by using the following command
    ```
    ./scripts/start-dev.sh
    ```
1. You can view the GUI @http://localhost:4201. The default credentials for login are admin/password

**NOTE**: *In the development mode ui by default connects to REST at http://node1:8082 for fetching data. If you wish to change it you can change the REST url at metron/metron-interface/metron-alerts/proxy.conf.json*

## E2E Tests

### Caveats
1. E2E tests uses data from full-dev wherever applicable. The tests assume rest-api's are available @http://node1:8082. It is recommended to shutdown all other Metron services while running the E2E tests including Parsers, Enrichment, Indexing and the Profiler.

1. E2E tests are run on headless chrome. To see the chrome browser in action, remove the '--headless' parameter of chromeOptions in metron/metron-interface/metron-alerts/protractor.conf.js file

1. E2E tests delete all the data in HBase table 'metron_update' and Elastic search index 'meta_alerts_index' for testing against its test data

1. E2E tests use [protractor-flake](https://github.com/NickTomlin/protractor-flake) to re-run flaky tests.

### Steps to run

1. An Express.js server is available for accessing the rest api. Run the e2e webserver:
    ```
    cd metron/metron-interface/metron-alerts
    sh ./scripts/start-server-for-e2e.sh
    ```

1. Run e2e tests using the following command:
    ```
    cd metron/metron-interface/metron-alerts
    npm run e2e
    ```

**NOTE**: *e2e tests cover all the general workflows and we will extend them as we need*

## Cypress Tests

Cypress is a test framework that allows developers and test engineers to create E2E or Integration tests and run them inside the browser. It differs from other testing tools by choosing to not use selenium webdriver to control the browser.

Cypress is added to our project as a developer dependency. This means it's installed when you run:
```
npm ci
```
or
```
npm install
```

Currently both Protractor and Cypress tests are available, so the previous section of this text is still relevant. However, in the near future we're planning to migrate fully to Cypress.
You can find the public discussion about this in the following link: [Discussion thread](https://lists.apache.org/thread.html/b6a0272c7809c05e8b7aff20171720e8ec76f8a0e9481169c37a4a4a@%3Cdev.metron.apache.org%3E)


### Steps to run

The easiest way to run Cypress locally is with the following command:
```
npm run cypress:ci
```
The same command runs on Travis CI as part of our build process.

If you would like to get some additional information or run test suites one by one you could start the test server manually
```
npm run build && npm run start:ci
```
and then you can reach the Cypress Dashboard locally with the following command
```
npm run cypress:open
```
From the dashboard, you'll be able to run tests separately and reach additional information about the test runs.

### Learn more

If you like to learn more about Cypress based tests please visit [Cypress.io](http://cypress.io).
You can find more information about debuggin in this [section of the official documentation](https://docs.cypress.io/guides/guides/debugging.html#Using-debugger).
