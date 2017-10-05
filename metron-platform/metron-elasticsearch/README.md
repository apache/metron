# Contents

Elasticsearch is a very popular indexing target for Metron.  In order to
configure Elasticsearch, there are a few properties that one must set up in the global
configuration.

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
