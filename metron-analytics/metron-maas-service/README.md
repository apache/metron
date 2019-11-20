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
# Model Management Infrastructure

## Introduction

One of the main features envisioned and requested is the ability to augment the threat intelligence and enrichment processes with insights derived from machine learning or statistical models.  The challenges with this sort of infrastructure are
* Applying the model may be sufficiently computationally/resource intensive that we need to support scaling via load balancing, which will require service discovery and management.
* Models require out of band and frequent training to react to growing threats and new patterns that emerge.
* Models should be language/environment agnostic as much as possible.  These should include small-data and big-data libraries and languages.

To support a high throughput environment that is manageable, it is evident that 
* Multiple versions of models will need to be exposed
* Deployment should happen using Yarn to manage resources
* Clients should have new model endpoints pushed to them

## Architecture

![Architecture](maas_arch.png)

To support these requirements, the following components have been created:
* A Yarn application which will listen for model deployment requests and upon execution, register their endpoints in zookeeper:
  * Operation type: ADD, REMOVE, LIST
  * Model Name
  * Model Version
  * Memory requirements (in megabytes)
  * Number of instances
* A command line deployment client which will localize the model payload onto HDFS and submit a model request
* A Java client which will interact with zookeeper and receive updates about model state changes (new deployments, removals, etc.)
* A series of Stellar functions for interacting with models deployed via the Model as a Service infrastructure.

## `maas_service.sh`

The `maas_service.sh` script starts the Yarn application which will listen for requests.  Right now the queue for the requests is a distributed queue stored in [zookeeper](http://curator.apache.org/curator-recipes/distributed-queue.html) for convenience.

```
./maas_service.sh
usage: MaaSClient
 -c,--create                          Flag to indicate whether to create
                                      the domain specified with -domain.
 -d,--domain <arg>                    ID of the timeline domain where the
                                      timeline entities will be put
 -e,--shell_env <arg>                 Environment for shell script.
                                      Specified as env_key=env_val pairs
 -h,--help                            This screen
 -j,--jar <arg>                       Jar file containing the application
                                      master
 -l,--log4j <arg>                     The log4j properties file to load
 -ma,--modify_acls <arg>              Users and groups that allowed to
                                      modify the timeline entities in the
                                      given domain
 -mc,--master_vcores <arg>            Amount of virtual cores to be
                                      requested to run the application
                                      master
 -mm,--master_memory <arg>            Amount of memory in MB to be
                                      requested to run the application
                                      master
 -nle,--node_label_expression <arg>   Node label expression to determine
                                      the nodes where all the containers
                                      of this application will be
                                      allocated, "" means containers can
                                      be allocated anywhere, if you don't
                                      specify the option, default
                                      node_label_expression of queue will
                                      be used.
 -q,--queue <arg>                     RM Queue in which this application
                                      is to be submitted
 -t,--timeout <arg>                   Application timeout in milliseconds
 -va,--view_acls <arg>                Users and groups that allowed to
                                      view the timeline entities in the
                                      given domain
 -zq,--zk_quorum <arg>                Zookeeper Quorum
 -zr,--zk_root <arg>                  Zookeeper Root
```

## `maas_deploy.sh`

The `maas_deploy.sh` script allows users to deploy models and their collateral from their local disk to the cluster.
It is assumed that the 
* Collateral has exactly one `.sh` script capable of starting the endpoint
* The model service executable will expose itself as a URL endpoint (e.g. as a REST interface, but not necessarily)
* The model service executable will write out to local disk a JSON blob indicating the endpoint (see [here](https://gist.github.com/cestella/cba10aff0f970078a4c2c8cade3a4d1a#file-dga-py-L21) for an example mock service using Python and Flask).

```
./maas_deploy.sh
usage: ModelSubmission
 -h,--help                       This screen
 -hmp,--hdfs_model_path <arg>    Model Path (HDFS)
 -lmp,--local_model_path <arg>   Model Path (local)
 -l,--log4j <arg>                The log4j properties file to load
 -m,--memory <arg>               Memory for container
 -mo,--mode <arg>                ADD, LIST or REMOVE
 -n,--name <arg>                 Model Name
 -ni,--num_instances <arg>       Number of model instances
 -v,--version <arg>              Model version
 -zq,--zk_quorum <arg>           Zookeeper Quorum
 -zr,--zk_root <arg>             Zookeeper Root
```

## Kerberos Support

Model as a service will run on a kerberized cluster (see
[here](../../metron-deployment/vagrant/Kerberos-setup.md) for
instructions for vagrant) with a caveat.  The user who submits 
the service will be the user who executes the models on the cluster.  That
is to say that user impersonation of models deployed is not done at the moment.

## Stellar Integration

Two Stellar functions have been added to provide the ability to call out to models deployed via Model as a Service.
One aimed at recovering a load balanced endpoint of a deployed model given the name and, optionally, the version.
The second is aimed at calling that endpoint assuming that it is exposed as a REST endpoint.

* `MAAS_MODEL_APPLY(endpoint, function?, model_args)` : Returns the output of a model deployed via model which is deployed at endpoint.  `endpoint` is a map containing `name`, `version`, `url` for the REST endpoint, `function` is the endpoint path and is optional, and `model_args` is a dictionary of arguments for the model (these become request params).
* `MAAS_GET_ENDPOINT(model_name, model_version?)` : Inspects zookeeper and returns a map containing the `name`, `version` and `url` for the model referred to by `model_name` and `model_version`.  If `model_version` is not specified, the most current model associated with `model_name` is returned.  In the instance where more than one model is deployed, a random one is selected with uniform probability.

# Example

Let's augment the `squid` proxy sensor to use a model that will determine if the destination host is a domain generating algorithm.  For the purposes of demonstration, this algorithm is super simple and is implemented using Python with a REST interface exposed via the Flask python library.

## Install Prerequisites and Mock DGA Service
Now let's install some prerequisites:
* Flask via `yum -y install python-flask`
* Jinja2 via `yum -y install python-jinja2`
* Squid client via `yum -y install squid`

Start Squid via `service squid start`

Now that we have flask and jinja, we can create a mock DGA service to deploy with MaaS:
* Download the files in [this](https://gist.github.com/cestella/cba10aff0f970078a4c2c8cade3a4d1a) gist into the `$HOME/mock_dga` directory
* Make `rest.sh` executable via `chmod +x $HOME/mock_dga/rest.sh`

This service will treat `yahoo.com` and `amazon.com` as legit and everything else as malicious.  The contract is that the REST service exposes an endpoint `/apply` and returns back JSON maps with a single key `is_malicious` which can be `malicious` or `legit`.

## Deploy Mock DGA Service via MaaS

The following presumes that you are a logged in as a user who has a
home directory in HDFS under `/user/$USER`.  If you do not, please create one
and ensure the permissions are set appropriate:
```
su - hdfs -c "hdfs dfs -mkdir /user/$USER"
su - hdfs -c "hdfs dfs -chown $USER:$USER /user/$USER"
```
Or, in the common case for the `metron` user (if the user does not already exist):
```
su - hdfs -c "hdfs dfs -mkdir /user/metron"
su - hdfs -c "hdfs dfs -chown metron:metron /user/metron"
```

Now let's start MaaS and deploy the Mock DGA Service:
* Start MaaS via `$METRON_HOME/bin/maas_service.sh -zq node1:2181`
* Start one instance of the mock DGA model with 512M of memory via `$METRON_HOME/bin/maas_deploy.sh -zq node1:2181 -lmp $HOME/mock_dga -hmp /user/$USER/models -mo ADD -m 512 -n dga -v 1.0 -ni 1`
* As a sanity check:
  * Ensure that the model is running via `$METRON_HOME/bin/maas_deploy.sh -zq node1:2181 -mo LIST`.  You should see `Model dga @ 1.0` be displayed and under that a url such as (but not exactly) `http://node1:36161`
  * Try to hit the model via curl: `curl 'http://localhost:36161/apply?host=caseystella.com'` and ensure that it returns a JSON map indicating the domain is malicious.

## Adjust Configurations for Squid to Call Model
Now that we have a deployed model, let's adjust the configurations for the Squid topology to annotate the messages with the output of the model.

* First pull down the latest configuration from Zookeeper
```
$METRON_HOME/bin/zk_load_configs.sh -m PULL -o ${METRON_HOME}/config/zookeeper -z $ZOOKEEPER -f
```
* Edit the squid parser configuration at `$METRON_HOME/config/zookeeper/parsers/squid.json` in your favorite text editor and add a new FieldTransformation to indicate a threat alert based on the model (note the addition of `is_malicious` and `is_alert`):
```
{
  "parserClassName": "org.apache.metron.parsers.GrokParser",
  "sensorTopic": "squid",
  "parserConfig": {
    "grokPath": "/patterns/squid",
    "patternLabel": "SQUID_DELIMITED",
    "timestampField": "timestamp"
  },
  "fieldTransformations" : [
    {
      "transformation" : "STELLAR"
    ,"output" : [ "full_hostname", "domain_without_subdomains", "is_malicious", "is_alert" ]
    ,"config" : {
      "full_hostname" : "URL_TO_HOST(url)"
      ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
      ,"is_malicious" : "MAP_GET('is_malicious', MAAS_MODEL_APPLY(MAAS_GET_ENDPOINT('dga'), {'host' : domain_without_subdomains}))"
      ,"is_alert" : "if is_malicious == 'malicious' then 'true' else null"
                }
    }
                           ]
}
```
* Edit the squid enrichment configuration at `$METRON_HOME/config/zookeeper/enrichments/squid.json` (this file will not exist, so create a new one) to make the threat triage adjust the level of risk based on the model output:
```
{
  "enrichment" : {
    "fieldMap": {}
  },
  "threatIntel" : {
    "fieldMap":{},
    "triageConfig" : {
      "riskLevelRules" : [
        {
          "rule" : "is_malicious == 'malicious'",
          "score" : 100
        }
      ],
      "aggregator" : "MAX"
    }
  }
}
```
* Setup an indexing configuration here `${METRON_HOME}/config/zookeeper/indexing/squid.json` with the following contents:
```
{
    "hdfs" : {
        "index": "squid",
        "batchSize": 5,
        "enabled" : true
    },
    "elasticsearch" : {
        "index": "squid",
        "batchSize": 5,
        "enabled" : true
    },
    "solr" : {
        "index": "squid",
        "batchSize": 5,
        "enabled" : true
    }
}
```
* Upload new configs via `$METRON_HOME/bin/zk_load_configs.sh --mode PUSH -i $METRON_HOME/config/zookeeper -z node1:2181`
* Make the Squid topic in kafka via `/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper node1:2181 --create --topic squid --partitions 1 --replication-factor 1`
* Setup your squid indexing template for Elasticsearch (if using Elasticsearch)
```
curl -XPUT 'http://node1:9200/_template/squid_index' -d '
{
  "template": "squid_index*",
  "mappings": {
    "squid_doc": {
      "dynamic_templates": [
      {
        "geo_location_point": {
          "match": "enrichments:geo:*:location_point",
          "match_mapping_type": "*",
          "mapping": {
            "type": "geo_point"
          }
        }
      },
      {
        "geo_country": {
          "match": "enrichments:geo:*:country",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_city": {
          "match": "enrichments:geo:*:city",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_location_id": {
          "match": "enrichments:geo:*:locID",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_dma_code": {
          "match": "enrichments:geo:*:dmaCode",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_postal_code": {
          "match": "enrichments:geo:*:postalCode",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_latitude": {
          "match": "enrichments:geo:*:latitude",
          "match_mapping_type": "*",
          "mapping": {
            "type": "float"
          }
        }
      },
      {
        "geo_longitude": {
          "match": "enrichments:geo:*:longitude",
          "match_mapping_type": "*",
          "mapping": {
            "type": "float"
          }
        }
      },
      {
        "timestamps": {
          "match": "*:ts",
          "match_mapping_type": "*",
          "mapping": {
            "type": "date",
            "format": "epoch_millis"
          }
        }
      },
      {
        "threat_triage_score": {
          "mapping": {
            "type": "float"
          },
          "match": "threat:triage:*score",
          "match_mapping_type": "*"
        }
      },
      {
        "threat_triage_reason": {
          "mapping": {
            "type": "text",
            "fielddata": "true"
          },
          "match": "threat:triage:rules:*:reason",
          "match_mapping_type": "*"
        }
      },
      {
        "threat_triage_name": {
          "mapping": {
            "type": "text",
            "fielddata": "true"
          },
          "match": "threat:triage:rules:*:name",
          "match_mapping_type": "*"
        }
      }
      ],
      "properties": {
        "timestamp": {
          "type": "date",
          "format": "epoch_millis"
        },
        "source:type": {
          "type": "keyword"
        },
        "ip_dst_addr": {
          "type": "ip"
        },
        "ip_dst_port": {
          "type": "integer"
        },
        "ip_src_addr": {
          "type": "ip"
        },
        "ip_src_port": {
          "type": "integer"
        },
        "alert": {
          "type": "nested"
        },
        "metron_alert" : {
          "type" : "nested"
        },
        "guid": {
          "type": "keyword"
        }
      }
    }
  }
}
'
# Verify the template installs as expected 
curl -XGET 'http://node1:9200/_template/squid_index?pretty'
```

## Start Topologies and Send Data
Now we need to start the topologies and send some data:
* Start the squid topology via `$METRON_HOME/bin/start_parser_topology.sh -k node1:6667 -z node1:2181 -s squid`
* Generate some data via the squid client:
  * Generate a legit example: `squidclient http://yahoo.com`
  * Generate a malicious example: `squidclient http://cnn.com`
* (Optional) In another terminal, tail the "enrichments" Kafka topic. You should be able to see the new data output there after the next couple steps.
```
${HDP_HOME}/kafka-broker/bin/kafka-console-consumer.sh --bootstrap-server $BROKERLIST --topic enrichments
```
* Send the data to kafka via `cat /var/log/squid/access.log | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list node1:6667 --topic squid`
* If you setup the optional Kafka consumer in another terminal, you should see a couple records output as follows. Notice that one has `"is_malicious":"legit"` while the other has `"is_malicious":"malicious"`:
```
{"is_malicious":"legit","full_hostname":"yahoo.com","code":301,"method":"GET","url":"http:\/\/yahoo.com\/","source.type":"squid","elapsed":192,"ip_dst_addr":"72.30.35.10","original_string":"1571163620.277    192 127.0.0.1 TCP_MISS\/301 366 GET http:\/\/yahoo.com\/ - DIRECT\/72.30.35.10 text\/html","bytes":366,"domain_without_subdomains":"yahoo.com","action":"TCP_MISS","guid":"9d19f502-0770-4ca1-9eeb-d0bcbb0942c7","ip_src_addr":"127.0.0.1","timestamp":1571163620277}
{"is_malicious":"malicious","full_hostname":"cnn.com","code":301,"method":"GET","is_alert":"true","url":"http:\/\/cnn.com\/","source.type":"squid","elapsed":106,"ip_dst_addr":"151.101.1.67","original_string":"1571163632.536    106 127.0.0.1 TCP_MISS\/301 539 GET http:\/\/cnn.com\/ - DIRECT\/151.101.1.67 -","bytes":539,"domain_without_subdomains":"cnn.com","action":"TCP_MISS","guid":"69254e10-bed9-4743-ba8b-d3c01c29430d","ip_src_addr":"127.0.0.1","timestamp":1571163632536}
```
* Browse the data in the Alerts UI @ [http://node1:4201/alerts-list](http://node1:4201/alerts-list) and verify that in the squid index you have two documents.
  * One from `yahoo.com` which does not have `is_alert` set and does have `is_malicious` set to `legit`
  * One from `cnn.com` which does have `is_alert` set to `true`, `is_malicious` set to `malicious` and `threat:triage:level` set to 100
