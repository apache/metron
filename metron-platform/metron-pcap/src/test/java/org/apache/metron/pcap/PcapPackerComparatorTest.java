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

package org.apache.metron.pcap;

import org.junit.jupiter.api.Test;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PacketPayload;
import org.krakenapps.pcap.packet.PcapPacket;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PcapPackerComparatorTest {
  private static final String BASE_DATE = "July 26, 2016 8:21:13 AM UTC";
  private static final String DATE_FORMAT = "MMMM dd, yyyy h:mm:ss a z";

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
  private static final ZonedDateTime JULY_26 = ZonedDateTime.parse(BASE_DATE, FORMATTER);

  private static final long JULY_26_SECONDS = JULY_26.toInstant().getEpochSecond();
  private static final long JULY_26_PLUS_ONE_SECOND_SECONDS = JULY_26.plusSeconds(1L).toInstant().getEpochSecond();
  private static final long JULY_26_PLUS_TWENTY_YEARS_SECONDS = JULY_26.plusYears(20L).toInstant().getEpochSecond();

  private static final PacketPayload EMPTY_PAYLOAD = new PacketPayload(new byte[0]);

  private static final PcapPacketComparator comp = new PcapPacketComparator();

  @Test
  public void testEqual() {
    PacketHeader ph = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket packet = new PcapPacket(ph, EMPTY_PAYLOAD);

    PacketHeader ph2 = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket packet2 = new PcapPacket(ph2, EMPTY_PAYLOAD);

    assertEquals(comp.compare(packet, packet2), 0, "Timestamps should be equal");
    assertEquals(comp.compare(packet2, packet), 0, "Timestamps should be equal");
  }

  @Test
  public void testDifferingSeconds() {
    PacketHeader ph = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket earlier = new PcapPacket(ph, EMPTY_PAYLOAD);

    PacketHeader ph2 = new PacketHeader((int) JULY_26_PLUS_ONE_SECOND_SECONDS, 0, 0, 0);
    PcapPacket later = new PcapPacket(ph2, EMPTY_PAYLOAD);

    PcapPacketComparator comp = new PcapPacketComparator();
    assertTrue(comp.compare(earlier, later) < 0, "Earlier should be less than later");
    assertTrue(comp.compare(later, earlier) > 0, "Later should be greater than earlier");
  }

  @Test
  public void testDifferingMicroseconds() {
    PacketHeader ph = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket earlier = new PcapPacket(ph, EMPTY_PAYLOAD);

    PacketHeader ph2 = new PacketHeader((int) JULY_26_SECONDS, 1, 0, 0);
    PcapPacket later = new PcapPacket(ph2, EMPTY_PAYLOAD);

    PcapPacketComparator comp = new PcapPacketComparator();
    assertTrue(comp.compare(earlier, later) < 0, "Earlier should be less than later");
    assertTrue(comp.compare(later, earlier) > 0, "Later should be greater than earlier");
  }

  @Test
  public void testBothSmallDifferences() {
    PacketHeader ph = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket earlier = new PcapPacket(ph, EMPTY_PAYLOAD);

    PacketHeader ph2 = new PacketHeader((int) JULY_26_PLUS_ONE_SECOND_SECONDS, 1, 0, 0);
    PcapPacket later = new PcapPacket(ph2, EMPTY_PAYLOAD);

    PcapPacketComparator comp = new PcapPacketComparator();
    assertTrue(comp.compare(earlier, later) < 0, "Earlier should be less than later");
    assertTrue(comp.compare(later, earlier) > 0, "Later should be greater than earlier");
  }

  @Test
  public void testLargeDifference() {
    PacketHeader ph = new PacketHeader((int) JULY_26_SECONDS, 0, 0, 0);
    PcapPacket earlier = new PcapPacket(ph, EMPTY_PAYLOAD);

    PacketHeader ph2 = new PacketHeader((int) JULY_26_PLUS_TWENTY_YEARS_SECONDS, 999999, 0, 0);
    PcapPacket later = new PcapPacket(ph2, EMPTY_PAYLOAD);

    PcapPacketComparator comp = new PcapPacketComparator();
    assertTrue(comp.compare(earlier, later) < 0, "Earlier should be less than later");
    assertTrue(comp.compare(later, earlier) > 0, "Later should be greater than earlier");
  }
}
