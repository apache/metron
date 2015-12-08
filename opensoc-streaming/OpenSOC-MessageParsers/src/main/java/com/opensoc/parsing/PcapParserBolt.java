package com.opensoc.parsing;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.opensoc.parsing.parsers.PcapParser;
import com.opensoc.pcap.PacketInfo;
import com.opensoc.topologyhelpers.ErrorGenerator;

import backtype.storm.generated.Grouping;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;



/**
 * The Class PcapParserBolt parses each input tuple and emits a new tuple which
 * contains the information (header_json,group_key,pcap_id, timestamp, pcap) as
 * defined in the output schema.
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public class PcapParserBolt implements IRichBolt {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -1449830233777209255L;

  /** The Constant LOG. */
  private static final Logger LOG = Logger.getLogger(PcapParserBolt.class);

  /** The collector. */
  private OutputCollector collector = null;

  /** The conf. */
  @SuppressWarnings("rawtypes")
private Map conf;

  /** The number of chars to use for shuffle grouping. */
  @SuppressWarnings("unused")
private int numberOfCharsToUseForShuffleGrouping = 4;

  /** The micro sec multiplier. */
  private long microSecMultiplier = 1L;

  /** The sec multiplier. */
  private long secMultiplier = 1000000L;

  // HBaseStreamPartitioner hBaseStreamPartitioner = null ;

  /**
   * The Constructor.
   */
  public PcapParserBolt() {

  }

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.topology.IComponent#declareOutputFields(backtype.storm
   * .topology.OutputFieldsDeclarer)
   */
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
	  declarer.declareStream("message", new Fields("key", "message")); 
    //declarer.declareStream("pcap_index_stream", new Fields("index_json", "pcap_id"));
    declarer.declareStream("pcap_header_stream", new Fields("header_json", "pcap_id"));
    declarer.declareStream("pcap_data_stream", new Fields("pcap_id", "timestamp", "pcap"));
    declarer.declareStream("error", new Fields("error"));

  }

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.topology.IComponent#getComponentConfiguration()
   */
  /**
   * Method getComponentConfiguration.
   * 
   * 
   * 
   * @return Map<String,Object> * @see
   *         backtype.storm.topology.IComponent#getComponentConfiguration() * @see
   *         backtype.storm.topology.IComponent#getComponentConfiguration() * @see
   *         backtype.storm.topology.IComponent#getComponentConfiguration()
   */

  public Map<String, Object> getComponentConfiguration() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.task.IBolt#prepare(java.util.Map,
   * backtype.storm.task.TopologyContext, backtype.storm.task.OutputCollector)
   */

  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
    this.conf = stormConf;
    if (conf.containsKey("bolt.parser.num.of.key.chars.to.use.for.shuffle.grouping")) {
      this.numberOfCharsToUseForShuffleGrouping = Integer.valueOf(conf.get(
          "bolt.parser.num.of.key.chars.to.use.for.shuffle.grouping").toString());
    }
    
    Grouping._Fields a;

    if (conf.containsKey("bolt.parser.ts.precision")) {
      String timePrecision = conf.get("bolt.parser.ts.precision").toString();
      if (timePrecision.equalsIgnoreCase("MILLI")) {
        microSecMultiplier = 1L / 1000;
        secMultiplier = 1000L;
      } else if (timePrecision.equalsIgnoreCase("MICRO")) {
        microSecMultiplier = 1L;
        secMultiplier = 1000000L;
      } else if (timePrecision.equalsIgnoreCase("NANO")) {
        microSecMultiplier = 1000L;
        secMultiplier = 1000000000L;
      }
    }
    // hBaseStreamPartitioner = new HBaseStreamPartitioner(
    // conf.get("bolt.hbase.table.name").toString(),
    // 0,
    // Integer.parseInt(conf.get("bolt.hbase.partitioner.region.info.refresh.interval.mins").toString()))
    // ;
    // hBaseStreamPartitioner.prepare();

  }

  /**
   * Processes each input tuple and emits tuple which holds the following
   * information about a network packet : group_key : first 3 digits of the
   * pcap_id pcap_id : generated from network packet srcIp, dstIp, protocol,
   * srcPort, dstPort header_json : contains global header, ipv4 header, tcp
   * header(if the n/w protocol is tcp), udp header (if the n/w protocol is udp)
   * timestamp : the n/w packet capture timestamp pcap : tuple in binary array.
   * 
   * @param input
   *          Tuple
   * @see backtype.storm.task.IBolt#execute(Tuple)
   */

  @SuppressWarnings("unchecked")
public void execute(Tuple input) {

    // LOG.debug("In PcapParserBolt bolt: Got tuple " + input);
    // LOG.debug("Got this pcap : " + new String(input.getBinary(0)));

    List<PacketInfo> packetInfoList = null;
    try {
      packetInfoList = PcapParser.parse(input.getBinary(0));

      if (packetInfoList != null) {

        for (PacketInfo packetInfo : packetInfoList) {
        	
        	String string_pcap = packetInfo.getJsonIndexDoc();
        	Object obj=JSONValue.parse(string_pcap);
        	  JSONObject header=(JSONObject)obj;
        	
        	JSONObject message = new JSONObject();
        	//message.put("key", packetInfo.getKey());
        	
        	if(header.containsKey("src_addr"))
        	{
        		String tmp = header.get("src_addr").toString();
        		header.remove("src_addr");
        		header.put("ip_src_addr", tmp);
        	}
        	
        	if(header.containsKey("dst_addr"))
        	{
        		String tmp = header.get("dst_addr").toString();
        		header.remove("dst_addr");
        		header.put("ip_dst_addr", tmp);
        	}
        	
        	if(header.containsKey("src_port"))
        	{
        		String tmp = header.get("src_port").toString();
        		header.remove("src_port");
        		header.put("ip_src_port", tmp);
        	}
        	
        	if(message.containsKey("dst_port"))
        	{
        		String tmp = header.get("dst_port").toString();
        		header.remove("dst_port");
        		header.put("ip_dst_port", tmp);
        	}
        	if(message.containsKey("ip_protocol"))
        	{
        		String tmp = header.get("ip_protocol").toString();
        		header.remove("ip_protocol");
        		header.put("protocol", tmp);
        	}
        	
        	message.put("message", header);
        	
        	collector.emit("message", new Values(packetInfo.getKey(), message));
        	
        	//collector.emit("pcap_index_stream", new Values(packetInfo.getJsonIndexDoc(), packetInfo.getKey()));
        	
          collector.emit("pcap_header_stream", new Values(packetInfo.getJsonDoc(), packetInfo.getKey()));
          collector.emit("pcap_data_stream", new Values(packetInfo.getKey(),
              (packetInfo.getPacketHeader().getTsSec() * secMultiplier + packetInfo.getPacketHeader().getTsUsec() * microSecMultiplier),
              input.getBinary(0)));

          // collector.emit(new Values(packetInfo.getJsonDoc(), packetInfo
          // .getKey().substring(0, numberOfCharsToUseForShuffleGrouping),
          // packetInfo.getKey(), (packetInfo.getPacketHeader().getTsSec()
          // * secMultiplier + packetInfo.getPacketHeader().getTsUsec()
          // * microSecMultiplier), input.getBinary(0)));
        }
      }

    } catch (Exception e) {
      collector.fail(input);
      e.printStackTrace();
      LOG.error("Exception while processing tuple", e);
      
      String error_as_string = org.apache.commons.lang.exception.ExceptionUtils
				.getStackTrace(e);

		JSONObject error = ErrorGenerator.generateErrorMessage(
				"Alerts problem: " + input.getBinary(0), error_as_string);
		collector.emit("error", new Values(error));
		
      return;
    }
    collector.ack(input);

  }

  /*
   * (non-Javadoc)
   * 
   * @see backtype.storm.task.IBolt#cleanup()
   */

  public void cleanup() {
    // TODO Auto-generated method stub

  }
}