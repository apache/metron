/**
 * 
 */
package org.apache.metron.pcapservice;

import java.io.IOException;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * The Class HBaseIntegrationTest.
 * 
 * @author Sayi
 */
public class HBaseIntegrationTest {

  /** The test util. */
  private final HBaseTestingUtility testUtil = new HBaseTestingUtility();

  /** The test table. */
  private HTable testTable;

  /**
   * Inits the cluster.
   * 
   * @throws Exception
   *           the exception
   */
  void initCluster() throws Exception {
    // testUtil.getConfiguration().addResource("hbase-site-local.xml");
    // testUtil.getConfiguration().reloadConfiguration();
    // start mini hbase cluster
    testUtil.startMiniCluster(1);
    // create tables
    createTable();

  }

  /**
   * Creates the table.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private void createTable() throws IOException {
    testTable = testUtil.createTable("test_pcaps_local", "cf");
    System.out.println("after 'test_pcaps_local' table creation ");
    // create put
    Put put = new Put(Bytes.toBytes("1111")); // row key =1111
    put.add(Bytes.toBytes("cf"), Bytes.toBytes("packet"),
        Bytes.toBytes("aaaaaaaa"));
    testTable.put(put);
    System.out.println("after testTable.put(put)");

  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   * @throws Exception
   *           the exception
   */
  public static void main(String[] args) throws Exception {
    // HBaseIntegrationTest test = new HBaseIntegrationTest();
    // test.initCluster();

  }

}
