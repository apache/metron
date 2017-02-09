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

    try {
      JSONObject message = JSONUtils.INSTANCE.toJSONObject(measurement);
      collector.emit(getStreamId(), new Values(message));

    } catch(Exception e) {
      throw new IllegalStateException("unable to serialize a profile measurement", e);
    }
  }
}
