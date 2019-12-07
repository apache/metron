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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PcapFilenameHelperTest {

  @Test
  public void extracts_info_from_filename() {
    {
      String pcapFilename = "pcap_pcap128_1494962815457986000_18_pcap-63-1495027314";
      assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("pcap128"));
      assertThat(
          Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
          equalTo(0));
      assertThat(PcapFilenameHelper.getKafkaPartition(pcapFilename), equalTo(18));
      assertThat(PcapFilenameHelper.getUUID(pcapFilename), equalTo("pcap-63-1495027314"));
    }
    {
      String pcapFilename = "pcap_pcap-128_1494962815457986000_18_pcap-63-1495027314";
      assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("pcap-128"));
      assertThat(
          Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
          equalTo(0));
    }
    {
      String pcapFilename = "pcap_pcap_128_1494962815457986000_18_pcap-63-1495027314";
      assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("pcap_128"));
      assertThat(
          Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
          equalTo(0));
    }
    {
      String pcapFilename = "pcap_pcap___128___1494962815457986000_18_pcap-63-1495027314";
      assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("pcap___128__"));
      assertThat(
          Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
          equalTo(0));
    }
    {
      String pcapFilename = "pcap___pcap___128___1494962815457986000_18_pcap-63-1495027314";
      assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("__pcap___128__"));
      assertThat(
          Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
          equalTo(0));
    }
  }

  @Test
  public void extracts_null_info_from_bad_filename_parts() {
    String pcapFilename = "pcap_pcap128_AAA4962815457986000_BB_pcap-63-1495027314";
    assertThat(PcapFilenameHelper.getTimestamp(pcapFilename), equalTo(null));
    assertThat(PcapFilenameHelper.getKafkaPartition(pcapFilename), equalTo(null));
  }
}
