package org.apache.metron.pcapservice;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * The Class RestTestingUtil.
 */
public class RestTestingUtil {
  
  /** The host name. */
  public static String hostName = null;

  /**
   * Gets the pcaps by keys.
   * 
   * @param keys
   *          the keys
   * @return the pcaps by keys
   */
  @SuppressWarnings("unchecked")
  private static void getPcapsByKeys(String keys) {
    System.out
        .println("**********************getPcapsByKeys ******************************************************************************************");
    // 1.
    String url = "http://" + hostName
        + "/cisco-rest/pcapGetter/getPcapsByKeys?keys={keys}"
        + "&includeReverseTraffic={includeReverseTraffic}"
        + "&startTime={startTime}" + "&endTime={endTime}"
        + "&maxResponseSize={maxResponseSize}";
    // default values
    String startTime = "-1";
    String endTime = "-1";
    String maxResponseSize = "6";
    String includeReverseTraffic = "false";

    @SuppressWarnings("rawtypes")
    Map map = new HashMap();
    map.put("keys", keys);
    map.put("includeReverseTraffic", includeReverseTraffic);
    map.put("startTime", startTime);
    map.put("endTime", endTime);
    map.put("maxResponseSize", maxResponseSize);

    RestTemplate template = new RestTemplate();

    // set headers and entity to send
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE);
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);

    // 1.
    ResponseEntity<byte[]> response1 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeys : request= <keys=%s; includeReverseTraffic=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            keys, includeReverseTraffic, startTime, endTime, maxResponseSize,
            response1);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

    // 2. with reverse traffic
    includeReverseTraffic = "true";
    map.put("includeReverseTraffic", includeReverseTraffic);
    ResponseEntity<byte[]> response2 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeys : request= <keys=%s; includeReverseTraffic=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            keys, includeReverseTraffic, startTime, endTime, maxResponseSize,
            response2);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

    // 3.with time range
    startTime = System.getProperty("startTime", "-1");
    endTime = System.getProperty("endTime", "-1");
    map.put("startTime", startTime);
    map.put("endTime", endTime);
    ResponseEntity<byte[]> response3 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeys : request= <keys=%s; includeReverseTraffic=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            keys, includeReverseTraffic, startTime, endTime, maxResponseSize,
            response3);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

    // 4.with maxResponseSize
    maxResponseSize = System.getProperty("maxResponseSize", "6");
    map.put("maxResponseSize", maxResponseSize);
    ResponseEntity<byte[]> response4 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeys : request= <keys=%s; includeReverseTraffic=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            keys, includeReverseTraffic, startTime, endTime, maxResponseSize,
            response4);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

  }

  /**
   * Gets the pcaps by keys range.
   * 
   * @param startKey
   *          the start key
   * @param endKey
   *          the end key
   * @return the pcaps by keys range
   */
  @SuppressWarnings("unchecked")
  private static void getPcapsByKeysRange(String startKey, String endKey) {
    System.out
        .println("**********************getPcapsByKeysRange ******************************************************************************************");
    // 1.
    String url = "http://" + hostName
        + "/cisco-rest/pcapGetter/getPcapsByKeyRange?startKey={startKey}"
        + "&endKey={endKey}" + "&startTime={startTime}" + "&endTime={endTime}"
        + "&maxResponseSize={maxResponseSize}";
    // default values
    String startTime = "-1";
    String endTime = "-1";
    String maxResponseSize = "6";
    @SuppressWarnings("rawtypes")
    Map map = new HashMap();
    map.put("startKey", startKey);
    map.put("endKey", "endKey");
    map.put("startTime", startTime);
    map.put("endTime", endTime);
    map.put("maxResponseSize", maxResponseSize);

    RestTemplate template = new RestTemplate();

    // set headers and entity to send
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE);
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);

    // 1.
    ResponseEntity<byte[]> response1 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeysRange : request= <startKey=%s; endKey=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            startKey, endKey, startTime, endTime, maxResponseSize, response1);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

    // 2. with time range
    startTime = System.getProperty("startTime", "-1");
    endTime = System.getProperty("endTime", "-1");
    map.put("startTime", startTime);
    map.put("endTime", endTime);
    ResponseEntity<byte[]> response2 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeysRange : request= <startKey=%s; endKey=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            startKey, endKey, startTime, endTime, maxResponseSize, response2);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

    // 3. with maxResponseSize
    maxResponseSize = System.getProperty("maxResponseSize", "6");
    map.put("maxResponseSize", maxResponseSize);
    ResponseEntity<byte[]> response3 = template.exchange(url, HttpMethod.GET,
        requestEntity, byte[].class, map);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out
        .format(
            "getPcapsByKeysRange : request= <startKey=%s; endKey=%s; startTime=%s; endTime=%s; maxResponseSize=%s> \n response= %s \n",
            startKey, endKey, startTime, endTime, maxResponseSize, response3);
    System.out
        .println("----------------------------------------------------------------------------------------------------");
    System.out.println();

  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {

    /*
     * Run this program with system properties
     * 
     * -DhostName=mon.hw.com:8090
     * -Dkeys=18800006-1800000b-06-0019-b39d,18800006-
     * 1800000b-06-0050-5af6-64840-40785
     * -DstartKey=18000002-18800002-06-0436-0019-2440-34545
     * -DendKey=18000002-18800002-06-b773-0019-2840-34585
     */

    hostName = System.getProperty("hostName");

    String keys = System.getProperty("keys");

    String statyKey = System.getProperty("startKey");
    String endKey = System.getProperty("endKey");

    getPcapsByKeys(keys);
    getPcapsByKeysRange(statyKey, endKey);

  }
}
