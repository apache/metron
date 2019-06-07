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
# About the feature
Click Through Navigation is a feature makes Metron Users able to reach other web services via dynamically created URLs by clicking link item in a context menu.
This context menu (aka. click-through menu) is attached to the alerts table and the links are populated with alert data from the specific row of the table.

# Configuration

In it's current state the click through navigation is configurable via a JSON file bundled with Alerts UI.

> We're planning to provide a administration interface for click through navigation not too far in the future. A UI would make configuration process easier and more interactive. However, the feature is fully functional in it's current form. You can follow the progress via the following [Jira ticket](https://issues.apache.org/jira/browse/METRON-2102).

## Location of the config JSON file
### In Metron source code
If you are a developer and like to experimental with the feature on your localhost you can find the config file here:
```
metron-interface/metron-alerts/src/assets/context-menu.conf.json
```
### In a deployed environment
If you are an operations person or you like to configure a Metron instance already deployed you can find the config file here:
```
/usr/metron/{version}/web/alerts-ui/assets/context-menu.conf.json
```

## Applying changes in the config JSON
If you made any changes in the config JSON file you need to restart Metron Alerts UI in Ambari to apply them.

## Config validation and troubleshooting
Click through feature in Alerts UI try to help debugging any possible issues (misspelling, invalid values etc.) by validating context-menu.conf.json. Alert UI provides you error messages in your browser console if config JSON is corrupt in any possible ways.

## Enabling feature
The feature is by default turned off. A sample configuration is added as an example and for testing purposes.
If you like to enable click through navigation you should set isEnabled to true in the config JSON file:
```
{
  isEnabled: true,
  config: {
  ...
```

## Attaching and configuring click-through menu to a column
Items and URLs in the context menu based on a configuration (this is currently a JSON file). A configuration could be attached to a cell or a row.
If you like to attach a menu configuration to a cell of a column you should use the field id (what field of the alert populates the column) to target the particular column.

For example, the following configuration adding the "Whois Reputation Service" item to the context menu which appears if the user left click on a value in the "host" column:
```
{
  isEnabled: true,
  config: {
    "host": [
      {
        "label": "Whois Reputation Service",
        "urlPattern": "https://www.whois.com/whois/{}"
      }
    ],
  ...
```
Clicking on the item opens another browser tab and call the URL in the urlPattern config field. "{}" at the end of the pattern stands for being a default placeholder and it will be replaced by the value of the host field in the particular row which was clicked.
But in the configuration, any available alert property field could be referenced like the following:
```
{
  isEnabled: true,
  config: {
    "host": [
      {
        "label": "Whois Reputation Service",
        "urlPattern": "https://www.whois.com/whois/{ip_src_addr}"
      }
    ],
  ...
```
In this case however the menu attached to the host column the place holder will be resolved with the value of the ip_src_addr field of the particular alert item.
You can reference multiple fields and can combine default and specific placeholders:
```
{
  isEnabled: true,
  config: {
    "host": [
      {
        "label": "Whois Reputation Service",
        "urlPattern": "https://www.whois.com/whois/{}?srcip={ip_src_addr}&destip={ip_dest_addr}"
      }
    ],
  ...
```
Configuration to a particular column could contain multiple menu items like in the following example:
```
{
  isEnabled: true,
  config: {
    "ip_src_addr": [
      {
        "label": "IP Investigation Notebook",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip={ip_src_addr}"
      },
      {
        "label": "IP Conversation Investigation",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip_src_addr={ip_src_addr}&ip_dst_addr={ip_dst_addr}"
      }
    ],
    "host": [
      {
        "label": "Whois Reputation Service",
        "urlPattern": "https://www.whois.com/whois/{}?srcip={ip_src_addr}&destip={ip_dest_addr}"
      }
    ],
  ...
```

## Attaching and configuring click-through menu to rows

In the case of rows, we distinguish simple alerts and meta alerts. So these two types are configurable separately.
```
{
  isEnabled: true,
  config: {
    "alertEntry": [
      {
        "label": "Internal ticketing system",
        "urlPattern": "http://mytickets.org/tickets/{id}"
      }
    ],
    "metaAlertEntry": [
      {
        "label": "MetaAlert specific item",
        "urlPattern": "http://mytickets.org/tickets/{id}"
      }
    ],
  ...
```
These two keywords: **"alertEntry"** and **"metaAlertEntry"** stand for configuring menu attached to alert and meta alert rows.
When the user clicking on a value it is recognized as a cell/column specific click and the menu configured to the particular field/column will appear.
If the user clicks outside of value (to the blank space between values) it will be recognized as a row click and an alert or meta alert specific click-through menu will show up depending on the type of the row.

# Sample configuration provided by default

The default configuration at the time of writing looks like the following:
```
{
  isEnabled: false,
  config: {
    "alertEntry": [
      {
        "label": "Internal ticketing system",
        "urlPattern": "http://mytickets.org/tickets/{id}"
      }
    ],
    "metaAlertEntry": [
      {
        "label": "MetaAlert specific item",
        "urlPattern": "http://mytickets.org/tickets/{id}"
      }
    ],
    "id": [
      {
        "label": "Dynamic menu item 01",
        "urlPattern": "http://mytickets.org/tickets/{}"
      }
    ],
    "ip_src_addr": [
      {
        "label": "IP Investigation Notebook",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip={ip_src_addr}"
      },
      {
        "label": "IP Conversation Investigation",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip_src_addr={ip_src_addr}&ip_dst_addr={ip_dst_addr}"
      }
    ],
    "ip_dst_addr": [
      {
        "label": "IP Investigation Notebook",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip={ip_dst_addr}"
      },
      {
        "label": "IP Conversation Investigation",
        "urlPattern": "http://zepellin.example.com:9000/notebook/someid?ip_src_addr={ip_src_addr}&ip_dst_addr={ip_dst_addr}"
      }
    ],
    "host": [
      {
      "label": "Whois Reputation Service",
      "urlPattern": "https://www.whois.com/whois/{}"
      }
    ]
  }
}
```