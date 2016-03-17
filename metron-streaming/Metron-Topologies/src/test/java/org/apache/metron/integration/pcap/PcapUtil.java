package org.apache.metron.integration.pcap;

import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapByteInputStream;
import org.apache.metron.pcap.PcapParser;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PcapUtil {
  public Iterable<byte[]> pcapToPackets(File pcapFile) throws IOException {
    List<byte[]> ret = new ArrayList<>();
    PcapParser pcapParser = new PcapParser();
    pcapParser.init();
    byte[] pcapBytes = FileUtils.readFileToByteArray(pcapFile);
    ByteArrayInputStream bis = new ByteArrayInputStream(pcapBytes);
    PcapByteInputStream is = new PcapByteInputStream(bis);
    is.getGlobalHeader();
    int globalHeaderOffset = is.getOffset();
    byte[] globalHeader = new byte[globalHeaderOffset];
    System.arraycopy(pcapBytes, 0, globalHeader, 0, globalHeaderOffset);
    globalHeaderOffset -= 3;
    List<Integer> positions = new ArrayList<>();
    while(true) {
      try {
        PcapPacket packet = is.getPacket();
        packet.getPacketHeader();
        positions.add(is.getOffset());
      }
      catch(EOFException e) {
       break;
      }
    }
    int startPos = globalHeaderOffset;
    for(Integer position : Iterables.limit(positions, 1)) {
      int length = position - startPos ;
      byte[] b = new byte[length + globalHeader.length];
      System.arraycopy(globalHeader, 0, b, 0, globalHeader.length);
      System.arraycopy(pcapBytes, startPos, b, globalHeader.length, length);
      startPos = position;
      ret.add(b);
    }
    return ret;
  }

  public static void main(String...argv) throws IOException {
    PcapUtil util = new PcapUtil();
    PcapParser parser = new PcapParser();
    parser.init();
    File f = new File("/Users/cstella/Documents/workspace/metron/fork/incubator-metron/metron-streaming/Metron-Topologies/src/test/resources/pcap_raw/sample.pcap");
    /*byte[] pcapBytes = FileUtils.readFileToByteArray(f);
    List<PacketInfo> pis = parser.getPacketInfo(pcapBytes);
    for(PacketInfo pi : pis) {
      System.out.println(pi.getJsonDoc());
    }*/
    for(byte[] b : util.pcapToPackets(f))
    {
      List<PacketInfo> pis = parser.getPacketInfo(b);
      for(PacketInfo pi : pis) {
        System.out.println(pi.getJsonDoc());
      }
    }
  }
}
