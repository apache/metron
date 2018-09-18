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
# Metron Management UI

This module provides a user interface for management functions in Metron.

## Prerequisites

* A network accessible Metron REST application
* nvm (or a similar node verison manager) should be installed. The node version required for this project is listed in the [.nvmrc](https://github.com/creationix/nvm#nvmrc) file.

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

1. Switch to the correct node version and install all the dependent node_modules using the following commands:

    ```
    cd metron-interface/metron-config
    nvm use
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

## License

This projects bundles Font Awesome which is available under the SIL Open Font License.  See http://fontawesome.io/license/ for more details.
