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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Responsible for emitting a {@link ProfileMeasurement} to an output stream that will
 * persist data in HBase.
 */
public class KafkaEmitter implements ProfileMeasurementEmitter, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The stream identifier used for this destination;
   */
  private String streamId = "kafka";

  /**
   * The 'source.type' of messages originating from the Profiler.
   */
  private String sourceType = "profiler";

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // the kafka writer expects a field named 'message'
    declarer.declareStream(getStreamId(), new Fields("message"));
  }

  @Override
  public void emit(ProfileMeasurement measurement, OutputCollector collector) {

    // only need to emit, if there are triage values
    Map<String, Object> triageValues = measurement.getTriageValues();
    if(MapUtils.isNotEmpty(triageValues)) {

      JSONObject message = createMessage(measurement);
      appendTriageValues(measurement, message);
      collector.emit(getStreamId(), new Values(message));

      LOG.debug("Emitted measurement; stream={}, profile={}, entity={}, period={}, start={}, end={}",
              getStreamId(),
              measurement.getProfileName(),
              measurement.getEntity(),
              measurement.getPeriod().getPeriod(),
              measurement.getPeriod().getStartTimeMillis(),
              measurement.getPeriod().getEndTimeMillis());

    } else {

      LOG.debug("No triage values, nothing to emit; stream={}, profile={}, entity={}, period={}, start={}, end={}",
              getStreamId(),
              measurement.getProfileName(),
              measurement.getEntity(),
              measurement.getPeriod().getPeriod(),
              measurement.getPeriod().getStartTimeMillis(),
              measurement.getPeriod().getEndTimeMillis());
    }
  }

  /**
   * Appends triage values obtained from a {@code ProfileMeasurement} to the
   * outgoing message.
   *
   * @param measurement The measurement that may contain triage values.
   * @param message The message that the triage values are appended to.
   */
  private void appendTriageValues(ProfileMeasurement measurement, JSONObject message) {

    // for each triage value...
    Map<String, Object> triageValues = MapUtils.emptyIfNull(measurement.getTriageValues());
    triageValues.forEach((key, value) -> {

      // append the triage value to the message
      if(isValidType(value)) {
        message.put(key, value);

      } else {
        LOG.error(String.format(
                "triage expression must result in primitive type, skipping; type=%s, profile=%s, entity=%s, expr=%s",
                ClassUtils.getShortClassName(value, "null"),
                measurement.getDefinition().getProfile(),
                measurement.getEntity(),
                key));
      }
    });
  }

  /**
   * Creates a message that will be emitted to Kafka.
   *
   * @param measurement The profile measurement used as a basis for the message.
   * @return A message that can be emitted to Kafka.
   */
  private JSONObject createMessage(ProfileMeasurement measurement) {

    JSONObject message = new JSONObject();
    message.put("profile", measurement.getDefinition().getProfile());
    message.put("entity", measurement.getEntity());
    message.put("period", measurement.getPeriod().getPeriod());
    message.put("period.start", measurement.getPeriod().getStartTimeMillis());
    message.put("period.end", measurement.getPeriod().getEndTimeMillis());
    message.put("timestamp", System.currentTimeMillis());
    message.put("source.type", sourceType);
    message.put("is_alert", "true");
    return message;
  }

  /**
   * The result of a profile's triage expressions must be a string or primitive type.
   *
   * This ensures that the value can be easily serialized and appended to a message destined for Kafka.
   *
   * @param value The value of a triage expression.
   * @return True, if the type of the value is valid.
   */
  private boolean isValidType(Object value) {
    return value != null && (value instanceof String || ClassUtils.isPrimitiveOrWrapper(value.getClass()));
  }

  @Override
  public String getStreamId() {
    return streamId;
  }

  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }
}
