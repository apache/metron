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

      // TODO How to embed binary in JSON?
      // TODO How to serialize an object (like a StatisticsProvider) in a form that can be used on the other side? (Threat Triage)

      //JSONObject message = JSONUtils.INSTANCE.toJSONObject(measurement);

      JSONObject message = new JSONObject();
      message.put("profile", measurement.getDefinition().getProfile());
      message.put("entity", measurement.getEntity());
      message.put("period", measurement.getPeriod().getPeriod());
      message.put("periodStartTime", measurement.getPeriod().getStartTimeMillis());
      message.put("value", measurement.getValue());

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
