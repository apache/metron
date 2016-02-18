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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

// TODO: Auto-generated Javadoc
/**
 * The Class PcapScannerHBaseImplTest.
 */
public class PcapScannerHBaseImplTest {

  /**
   * Sets the up.
   * 
   * @throws Exception
   *           the exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * Tear down.
   * 
   * @throws Exception
   *           the exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test_create scan request.
   * 
   * @throws IOException
   *           the IO exception
   */
  @Test
  public void test_createScanRequest() throws IOException {
    // mocking
    PcapScannerHBaseImpl pcapScanner = (PcapScannerHBaseImpl) PcapScannerHBaseImpl
        .getInstance();
    byte[] cf = "cf".getBytes();
    byte[] cq = "pcap".getBytes();
    String startKey = "0a07002b-0a078039-06-1e8b-0087";
    String endKey = "0a070025-0a07807a-06-aab8-c360";
    long maxResultSize = 60;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;

    // actual call
    Scan scan = pcapScanner.createScanRequest(cf, cq, startKey, endKey,
        maxResultSize, -1, -1);

    // verify
    Assert.assertTrue(scan.getTimeRange().getMin() == 0);
    Assert.assertTrue(Arrays.equals(scan.getStartRow(), startKey.getBytes()));
    Assert.assertTrue(Arrays.equals(scan.getStopRow(), endKey.getBytes()));
  }

  /**
   * Test_create scan request_with timestamps.
   * 
   * @throws IOException
   *           the IO exception
   */
  @Test
  public void test_createScanRequest_withTimestamps() throws IOException {
    // mocking
    PcapScannerHBaseImpl pcapScanner = (PcapScannerHBaseImpl) PcapScannerHBaseImpl
        .getInstance();
    byte[] cf = "cf".getBytes();
    byte[] cq = "pcap".getBytes();
    String startKey = "0a07002b-0a078039-06-1e8b-0087";
    String endKey = "0a070025-0a07807a-06-aab8-c360";
    long maxResultSize = 60;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;

    // actual call
    Scan scan = pcapScanner.createScanRequest(cf, cq, startKey, endKey,
        maxResultSize, startTime, endTime);

    // verify
    Assert.assertTrue(scan.getTimeRange().getMin() == 1376782349234L);
    Assert.assertTrue(Arrays.equals(scan.getStartRow(), startKey.getBytes()));
    Assert.assertTrue(Arrays.equals(scan.getStopRow(), endKey.getBytes()));
  }

  /**
   * Test_get pcaps_with all arguments.
   * 
   * @throws IOException
   *           the IO exception
   */
  @SuppressWarnings({ "unchecked", "unused" })
  @Test
  public void test_getPcaps_withAllArguments() throws IOException {
    // mocking
    PcapScannerHBaseImpl pcapScanner = (PcapScannerHBaseImpl) PcapScannerHBaseImpl
        .getInstance();
    PcapScannerHBaseImpl spy = Mockito.spy(pcapScanner);
    byte[] cf = "cf".getBytes();
    byte[] cq = "pcap".getBytes();
    String startKey = "0a07002b-0a078039-06-1e8b-0087";
    String endKey = "0a070025-0a07807a-06-aab8-c360";
    long maxResultSize = 60;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());

    Mockito
        .doReturn(mockPcaps)
        .when(spy)
        .scanPcaps(Mockito.any(ArrayList.class), Mockito.any(HTable.class),
            Mockito.any(Scan.class), Mockito.any(byte[].class),
            Mockito.any(byte[].class));

    // actual call
    byte[] response = spy.getPcaps(startKey, endKey, maxResultSize, startTime,
        endTime);

    // verify
    Assert.assertTrue(response.length == mockPcaps.get(0).length);
  }

  /**
   * Test_get pcaps_with minimal arguments.
   * 
   * @throws IOException
   *           the IO exception
   */
  @SuppressWarnings({ "unchecked", "unused" })
  @Test
  public void test_getPcaps_withMinimalArguments() throws IOException {
    // mocking
    PcapScannerHBaseImpl pcapScanner = (PcapScannerHBaseImpl) PcapScannerHBaseImpl
        .getInstance();
    PcapScannerHBaseImpl spy = Mockito.spy(pcapScanner);
    byte[] cf = "cf".getBytes();
    byte[] cq = "pcap".getBytes();
    String startKey = "0a07002b-0a078039-06-1e8b-0087";
    String endKey = "0a070025-0a07807a-06-aab8-c360";
    long maxResultSize = 60;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());

    Mockito
        .doReturn(mockPcaps)
        .when(spy)
        .scanPcaps(Mockito.any(ArrayList.class), Mockito.any(HTable.class),
            Mockito.any(Scan.class), Mockito.any(byte[].class),
            Mockito.any(byte[].class));

    // actual call
    byte[] response = spy.getPcaps(startKey, endKey);

    // verify
    Assert.assertTrue(response.length == mockPcaps.get(0).length);
  }

  /**
   * Test_get pcaps_multiple pcaps.
   * 
   * @throws IOException
   *           the IO exception
   */
  @SuppressWarnings({ "unchecked", "unused" })
  @Test
  public void test_getPcaps_multiplePcaps() throws IOException {
    // mocking
    PcapScannerHBaseImpl pcapScanner = (PcapScannerHBaseImpl) PcapScannerHBaseImpl
        .getInstance();
    PcapScannerHBaseImpl spy = Mockito.spy(pcapScanner);
    byte[] cf = "cf".getBytes();
    byte[] cq = "pcap".getBytes();
    String startKey = "0a07002b-0a078039-06-1e8b-0087";
    String endKey = "0a070025-0a07807a-06-aab8-c360";
    long maxResultSize = 60;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());
    mockPcaps.add(getTestPcapBytes());

    Mockito
        .doReturn(mockPcaps)
        .when(spy)
        .scanPcaps(Mockito.any(ArrayList.class), Mockito.any(HTable.class),
            Mockito.any(Scan.class), Mockito.any(byte[].class),
            Mockito.any(byte[].class));

    // actual call
    byte[] response = spy.getPcaps(startKey, endKey);

    // verify
    Assert.assertNotNull(response);
    Assert.assertTrue(response.length > mockPcaps.get(0).length);
  }

  /**
   * Gets the test pcap bytes.
   * 
   * @return the test pcap bytes
   * @throws IOException
   *           the IO exception
   */
  private byte[] getTestPcapBytes() throws IOException {
    File fin = new File("src/test/resources/test-tcp-packet.pcap");
    byte[] pcapBytes = FileUtils.readFileToByteArray(fin);
    return pcapBytes;
  }
}
