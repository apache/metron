package org.apache.metron.pcap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PcapFilenameHelperTest {

  @Test
  public void extracts_info_from_filename() {
    String pcapFilename = "pcap_pcap128_1494962815457986000_18_pcap-63-1495027314";
    assertThat(PcapFilenameHelper.getKafkaTopic(pcapFilename), equalTo("pcap128"));
    assertThat(
        Long.compareUnsigned(PcapFilenameHelper.getTimestamp(pcapFilename), 1494962815457986000L),
        equalTo(0));
    assertThat(PcapFilenameHelper.getKafkaPartition(pcapFilename), equalTo(18));
    assertThat(PcapFilenameHelper.getUUID(pcapFilename), equalTo("pcap-63-1495027314"));
  }

  @Test
  public void extracts_null_info_from_bad_filename_parts() {
    String pcapFilename = "pcap_pcap128_AAA4962815457986000_BB_pcap-63-1495027314";
    assertThat(PcapFilenameHelper.getTimestamp(pcapFilename), equalTo(null));
    assertThat(PcapFilenameHelper.getKafkaPartition(pcapFilename), equalTo(null));
  }
}
