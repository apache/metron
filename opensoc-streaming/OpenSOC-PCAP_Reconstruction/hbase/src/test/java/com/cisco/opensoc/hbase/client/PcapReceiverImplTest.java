/**
 * 
 */
package com.cisco.opensoc.hbase.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

import com.cisco.opensoc.hbase.client.PcapGetterHBaseImpl;
import com.cisco.opensoc.hbase.client.PcapReceiverImpl;
import com.cisco.opensoc.hbase.client.PcapsResponse;

// TODO: Auto-generated Javadoc
/**
 * The Class PcapReceiverImplTest.
 * 
 * @author Sayi
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PcapGetterHBaseImpl.class)
public class PcapReceiverImplTest {

  /** The pcap receiver. */
  PcapReceiverImpl pcapReceiver = new PcapReceiverImpl();

  /** The exception. */
  @Rule
  public ExpectedException exception = ExpectedException.none();

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
   * Test parse keys_single.
   */
  @Test
  public void testParseKeys_single() {
    String[] keysArr = { "234234234,565675675" };
    List<String> keysList = Arrays.asList(keysArr);
    List<String> parsedKeys = pcapReceiver.parseKeys(keysList);
    Assert.isTrue(parsedKeys.size() == 2);
  }

  /**
   * Test parse keys_multiple.
   */
  @Test
  public void testParseKeys_multiple() {
    String[] keysArr = { "234234234,565675675", "675757,234242" };
    List<String> keysList = Arrays.asList(keysArr);
    List<String> parsedKeys = pcapReceiver.parseKeys(keysList);
    Assert.isTrue(parsedKeys.size() == 4);
  }

  /**
   * Test parse keys_empty.
   */
  @Test
  public void testParseKeys_empty() {
    exception.expect(IllegalArgumentException.class);
    pcapReceiver.parseKeys(Collections.<String> emptyList());
  }

  /**
   * Test parse keys_null.
   */
  @Test
  public void testParseKeys_null() {
    exception.expect(IllegalArgumentException.class);
    pcapReceiver.parseKeys(null);
  }

  /**
   * Test_get pcaps by keys_complete response.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_getPcapsByKeys_completeResponse() throws IOException {
    // mocking
    String[] keysArry = { "0a07002b-0a078039-06-1e8b-0087",
        "0a070025-0a07807a-06-aab8-c360" };
    List<String> keys = Arrays.asList(keysArry);
    String lastRowKey = null;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;
    PcapsResponse response = new PcapsResponse();
    response.setStatus(PcapsResponse.Status.COMPLETE);
    List<byte[]> pcaps = new ArrayList<byte[]>();
    byte[] pcap = { 12, 123 };
    pcaps.add(pcap);
    response.setPcaps(pcaps);

    PcapGetterHBaseImpl pcapGetter = Mockito.mock(PcapGetterHBaseImpl.class);

    PowerMockito.mockStatic(PcapGetterHBaseImpl.class);
    PowerMockito.when(PcapGetterHBaseImpl.getInstance()).thenReturn(pcapGetter);
    PowerMockito.when(
        pcapGetter.getPcaps(keys, lastRowKey, startTime, endTime,
            false, false, 6291456)).thenReturn(response);

    PcapReceiverImpl restImpl = new PcapReceiverImpl();

    // actual call
    ResponseEntity<byte[]> result = restImpl.getPcapsByKeys(keys,
        lastRowKey, startTime, endTime, false, false, null);

    // verify
    Assert.notNull(result);
    Assert.notNull(result.getBody());
    Assert.isTrue(result.getStatusCode() == HttpStatus.OK);
    Assert.isTrue(result.getHeaders().size() == 1); // 'Content-Disposition'
  }

  /**
   * Test_get pcaps by keys_partial response.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_getPcapsByKeys_partialResponse() throws IOException {
    // mocking
    String[] keysArry = { "0a07002b-0a078039-06-1e8b-0087",
        "0a070025-0a07807a-06-aab8-c360" };
    List<String> keys = Arrays.asList(keysArry);
    String lastRowKey = null;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;
    PcapsResponse response = new PcapsResponse();
    response.setStatus(PcapsResponse.Status.PARTIAL);
    List<byte[]> pcaps = new ArrayList<byte[]>();
    byte[] pcap = { 12, 123 };
    pcaps.add(pcap);
    response.setPcaps(pcaps);

    PcapGetterHBaseImpl pcapGetter = Mockito.mock(PcapGetterHBaseImpl.class);

    PowerMockito.mockStatic(PcapGetterHBaseImpl.class);
    PowerMockito.when(PcapGetterHBaseImpl.getInstance()).thenReturn(pcapGetter);
    PowerMockito.when(
        pcapGetter.getPcaps(keys, lastRowKey, startTime, endTime,
            false, false, 6291456)).thenReturn(response);

    PcapReceiverImpl restImpl = new PcapReceiverImpl();

    // actual call
    ResponseEntity<byte[]> result = restImpl.getPcapsByKeys(keys,
        lastRowKey, startTime, endTime, false, false, null);

    // verify
    Assert.notNull(result);
    Assert.notNull(result.getBody());
    Assert.isTrue(result.getStatusCode() == HttpStatus.PARTIAL_CONTENT);
    Assert.isTrue(result.getHeaders().size() == 2); // 'lastRowKey',
                                                    // 'Content-Disposition'
  }

  /**
   * Test_get pcaps by keys_partial no content.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_getPcapsByKeys_partialNoContent() throws IOException {
    // mocking
    String[] keysArry = { "0a07002b-0a078039-06-1e8b-0087",
        "0a070025-0a07807a-06-aab8-c360" };
    List<String> keys = Arrays.asList(keysArry);
    String lastRowKey = null;
    long startTime = 1376782349234555L;
    long endTime = 1396782349234555L;
    PcapsResponse response = new PcapsResponse();

    PcapGetterHBaseImpl pcapGetter = Mockito.mock(PcapGetterHBaseImpl.class);

    PowerMockito.mockStatic(PcapGetterHBaseImpl.class);
    PowerMockito.when(PcapGetterHBaseImpl.getInstance()).thenReturn(pcapGetter);
    PowerMockito.when(
        pcapGetter.getPcaps(keys, lastRowKey, startTime, endTime,
            false, false, 6291456)).thenReturn(response);

    PcapReceiverImpl restImpl = new PcapReceiverImpl();

    // actual call
    ResponseEntity<byte[]> result = restImpl.getPcapsByKeys(keys,
        lastRowKey, startTime, endTime, false, false, null);

    // verify
    Assert.notNull(result);
    Assert.isNull(result.getBody());
    Assert.isTrue(result.getStatusCode() == HttpStatus.NO_CONTENT);
    Assert.isTrue(result.getHeaders().isEmpty());
  }

}
