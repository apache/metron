package org.apache.metron.profiler.bolt;

import org.apache.commons.beanutils.BeanMap;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.Serializable;

/**
 * Handles emitting a ProfileMeasurement to the stream which writes
 * profile measurements to Kafka.
 */
public class KafkaDestinationHandler implements DestinationHandler, Serializable {

  @Override
  public String getStreamId() {
    return "kafka";
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // the kafka writer expects a field named 'message'
    declarer.declareStream(getStreamId(), new Fields("message"));
  }

  @Override
  public void emit(ProfileMeasurement measurement, OutputCollector collector) {

    // the kafka writer expects a JSONObject
//    JSONObject message = new JSONObject(new BeanMap(measurement));
//    message.put("period", new JSONObject(new BeanMap(measurement.getPeriod())));
//    message.put("definition", new JSONObject(new BeanMap(measurement.getDefinition())));

    try {
//      String jsonStr = JSONUtils.INSTANCE.toJSON(measurement, true);
//      JSONParser parser = new JSONParser();
//      JSONObject message = (JSONObject) parser.parse(jsonStr);

      JSONObject message = JSONUtils.INSTANCE.toJSONObject(measurement);
      collector.emit(getStreamId(), new Values(message));

    } catch(Exception e) {
      throw new IllegalStateException("foo", e);
    }
  }
}
