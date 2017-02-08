package org.apache.metron.profiler.bolt;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;

/**
 * This class handles the mechanics of emitting a profile measurement to a
 * stream responsible for writing to a specific destination.
 *
 * The measurements produced by a profile can be written to one or more
 * destinations; HBase, Kafka, etc.  Each of the destinations leverage a
 * separate stream within the topology definition.
 */
public interface DestinationHandler {

  /**
   * Each destination leverages a unique stream.  This method defines
   * the unique stream identifier.
   *
   * The stream identifier must also be declared within the topology
   * definition.
   */
  String getStreamId();

  /**
   * Declares the output fields for the stream.
   * @param declarer
   */
  void declareOutputFields(OutputFieldsDeclarer declarer);

  /**
   * Emit the measurement.
   * @param measurement The measurement to emit.
   * @param collector The output collector.
   */
  void emit(ProfileMeasurement measurement, OutputCollector collector);
}
