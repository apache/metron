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

## The `/pcapGetter/getPcapsByIdentifiers` Endpoint

This endpoint takes the following query parameters and returns the subset of
packets matching this query:
* `srcIp` : The source IP to match on
* `srcPort` : The source port to match on
* `dstIp` : The destination IP to match on
* `dstPort` : The destination port to match on
* `startTime` : The start time in milliseconds
* `endTime` : The end time in milliseconds

All of these parameters are optional.  In the case of a missing
parameter, it is treated as a wildcard.
