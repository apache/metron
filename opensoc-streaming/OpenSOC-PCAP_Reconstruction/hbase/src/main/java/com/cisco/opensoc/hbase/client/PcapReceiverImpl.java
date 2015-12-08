package com.cisco.opensoc.hbase.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cisco.opensoc.pcap.parsing.PcapUtils;
import com.google.common.annotations.VisibleForTesting;

/**
 * Single point of entry for all REST calls. Exposes methods to fetch pcaps for
 * the given list of keys or range of keys and optional start time and end time.
 * If the caller doesn't provide start time and end time, all pcaps from
 * beginning of the time to until now are returned.
 * 
 * @author Sayi
 * 
 */
@Controller
public class PcapReceiverImpl implements IPcapReceiver {

  /** The Constant LOGGER. */
  private static final Logger LOGGER = Logger.getLogger(PcapReceiverImpl.class);

  /** The Constant HEADER_CONTENT_DISPOSITION_NAME. */
  private static final String HEADER_CONTENT_DISPOSITION_NAME = "Content-Disposition";

  /** The Constant HEADER_CONTENT_DISPOSITION_VALUE. */
  private static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment; filename=\"managed-threat.pcap\"";

  /** partial response key header name. */
  private static final String HEADER_PARTIAL_RESPONE_KEY = "lastRowKey";

  /*
   * (non-Javadoc)
   * 
   * @see com.cisco.opensoc.hbase.client.IPcapReceiver#getPcapsByKeys(java.util.List,
   * java.lang.String, long, long, boolean, boolean,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  @RequestMapping(value = "/pcapGetter/getPcapsByKeys", produces = "application/octet-stream")
  public ResponseEntity<byte[]> getPcapsByKeys(
      @RequestParam(required = false) List<String> keys,
      @RequestParam(required = false) String lastRowKey,
      @RequestParam(defaultValue = "-1") long startTime,
      @RequestParam(defaultValue = "-1") long endTime,
      @RequestParam(required = false) boolean includeDuplicateLastRow,
      @RequestParam(defaultValue = "false") boolean includeReverseTraffic,
      @RequestParam(required = false) String maxResponseSize)
      throws IOException {
    Assert.notEmpty(keys, "'keys' must not be null or empty");
    PcapsResponse pcapResponse = null;
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    try {
      IPcapGetter pcapGetter = PcapGetterHBaseImpl.getInstance();
      pcapResponse = pcapGetter.getPcaps(parseKeys(keys), lastRowKey,
          startTime, endTime, includeReverseTraffic, includeDuplicateLastRow,
          ConfigurationUtil.validateMaxResultSize(maxResponseSize));
      LOGGER.info("pcaps response in REST layer =" + pcapResponse.toString());

      // return http status '204 No Content' if the pcaps response size is 0
      if (pcapResponse == null || pcapResponse.getResponseSize() == 0) {
        return new ResponseEntity<byte[]>(HttpStatus.NO_CONTENT);
      }

      // return http status '206 Partial Content', the partial response file and
      // 'lastRowKey' header , if the pcaps response status is 'PARTIAL'
      headers.add(HEADER_CONTENT_DISPOSITION_NAME,
          HEADER_CONTENT_DISPOSITION_VALUE);
      if (pcapResponse.getStatus() == PcapsResponse.Status.PARTIAL) {
        headers.add(HEADER_PARTIAL_RESPONE_KEY,
            pcapResponse.getLastRowKey());
        return new ResponseEntity<byte[]>(pcapResponse.getPcaps(), headers,
            HttpStatus.PARTIAL_CONTENT);
      }

    } catch (IOException e) {
      LOGGER.error("Exception occurred while fetching Pcaps for the keys :"
          + keys.toString(), e);
      throw e;
    }

    // return http status '200 OK' along with the complete pcaps response file,
    // and headers
    return new ResponseEntity<byte[]>(pcapResponse.getPcaps(), headers,
        HttpStatus.OK);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.cisco.opensoc.hbase.client.IPcapReceiver#getPcapsByKeyRange(java.lang.String
   * , java.lang.String, java.lang.String, long, long,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  @RequestMapping(value = "/pcapGetter/getPcapsByKeyRange", produces = "application/octet-stream")
  public ResponseEntity<byte[]> getPcapsByKeyRange(
      @RequestParam String startKey,
      @RequestParam(required = false) String endKey,
      @RequestParam(required = false) String maxResponseSize,
      @RequestParam(defaultValue = "-1") long startTime,
      @RequestParam(defaultValue = "-1") long endTime) throws IOException {
    Assert.hasText(startKey, "'startKey' must not be null or empty");
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    byte[] response = null;
    try {
      IPcapScanner pcapScanner = PcapScannerHBaseImpl.getInstance();
      response = pcapScanner.getPcaps(startKey, endKey,
          ConfigurationUtil.validateMaxResultSize(maxResponseSize), startTime,
          endTime);
      if (response == null || response.length == 0) {
        return new ResponseEntity<byte[]>(HttpStatus.NO_CONTENT);
      }
      headers.add(HEADER_CONTENT_DISPOSITION_NAME,
          HEADER_CONTENT_DISPOSITION_VALUE);

    } catch (IOException e) {
      LOGGER.error(
          "Exception occurred while fetching Pcaps for the key range : startKey="
              + startKey + ", endKey=" + endKey, e);
      throw e;
    }
    // return http status '200 OK' along with the complete pcaps response file,
    // and headers
    return new ResponseEntity<byte[]>(response, headers, HttpStatus.OK);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.cisco.opensoc.hbase.client.IPcapReceiver#getPcapsByIdentifiers(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.String,
   * java.lang.String, long, long, boolean,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  @RequestMapping(value = "/pcapGetter/getPcapsByIdentifiers", produces = "application/octet-stream")
  public ResponseEntity<byte[]> getPcapsByIdentifiers(
      @RequestParam String srcIp, @RequestParam String dstIp,
      @RequestParam String protocol, @RequestParam String srcPort,
      @RequestParam String dstPort,
      @RequestParam(defaultValue = "-1") long startTime,
      @RequestParam(defaultValue = "-1") long endTime,
      @RequestParam(defaultValue = "false") boolean includeReverseTraffic)
      throws IOException {
    Assert.hasText(srcIp, "'srcIp' must not be null or empty");
    Assert.hasText(dstIp, "'dstIp' must not be null or empty");
    Assert.hasText(protocol, "'protocol' must not be null or empty");
    Assert.hasText(srcPort, "'srcPort' must not be null or empty");
    Assert.hasText(dstPort, "'dstPort' must not be null or empty");
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    PcapsResponse response = null;
    try {
      String sessionKey = PcapUtils.getSessionKey(srcIp, dstIp, protocol,
          srcPort, dstPort);
      LOGGER.info("sessionKey =" + sessionKey);
      IPcapGetter pcapGetter = PcapGetterHBaseImpl.getInstance();
      response = pcapGetter.getPcaps(Arrays.asList(sessionKey), null,
          startTime, endTime, includeReverseTraffic, false,
          ConfigurationUtil.getDefaultResultSize());
      if (response == null || response.getResponseSize() == 0) {
        return new ResponseEntity<byte[]>(HttpStatus.NO_CONTENT);
      }
      headers.add(HEADER_CONTENT_DISPOSITION_NAME,
          HEADER_CONTENT_DISPOSITION_VALUE);

    } catch (IOException e) {
      LOGGER.error("Exception occurred while fetching Pcaps by identifiers :",
          e);
      throw e;
    }
    // return http status '200 OK' along with the complete pcaps response file,
    // and headers
    return new ResponseEntity<byte[]>(response.getPcaps(), headers,
        HttpStatus.OK);
  }

  /**
   * This method parses the each value in the List using delimiter ',' and
   * builds a new List;.
   * 
   * @param keys
   *          list of keys to be parsed
   * @return list of keys
   */
  @VisibleForTesting
  List<String> parseKeys(List<String> keys) {
    Assert.notEmpty(keys);
    List<String> parsedKeys = new ArrayList<String>();
    for (String key : keys) {
      parsedKeys.addAll(Arrays.asList(StringUtils.split(StringUtils.trim(key),
          ",")));
    }
    return parsedKeys;
  }
}
