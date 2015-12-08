package com.cisco.opensoc.hbase.client;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Single point of entry for all REST calls. Exposes methods to fetch pcaps for
 * the given list of keys or range of keys and optional start time and end time.
 * If the caller doesn't provide start time and end time, all pcaps from
 * beginning of the time to until now are returned.
 * 
 * @author Sayi
 * 
 */
public interface IPcapReceiver {

  /**
   * Gets the pcaps for the given list of keys and optional startTime and
   * endTime.
   * 
   * @param keys
   *          the list of keys for which pcaps are to be retrieved
   * @param lastRowKey
   *          last row key from the previous partial response
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps.
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps. The default value is set to Long.MAX_VALUE. 'endTime' must
   *          be greater than the 'startTime'.
   * @param includeReverseTraffic
   *          indicates whether or not to include pcaps from the reverse traffic
   * @param includeDuplicateLastRow
   *          indicates whether or not to include the last row from the previous
   *          partial response
   * @param maxResponseSize
   *          indicates the maximum response size in MegaBytes. User needs to
   *          pass positive value and must be less than 60 (MB)
   * @return byte array with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public ResponseEntity<byte[]> getPcapsByKeys(@RequestParam List<String> keys,
      @RequestParam String lastRowKey, @RequestParam long startTime,
      @RequestParam long endTime, @RequestParam boolean includeReverseTraffic,
      @RequestParam boolean includeDuplicateLastRow,
      @RequestParam String maxResponseSize) throws IOException;

  /**
   * get pcaps for a given key range.
   * 
   * @param startKey
   *          the start key of a key range for which pcaps are to be retrieved
   * @param endKey
   *          the end key of a key range for which pcaps are to be retrieved
   * @param maxResponseSize
   *          indicates the maximum response size in MegaBytes. User needs to
   *          pass positive value and must be less than 60 (MB)
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps.
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps. 'endTime' must be greater than the 'startTime'.
   * @return byte array with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public ResponseEntity<byte[]> getPcapsByKeyRange(
      @RequestParam String startKey, @RequestParam String endKey,
      @RequestParam String maxResponseSize, @RequestParam long startTime,
      @RequestParam long endTime) throws IOException;

  /**
   * get pcaps for the given identifiers.
   * 
   * @param srcIp
   *          source ip address
   * @param destIp
   *          destination ip address
   * @param protocol
   *          network protocol
   * @param srcPort
   *          source port
   * @param destPort
   *          destination port
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps.
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps. 'endTime' must be greater than the 'startTime'.
   * @param includeReverseTraffic
   *          indicates whether or not to include pcaps from the reverse traffic
   * @return byte array with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public ResponseEntity<byte[]> getPcapsByIdentifiers(
      @RequestParam String srcIp, @RequestParam String destIp,
      @RequestParam String protocol, @RequestParam String srcPort,
      @RequestParam String destPort, @RequestParam long startTime,
      @RequestParam long endTime, @RequestParam boolean includeReverseTraffic)
      throws IOException;
}
