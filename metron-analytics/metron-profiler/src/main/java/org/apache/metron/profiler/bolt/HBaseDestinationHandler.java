package org.apache.metron.profiler.bolt;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.io.Serializable;

/**
 * Handles emitting a ProfileMeasurement to the stream which writes
 * profile measurements to HBase.
 */
public class HBaseDestinationHandler implements DestinationHandler, Serializable {

  /**
   * The stream identifier used for this destination;
   */
  private String streamId = "hbase";

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(getStreamId(), new Fields("measurement"));
  }

  @Override
  public void emit(ProfileMeasurement measurement, OutputCollector collector) {
    collector.emit(getStreamId(), new Values(measurement));
  }

  @Override
  public String getStreamId() {
    return streamId;
  }

  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }
}
