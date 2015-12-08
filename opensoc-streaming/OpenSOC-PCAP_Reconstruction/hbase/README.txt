'hbase' module of 'opensoc' project contains the code to communicate with HBase. This module has several APIs ( refer IPcapGetter.java, IPcapScanner.java files ) 
to fetch pcaps from HBase. Following APIs have been created under this module implementation.

APIs ( in IPcapGetter.java) to get pcaps using keys :
 1. public PcapsResponse getPcaps(List<String> keys, String lastRowKey, long startTime, long endTime, boolean includeReverseTraffic, boolean includeDuplicateLastRow, long maxResultSize) throws IOException;
 2. public PcapsResponse getPcaps(String key, long startTime, long endTime, boolean includeReverseTraffic) throws IOException;
 3. public PcapsResponse getPcaps(List<String> keys) throws IOException;
 4. public PcapsResponse getPcaps(String key) throws IOException;

APIs ( in IPcapScanner.java) to get pcaps using key range :
 1. public byte[] getPcaps(String startKey, String endKey, long maxResponseSize, long startTime, long endTime) throws IOException;
 2. public byte[] getPcaps(String startKey, String endKey) throws IOException;
 
 
Refer the wiki documentation for further details : https://hwcsco.atlassian.net/wiki/pages/viewpage.action?pageId=5242892
 	