package org.apache.metron.bolt;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.json.simple.JSONObject;

import java.util.List;

public class PcapParserBolt extends TelemetryParserBolt {

  @Override
  public void declareOther(OutputFieldsDeclarer declarer) {
    declarer.declareStream("raw", new Fields("key", "value", "timestamp") );
  }

  @Override
  public void emitOther(Tuple tuple, List<JSONObject> messages) {
    for(JSONObject message: messages) {
      String key = (String) message.get("pcap_id");
      long timestamp = (long) message.get("ts_micro");
      collector.emit("raw", tuple, new Values(key, tuple.getBinary(0),
              timestamp));
    }
  }
}
