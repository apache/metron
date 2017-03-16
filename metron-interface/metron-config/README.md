# Metron Management UI

This module provides a user interface for management functions in Metron.

## Prerequisites

* A running metron-rest application
* npm 3.8.9+

## Installation
1. Package the application with Maven:
  ```
  mvn clean package
  ```
1. Copy the `metron-config-$METRON_VERSION-archive.tar.gz` archive to the desired host.

1. Untar the archive in the target directory.  The directory structure will look like:
  ```
  bin
    start_management_ui.sh
  dist
    web assets (html, css, js, ...)
  ```

1. Set the `METRON_REST_URL` environment variable to the url of REST application.  This defaults to `http://localhost:8080`.  Set the `METRON_MANAGEMENT_UI_PORT` environment variable to the desired port.  This defaults to `4200`.

1. Start the application with this command:
  ```
  ./bin/start_management_ui.sh
  ```

## Usage

The application will be available at http://host:4200 assuming the default port.

## Development

The management UI can also be started in development mode.  This allows changes to web assets to be seen interactively.

1. Install the application with dev dependencies:
  ```
  npm install
  ```
  
2. Start the application:
  ```
  ./scripts/start_dev.sh
  ```

The application will be available at http://localhost:4200/.  The REST application url defaults to `http://localhost:8080` but can be changed in the `proxy.conf.json` file.
  
## Testing

1. Install the application with dev dependencies:
  ```
  npm install
  ```

2. Unit tests can be run with:
  ```
  ng test
  ```
  
Note:  When testing on quick dev, npm needs to be installed and updated with:
  ```
  yum install npm
  npm install npm@3.8.9 -g
  ```