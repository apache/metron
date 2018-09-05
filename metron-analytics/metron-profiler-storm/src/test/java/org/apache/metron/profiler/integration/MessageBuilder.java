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

package org.apache.metron.profiler.integration;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Enables simple creation of telemetry messages for testing.
 */
public class MessageBuilder {

  private Map<Object, Object> fields;

  /**
   * Create a new {@link MessageBuilder}.
   */
  public MessageBuilder() {
    this.fields = new HashMap<>();
  }

  /**
   * Adds all of the fields from a message to this message.
   *
   * @param prototype The other message that is treated as a prototype.
   * @return A {@link MessageBuilder}
   */
  public MessageBuilder withFields(JSONObject prototype) {
    prototype.forEach((key, val) -> this.fields.put(key, val));
    return this;
  }

  /**
   * Adds a field to the message.
   *
   * @param key The field name.
   * @param value The field value.
   * @return A {@link MessageBuilder}
   */
  public MessageBuilder withField(String key, Object value) {
    this.fields.put(key, value);
    return this;
  }

  /**
   * Build the message.
   *
   * <p>This should be called after defining all of the message fields.
   *
   * @return A {@link MessageBuilder}.
   */
  public JSONObject build() {
    return new JSONObject(fields);
  }
}
