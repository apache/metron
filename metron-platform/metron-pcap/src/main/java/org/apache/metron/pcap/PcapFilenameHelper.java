package org.apache.metron.pcap;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class PcapFilenameHelper {

  public static final int POS_KAFKA_TOPIC = 1;
  public static final int POS_TIMESTAMP = 2;
  public static final int POS_KAFKA_PARTITION = 3;
  public static final int POS_UUID = 4;

  public static String getKafkaTopic(String pcapFilename) {
    return Iterables.get(Splitter.on('_').split(pcapFilename), POS_KAFKA_TOPIC);
  }

  /**
   * Gets unsigned long timestamp from the PCAP filename
   *
   * @return timestamp, or null if unable to parse
   */
  public static Long getTimestamp(String pcapFilename) {
    try {
      return Long
          .parseUnsignedLong(Iterables.get(Splitter.on('_').split(pcapFilename), POS_TIMESTAMP));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Gets Kafka partition number from the PCAP filename
   *
   * @return partition, or null if unable to parse
   */
  public static Integer getKafkaPartition(String pcapFilename) {
    try {
      return Integer
          .parseInt(Iterables.get(Splitter.on('_').split(pcapFilename), POS_KAFKA_PARTITION));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String getUUID(String pcapFilename) {
    return Iterables.get(Splitter.on('_').split(pcapFilename), POS_UUID);
  }

}
