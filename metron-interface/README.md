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
# Metron Interface

Metron interface contains code and assets to support the various web applications in Metron.  The existing modules are:

* metron-alerts : An Angular application that exposes a way to browse, filter and act on alerts.
* metron-config: An Angular application that allows an administrator to configure and maintain Metron.
* metron-rest: A Spring REST application that supports the UI and exposes Metron features through a REST/json interface.
* metron-rest-client: Model objects used for passing data back and forth in the REST application.  A Metron client would use these classes for de/serializing requests and responses.

## Architecture

The UIs and REST server are all run in separate web containers.  The UIs are served from separate [Express](https://expressjs.com/) servers that are configured to proxy REST requests
to the REST application.  Proxying REST requests satisfies the same-origin browser restriction because all requests go to Express, or the same origin.  

REST requests are handled by a [Spring Boot](https://spring.io/projects/spring-boot) application.  A [Swagger](https://swagger.io/) interface is available and served by the REST application.

### Security

The UIs depend on REST for authentication.  Credentials are passed with [Basic authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication) and only REST requests require
authentication, static assets are not secured.  Once authentication has been performed a session is established and a cookie is stored and used to authenticate subsequent requests.

### Request/Response Flow

The following diagram illustrates the flow of data for the various types of requests:

![Flow Diagram](flow_diagram.png)


## Architecture with Knox

[Apache Knox](https://knox.apache.org/) is a "REST API and Application Gateway for the Apache Hadoop Ecosystem".  It can be enabled for Metron and provides several security benefits:

* All requests go through Knox so same-origin browser restrictions are not a concern.
* Knox, in combination with a firewall, can restrict traffic to always go through Knox.  This greatly reduces the security attack surface area of the UIs and REST application.
* Provides access to other common Apache Hadoop services
* Provides a single sign on experience between the UIs and REST application
* All requests can be protected and secured

We primarily use Knox's proxying and authentication services.  Knox acts as a reverse proxy for all UIs and the REST application.  

### Knox Security

With Knox enabled, Knox now handles authentication when accessing the UIs and REST together.  Basic authentication is still an option for making requests directly to the REST application.  Any request to the UIs must go through Knox first and contain the proper security token.  
If a valid token is not found, Knox will redirect to the Knox SSO login form.  Once a valid token is found, Knox will then redirect to the original url and the request will be forwarded on.  Accessing the REST application through Knox also follows this pattern.
The UIs make REST requests this way with Knox enabled since they no longer depend on Express to proxy requests.  The context path now determines which type of request it is rather than the host and port.  

REST still requires authentication so a filter is provided that can validate a Knox token using token properties and a Knox public key.  The REST application also supports Basic authentication.  Since both Knox and the REST application should use
the same authentication mechanism, LDAP authentication is required for the REST application.

Roles are mapped directly to LDAP groups when Knox is enabled for REST.  LDAP group names are converted to upper case and prepended with "ROLE_".  For example, if a user's groups in LDAP were "user" and "admin", the corresponding roles in REST with Knox enabled would be "ROLE_USER" and "ROLE_ADMIN".

### Knox Request/Response Flow

The following diagram illustrates the flow of data for the various types of requests when Knox is enabled:

![Knox Flow Diagram](knox_flow_diagram.png)

Note how the flow diagrams for Static asset requests and Rest requests (through Knox) are identical.

## Enabling Knox for Metron

Follow the instructions in the next 3 sections to enable Knox for Metron.  The new Knox urls will be similar to (substitute the Knox host/port in your environment for `node1:8443`):

- Metron Alerts UI - https://node1:8443/gateway/metron/metron-alerts/
- Metron Management UI - https://node1:8443/gateway/metron/metron-management/sensors
- Metron REST - https://node1:8443/gateway/metron/metron-rest/swagger-ui.html



### Install Metron Clients

The Metron Client component in Ambari is responsible for installing the service definition and topology files in the appropriate Knox directories.  These files are installed whenever the Metron Client component(s) are installed, started or restarted AND the `Knox Enabled` Metron Ambari property
is set to true (under the `Security` tab in the Metron Config section).  Ambari calls the script at `$METRON_HOME/bin/install_metron_knox.sh` which installs the following files:

- `$KNOX_HOME/conf/topologies/metron.xml`
- `$KNOX_HOME/conf/topologies/metronsso.xml`
- `$KNOX_HOME/data/services/metron-alerts/$METRON_VERSION/rewrite.xml`
- `$KNOX_HOME/data/services/metron-alerts/$METRON_VERSION/service.xml`
- `$KNOX_HOME/data/services/metron-management/$METRON_VERSION/rewrite.xml`
- `$KNOX_HOME/data/services/metron-management/$METRON_VERSION/service.xml`
- `$KNOX_HOME/data/services/metron-rest/$METRON_VERSION/rewrite.xml`
- `$KNOX_HOME/data/services/metron-rest/$METRON_VERSION/service.xml`

A Metron Client should be installed anywhere a Knox Gateway is installed.  It is not strictly required but Metron will not be available through any Knox Gateways that do not have these files installed.
  
### Enable Knox for Metron in Ambari

After Metron Client components have been installed on the appropriate hosts, there are a couple settings that need to be changed in the Ambari "Security" tab.  
First the Knox SSO public key needs to be read from Knox.  Run the following command on a Knox Gateway to get the key:
```
openssl s_client -connect node1:8443 < /dev/null | openssl x509 | grep -v 'CERTIFICATE' | paste -sd "" -
```
The `Knox SSO Public Key` Ambari property should be set to the output of that command.  In the same section, set the `Knox Enabled` setting to "ON".  

After these changes have been made, the following Metron components must be restarted:

- Metron Client
- Metron Alerts UI
- Metron Management UI
- Metron REST

Any change to the settings described in this section will cause Ambari to suggest restarting these services.  Either restart them manually or follow the prompt in Ambari.

Note:  Knox can only be enabled for Metron when Metron authentication is set to `LDAP` (the `LDAP Enabled` setting in Ambari).  
If `LDAP` is not enabled an error will be thrown when any of the previous components are restarted. 

### Update Quicklinks (Optional)

Currently there is no way to dynamicly change the quick links in Ambari to the new Knox urls so it must be done manually.  Locate this file on the Ambari server host:
```
/var/lib/ambari-server/resources/mpacks/metron-ambari.mpack-$METRON_MPACK_VERSION/common-services/METRON/$METRON_VERSION/quicklinks/quicklinks.json
```
Quicklinks for each component are defined as an array of json objects under the `/configuration/links/` attribute.

Locate the json object for the component you want to update.  Change the `url` attribute to match the new Knox url.  For example, to update the Metron REST quick link, locate the json object with the `label` property set to `Swagger UI`.  Change the `url` property from
`%@://%@:%@/swagger-ui.html` to `https://<knox gateway host>:<knox gateway port>/gateway/metron/metron-rest/swagger-ui.html`.

The json object should look like this in full dev:
```
{
    "name": "metron_rest_ui",
    "label": "Swagger UI",
    "requires_user_name": "false",
    "component_name": "METRON_REST",
    "url":"https://node1:8443/gateway/metron/metron-rest/swagger-ui.html",
    "port":{
      "http_property": "metron_rest_port",
      "http_default_port": "8082",
      "https_property": "metron_rest_port",
      "https_default_port": "8082",
      "regex": "^(\\d+)$",
      "site": "metron-rest-env"
    }
}
```
Repeat for the Alerts UI and Management UI.  Any update to this file requires an Ambari server restart.