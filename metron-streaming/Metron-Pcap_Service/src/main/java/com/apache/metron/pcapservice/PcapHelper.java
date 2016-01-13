package com.opensoc.pcapservice;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mortbay.log.Log;
import org.springframework.util.Assert;

import com.google.common.annotations.VisibleForTesting;

/**
 * utility class which holds methods related to time conversions, building
 * reverse keys.
 */
public class PcapHelper {

  /** The Constant LOGGER. */
  private static final Logger LOGGER = Logger.getLogger(PcapHelper.class);

  /** The cell timestamp comparator. */
  private static CellTimestampComparator CELL_TIMESTAMP_COMPARATOR = new CellTimestampComparator();

  /**
   * The Enum TimeUnit.
   */
  public enum TimeUnit {

    /** The seconds. */
    SECONDS,
    /** The millis. */
    MILLIS,
    /** The micros. */
    MICROS,
    /** The unknown. */
    UNKNOWN
  };

  /**
   * Converts the given time to the 'hbase' data creation time unit.
   * 
   * @param inputTime
   *          the input time
   * @return the long
   */
  public static long convertToDataCreationTimeUnit(long inputTime) {
    if (inputTime <= 9999999999L) {
      return convertSecondsToDataCreationTimeUnit(inputTime); // input time unit
                                                              // is in seconds
    } else if (inputTime <= 9999999999999L) {
      return convertMillisToDataCreationTimeUnit(inputTime); // input time unit
                                                             // is in millis
    } else if (inputTime <= 9999999999999999L) {
      return convertMicrosToDataCreationTimeUnit(inputTime); // input time unit
                                                             // it in micros
    }
    return inputTime; // input time unit is unknown
  }

  /**
   * Returns the 'hbase' data creation time unit by reading
   * 'hbase.table.data.time.unit' property in 'hbase-config' properties file; If
   * none is mentioned in properties file, returns <code>TimeUnit.UNKNOWN</code>
   * 
   * @return TimeUnit
   */
  @VisibleForTesting
  public static TimeUnit getDataCreationTimeUnit() {
    String timeUnit = ConfigurationUtil.getConfiguration().getString(
        "hbase.table.data.time.unit");
    LOGGER.debug("hbase.table.data.time.unit=" + timeUnit.toString());
    if (StringUtils.isNotEmpty(timeUnit)) {
      return TimeUnit.valueOf(timeUnit);
    }
    return TimeUnit.UNKNOWN;
  }

  /**
   * Convert seconds to data creation time unit.
   * 
   * @param inputTime
   *          the input time
   * @return the long
   */
  @VisibleForTesting
  public static long convertSecondsToDataCreationTimeUnit(long inputTime) {
    System.out.println("convert Seconds To DataCreation TimeUnit");
    TimeUnit dataCreationTimeUnit = getDataCreationTimeUnit();
    if (TimeUnit.SECONDS == dataCreationTimeUnit) {
      return inputTime;
    } else if (TimeUnit.MILLIS == dataCreationTimeUnit) {
      return inputTime * 1000;
    } else if (TimeUnit.MICROS == dataCreationTimeUnit) {
      return inputTime * 1000 * 1000;
    }
    return inputTime;
  }

  /**
   * Builds the reverseKey to fetch the pcaps in the reverse traffic
   * (destination to source).
   * 
   * @param key
   *          indicates hbase rowKey (partial or full) in the format
   *          "srcAddr-dstAddr-protocol-srcPort-dstPort-fragment"
   * @return String indicates the key in the format
   *         "dstAddr-srcAddr-protocol-dstPort-srcPort"
   */
  public static String reverseKey(String key) {
    Assert.hasText(key, "key must not be null or empty");
    String delimeter = HBaseConfigConstants.PCAP_KEY_DELIMETER;
    String regex = "\\" + delimeter;
    StringBuffer sb = new StringBuffer();
    try {
      String[] tokens = key.split(regex);
      Assert
          .isTrue(
              (tokens.length == 5 || tokens.length == 6 || tokens.length == 7),
              "key is not in the format : 'srcAddr-dstAddr-protocol-srcPort-dstPort-{ipId-fragment identifier}'");
      sb.append(tokens[1]).append(delimeter).append(tokens[0])
          .append(delimeter).append(tokens[2]).append(delimeter)
          .append(tokens[4]).append(delimeter).append(tokens[3]);
    } catch (Exception e) {
      Log.warn("Failed to reverse the key. Reverse scan won't be performed.", e);
    }
    return sb.toString();
  }

  /**
   * Builds the reverseKeys to fetch the pcaps in the reverse traffic
   * (destination to source). If all keys in the input are not in the expected
   * format, it returns an empty list;
   * 
   * @param keys
   *          indicates list of hbase rowKeys (partial or full) in the format
   *          "srcAddr-dstAddr-protocol-srcPort-dstPort-fragment"
   * @return List<String> indicates the list of keys in the format
   *         "dstAddr-srcAddr-protocol-dstPort-srcPort"
   */
  public static List<String> reverseKey(List<String> keys) {
    Assert.notEmpty(keys, "'keys' must not be null or empty");
    List<String> reverseKeys = new ArrayList<String>();
    for (String key : keys) {
      if (key != null) {
        String reverseKey = reverseKey(key);
        if (StringUtils.isNotEmpty(reverseKey)) {
          reverseKeys.add(reverseKey);
        }
      }
    }
    return reverseKeys;
  }

  /**
   * Returns Comparator for sorting pcaps cells based on the timestamp (dsc).
   * 
   * @return CellTimestampComparator
   */
  public static CellTimestampComparator getCellTimestampComparator() {
    return CELL_TIMESTAMP_COMPARATOR;
  }

  /**
   * Convert millis to data creation time unit.
   * 
   * @param inputTime
   *          the input time
   * @return the long
   */
  @VisibleForTesting
  private static long convertMillisToDataCreationTimeUnit(long inputTime) {
    System.out.println("convert Millis To DataCreation TimeUnit");
    TimeUnit dataCreationTimeUnit = getDataCreationTimeUnit();
    if (TimeUnit.SECONDS == dataCreationTimeUnit) {
      return (inputTime / 1000);
    } else if (TimeUnit.MILLIS == dataCreationTimeUnit) {
      return inputTime;
    } else if (TimeUnit.MICROS == dataCreationTimeUnit) {
      return inputTime * 1000;
    }
    return inputTime;
  }

  /**
   * Convert micros to data creation time unit.
   * 
   * @param inputTime
   *          the input time
   * @return the long
   */
  @VisibleForTesting
  private static long convertMicrosToDataCreationTimeUnit(long inputTime) {
    System.out.println("convert Micros To DataCreation TimeUnit");
    TimeUnit dataCreationTimeUnit = getDataCreationTimeUnit();
    if (TimeUnit.SECONDS == dataCreationTimeUnit) {
      return inputTime / (1000 * 1000);
    } else if (TimeUnit.MILLIS == dataCreationTimeUnit) {
      return inputTime / 1000;
    } else if (TimeUnit.MICROS == dataCreationTimeUnit) {
      return inputTime;
    }
    return inputTime;
  }
}
