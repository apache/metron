package org.apache.metron.parsing.parsers;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.metron.parser.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
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

public class PcapParser implements MessageParser<JSONObject>, Serializable {

  private static final Logger LOG = Logger.getLogger(PcapParser.class);

  private EthernetDecoder ethernetDecoder;
  private long timePrecisionDivisor = 1L;

  public PcapParser withTsPrecision(String tsPrecision) {
    if (tsPrecision.equalsIgnoreCase("MILLI")) {
      //Convert nanos to millis
      LOG.info("Configured for MILLI, setting timePrecisionDivisor to 1000000L" );
      timePrecisionDivisor = 1000000L;
    } else if (tsPrecision.equalsIgnoreCase("MICRO")) {
      //Convert nanos to micro
      LOG.info("Configured for MICRO, setting timePrecisionDivisor to 1000L" );
      timePrecisionDivisor = 1000L;
    } else if (tsPrecision.equalsIgnoreCase("NANO")) {
      //Keep nano as is.
      LOG.info("Configured for NANO, setting timePrecisionDivisor to 1L" );
      timePrecisionDivisor = 1L;
    } else {
      LOG.info("bolt.parser.ts.precision not set. Default to NANO");
      timePrecisionDivisor = 1L;
    }
    return this;
  }

  @Override
  public void init() {
    ethernetDecoder = new MetronEthernetDecoder();
    IpDecoder ipDecoder = new IpDecoder();
    ethernetDecoder.register(EthernetType.IPV4, ipDecoder);
  }

  @Override
  public List<JSONObject> parse(byte[] pcap) {
    List<JSONObject> messages = new ArrayList<>();
    List<PacketInfo> packetInfoList = new ArrayList<>();
    try {
      packetInfoList = getPacketInfo(pcap);
    } catch (IOException e) {
      e.printStackTrace();
    }
    for (PacketInfo packetInfo : packetInfoList) {
      JSONObject message = (JSONObject) JSONValue.parse(packetInfo.getJsonIndexDoc());
      messages.add(message);
    }
    return messages;
  }

  @Override
  public boolean validate(JSONObject message) {
    return true;
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
  public List<PacketInfo> getPacketInfo(byte[] pcap) throws IOException {
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
        ethernetDecoder.decode(packet);

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
    PcapParser pcapParser = new PcapParser();
    File fin = new File("/Users/sheetal/Downloads/bad_packets/bad_packet_1405988125427.pcap");
    File fout = new File(fin.getAbsolutePath() + ".parsed");
    byte[] pcapBytes = FileUtils.readFileToByteArray(fin);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < totalIterations; i++) {
      List<PacketInfo> list = pcapParser.getPacketInfo(pcapBytes);

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