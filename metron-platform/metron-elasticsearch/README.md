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
# Elasticsearch in Metron

## Table of Contents

* [Introduction](#introduction)
* [Properties](#properties)
* [Upgrading from 2.3.3 to 5.6](#upgrading-from-233-to-56)
* [Type Mappings](#type-mappings)
* [Using Metron with Elasticsearch 5.6](#using-metron-with-elasticsearch-56)
* [Installing Elasticsearch Templates](#installing-elasticsearch-templates)

## Introduction

Elasticsearch can be used as the real-time portion of the datastore resulting from [metron-indexing](../metron-indexing/README.md).

## Properties

### `es.clustername`

The name of the elasticsearch Cluster.  See [here](https://www.elastic.co/guide/en/elasticsearch/reference/current/important-settings.html#cluster.name)

### `es.ip`

Specifies the nodes in the elasticsearch cluster to use for writing.
The format is one of the following:
* A hostname or IP address with a port (e.g. `hostname1:1234`), in which case `es.port` is ignored.
* A hostname or IP address without a port (e.g. `hostname1`), in which case `es.port` is used.
* A string containing a CSV of hostnames without ports (e.g. `hostname1,hostname2,hostname3`) without spaces between.  `es.port` is assumed to be the port for each host.
* A string containing a CSV of hostnames with ports (e.g. `hostname1:1234,hostname2:1234,hostname3:1234`) without spaces between.  `es.port` is ignored.
* A list of hostnames with ports (e.g. `[ "hostname1:1234", "hostname2:1234"]`).  Note, `es.port` is NOT used in this construction.

### `es.port`

The port for the elasticsearch hosts.  This will be used in accordance with the discussion of `es.ip`.

### `es.date.format`

The date format to use when constructing the indices.  For every message, the date format will be applied
to the current time and that will become the last part of the index name where the message is written to.

For instance, an `es.date.format` of `yyyy.MM.dd.HH` would have the consequence that the indices would
roll hourly, whereas an `es.date.format` of `yyyy.MM.dd` would have the consequence that the indices would
roll daily.

### `es.client.settings`

This field in global config allows you to specify Elasticsearch REST client options. These are used in conjunction with the previously mentioned Elasticsearch properties
when setting up client connections to an Elasticsearch cluster. The available properties should be supplied as an object map. Current available options are as follows:

| Property Name                       | Type      | Required? | Default Value  | Description                                                                                                                                                         |
|-------------------------------------|-----------|-----------|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| connection.timeout.millis           | Integer   | No        | 1000           | Sets connection timeout.                                                                                                                                            |
| socket.timeout.millis               | Integer   | No        | 30000          | Sets socket timeout.                                                                                                                                                |
| max.retry.timeout.millis            | Integer   | No        | 30000          | Sets the maximum timeout (in milliseconds) to honour in case of multiple retries of the same request.                                                               |
| num.client.connection.threads       | Integer   | No        | 1              | Number of worker threads used by the connection manager. Defaults to Runtime.getRuntime().availableProcessors().                                                    |
| xpack.username                      | String    | No        | null           | X-Pack username.                                                                                                                                                    |
| xpack.password.file                 | String    | No        | null           | 1-line HDFS file where the X-Pack password is set.                                                                                                                  |
| ssl.enabled                         | Boolean   | No        | false          | Turn on SSL connections.                                                                                                                                            |
| keystore.type                       | String    | No        | "jks"          | Allows you to specify a keytstore type. See https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore for more details.           |
| keystore.path                       | String    | No        | null           | Path to the Trust Store that holds your Elasticsearch certificate authorities and certificate.                                                                      |
| keystore.password.file              | String    | No        | null           | 1-line HDFS file where the keystore password is set.                                                                                                                |

__Note:__ The migration from Elasticsearch's TransportClient to the Java REST client has resulted in some existing properties to change. Below is a mapping of the old properties to the new ones:

| Old Property Name                      | New Property Name                   |
|----------------------------------------|-------------------------------------|
| client.transport.ping_timeout          | n/a                                 |
| n/a                                    | connection.timeout.millis           |
| n/a                                    | socket.timeout.millis               |
| n/a                                    | max.retry.timeout.millis            |
| n/a                                    | num.client.connection.threads       |
| es.client.class                        | n/a                                 |
| es.xpack.username                      | xpack.username                      |
| es.xpack.password.file                 | xpack.password.file                 |
| xpack.security.transport.ssl.enabled   | ssl.enabled                         |
| xpack.ssl.key                          | n/a                                 |
| xpack.ssl.certificate                  | n/a                                 |
| xpack.ssl.certificate_authorities      | n/a                                 |
| n/a                                    | keystore.type                       |
| keystore.path                          | keystore.path                       |
| n/a                                    | keystore.password.file              |

__Notes:__
* The transport client implementation provides for a 'xpack.security.user' property, however we never used this property directly. Rather, in order to secure the password we used custom properties for user/pass. These properties have been carried over as `xpack.username` and `xpack.password.file`.
* See [https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_common_configuration.html](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_common_configuration.html) for more specifics on the new client properties.
* Other notes on JSSE - [https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)

## Upgrading from 2.3.3 to 5.6

Users should be prepared to re-index when migrating from Elasticsearch 2.3.3 to 5.6. There are a number of template changes, most notably around
string type handling, that may cause issues when upgrading.

[https://www.elastic.co/guide/en/elasticsearch/reference/5.6/setup-upgrade.html](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/setup-upgrade.html)

Be aware that if you add a new string value and want to be able to filter and search on this value from the Alerts UI, you **must** add a mapping for that type to
the appropriate Elasticsearch template. Below is more detail on how to choose the appropriate mapping type for your string value.

## Type Mappings

Type mappings have changed quite a bit from ES 2.x -> 5.x. Here is a brief rundown of the biggest changes. More detailed references from Elasticsearch
are provided in the [Type Mapping References](#type-mapping-references) section below.
* string fields replaced by text/keyword type
* strings have new default mappings as follows

    ```
    {
      "type": "text",
      "fields": {
        "keyword": {
          "type": "keyword",
          "ignore_above": 256
        }
      }
    }
    ```

* There is no longer a `_timestamp` field that you can set "enabled" on. This field now causes an exception on templates.
Replace with an application-created timestamp of "date" type.

The semantics for string types have changed. In 2.x, you have the concept of index settings as either "analyzed" or "not_analyzed" which basically means "full text" and "keyword", respectively.
Analyzed text basically means the indexer will split the text using a text analyzer thus allowing you to search on substrings within the original text. "New York" is split and indexed as two buckets,
 "New" and "York", so you can search or query for aggregate counts for those terms independently and will match against the individual terms "New" or "York." "Keyword" means that the original text
 will not be split/analyzed during indexing and instead treated as a whole unit, i.e. "New" or "York" will not match in searches against the document containing "New York", but searching on "New York"
 as the full city name will. In 5.x language instead of using the "index" setting, you now set the "type" to either "text" for full text, or "keyword" for keywords.

Below is a table depicting the changes to how String types are now handled.

<table>
<tr>
	<th>sort, aggregate, or access values</th>
	<th>ES 2.x</th>
	<th>ES 5.x</th>
	<th>Example</th>
</tr>
<tr>
	<td>no</td>
	<td>
<pre><code>"my_property" : {
  "type": "string",
  "index": "analyzed"
}
</code></pre>
	</td>
	<td>
<pre><code>"my_property" : {
  "type": "text"
}
</code></pre>
    Additional defaults: "index": "true", "fielddata": "false"
	</td>
	<td>
		"New York" handled via in-mem search as "New" and "York" buckets. <strong>No</strong> aggregation or sort.
	</td>
</tr>
<tr>
	<td>
	yes
	</td>
	<td>
<pre><code>"my_property": {
  "type": "string",
  "index": "analyzed"
}
</code></pre>
	</td>
	<td>
<pre><code>"my_property": {
  "type": "text",
  "fielddata": "true"
}
</code></pre>
	</td>
	<td>
	"New York" handled via in-mem search as "New" and "York" buckets. <strong>Can</strong> aggregate and sort.
	</td>
</tr>
<tr>
	<td>
	yes
	</td>
	<td>
<pre><code>"my_property": {
  "type": "string",
  "index": "not_analyzed"
}
</code></pre>
	</td>
	<td>
<pre><code>"my_property" : {
  "type": "keyword"
}
</code></pre>
	</td>
	<td>
	"New York" searchable as single value. <strong>Can</strong> aggregate and sort. A search for "New" or "York" will not match against the whole value.
	</td>
</tr>
<tr>
	<td>
	yes
	</td>
	<td>
<pre><code>"my_property": {
  "type": "string",
  "index": "analyzed"
}
</code></pre>
	</td>
	<td>
<pre><code>"my_property": {
  "type": "text",
  "fields": {
    "keyword": {
      "type": "keyword",
      "ignore_above": 256
    }
  }
}
</code></pre>
	</td>
	<td>
	"New York" searchable as single value or as text document, can aggregate and sort on the sub term "keyword."
	</td>
</tr>
</table>

If you want to set default string behavior for all strings for a given index and type, you can do so with a mapping similar to the following (replace ${your_type_here} accordingly):

```
# curl -XPUT 'http://${ES_HOST}:${ES_PORT}/_template/default_string_template' -d '
{
  "template": "*",
  "mappings" : {
    "${your_type_here}": {
      "dynamic_templates": [
        {
          "strings": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "text"
            }
          }
        }
      ]
    }
  }
}
'
```

By specifying the "template" property with value "*" the template will apply to all indexes that have documents indexed of the specified type (${your_type_here}). This results in the following template.

```
# curl -XGET 'http://${ES_HOST}:${ES_PORT}/_template/default_string_template?pretty'
{
  "default_string_template" : {
    "order" : 0,
    "template" : "*",
    "settings" : { },
    "mappings" : {
      "${your_type_here}" : {
        "dynamic_templates" : [
          {
            "strings" : {
              "match_mapping_type" : "string",
              "mapping" : {
                "type" : "text"
              }
            }
          }
        ]
      }
    },
    "aliases" : { }
  }
}
```

Notes on other settings for types in ES
* doc_values
    * on-disk data structure
    * provides access for sorting, aggregation, and field values
    * stores same values as _source, but in column-oriented fashion better for sorting and aggregating
    * not supported on text fields
    * enabled by default
* fielddata
    * in-memory data structure
    * provides access for sorting, aggregation, and field values
    * primarily for text fields
    * disabled by default because the heap space required can be large


##### Type Mapping References
* [https://www.elastic.co/guide/en/elasticsearch/reference/5.6/mapping.html](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/mapping.html)
* [https://www.elastic.co/guide/en/elasticsearch/reference/5.6/breaking_50_mapping_changes.html](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/breaking_50_mapping_changes.html)
* [https://www.elastic.co/blog/strings-are-dead-long-live-strings](https://www.elastic.co/blog/strings-are-dead-long-live-strings)

### Metron Properties

Metron depends on some internal fields being defined in sensor templates.  A field is defined in Elasticsearch by adding an entry to the `properties` section of the template:
```
"properties": {
  "metron_field": {
    "type": "keyword"
  }
}
```

The following is a list of properties that need to be defined along with their type:
* source:type - keyword
* alert_status - keyword
* metron_alert - nested

## Using Metron with Elasticsearch 5.6

Although infrequent, sometimes an internal field is added in Metron and existing templates must be updated.  The following steps outlines how to do this, using `metron_alert` as an example.

With the addition of the meta alert feature, there is a requirement that all sensors templates have a nested `metron_alert` field defined.  This field is a dummy field.  See [Ignoring Unmapped Fields](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-sort.html#_ignoring_unmapped_fields) for more information

Without this field, an error will be thrown during ALL searches (including from UIs, resulting in no alerts being found for any sensor). This error will be found in the REST service's logs.

Exception seen:
```
QueryParsingException[[nested] failed to find nested object under path [metron_alert]];
```

There are two steps to resolve this issue.  First is to update the Elasticsearch template for each sensor, so any new indices have the field. This requires retrieving the template, removing an extraneous JSON field so we can put it back later, and adding our new field.

Make sure to set the ELASTICSEARCH variable appropriately. $SENSOR can contain wildcards, so if rollover has occurred, it's not necessary to do each index individually. The example here appends `index*` to get all indexes for the provided sensor.

```
export ELASTICSEARCH="node1"
export SENSOR="bro"
curl -XGET "http://${ELASTICSEARCH}:9200/_template/${SENSOR}_index*?pretty=true" -o "${SENSOR}.template"
sed -i '' '2d;$d' ./${SENSOR}.template
sed -i '' '/"properties" : {/ a\
"metron_alert": { "type": "nested"},' ${SENSOR}.template
```

To manually verify this, you can optionally pretty print it again with:
```
python -m json.tool bro.template
```

We'll want to put the template back into Elasticsearch:
```
curl -XPUT "http://${ELASTICSEARCH}:9200/_template/${SENSOR}_index" -d @${SENSOR}.template
```

To update existing indexes, update Elasticsearch mappings with the new field for each sensor. 

```
curl -XPUT "http://${ELASTICSEARCH}:9200/${SENSOR}_index*/_mapping/${SENSOR}_doc" -d '
{
  "properties" : {
    "metron_alert" : {
      "type" : "nested"
    }
  }
}
'
rm ${SENSOR}.template
```

## Installing Elasticsearch Templates

The stock set of Elasticsearch templates for bro, snort, yaf, error index and meta index are installed automatically during the first time install and startup of Metron Indexing service.

It is possible that Elasticsearch service is not available when the Metron Indexing Service startup, in that case the Elasticsearch template will not be installed. 

For such a scenario, an Admin can have the template installed in two ways:

_Method 1_ - Manually from the Ambari UI by following the flow:
Ambari UI -> Services -> Metron -> Service Actions -> Elasticsearch Template Install

_Method 2_ - Stop the Metron Indexing service, and start it again from Ambari UI. Note that the Metron Indexing service tracks if it has successfully installed the Elasticsearch templates, and will attempt to do so each time it is Started until successful.

> Note: If you have made any customization to your index templates, then installing Elasticsearch templates afresh will lead to overwriting your existing changes. Please exercise caution.
