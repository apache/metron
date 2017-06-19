/*
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

package org.apache.metron.pcap;

import java.util.Arrays;

/**
 * Expects files in the following format
 * pcap_$TOPIC_$TS_$PARTITION_$UUID
 */
public class PcapFilenameHelper {

  public static final String PREFIX = "pcap_";

  /**
   * Extract kafka topic from pcap filename. Resilient to underscores and hyphens in the kafka
   * topic name when splitting the value.
   */
  public static String getKafkaTopic(String pcapFilename) {
    String[] tokens = stripPrefix(pcapFilename).split("_");
    return String.join("_", Arrays.copyOfRange(tokens, 0, tokens.length - 3));
  }

  private static String stripPrefix(String s) {
    return s.substring(PREFIX.length());
  }

  /**
   * Gets unsigned long timestamp from the PCAP filename
   *
   * @return timestamp, or null if unable to parse
   */
  public static Long getTimestamp(String pcapFilename) {
    String[] tokens = stripPrefix(pcapFilename).split("_");
    try {
      return Long.parseUnsignedLong(tokens[tokens.length - 3]);
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
    String[] tokens = stripPrefix(pcapFilename).split("_");
    try {
      return Integer.parseInt(tokens[tokens.length - 2]);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String getUUID(String pcapFilename) {
    String[] tokens = stripPrefix(pcapFilename).split("_");
    return tokens[tokens.length - 1];
  }

}
