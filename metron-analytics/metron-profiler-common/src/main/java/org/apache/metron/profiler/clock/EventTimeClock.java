/*
 *
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
package org.apache.metron.profiler.clock;

import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * A {@link Clock} that advances based on event time.
 *
 * Event time is advanced by the timestamps contained within telemetry messages, rather
 * than the system clock.
 */
public class EventTimeClock implements Clock {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The name of the field from which the timestamp will
   */
  private String timestampField;

  /**
   * @param timestampField The name of the field containing a timestamp.
   */
  public EventTimeClock(String timestampField) {
    this.timestampField = timestampField;
  }

  @Override
  public Optional<Long> currentTimeMillis(JSONObject message) {

    Long result;
    if(message != null && message.containsKey(timestampField)) {

      // extract the timestamp and convert to a long
      Object timestamp = message.get(timestampField);
      result = ConversionUtils.convert(timestamp, Long.class);

    } else {

      // the message does not contain the specified timestamp field
      LOG.debug("message does not contain timestamp field '{}': message will be ignored: message='{}'",
              timestampField, JSONObject.toJSONString(message));
      result = null;
    }

    return Optional.ofNullable(result);
  }
}
