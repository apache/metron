# Metron Management UI

This module provides a user interface for management functions in Metron.

## Prerequisites

* A network accessible Metron REST application
* npm 3.8.9+ (npm can be installed on quick dev with `yum install npm && npm install npm@3.8.9 -g`)

## Installation
1. Build Metron:
    ```
    mvn clean package -DskipTests
    ```
  
1. Copy `incubator-metron/metron-interface/metron-config/target/metron-config-METRON_VERSION-archive.tar.gz` to the desired host.

1. Untar the archive in the target directory.  The directory structure will look like:
    ```
    bin
      start_management_ui.sh
    dist
      web assets (html, css, js, ...)
    ```

1. For production use, the contents of the `dist` directory should be deployed to a web server with paths `/api/v1` and `/logout` mapped to the REST application url.  

1. As an example, a convenience script is included that will install a simple [http-server](https://github.com/indexzero/http-server), set the root context path to `./dist`, and exposes proxy settings as environment variables.  Set the `METRON_REST_URL` environment variable (`http://localhost:8080` by default) to the url of REST application.  Set the `METRON_MANAGEMENT_UI_PORT` environment variable (`4200` by default) to the desired port.

1. Then start the application with the script:
    ```
    ./bin/start_management_ui.sh
    ```

## Usage

The application will be available at http://host:4200 with credentials `user/password`, assuming the default port is configured and the `dev` profile is included when starting the REST application.  See the [REST application](../metron-rest#security) documentation for more information about security configuration for production.

## Development

The Management UI can also be started in development mode.  This allows changes to web assets to be seen interactively.

1. Install the application with dev dependencies:
    ```
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
    npm install
    ```

1. Unit tests can be run with:
    ```
    npm test
    ```
  