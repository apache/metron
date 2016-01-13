# OpenSOC-DataLoads

This project is a collection of classes to assist with loading of various enrichment sources into OpenSOC.

## Threat Intel Enrichment

Threat Intel enrichment data sources can be loaded into OpenSOC using the ThreatIntelLoader class and an implementation of a ThreatIntelSource interface. Both are described below.

### ThreatIntelSource Interface

This inteface extends the Iterator interface and must implement the following methods:

`void initializeSource(Configuration config);`

Put any setup that needs to be done here. This will be called by ThreatIntelLoader before attempting to fetch any data from the source. The paramter config is a Configuration object created from the configuration file passed to ThreatIntelLoader. See the ThreatIntelLoader section below for more details

`void cleanupSource();`

This is called after all data is retrieved, just before ThreatIntelLoader exists. Perform any clean up here if needed.

`JSONObject next()`

This method should return the next piece of intel to be stored in OpenSOC. The returned JSONObject must have the following fields:

* indicator - The indicator that will be checked against during enrichment. For example, and IP Address or a Hostname.
* source - The source of the data, which can be any unique string to identify the origin of the intel. This will be the column qualifer in HBase and be used to group matches on in Storm
* data - A JSONArray of JSONObjects that detail the intel for the indicator. The JSONObjects have no required format


`boolean hasNext()`

Returns true if there are more sources to read. Otherwise, false.


### ThreatIntelLoader

This class is intenteded to be called from the commandline on the OpenSOC cluster and is responsible for taking intel from a ThreatIntelSource implementation and putting them into HBase.

#### Usage

````
usage: ThreatIntelLoader [--configFile <c>] --source <s> --table <t>
    --configFile <c>   Configuration file for source class
    --source <s>       Source class to use
    --table <t>        HBase table to load into
````

* configFile - the file passed in by this class is used to provide configuration options to the ThreatIntelSource implementation being used.
* source - the implementation of ThreatIntelSource to use
* table - the hbase table to store the threat intel in for enrichment later. This should match what the corresponding enrichment bolt is using in Storm
