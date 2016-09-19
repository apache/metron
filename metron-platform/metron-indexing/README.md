#Indexing

## Introduction

The `indexing` topology is a topology dedicated to taking the data
from the enrichment topology that have been enriched and storing the data in one or more supported indices
* HDFS as rolled text files, one JSON blob per line
* Elasticsearch
* Solr

By default, this topology writes out to both HDFS and one of
Elasticsearch and Solr.

Indices are written in batch and the batch size is specified in the
[Enrichment Config](../metron-enrichment) via the `batchSize` parameter.
This config is variable by sensor type.

## Indexing Architecture

The indexing topology is extremely simple.  Data is ingested into kafka
and sent to 
* An indexing bolt configured to write to either elasticsearch or Solr
* An indexing bolt configured to write to HDFS under `/apps/metron/enrichment/indexed`

Errors during indexing are sent to a kafka queue called `index_errors`
