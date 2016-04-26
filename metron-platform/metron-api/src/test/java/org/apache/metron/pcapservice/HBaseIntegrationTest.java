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
    testTable = testUtil.createTable(Bytes.toBytes("test_pcaps_local"), Bytes.toBytes("cf"));
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
