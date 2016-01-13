package org.apache.metron.pcapservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.NoServerForRegionException;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import com.google.common.annotations.VisibleForTesting;
import org.apache.metron.pcap.PcapMerger;

/**
 * Singleton class which integrates with HBase table and returns sorted pcaps
 * based on the timestamp for the given range of keys. Creates HConnection if it
 * is not already created and the same connection instance is being used for all
 * requests
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public class PcapScannerHBaseImpl implements IPcapScanner {

  /** The Constant LOGGER. */
  private static final Logger LOGGER = Logger
      .getLogger(PcapScannerHBaseImpl.class);

  /** The Constant DEFAULT_HCONNECTION_RETRY_LIMIT. */
  private static final int DEFAULT_HCONNECTION_RETRY_LIMIT = 0;

  /** The pcap scanner h base. */
  private static IPcapScanner pcapScannerHBase = null;

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapScanner#getPcaps(java.lang.String,
   * java.lang.String, long, long, long)
   */
  
  public byte[] getPcaps(String startKey, String endKey, long maxResultSize,
      long startTime, long endTime) throws IOException {
    Assert.hasText(startKey, "startKey must no be null or empty");
    byte[] cf = Bytes.toBytes(ConfigurationUtil.getConfiguration()
        .getString("hbase.table.column.family"));
    byte[] cq = Bytes.toBytes(ConfigurationUtil.getConfiguration()
        .getString("hbase.table.column.qualifier"));
    // create scan request
    Scan scan = createScanRequest(cf, cq, startKey, endKey, maxResultSize,
        startTime, endTime);
    List<byte[]> pcaps = new ArrayList<byte[]>();
    HTable table = null;
    try {
      pcaps = scanPcaps(pcaps, table, scan, cf, cq);
    } catch (IOException e) {
      LOGGER.error(
          "Exception occurred while fetching Pcaps for the key range : startKey="
              + startKey + ", endKey=" + endKey, e);
      if (e instanceof ZooKeeperConnectionException
          || e instanceof MasterNotRunningException
          || e instanceof NoServerForRegionException) {
        int maxRetryLimit = getConnectionRetryLimit();
        for (int attempt = 1; attempt <= maxRetryLimit; attempt++) {
          try {
            HBaseConfigurationUtil.closeConnection(); // closing the existing
                                                      // connection and retry,
                                                      // it will create a new
                                                      // HConnection
            pcaps = scanPcaps(pcaps, table, scan, cf, cq);
            break;
          } catch (IOException ie) {
            if (attempt == maxRetryLimit) {
              System.out.println("Throwing the exception after retrying "
                  + maxRetryLimit + " times.");
              throw e;
            }
          }
        }
      } else {
        throw e;
      }
    } finally {
      if (table != null) {
        table.close();
      }
    }
    if (pcaps.size() == 1) {
      return pcaps.get(0);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PcapMerger.merge(baos, pcaps);
    byte[] response = baos.toByteArray();
    return response;
  }

  /**
   * Creates the scan request.
   * 
   * @param cf
   *          the cf
   * @param cq
   *          the cq
   * @param startKey
   *          the start key
   * @param endKey
   *          the end key
   * @param maxResultSize
   *          the max result size
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   * @return the scan
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @VisibleForTesting
  Scan createScanRequest(byte[] cf, byte[] cq, String startKey, String endKey,
      long maxResultSize, long startTime, long endTime) throws IOException {
    Scan scan = new Scan();
    scan.addColumn(cf, cq);
    scan.setMaxVersions(ConfigurationUtil.getConfiguration().getInt(
        "hbase.table.column.maxVersions"));
    scan.setStartRow(startKey.getBytes());
    if (endKey != null) {
      scan.setStopRow(endKey.getBytes());
    }
    scan.setMaxResultSize(maxResultSize);
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
    }
    // create Scan request;
    if (setTimeRange) {
      scan.setTimeRange(startTime, endTime);
    }
    return scan;
  }

  /**
   * Scan pcaps.
   * 
   * @param pcaps
   *          the pcaps
   * @param table
   *          the table
   * @param scan
   *          the scan
   * @param cf
   *          the cf
   * @param cq
   *          the cq
   * @return the list
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @VisibleForTesting
  List<byte[]> scanPcaps(List<byte[]> pcaps, HTable table, Scan scan,
      byte[] cf, byte[] cq) throws IOException {
    LOGGER.info("Scan =" + scan.toString());
    table = (HTable) HBaseConfigurationUtil.getConnection().getTable(
    		ConfigurationUtil.getConfiguration().getString("hbase.table.name"));
    ResultScanner resultScanner = table.getScanner(scan);
    List<Cell> scannedCells = new ArrayList<Cell>();
    for (Result result = resultScanner.next(); result != null; result = resultScanner
        .next()) {
      List<Cell> cells = result.getColumnCells(cf, cq);
      if (cells != null) {
        for (Cell cell : cells) {
          scannedCells.add(cell);
        }
      }
    }
    Collections.sort(scannedCells, PcapHelper.getCellTimestampComparator());
    LOGGER.info("sorted cells :" + scannedCells.toString());
    for (Cell sortedCell : scannedCells) {
      pcaps.add(CellUtil.cloneValue(sortedCell));
    }
    return pcaps;
  }

  /**
   * Gets the connection retry limit.
   * 
   * @return the connection retry limit
   */
  private int getConnectionRetryLimit() {
    return ConfigurationUtil.getConfiguration().getInt(
        "hbase.hconnection.retries.number", DEFAULT_HCONNECTION_RETRY_LIMIT);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapScanner#getPcaps(java.lang.String,
   * java.lang.String)
   */
  
  public byte[] getPcaps(String startKey, String endKey) throws IOException {
    Assert.hasText(startKey, "startKey must no be null or empty");
    Assert.hasText(endKey, "endKey must no be null or empty");
    return getPcaps(startKey, endKey, ConfigurationUtil.getDefaultResultSize(),
        -1, -1);
  }

  /**
   * Always returns the singleton instance.
   * 
   * @return IPcapScanner singleton instance
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static IPcapScanner getInstance() throws IOException {
    if (pcapScannerHBase == null) {
      synchronized (PcapScannerHBaseImpl.class) {
        if (pcapScannerHBase == null) {
          pcapScannerHBase = new PcapScannerHBaseImpl();
        }
      }
    }
    return pcapScannerHBase;
  }

  /**
   * Instantiates a new pcap scanner h base impl.
   */
  private PcapScannerHBaseImpl() {
  }

  /**
   * The main method.
   */
  // public static void main(String[] args) throws IOException {
  // if (args == null || args.length < 3) {
  // usage();
  // return;
  // }
  // String outputFileName = null;
  // String startKey = null;
  // String stopKey = null;
  // outputFileName = args[0];
  // startKey = args[1];
  // if (args.length > 2) { // NOPMD by sheetal on 1/29/14 3:55 PM
  // stopKey = args[2];
  // }
  // PcapScannerHBaseImpl downloader = new PcapScannerHBaseImpl();
  // byte[] pcaps = downloader.getPcaps(startKey, stopKey, defaultResultSize, 0,
  // Long.MAX_VALUE);
  // File file = new File(outputFileName);
  // FileUtils.write(file, "", false);
  // ByteArrayOutputStream baos = new ByteArrayOutputStream(); //
  // $codepro.audit.disable
  // // closeWhereCreated
  // PcapMerger.merge(baos, pcaps);
  // FileUtils.writeByteArrayToFile(file, baos.toByteArray(), true);
  // }

  /**
   * Usage.
   */
  @SuppressWarnings("unused")
  private static void usage() {
    System.out.println("java " + PcapScannerHBaseImpl.class.getName() // NOPMD
                                                                      // by
        // sheetal
        // <!-- //
        // $codepro.audit.disable
        // debuggingCode
        // -->
        // on
        // 1/29/14
        // 3:55
        // PM
        + " <zk quorum> <output file> <start key> [stop key]");
  }
}
