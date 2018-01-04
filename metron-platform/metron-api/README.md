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
# Metron PCAP Service

The purpose of the Metron PCAP service is to provide a middle tier to
negotiate retrieving packet capture data which flows into Metron.  This
packet data is of a form which `libpcap` based tools can read.

## Starting the Service

You can start the service either via the init.d script installed,
`/etc/init.d/pcapservice` or directly via the `yarn jar` command:
`yarn jar $METRON_HOME/lib/metron-api-$METRON_VERSION.jar org.apache.metron.pcapservice.rest.PcapService -port $SERVICE_PORT -query_hdfs_path $QUERY_PATH -pcap_hdfs_path $PCAP_PATH`

where
* `METRON_HOME` is the location of the metron installation
* `METRON_VERSION` is the version of the metron installation
* `SERVICE_PORT` is the port to bind the REST service to.
* `QUERY_PATH` is the temporary location to store query results.  They are deleted after the service reads them.
* `PCAP_PATH` is the path to the packet data on HDFS

## The `/pcapGetter/getPcapsByIdentifiers` endpoint

This endpoint takes the following query parameters and returns the subset of
packets matching this query:
* `srcIp` : The source IP to match on
* `srcPort` : The source port to match on
* `dstIp` : The destination IP to match on
* `dstPort` : The destination port to match on
* `startTime` : The start time in milliseconds
* `endTime` : The end time in milliseconds
* `numReducers` : Specify the number of reducers to use when executing the mapreduce job
* `includeReverseTraffic` : Indicates if filter should check swapped src/dest addresses and IPs

## The `/pcapGetter/getPcapsByQuery` endpoint

This endpoint takes the following query parameters and returns the subset of
packets matching this query. This endpoint exposes Stellar querying capabilities:
* `query` : The Stellar query to execute
* `startTime` : The start time in milliseconds
* `endTime` : The end time in milliseconds
* `numReducers` : Specify the number of reducers to use when executing the mapreduce job

Example:
`curl -XGET "http://node1:8081/pcapGetter/getPcapsByQuery?query=ip_src_addr+==+'192.168.66.121'+and+ip_src_port+==+'60500'&startTime=1476936000000"`

All of these parameters are optional.  In the case of a missing
parameter, it is treated as a wildcard.

Unlike the CLI tool, there is no paging mechanism. The REST API will stream back data as a single file.
