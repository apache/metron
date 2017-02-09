package org.apache.metron.profiler.bolt;

import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;

import java.io.Serializable;

/**
 * Handles emitting a ProfileMeasurement to the stream which writes
 * profile measurements to Kafka.
 */
public class KafkaDestinationHandler implements DestinationHandler, Serializable {

  /**
   * The stream identifier used for this destination;
   */
  private String streamId = "kafka";

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

  @Override
  public String getStreamId() {
    return streamId;
  }

  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }
}
