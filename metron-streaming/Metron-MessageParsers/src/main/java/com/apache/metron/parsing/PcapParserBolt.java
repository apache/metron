package com.opensoc.parsing;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.opensoc.helpers.topology.ErrorGenerator;
import com.opensoc.parsing.parsers.PcapParser;
import com.opensoc.pcap.PacketInfo;

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

  /** The divisor to convert nanos to expected time precision. */
  private long timePrecisionDivisor = 1L;


  // HBaseStreamPartitioner hBaseStreamPartitioner = null ;

  /**
   * The Constructor.
   */
  public PcapParserBolt() {

  }

  public PcapParserBolt withTsPrecision(String tsPrecision) {
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
        	
        	message.put("message", header);
        	
        	collector.emit("message", new Values(packetInfo.getKey(), message));
        	
        	//collector.emit("pcap_index_stream", new Values(packetInfo.getJsonIndexDoc(), packetInfo.getKey()));
        	
          collector.emit("pcap_header_stream", new Values(packetInfo.getJsonDoc(), packetInfo.getKey()));
          collector.emit("pcap_data_stream", new Values(packetInfo.getKey(),
             packetInfo.getPacketTimeInNanos() / timePrecisionDivisor,
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
      

		JSONObject error = ErrorGenerator.generateErrorMessage(
				"Alerts problem: " + input.getBinary(0), e);
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