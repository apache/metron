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
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

/**
 * Responsible for emitting a {@link ProfileMeasurement} to an output stream that will
 * persist data in HBase.
 */
public class HBaseEmitter implements ProfileMeasurementEmitter, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    // measurements are always emitted to hbase
    collector.emit(getStreamId(), new Values(measurement));

    LOG.debug("Emitted measurement; stream={}, profile={}, entity={}, period={}, start={}, end={}",
            getStreamId(),
            measurement.getProfileName(),
            measurement.getEntity(),
            measurement.getPeriod().getPeriod(),
            measurement.getPeriod().getStartTimeMillis(),
            measurement.getPeriod().getEndTimeMillis());
  }

  @Override
  public String getStreamId() {
    return streamId;
  }

  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }
}
