# Elasticsearch in Metron

## Introduction

Elasticsearch can be used as the real-time portion of the datastore resulting from [metron-indexing](../metron-indexing.README.md).

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

## Using Metron with Elasticsearch 2.x

With Elasticsearch 2.x, there is a requirement that all sensors templates have a nested alert field defined.  This field is a dummy field, and will be obsolete in Elasticsearch 5.x.  See [Ignoring Unmapped Fields](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-sort.html#_ignoring_unmapped_fields) for more information

Without this field, an error will be thrown during ALL searches (including from UIs, resulting in no alerts being found for any sensor). This error will be found in the REST service's logs.

Exception seen:
```
QueryParsingException[[nested] failed to find nested object under path [alert]];
```

There are two steps to resolve this issue.  First is to update the Elasticsearch template for each sensor, so any new indices have the field. This requires retrieving the template, removing an extraneous JSON field so we can put it back later, and adding our new field.

Make sure to set the ELASTICSEARCH variable appropriately. $SENSOR can contain wildcards, so if rollover has occurred, it's not necessary to do each index individually. The example here appends `index*` to get all indexes for a the provided sensor.

```
export ELASTICSEARCH="node1"
export SENSOR="bro"
curl -XGET "http://${ELASTICSEARCH}:9200/_template/${SENSOR}_index*?pretty=true" -o "${SENSOR}.template"
sed -i '' '2d;$d' ./${SENSOR}.template
sed -i '' '/"properties" : {/ a\
"alert": { "type": "nested"},' ${SENSOR}.template
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
          "alert" : {
            "type" : "nested"
          }
        }
}
'
rm ${SENSOR}.template
```
