/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.profiler.bolt;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;

/**
 * Handles the mechanics of emitting a {@link ProfileMeasurement} to an output
 * stream.
 *
 * <p>The Profiler allows the measurements produced by a profile to be written to
 * multiple endpoints such as HBase and Kafka.  Each of these endpoints will have
 * a unique stream that the measurements are written to.
 *
 * <p>Implementors of this interface are responsible for defining and managing the
 * output stream for a specific endpoint.
 */
public interface ProfileMeasurementEmitter {

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
