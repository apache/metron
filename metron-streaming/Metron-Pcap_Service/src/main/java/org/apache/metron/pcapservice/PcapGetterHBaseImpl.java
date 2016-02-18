/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.pcapservice;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.NoServerForRegionException;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;

/**
 * Singleton class which integrates with HBase table and returns pcaps sorted by
 * timestamp(dsc) for the given list of keys. Creates HConnection if it is not
 * already created and the same connection instance is being used for all
 * requests
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */

@Path("/")
public class PcapGetterHBaseImpl implements IPcapGetter {

  /** The pcap getter h base. */
  private static IPcapGetter pcapGetterHBase = null;

  /** The Constant LOG. */
  private static final Logger LOGGER = Logger
      .getLogger(PcapGetterHBaseImpl.class);

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapGetter#getPcaps(java.util.List,
   * java.lang.String, long, long, boolean, boolean, long)
   */
 
  
	@GET
	@Path("pcap/test")
	@Produces("text/html")
	public Response  index() throws URISyntaxException { 
		return Response.ok("ALL GOOD").build();   
	}
	
	
  public PcapsResponse getPcaps(List<String> keys, String lastRowKey,
      long startTime, long endTime, boolean includeReverseTraffic,
      boolean includeDuplicateLastRow, long maxResultSize) throws IOException {
    Assert
        .isTrue(
            checkIfValidInput(keys, lastRowKey),
            "No valid input. One of the value must be present from {keys, lastRowKey}");
    LOGGER.info(" keys=" + keys.toString() + ";  lastRowKey="
        + lastRowKey);

    PcapsResponse pcapsResponse = new PcapsResponse();
    // 1. Process partial response key
    if (StringUtils.isNotEmpty(lastRowKey)) {
      pcapsResponse = processKey(pcapsResponse, lastRowKey, startTime,
          endTime, true, includeDuplicateLastRow, maxResultSize);
      // LOGGER.debug("after scanning lastRowKey=" +
      // pcapsResponse.toString()+"*********************************************************************");
      if (pcapsResponse.getStatus() == PcapsResponse.Status.PARTIAL) {
        return pcapsResponse;
      }
    }
    // 2. Process input keys
    List<String> sortedKeys = sortKeysByAscOrder(keys, includeReverseTraffic);
    List<String> unprocessedKeys = new ArrayList<String>();
    unprocessedKeys.addAll(sortedKeys);
    if (StringUtils.isNotEmpty(lastRowKey)) {
      unprocessedKeys.clear();
      unprocessedKeys = getUnprocessedSublistOfKeys(sortedKeys,
          lastRowKey);
    }
    LOGGER.info("unprocessedKeys in getPcaps" + unprocessedKeys.toString());
    if (!CollectionUtils.isEmpty(unprocessedKeys)) {
      for (int i = 0; i < unprocessedKeys.size(); i++) {
        pcapsResponse = processKey(pcapsResponse, unprocessedKeys.get(i),
            startTime, endTime, false, includeDuplicateLastRow, maxResultSize);
        // LOGGER.debug("after scanning input unprocessedKeys.get(" + i + ") ="
        // +
        // pcapsResponse.toString()+"*********************************************************************");
        if (pcapsResponse.getStatus() == PcapsResponse.Status.PARTIAL) {
          return pcapsResponse;
        }
      }
    }
    return pcapsResponse;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapGetter#getPcaps(java.lang.String, long,
   * long, boolean)
   */
 
  public PcapsResponse getPcaps(String key, long startTime, long endTime,
      boolean includeReverseTraffic) throws IOException {
    Assert.hasText(key, "key must not be null or empty");
    return getPcaps(Arrays.asList(key), null, startTime, endTime,
        includeReverseTraffic, false, ConfigurationUtil.getDefaultResultSize());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapGetter#getPcaps(java.util.List)
   */
 
  public PcapsResponse getPcaps(List<String> keys) throws IOException {
    Assert.notEmpty(keys, "'keys' must not be null or empty");
    return getPcaps(keys, null, -1, -1,
        ConfigurationUtil.isDefaultIncludeReverseTraffic(), false,
        ConfigurationUtil.getDefaultResultSize());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapGetter#getPcaps(java.lang.String)
   */
 
  public PcapsResponse getPcaps(String key) throws IOException {
    Assert.hasText(key, "key must not be null or empty");
    return getPcaps(Arrays.asList(key), null, -1, -1,
        ConfigurationUtil.isDefaultIncludeReverseTraffic(), false,
        ConfigurationUtil.getDefaultResultSize());
  }

  /**
   * Always returns the singleton instance.
   * 
   * @return IPcapGetter singleton instance
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static IPcapGetter getInstance() throws IOException {
    if (pcapGetterHBase == null) {
      synchronized (PcapGetterHBaseImpl.class) {
        if (pcapGetterHBase == null) {
          pcapGetterHBase = new PcapGetterHBaseImpl();
        }
      }
    }
    return pcapGetterHBase;
  }

  /**
   * Instantiates a new pcap getter h base impl.
   */
  private PcapGetterHBaseImpl() {
  }

  /**
   * Adds reverse keys to the list if the flag 'includeReverseTraffic' is set to
   * true; removes duplicates and sorts the list by ascending order;.
   * 
   * @param keys
   *          input keys
   * @param includeReverseTraffic
   *          flag whether or not to include reverse traffic
   * @return List<String>
   */
  @VisibleForTesting
  List<String> sortKeysByAscOrder(List<String> keys,
      boolean includeReverseTraffic) {
    Assert.notEmpty(keys, "'keys' must not be null");
    if (includeReverseTraffic) {
      keys.addAll(PcapHelper.reverseKey(keys));
    }
    List<String> deDupKeys = removeDuplicateKeys(keys);
    Collections.sort(deDupKeys);
    return deDupKeys;
  }

  /**
   * Removes the duplicate keys.
   * 
   * @param keys
   *          the keys
   * @return the list
   */
  @VisibleForTesting
public
  List<String> removeDuplicateKeys(List<String> keys) {
    Set<String> set = new HashSet<String>(keys);
    return new ArrayList<String>(set);
  }

  /**
   * <p>
   * Returns the sublist starting from the element after the lastRowKey
   * to the last element in the list; if the 'lastRowKey' is not matched
   * the complete list will be returned.
   * </p>
   * 
   * <pre>
   * Eg :
   *  keys = [18800006-1800000b-06-0019-caac, 18800006-1800000b-06-0050-5af6, 18800006-1800000b-11-0035-3810]
   *  lastRowKey = "18800006-1800000b-06-0019-caac-65140-40815"
   *  and the response from this method [18800006-1800000b-06-0050-5af6, 18800006-1800000b-11-0035-3810]
   * </pre>
   * 
   * @param keys
   *          keys
   * @param lastRowKey
   *          last row key of the previous partial response
   * @return List<String>
   */
  @VisibleForTesting
  List<String> getUnprocessedSublistOfKeys(List<String> keys,
      String lastRowKey) {
    Assert.notEmpty(keys, "'keys' must not be null");
    Assert.hasText(lastRowKey, "'lastRowKey' must not be null");
    String partialKey = getTokens(lastRowKey, 5);
    int startIndex = 0;
    for (int i = 0; i < keys.size(); i++) {
      if (partialKey.equals(keys.get(i))) {
        startIndex = i + 1;
        break;
      }
    }
    List<String> unprocessedKeys = keys.subList(startIndex, keys.size());
    return unprocessedKeys;
  }

  /**
   * Returns the first 'noOfTokens' tokens from the given key; token delimiter
   * "-";.
   * 
   * @param key
   *          given key
   * @param noOfTokens
   *          number of tokens to retrieve
   * @return the tokens
   */
  @VisibleForTesting
  String getTokens(String key, int noOfTokens) {
    String delimeter = HBaseConfigConstants.PCAP_KEY_DELIMETER;
    String regex = "\\" + delimeter;
    String[] keyTokens = key.split(regex);
    Assert.isTrue(noOfTokens < keyTokens.length,
        "Invalid value for 'noOfTokens'");
    StringBuffer sbf = new StringBuffer();
    for (int i = 0; i < noOfTokens; i++) {
      sbf.append(keyTokens[i]);
      if (i != (noOfTokens - 1)) {
        sbf.append(HBaseConfigConstants.PCAP_KEY_DELIMETER);
      }

    }
    return sbf.toString();
  }

  /**
   * Process key.
   * 
   * @param pcapsResponse
   *          the pcaps response
   * @param key
   *          the key
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @param isPartialResponse
   *          the is partial response
   * @param includeDuplicateLastRow
   *          the include duplicate last row
   * @param maxResultSize
   *          the max result size
   * @return the pcaps response
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @VisibleForTesting
  PcapsResponse processKey(PcapsResponse pcapsResponse, String key,
      long startTime, long endTime, boolean isPartialResponse,
      boolean includeDuplicateLastRow, long maxResultSize) throws IOException {
    HTable table = null;
    Scan scan = null;
    List<Cell> scannedCells = null;
    try {
      // 1. Create start and stop row for the key;
      Map<String, String> keysMap = createStartAndStopRowKeys(key,
          isPartialResponse, includeDuplicateLastRow);

      // 2. if the input key contains all fragments (7) and it is not part
      // of previous partial response (isPartialResponse),
      // 'keysMap' will be null; do a Get; currently not doing any
      // response size related checks for Get;
      // by default all cells from a specific row are sorted by timestamp
      if (keysMap == null) {
        Get get = createGetRequest(key, startTime, endTime);
        List<Cell> cells = executeGetRequest(table, get);
        for (Cell cell : cells) {
          pcapsResponse.addPcaps(CellUtil.cloneValue(cell));
        }
        return pcapsResponse;
      }
      // 3. Create and execute Scan request
      scan = createScanRequest(pcapsResponse, keysMap, startTime, endTime,
          maxResultSize);
      scannedCells = executeScanRequest(table, scan);
      LOGGER.info("scannedCells size :" + scannedCells.size());
      addToResponse(pcapsResponse, scannedCells, maxResultSize);

    } catch (IOException e) {
      LOGGER.error("Exception occurred while fetching Pcaps for the keys :"
          + key, e);
      if (e instanceof ZooKeeperConnectionException
          || e instanceof MasterNotRunningException
          || e instanceof NoServerForRegionException) {
        int maxRetryLimit = ConfigurationUtil.getConnectionRetryLimit();
        System.out.println("maxRetryLimit =" + maxRetryLimit);
        for (int attempt = 1; attempt <= maxRetryLimit; attempt++) {
          System.out.println("attempting  =" + attempt);
          try {
            HBaseConfigurationUtil.closeConnection(); // closing the
            // existing
            // connection
            // and retry,
            // it will
            // create a new
            // HConnection
            scannedCells = executeScanRequest(table, scan);
            addToResponse(pcapsResponse, scannedCells, maxResultSize);
            break;
          } catch (IOException ie) {
            if (attempt == maxRetryLimit) {
              LOGGER.error("Throwing the exception after retrying "
                  + maxRetryLimit + " times.");
              throw e;
            }
          }
        }
      }

    } finally {
      if (table != null) {
        table.close();
      }
    }
    return pcapsResponse;
  }

  /**
   * Adds the to response.
   * 
   * @param pcapsResponse
   *          the pcaps response
   * @param scannedCells
   *          the scanned cells
   * @param maxResultSize
   *          the max result size
   */
  private void addToResponse(PcapsResponse pcapsResponse,
      List<Cell> scannedCells, long maxResultSize) {
    String lastKeyFromCurrentScan = null;
    if (scannedCells != null && scannedCells.size() > 0) {
      lastKeyFromCurrentScan = new String(CellUtil.cloneRow(scannedCells
          .get(scannedCells.size() - 1)));
    }
    // 4. calculate the response size
    Collections.sort(scannedCells, PcapHelper.getCellTimestampComparator());
    for (Cell sortedCell : scannedCells) {
      pcapsResponse.addPcaps(CellUtil.cloneValue(sortedCell));
    }
    if (!pcapsResponse.isResonseSizeWithinLimit(maxResultSize)) {
      pcapsResponse.setStatus(PcapsResponse.Status.PARTIAL); // response size
                                                             // reached
      pcapsResponse.setLastRowKey(new String(lastKeyFromCurrentScan));
    }
  }

  /**
   * Builds start and stop row keys according to the following logic : 1.
   * Creates tokens out of 'key' using pcap_id delimiter ('-') 2. if the input
   * 'key' contains (assume : configuredTokensInRowKey=7 and
   * minimumTokensIninputKey=5): a). 5 tokens
   * ("srcIp-dstIp-protocol-srcPort-dstPort") startKey =
   * "srcIp-dstIp-protocol-srcPort-dstPort-00000-00000" stopKey =
   * "srcIp-dstIp-protocol-srcPort-dstPort-99999-99999" b). 6 tokens
   * ("srcIp-dstIp-protocol-srcPort-dstPort-id1") startKey =
   * "srcIp-dstIp-protocol-srcPort-dstPort-id1-00000" stopKey =
   * "srcIp-dstIp-protocol-srcPort-dstPort-id1-99999"
   * 
   * c). 7 tokens ("srcIp-dstIp-protocol-srcPort-dstPort-id1-id2") 1>. if the
   * key is NOT part of the partial response from previous request, return
   * 'null' 2>. if the key is part of partial response from previous request
   * startKey = "srcIp-dstIp-protocol-srcPort-dstPort-id1-(id2+1)"; 1 is added
   * to exclude this key as it was included in the previous request stopKey =
   * "srcIp-dstIp-protocol-srcPort-dstPort-99999-99999"
   * 
   * @param key
   *          the key
   * @param isLastRowKey
   *          if the key is part of partial response
   * @param includeDuplicateLastRow
   *          the include duplicate last row
   * @return Map<String, String>
   */
  @VisibleForTesting
  Map<String, String> createStartAndStopRowKeys(String key,
      boolean isLastRowKey, boolean includeDuplicateLastRow) {
    String delimeter = HBaseConfigConstants.PCAP_KEY_DELIMETER;
    String regex = "\\" + delimeter;
    String[] keyTokens = key.split(regex);

    String startKey = null;
    String endKey = null;
    Map<String, String> map = new HashMap<String, String>();

    int configuredTokensInRowKey = ConfigurationUtil
        .getConfiguredTokensInRowkey();
    int minimumTokensIninputKey = ConfigurationUtil
        .getMinimumTokensInInputkey();
    Assert
        .isTrue(
            minimumTokensIninputKey <= configuredTokensInRowKey,
            "tokens in the input key (separated by '-'), must be less than or equal to the tokens used in hbase table row key ");
    // in case if the input key contains 'configuredTokensInRowKey' tokens and
    // it is NOT a
    // partial response key, do a Get instead of Scan
    if (keyTokens.length == configuredTokensInRowKey) {
      if (!isLastRowKey) {
        return null;
      }
      // it is a partial response key; 'startKey' is same as input partial
      // response key; 'endKey' can be built by replacing
      // (configuredTokensInRowKey - minimumTokensIninputKey) tokens
      // of input partial response key with '99999'
      if (keyTokens.length == minimumTokensIninputKey) {
        return null;
      }
      int appendingTokenSlots = configuredTokensInRowKey
          - minimumTokensIninputKey;
      if (appendingTokenSlots > 0) {
        String partialKey = getTokens(key, minimumTokensIninputKey);
        StringBuffer sbfStartNew = new StringBuffer(partialKey);
        StringBuffer sbfEndNew = new StringBuffer(partialKey);
        for (int i = 0; i < appendingTokenSlots; i++) {
          if (i == (appendingTokenSlots - 1)) {
            if (!includeDuplicateLastRow) {
              sbfStartNew
                  .append(HBaseConfigConstants.PCAP_KEY_DELIMETER)
                  .append(
                      Integer.valueOf(keyTokens[minimumTokensIninputKey + i]) + 1);
            } else {
              sbfStartNew.append(HBaseConfigConstants.PCAP_KEY_DELIMETER)
                  .append(keyTokens[minimumTokensIninputKey + i]);
            }
          } else {
            sbfStartNew.append(HBaseConfigConstants.PCAP_KEY_DELIMETER).append(
                keyTokens[minimumTokensIninputKey + i]);
          }
          sbfEndNew.append(HBaseConfigConstants.PCAP_KEY_DELIMETER).append(
              getMaxLimitForAppendingTokens());
        }
        startKey = sbfStartNew.toString();
        endKey = sbfEndNew.toString();
      }
    } else {
      StringBuffer sbfStart = new StringBuffer(key);
      StringBuffer sbfEnd = new StringBuffer(key);
      for (int i = keyTokens.length; i < configuredTokensInRowKey; i++) {
        sbfStart.append(HBaseConfigConstants.PCAP_KEY_DELIMETER).append(
            getMinLimitForAppendingTokens());
        sbfEnd.append(HBaseConfigConstants.PCAP_KEY_DELIMETER).append(
            getMaxLimitForAppendingTokens());
      }
      startKey = sbfStart.toString();
      endKey = sbfEnd.toString();
    }
    map.put(HBaseConfigConstants.START_KEY, startKey);
    map.put(HBaseConfigConstants.END_KEY, endKey);

    return map;
  }

  /**
   * Returns false if keys is empty or null AND lastRowKey is null or
   * empty; otherwise returns true;.
   * 
   * @param keys
   *          input row keys
   * @param lastRowKey
   *          partial response key
   * @return boolean
   */
  @VisibleForTesting
  boolean checkIfValidInput(List<String> keys, String lastRowKey) {
    if (CollectionUtils.isEmpty(keys)
        && StringUtils.isEmpty(lastRowKey)) {
      return false;
    }
    return true;
  }

  /**
   * Executes the given Get request.
   * 
   * @param table
   *          hbase table
   * @param get
   *          Get
   * @return List<Cell>
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private List<Cell> executeGetRequest(HTable table, Get get)
      throws IOException {
    LOGGER.info("Get :" + get.toString());
    table = (HTable) HBaseConfigurationUtil.getConnection().getTable(
        ConfigurationUtil.getTableName());
    Result result = table.get(get);
    List<Cell> cells = result.getColumnCells(
        ConfigurationUtil.getColumnFamily(),
        ConfigurationUtil.getColumnQualifier());
    return cells;
  }

  /**
   * Execute scan request.
   * 
   * @param table
   *          hbase table
   * @param scan
   *          the scan
   * @return the list
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private List<Cell> executeScanRequest(HTable table, Scan scan)
      throws IOException {
    LOGGER.info("Scan :" + scan.toString());
    table = (HTable) HBaseConfigurationUtil.getConnection().getTable(
    		ConfigurationUtil.getConfiguration().getString("hbase.table.name"));
    ResultScanner resultScanner = table.getScanner(scan);
    List<Cell> scannedCells = new ArrayList<Cell>();
    for (Result result = resultScanner.next(); result != null; result = resultScanner
        .next()) {
      List<Cell> cells = result.getColumnCells(
          ConfigurationUtil.getColumnFamily(),
          ConfigurationUtil.getColumnQualifier());
      if (cells != null) {
        for (Cell cell : cells) {
          scannedCells.add(cell);
        }
      }
    }
    return scannedCells;
  }

  /**
   * Creates the get request.
   * 
   * @param key
   *          the key
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @return the gets the
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @VisibleForTesting
  Get createGetRequest(String key, long startTime, long endTime)
      throws IOException {
    Get get = new Get(Bytes.toBytes(key));
    // set family name
    get.addFamily(ConfigurationUtil.getColumnFamily());

    // set column family, qualifier
    get.addColumn(ConfigurationUtil.getColumnFamily(),
        ConfigurationUtil.getColumnQualifier());

    // set max versions
    get.setMaxVersions(ConfigurationUtil.getMaxVersions());

    // set time range
    setTimeRangeOnGet(get, startTime, endTime);
    return get;
  }

  /**
   * Creates the scan request.
   * 
   * @param pcapsResponse
   *          the pcaps response
   * @param keysMap
   *          the keys map
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @param maxResultSize
   *          the max result size
   * @return the scan
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @VisibleForTesting
  Scan createScanRequest(PcapsResponse pcapsResponse,
      Map<String, String> keysMap, long startTime, long endTime,
      long maxResultSize) throws IOException {
    Scan scan = new Scan();
    // set column family, qualifier
    scan.addColumn(ConfigurationUtil.getColumnFamily(),
        ConfigurationUtil.getColumnQualifier());

    // set start and stop keys
    scan.setStartRow(keysMap.get(HBaseConfigConstants.START_KEY).getBytes());
    scan.setStopRow(keysMap.get(HBaseConfigConstants.END_KEY).getBytes());

    // set max results size : remaining size = max results size - ( current
    // pcaps response size + possible maximum row size)
    long remainingSize = maxResultSize
        - (pcapsResponse.getResponseSize() + ConfigurationUtil.getMaxRowSize());

    if (remainingSize > 0) {
      scan.setMaxResultSize(remainingSize);
    }
    // set max versions
    scan.setMaxVersions(ConfigurationUtil.getConfiguration().getInt(
        "hbase.table.column.maxVersions"));

    // set time range
    setTimeRangeOnScan(scan, startTime, endTime);
    return scan;
  }

  /**
   * Sets the time range on scan.
   * 
   * @param scan
   *          the scan
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private void setTimeRangeOnScan(Scan scan, long startTime, long endTime)
      throws IOException {
    boolean setTimeRange = true;
    if (startTime < 0 && endTime < 0) {
      setTimeRange = false;
    }
    if (setTimeRange) {
      if (startTime < 0) {
        startTime = 0;
      } else {
        startTime = PcapHelper.convertToDataCreationTimeUnit(startTime);
      }
      if (endTime < 0) {
        endTime = Long.MAX_VALUE;
      } else {
        endTime = PcapHelper.convertToDataCreationTimeUnit(endTime);
      }
      Assert.isTrue(startTime < endTime,
          "startTime value must be less than endTime value");
      scan.setTimeRange(startTime, endTime);
    }
  }

  /**
   * Sets the time range on get.
   * 
   * @param get
   *          the get
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private void setTimeRangeOnGet(Get get, long startTime, long endTime)
      throws IOException {
    boolean setTimeRange = true;
    if (startTime < 0 && endTime < 0) {
      setTimeRange = false;
    }
    if (setTimeRange) {
      if (startTime < 0) {
        startTime = 0;
      } else {
        startTime = PcapHelper.convertToDataCreationTimeUnit(startTime);
      }
      if (endTime < 0) {
        endTime = Long.MAX_VALUE;
      } else {
        endTime = PcapHelper.convertToDataCreationTimeUnit(endTime);
      }
      Assert.isTrue(startTime < endTime,
          "startTime value must be less than endTime value");
      get.setTimeRange(startTime, endTime);
    }
  }

  /**
   * Gets the min limit for appending tokens.
   * 
   * @return the min limit for appending tokens
   */
  private String getMinLimitForAppendingTokens() {
    int digits = ConfigurationUtil.getAppendingTokenDigits();
    StringBuffer sbf = new StringBuffer();
    for (int i = 0; i < digits; i++) {
      sbf.append("0");
    }
    return sbf.toString();
  }

  /**
   * Gets the max limit for appending tokens.
   * 
   * @return the max limit for appending tokens
   */
  private String getMaxLimitForAppendingTokens() {
    int digits = ConfigurationUtil.getAppendingTokenDigits();
    StringBuffer sbf = new StringBuffer();
    for (int i = 0; i < digits; i++) {
      sbf.append("9");
    }
    return sbf.toString();
  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 2) {
      usage();
      return;
    }
    String outputFileName = null;
    outputFileName = args[1];
    List<String> keys = Arrays.asList(StringUtils.split(args[2], ","));
    System.out.println("Geting keys " + keys);
    long startTime = 0;
    long endTime = Long.MAX_VALUE;
    if (args.length > 3) {
      startTime = Long.valueOf(args[3]);
    }
    if (args.length > 4) {
      endTime = Long.valueOf(args[4]);
    }
    System.out.println("With start time " + startTime + " and end time "
        + endTime);
    PcapGetterHBaseImpl downloader = new PcapGetterHBaseImpl();
    PcapsResponse pcaps = downloader.getPcaps(keys, null, startTime, endTime,
        false, false, 6);
    File file = new File(outputFileName);
    FileUtils.write(file, "", false);
    FileUtils.writeByteArrayToFile(file, pcaps.getPcaps(), true);
  }

  /**
   * Usage.
   */
  private static void usage() {
    System.out.println("java " + PcapGetterHBaseImpl.class.getName() // $codepro.audit.disable
        // debuggingCode
        + " <zk quorum> <output file> <start key> [stop key]");
  }

}
