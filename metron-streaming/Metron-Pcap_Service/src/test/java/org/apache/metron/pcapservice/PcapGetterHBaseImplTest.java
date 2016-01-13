package com.apache.metron.pcapservice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.Assert;

import com.apache.metron.pcapservice.PcapGetterHBaseImpl;
import com.apache.metron.pcapservice.PcapsResponse;

/**
 * The Class PcapGetterHBaseImplTest.
 */
public class PcapGetterHBaseImplTest {

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
   * Test_get pcaps_with list.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void test_getPcaps_withList() throws IOException {
    // mocking
    String[] keys = { "0a07002b-0a078039-06-1e8b-0087",
        "0a070025-0a07807a-06-aab8-c360" };
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    PcapGetterHBaseImpl spy = Mockito.spy(pcapGetter);

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());

    // Mockito.doReturn(mockPcaps).when(spy).scanPcaps(Mockito.any(ArrayList.class),
    // Mockito.any(HTable.class), Mockito.any(Scan.class),
    // Mockito.any(byte[].class), Mockito.any(byte[].class));
    //
    //
    // actual call
    // PcapsResponse response = spy.getPcaps(Arrays.asList(keys));

    // verify
    // Assert.assertTrue(response.getResponseSize() == mockPcaps.get(0).length);
  }

  /**
   * Test_get pcaps_with key.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void test_getPcaps_withKey() throws IOException {
    // mocking
    String key = "0a07002b-0a078039-06-1e8b-0087";
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    PcapGetterHBaseImpl spy = Mockito.spy(pcapGetter);

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());

    // //
    // Mockito.doReturn(mockPcaps).when(spy).scanPcaps(Mockito.any(ArrayList.class),
    // Mockito.any(HTable.class), Mockito.any(Scan.class),
    // Mockito.any(byte[].class), Mockito.any(byte[].class));
    //

    // actual call
    // PcapsResponse response = spy.getPcaps(key);

    // verify
    // Assert.assertTrue(response.getResponseSize() == mockPcaps.get(0).length);
  }

  /**
   * Test_get pcaps_with key and timestamps.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void test_getPcaps_withKeyAndTimestamps() throws IOException {
    // mocking
    String key = "0a07002b-0a078039-06-1e8b-0087";
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    PcapGetterHBaseImpl spy = Mockito.spy(pcapGetter);

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());

    // Mockito.doReturn(mockPcaps).when(spy).scanPcaps(Mockito.any(ArrayList.class),
    // Mockito.any(HTable.class), Mockito.any(Scan.class),
    // Mockito.any(byte[].class), Mockito.any(byte[].class));

    // actual call
    // PcapsResponse response = spy.getPcaps(key, startTime, endTime, false);

    // verify
    // Assert.assertTrue(response.getResponseSize() == mockPcaps.get(0).length);
  }

  /**
   * Test_get pcaps_with key_multiple pcaps.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void test_getPcaps_withKey_multiplePcaps() throws IOException {
    // mocking
    String key = "0a07002b-0a078039-06-1e8b-0087";
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    PcapGetterHBaseImpl spy = Mockito.spy(pcapGetter);

    List<byte[]> mockPcaps = new ArrayList<byte[]>();
    mockPcaps.add(getTestPcapBytes());
    mockPcaps.add(getTestPcapBytes());

    /*
     * Mockito.doReturn(mockPcaps).when(spy).scanPcaps(Mockito.any(ArrayList.class
     * ), Mockito.any(HTable.class), Mockito.any(Scan.class),
     * Mockito.any(byte[].class), Mockito.any(byte[].class));
     */
    // actual call
    // PcapsResponse response = spy.getPcaps(key);

    // verify
    // Assert.assertNotNull(response);
    // Assert.assertTrue(response.getResponseSize() > mockPcaps.get(0).length);
  }

  /**
   * Gets the test pcap bytes.
   * 
   * @return the test pcap bytes
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private byte[] getTestPcapBytes() throws IOException {
    File fin = new File("src/test/resources/test-tcp-packet.pcap");
    byte[] pcapBytes = FileUtils.readFileToByteArray(fin);
    return pcapBytes;
  }

  /**
   * Test_remove duplicates.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_removeDuplicates() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();

    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0019-caac");
    keys.add("18800006-1800000b-06-0050-5af6");

    List<String> deDupKeys = pcapGetter.removeDuplicateKeys(keys);
    Assert.isTrue(deDupKeys.size() == 3);
    List<String> testKeys = new ArrayList<String>();
    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0019-caac");

    ListUtils.isEqualList(deDupKeys, testKeys);
  }

  /**
   * Test_sort keys by asc order_with out reverse traffic.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_sortKeysByAscOrder_withOutReverseTraffic()
      throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-06-0019-caac");

    List<String> result = pcapGetter.sortKeysByAscOrder(keys, false);

    List<String> testKeys = new ArrayList<String>();
    testKeys.add("18800006-1800000b-06-0019-caac");
    testKeys.add("18800006-1800000b-06-0050-5af6");
    testKeys.add("18800006-1800000b-11-0035-3810");

    Assert.isTrue(ListUtils.isEqualList(result, testKeys));
  }

  /**
   * Test_sort keys by asc order_with reverse traffic.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_sortKeysByAscOrder_withReverseTraffic() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();
    keys.add("18800006-1800000b-11-0035-3812");
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-11-0035-3811");

    List<String> result = pcapGetter.sortKeysByAscOrder(keys, true);
    Assert.isTrue(result.size() == 6);
  }

  /**
   * Test_sort keys by asc order_get unprocessed sublist of keys.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_sortKeysByAscOrder_getUnprocessedSublistOfKeys()
      throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-06-0019-caac");
    System.out.println("original keys =" + keys.toString());

    List<String> sortedKeys = pcapGetter.sortKeysByAscOrder(keys, false);
    System.out.println("after sortKeysByAscOrder =" + sortedKeys.toString());

    List<String> unprocessedKeys1 = pcapGetter.getUnprocessedSublistOfKeys(
        sortedKeys, "18800006-1800000b-06-0019-caac-65140-40815");
    System.out.println("unprocessedKeys1 =" + unprocessedKeys1);
    Assert.isTrue(unprocessedKeys1.size() == 2);

    List<String> unprocessedKeys2 = pcapGetter.getUnprocessedSublistOfKeys(
        sortedKeys, "18800006-1800000b-06-0050-5af6-65140-40815");
    // System.out.println("unprocessedKeys2 ="+unprocessedKeys2);
    Assert.isTrue(unprocessedKeys2.size() == 1);

    List<String> unprocessedKeys3 = pcapGetter.getUnprocessedSublistOfKeys(
        sortedKeys, "18800006-1800000b-11-0035-3810-6514040815");
    // System.out.println("unprocessedKeys3 ="+unprocessedKeys3);
    Assert.isTrue(unprocessedKeys3.size() == 0);

  }

  /**
   * Test_sort keys by asc order_get unprocessed sublist of keys_with out match.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_sortKeysByAscOrder_getUnprocessedSublistOfKeys_withOutMatch()
      throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-06-0019-caac");
    System.out.println("original keys =" + keys.toString());

    List<String> sortedKeys = pcapGetter.sortKeysByAscOrder(keys, false);
    System.out.println("after sortKeysByAscOrder =" + sortedKeys.toString());

    List<String> unprocessedKeys1 = pcapGetter.getUnprocessedSublistOfKeys(
        sortedKeys, "18800006-1800000b-11-89-455-65140-40815");
    System.out.println("unprocessedKeys1 =" + unprocessedKeys1);
    Assert.isTrue(unprocessedKeys1.size() == 3);
  }

  /**
   * Test_create start and stop row keys.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createStartAndStopRowKeys() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String key = "18800006-1800000b-11-0035-3810";
    Map<String, String> map = pcapGetter.createStartAndStopRowKeys(key, false,
        false);
    System.out.println("map =" + map.toString());

    String lastRowKey = "18800006-1800000b-11-0035-3810-23234-32423";
    Map<String, String> map1 = pcapGetter.createStartAndStopRowKeys(
        lastRowKey, true, false);
    System.out.println("map1 =" + map1.toString());

    String lastRowKey2 = "18800006-1800000b-11-0035-3810-23234-32423";
    Map<String, String> map2 = pcapGetter.createStartAndStopRowKeys(
        lastRowKey2, true, true);
    System.out.println("map2 =" + map2.toString());

  }

  /**
   * Test_check if valid input_valid.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_checkIfValidInput_valid() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    List<String> keys = new ArrayList<String>();
    keys.add("18800006-1800000b-11-0035-3810");
    keys.add("18800006-1800000b-06-0050-5af6");
    keys.add("18800006-1800000b-06-0019-caac");

    String lastRowKey = "18800006-1800000b-11-0035-3810-23234-32423";

    boolean response = pcapGetter.checkIfValidInput(keys, lastRowKey);
    Assert.isTrue(response);

  }

  /**
   * Test_check if valid input_in valid.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_checkIfValidInput_inValid() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    @SuppressWarnings("unchecked")
    boolean response = pcapGetter.checkIfValidInput(Collections.EMPTY_LIST,
        null);
    Assert.isTrue(!response);

  }

  /**
   * Test_check if valid input_valid_mixed.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_checkIfValidInput_valid_mixed() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String lastRowKey = "18800006-1800000b-11-0035-3810-23234-32423";
    @SuppressWarnings("unchecked")
    boolean response = pcapGetter.checkIfValidInput(Collections.EMPTY_LIST,
        lastRowKey);
    Assert.isTrue(response);
  }

  /**
   * Test_create get request.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createGetRequest() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String key = "18800006-1800000b-11-0035-3810-23234-324230";

    long startTime = 139812323L; // in seconds
    long endTime = 139923424L; // in seconds

    Get get = pcapGetter.createGetRequest(key, startTime, endTime);
    Assert.notNull(get);

    Assert.isTrue(Arrays.equals(get.getRow(), key.getBytes()));
    // compare in micros as the data creation time unit is set to Micros in
    // properties file.
    Assert.isTrue(get.getTimeRange().getMin() == startTime * 1000 );
    Assert.isTrue(get.getTimeRange().getMax() == endTime * 1000 );
  }

  /**
   * Test_create get request_default time range.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createGetRequest_defaultTimeRange() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String key = "18800006-1800000b-11-0035-3810-23234-324230";

    Get get = pcapGetter.createGetRequest(key, -1, -1);
    Assert.notNull(get);

    Assert.isTrue(Arrays.equals(get.getRow(), key.getBytes()));
    Assert.isTrue(get.getTimeRange().getMin() == 0);
  }

  /**
   * Test_create get request_with start time.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createGetRequest_withStartTime() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String key = "18800006-1800000b-11-0035-3810-23234-324230";

    long startTime = 139812323L; // in seconds

    Get get = pcapGetter.createGetRequest(key, startTime, -1);
    Assert.notNull(get);

    Assert.isTrue(Arrays.equals(get.getRow(), key.getBytes()));
    Assert.isTrue(get.getTimeRange().getMin() == startTime * 1000 );
    Assert.isTrue(get.getTimeRange().getMax() == Long.valueOf(Long.MAX_VALUE));
  }

  /**
   * Test_create get request_with end time.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createGetRequest_withEndTime() throws IOException {
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();
    String key = "18800006-1800000b-11-0035-3810-23234-324230";

    long endTime = 139923424L; // in seconds

    Get get = pcapGetter.createGetRequest(key, -1, endTime);
    Assert.notNull(get);

    Assert.isTrue(Arrays.equals(get.getRow(), key.getBytes()));
    Assert.isTrue(get.getTimeRange().getMin() == 0);
    Assert.isTrue(get.getTimeRange().getMax() == endTime * 1000 );
  }

  /**
   * Test_create scan request.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_createScanRequest() throws IOException {
    // mocking
    PcapGetterHBaseImpl pcapGetter = (PcapGetterHBaseImpl) PcapGetterHBaseImpl
        .getInstance();

    PcapsResponse pcapsResponse = new PcapsResponse();

    Map<String, String> keysMap = new HashMap<String, String>();
    String startKey = "0a07002b-0a078039-06-1e8b-0087-00000-00000";
    String endKey = "0a070025-0a07807a-06-aab8-c360-99999-99999";
    keysMap.put("startKey", startKey);
    keysMap.put("endKey", endKey);

    long startTime = 139812323L; // in seconds
    long endTime = 139923424L; // in seconds
    long maxResultSize = 673424;

    // actual call
    Scan scan = pcapGetter.createScanRequest(pcapsResponse, keysMap, startTime,
        endTime, maxResultSize);

    // verify time range
    Assert.isTrue(scan.getTimeRange().getMin() == startTime * 1000 ); // compare
                                                                            // in
                                                                            // millis
    Assert.isTrue(scan.getTimeRange().getMax() == endTime * 1000 ); // compare
                                                                          // in
                                                                          // millis

    // verify start and stop rows
    Assert.isTrue(Arrays.equals(scan.getStartRow(), startKey.getBytes()));
    Assert.isTrue(Arrays.equals(scan.getStopRow(), endKey.getBytes()));

  }

}
