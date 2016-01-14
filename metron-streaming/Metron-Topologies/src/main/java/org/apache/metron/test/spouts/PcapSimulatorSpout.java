package org.apache.metron.test.spouts;

import java.util.Map;
import java.util.Random;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import org.apache.metron.pcap.PcapUtils;


/**
 * The Class PcapSimulatorSpout.
 */
public class PcapSimulatorSpout extends BaseRichSpout {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -5878104600899840638L;

  /** The collector. */
  private SpoutOutputCollector collector = null;

  /** The Constant randomIpSegmentGenerator. */
  private static final Random randomIpSegmentGenerator = new Random(255);

  /** The Constant randomPortGenerator. */
  private static final Random randomPortGenerator = new Random(64000);

  /** The Constant randomJsonGenerator. */
  private static final Random randomJsonGenerator = new Random(8);

  /** The Constant randomProtocolGenerator. */
  private static final Random randomProtocolGenerator = new Random(255);

  /** The message size. */
  private static int messageSize = 30000;

  /** The pcap. */
  private static byte[] pcap = new byte[messageSize];

  /** The Constant randomPcapGenerator. */
  private static final Random randomPcapGenerator = new Random();

  /** The json doc. */
  private static String jsonDoc;

  /** The ts. */
  private static long ts;

  /** The group key. */
  private static String groupKey;

  /** The ip addr. */
  StringBuffer ipAddr = new StringBuffer();

  /** The Constant jsonDocs. */
  private static final String[] jsonDocs = {
      "{ \"header\": { \"IncLen\": 124,\"OrigLen\": 124,\"TsSec\": 1391740061,\"TsUsec\": 723610},\"ipv4header\": { \"Destination\": -1407317716,\"DestinationAddress\": \"172.30.9.44\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 22550,\"Id\": 30686,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317715,\"SourceAddress\": \"172.30.9.45\",\"Tos\": 0,\"TotalLength\": 110,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": 1331776820,\"Checksum\": 21822,\"DataLength\": 58,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.44\",\"DestinationPort\": 9092,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": 337331842,\"SessionKey\": \"172.30.9.45:56412 -> 172.30.9.44:9092\",\"SourceAddress\": \"172.30.9.45\",\"SourcePort\": 56412,\"TotalLength\": 90,\"UrgentPointer\": 0,\"Window\": 115}}",
      "{ \"header\": { \"IncLen\": 60,\"OrigLen\": 60,\"TsSec\": 1391743533,\"TsUsec\": 523808},\"ipv4header\": { \"Destination\": 202,\"DestinationAddress\": \"0.0.0.202\",\"Flags\": 0,\"FragmentOffset\": 572,\"HeaderChecksum\": 21631,\"Id\": 2,\"Ihl\": 8,\"Protocol\": 0,\"Source\": -285366020,\"SourceAddress\": \"238.253.168.252\",\"Tos\": 66,\"TotalLength\": 768,\"Ttl\": 128,\"Version\": 4}} ",
      "{ \"header\": { \"IncLen\": 64,\"OrigLen\": 64,\"TsSec\": 1391729466,\"TsUsec\": 626286},\"ipv4header\": { \"Destination\": -55296,\"DestinationAddress\": \"255.255.40.0\",\"Flags\": 0,\"FragmentOffset\": 0,\"HeaderChecksum\": 28302,\"Id\": 62546,\"Ihl\": 0,\"Protocol\": 0,\"Source\": 151031295,\"SourceAddress\": \"9.0.141.255\",\"Tos\": 60,\"TotalLength\": 14875,\"Ttl\": 0,\"Version\": 0}}",
      "{ \"header\": { \"IncLen\": 64,\"OrigLen\": 64,\"TsSec\": 1391729470,\"TsUsec\": 404175},\"ipv4header\": { \"Destination\": -55296,\"DestinationAddress\": \"255.255.40.0\",\"Flags\": 0,\"FragmentOffset\": 0,\"HeaderChecksum\": 53034,\"Id\": 62546,\"Ihl\": 0,\"Protocol\": 0,\"Source\": 100699647,\"SourceAddress\": \"6.0.141.255\",\"Tos\": 60,\"TotalLength\": 15899,\"Ttl\": 0,\"Version\": 0}}",
      "{ \"header\": { \"IncLen\": 64,\"OrigLen\": 64,\"TsSec\": 1391729470,\"TsUsec\": 991207},\"ipv4header\": { \"Destination\": -55296,\"DestinationAddress\": \"255.255.40.0\",\"Flags\": 0,\"FragmentOffset\": 0,\"HeaderChecksum\": 59167,\"Id\": 62546,\"Ihl\": 0,\"Protocol\": 0,\"Source\": 251694591,\"SourceAddress\": \"15.0.141.255\",\"Tos\": 60,\"TotalLength\": 15899,\"Ttl\": 0,\"Version\": 0}}",
      "{ \"header\": { \"IncLen\": 78,\"OrigLen\": 78,\"TsSec\": 1391743531,\"TsUsec\": 617746},\"ipv4header\": { \"Destination\": -1407317706,\"DestinationAddress\": \"172.30.9.54\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 12015,\"Id\": 41253,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317711,\"SourceAddress\": \"172.30.9.49\",\"Tos\": 0,\"TotalLength\": 64,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": -854627611,\"Checksum\": 28439,\"DataLength\": 12,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.54\",\"DestinationPort\": 43457,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": -70750910,\"SessionKey\": \"172.30.9.49:9092 -> 172.30.9.54:43457\",\"SourceAddress\": \"172.30.9.49\",\"SourcePort\": 9092,\"TotalLength\": 44,\"UrgentPointer\": 0,\"Window\": 1453}}",
      "{ \"header\": { \"IncLen\": 78,\"OrigLen\": 78,\"TsSec\": 1391743532,\"TsUsec\": 78633},\"ipv4header\": { \"Destination\": -1407317706,\"DestinationAddress\": \"172.30.9.54\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 26235,\"Id\": 27034,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317712,\"SourceAddress\": \"172.30.9.48\",\"Tos\": 0,\"TotalLength\": 64,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": 965354559,\"Checksum\": 6890,\"DataLength\": 12,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.54\",\"DestinationPort\": 37051,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": -1654276327,\"SessionKey\": \"172.30.9.48:9092 -> 172.30.9.54:37051\",\"SourceAddress\": \"172.30.9.48\",\"SourcePort\": 9092,\"TotalLength\": 44,\"UrgentPointer\": 0,\"Window\": 1453}}",
      "{ \"header\": { \"IncLen\": 78,\"OrigLen\": 78,\"TsSec\": 1391743634,\"TsUsec\": 784540},\"ipv4header\": { \"Destination\": -1407317710,\"DestinationAddress\": \"172.30.9.50\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 46490,\"Id\": 6784,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317713,\"SourceAddress\": \"172.30.9.47\",\"Tos\": 0,\"TotalLength\": 64,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": -477288801,\"Checksum\": 60687,\"DataLength\": 12,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.50\",\"DestinationPort\": 53561,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": -1890443606,\"SessionKey\": \"172.30.9.47:9092 -> 172.30.9.50:53561\",\"SourceAddress\": \"172.30.9.47\",\"SourcePort\": 9092,\"TotalLength\": 44,\"UrgentPointer\": 0,\"Window\": 1453}}",
      "{ \"header\": { \"IncLen\": 78,\"OrigLen\": 78,\"TsSec\": 1391743683,\"TsUsec\": 495234},\"ipv4header\": { \"Destination\": -1407317711,\"DestinationAddress\": \"172.30.9.49\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 48322,\"Id\": 4956,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317716,\"SourceAddress\": \"172.30.9.44\",\"Tos\": 0,\"TotalLength\": 64,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": -1825947455,\"Checksum\": 27340,\"DataLength\": 12,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.49\",\"DestinationPort\": 37738,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": -496700614,\"SessionKey\": \"172.30.9.44:9092 -> 172.30.9.49:37738\",\"SourceAddress\": \"172.30.9.44\",\"SourcePort\": 9092,\"TotalLength\": 44,\"UrgentPointer\": 0,\"Window\": 1453}}",
      "{ \"header\": { \"IncLen\": 78,\"OrigLen\": 78,\"TsSec\": 1391743772,\"TsUsec\": 719493},\"ipv4header\": { \"Destination\": -1407317715,\"DestinationAddress\": \"172.30.9.45\",\"Flags\": 2,\"FragmentOffset\": 0,\"HeaderChecksum\": 39105,\"Id\": 14173,\"Ihl\": 20,\"Protocol\": 6,\"Source\": -1407317712,\"SourceAddress\": \"172.30.9.48\",\"Tos\": 0,\"TotalLength\": 64,\"Ttl\": 64,\"Version\": 4},\"tcpheader\": { \"Ack\": 898627232,\"Checksum\": 57115,\"DataLength\": 12,\"DataOffset\": 8,\"DestinationAddress\": \"172.30.9.45\",\"DestinationPort\": 45629,\"Direction\": null,\"Flags\": 24,\"ReassembledLength \": 0,\"RelativeAck\": 0,\"RelativeSeq\": 0,\"Seq\": 1030775351,\"SessionKey\": \"172.30.9.48:9092 -> 172.30.9.45:45629\",\"SourceAddress\": \"172.30.9.48\",\"SourcePort\": 9092,\"TotalLength\": 44,\"UrgentPointer\": 0,\"Window\": 1453}}" };

  /** The Constant protoCols. */
  private static final String[] protoCols = { "TCP", "UDP", "SNMP" };

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.spout.ISpout#open(java.util.Map,
   * backtype.storm.task.TopologyContext,
   * backtype.storm.spout.SpoutOutputCollector)
   */
  public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {

    System.out.println("Opening PcapSimulatorSpout");

    this.collector = collector;

    if (conf.containsKey("storm.topology.pcap.spout.pcap-kafka-simulator-spout.packet.size.in.bytes")) {

      messageSize = Integer.valueOf(conf.get("storm.topology.pcap.spout.pcap-kafka-simulator-spout.packet.size.in.bytes").toString());
      pcap = new byte[messageSize];

      System.out.println("Using message size : " + messageSize);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.spout.ISpout#nextTuple()
   */
  public void nextTuple() {

    // System.out.println("nextTuple of PcapSimulatorSpout");
    ipAddr.setLength(0);
    String srcAddr = ipAddr.append(randomIpSegmentGenerator.nextInt(255)).append('.').append(randomIpSegmentGenerator.nextInt(255))
        .append('.').append(randomIpSegmentGenerator.nextInt(255)).append('.').append(randomIpSegmentGenerator.nextInt(255)).toString();
    ipAddr.setLength(0);
    String dstAddr = ipAddr.append(randomIpSegmentGenerator.nextInt(255)).append('.').append(randomIpSegmentGenerator.nextInt(255))
        .append('.').append(randomIpSegmentGenerator.nextInt(255)).append('.').append(randomIpSegmentGenerator.nextInt(255)).toString();

    String key = PcapUtils.getSessionKey(srcAddr, dstAddr, String.valueOf(randomProtocolGenerator.nextInt(255)),
        String.valueOf(randomPortGenerator.nextInt(64000)), String.valueOf(randomPortGenerator.nextInt(64000)), "0", "0");

    jsonDoc = jsonDocs[randomJsonGenerator.nextInt(8)];
    ts = System.currentTimeMillis() + randomPortGenerator.nextInt();
    randomPcapGenerator.nextBytes(pcap);

    collector.emit(new Values(srcAddr, key.toString(), jsonDoc, ts, pcap));

    collector.emit("pcap_index_stream", new Values(jsonDoc, key));
    collector.emit("pcap_header_stream", new Values(jsonDoc, key));
    collector.emit("pcap_data_stream", new Values(key.toString(), ts, pcap));

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * backtype.storm.topology.IComponent#declareOutputFields(backtype.storm.topology
   * .OutputFieldsDeclarer)
   */
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

    System.out.println("Declaring output fields of PcapSimulatorSpout");

    declarer.declareStream("pcap_index_stream", new Fields("index_json"));
    declarer.declareStream("pcap_header_stream", new Fields("header_json"));
    declarer.declareStream("pcap_data_stream", new Fields("pcap_id", "timestamp", "pcap"));

  }
  
  @Override
  public void ack(Object id) {
  }

  @Override
  public void fail(Object id) {
  }

}