package org.apache.metron.parsing.parsers;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.krakenapps.pcap.decoder.ethernet.EthernetDecoder;
import org.krakenapps.pcap.decoder.ethernet.EthernetType;
import org.krakenapps.pcap.decoder.ip.IpDecoder;
import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.tcp.TcpPacket;
import org.krakenapps.pcap.decoder.udp.UdpPacket;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;

import org.apache.metron.pcap.Constants;
import org.apache.metron.pcap.MetronEthernetDecoder;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapByteInputStream;

/**
 * The Class PcapParser.
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public final class PcapParser {

  /** The Constant LOG. */
  private static final Logger LOG = Logger.getLogger(PcapParser.class);

  /** The ETHERNET_DECODER. */
  private static final EthernetDecoder ETHERNET_DECODER = new MetronEthernetDecoder();

  /** The ip decoder. */
  private static final IpDecoder IP_DECODER = new IpDecoder();

  // /** The tcp decoder. */
  // private static final TcpDecoder TCP_DECODER = new TcpDecoder(new
  // TcpPortProtocolMapper());
  //
  // /** The udp decoder. */
  // private static final UdpDecoder UDP_DECODER = new UdpDecoder(new
  // UdpPortProtocolMapper());

  static {
    // IP_DECODER.register(InternetProtocol.TCP, TCP_DECODER);
    // IP_DECODER.register(InternetProtocol.UDP, UDP_DECODER);
    ETHERNET_DECODER.register(EthernetType.IPV4, IP_DECODER);
  }

  /**
   * Instantiates a new pcap parser.
   */
  private PcapParser() { // $codepro.audit.disable emptyMethod

  }

  /**
   * Parses the.
   * 
   * @param pcap
   *          the pcap
   * @return the list * @throws IOException Signals that an I/O exception has
   *         occurred. * @throws IOException * @throws IOException * @throws
   *         IOException
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static List<PacketInfo> parse(byte[] pcap) throws IOException {
    List<PacketInfo> packetInfoList = new ArrayList<PacketInfo>();

    PcapByteInputStream pcapByteInputStream = new PcapByteInputStream(pcap);

    GlobalHeader globalHeader = pcapByteInputStream.getGlobalHeader();
    while (true) {
      try

      {
        PcapPacket packet = pcapByteInputStream.getPacket();
        // int packetCounter = 0;
        // PacketHeader packetHeader = null;
        // Ipv4Packet ipv4Packet = null;
        TcpPacket tcpPacket = null;
        UdpPacket udpPacket = null;
        // Buffer packetDataBuffer = null;
        int sourcePort = 0;
        int destinationPort = 0;

        // LOG.trace("Got packet # " + ++packetCounter);

        // LOG.trace(packet.getPacketData());
        ETHERNET_DECODER.decode(packet);

        PacketHeader packetHeader = packet.getPacketHeader();
        Ipv4Packet ipv4Packet = Ipv4Packet.parse(packet.getPacketData());

        if (ipv4Packet.getProtocol() == Constants.PROTOCOL_TCP) {
          tcpPacket = TcpPacket.parse(ipv4Packet);

        }

        if (ipv4Packet.getProtocol() == Constants.PROTOCOL_UDP) {

          Buffer packetDataBuffer = ipv4Packet.getData();
          sourcePort = packetDataBuffer.getUnsignedShort();
          destinationPort = packetDataBuffer.getUnsignedShort();

          udpPacket = new UdpPacket(ipv4Packet, sourcePort, destinationPort);

          udpPacket.setLength(packetDataBuffer.getUnsignedShort());
          udpPacket.setChecksum(packetDataBuffer.getUnsignedShort());
          packetDataBuffer.discardReadBytes();
          udpPacket.setData(packetDataBuffer);
        }

        packetInfoList.add(new PacketInfo(globalHeader, packetHeader, packet,
            ipv4Packet, tcpPacket, udpPacket));
      } catch (NegativeArraySizeException ignored) {
        LOG.debug("Ignorable exception while parsing packet.", ignored);
      } catch (EOFException eof) { // $codepro.audit.disable logExceptions
        // Ignore exception and break
        break;
      }
    }
    return packetInfoList;
  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @throws InterruptedException
   *           the interrupted exception
   */
  public static void main(String[] args) throws IOException,
      InterruptedException {

    double totalIterations = 1000000;
    double parallelism = 64;
    double targetEvents = 1000000;

    File fin = new File("/Users/sheetal/Downloads/bad_packets/bad_packet_1405988125427.pcap");
    File fout = new File(fin.getAbsolutePath() + ".parsed");
    byte[] pcapBytes = FileUtils.readFileToByteArray(fin);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < totalIterations; i++) {
      List<PacketInfo> list = parse(pcapBytes);

      for (PacketInfo packetInfo : list) {
        System.out.println(packetInfo.getJsonIndexDoc());
      }
    }
    long endTime = System.currentTimeMillis();

    System.out.println("Time taken to process " + totalIterations + " events :"
        + (endTime - startTime) + " milliseconds");

    System.out
        .println("With parallelism of "
            + parallelism
            + " estimated time to process "
            + targetEvents
            + " events: "
            + (((((endTime - startTime) / totalIterations) * targetEvents) / parallelism) / 1000)
            + " seconds");
    System.out.println("With parallelism of " + parallelism
        + " estimated # of events per second: "
        + ((parallelism * 1000 * totalIterations) / (endTime - startTime))
        + " events");
    System.out.println("Expected Parallelism to process " + targetEvents
        + " events in a second: "
        + (targetEvents / ((1000 * totalIterations) / (endTime - startTime))));
  }

}