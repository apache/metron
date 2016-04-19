package org.apache.metron.spout.pcap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PcapHelperTest {
  public static List<byte[]> readSamplePackets(String pcapLoc) throws IOException {
    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
            SequenceFile.Reader.file(new Path(pcapLoc))
    );
    List<byte[] > ret = new ArrayList<>();
    IntWritable key = new IntWritable();
    BytesWritable value = new BytesWritable();
    while (reader.next(key, value)) {
      byte[] pcapWithHeader = value.copyBytes();
      ret.add(pcapWithHeader);
    }
    return ret;
  }

  public static byte[] stripHeaders(byte[] pcap) {
    byte[] ret = new byte[pcap.length - PcapHelper.GLOBAL_HEADER_SIZE - PcapHelper.PACKET_HEADER_SIZE];
    int offset = PcapHelper.GLOBAL_HEADER_SIZE + PcapHelper.PACKET_HEADER_SIZE;
    System.arraycopy(pcap, offset, ret, 0, ret.length);
    return ret;
  }

  @Test
  public void testLittleEndianHeaderization() throws Exception {
    String pcapSampleFiles = "../Metron-Testing/src/main/resources/sample/data/SampleInput/PCAPExampleOutput";
    List<byte[]> pcaps = readSamplePackets(pcapSampleFiles);
    for(byte[] pcap : pcaps)
    {
      long ts = PcapHelper.getTimestamp(pcap);
      byte[] stripped = stripHeaders(pcap);
      byte[] reconstitutedPacket = PcapHelper.addGlobalHeader(PcapHelper.addPacketHeader(ts, stripped, Endianness.LITTLE), Endianness.LITTLE);
      if(!Arrays.equals(reconstitutedPacket, pcap)) {
        int eSecs = Bytes.toInt(pcap, 25);
        int rSec = Bytes.toInt(reconstitutedPacket, 25);
        System.out.println(eSecs + " vs " + rSec);
        for(int i = 0;i < reconstitutedPacket.length;++i) {
          System.out.println((i + 1) + ". " + String.format("%02X", pcap[i]) + " = " + String.format("%02X", reconstitutedPacket[i]));
        }
        Assert.assertArrayEquals(reconstitutedPacket, pcap);
      }
    }
  }
}
