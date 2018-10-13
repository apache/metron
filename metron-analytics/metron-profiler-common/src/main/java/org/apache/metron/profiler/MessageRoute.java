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

package org.apache.metron.profiler;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.Map;

/**
 * Defines the 'route' a message must take through the Profiler.
 *
 * <p>A {@link MessageRoute} defines the profile and entity that a telemetry message needs applied to.
 *
 * <p>If a message is needed by multiple profiles, then multiple {@link MessageRoute} values
 * will exist.  If a message is not needed by any profiles, then no {@link MessageRoute} values
 * will exist.
 *
 * @see MessageRouter
 */
public class MessageRoute implements Serializable {

  /**
   * The definition of the profile on this route.
   */
  private ProfileConfig profileDefinition;

  /**
   * The entity for this route.
   */
  private String entity;

  /**
   * The message taking this route.
   */
  private JSONObject message;

  /**
   * The timestamp of the message.
   */
  private Long timestamp;

  /**
   * Create a {@link MessageRoute}.
   *
   * @param profileDefinition The profile definition.
   * @param entity            The entity.
   */
  public MessageRoute(ProfileConfig profileDefinition, String entity, JSONObject message, Long timestamp) {
    this.entity = entity;
    this.profileDefinition = profileDefinition;
    this.message = message;
    this.timestamp = timestamp;
  }

  public MessageRoute() {
    // necessary for serialization
  }

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public ProfileConfig getProfileDefinition() {
    return profileDefinition;
  }

  public void setProfileDefinition(ProfileConfig profileDefinition) {
    this.profileDefinition = profileDefinition;
  }

  public JSONObject getMessage() {
    return message;
  }

  public void setMessage(JSONObject message) {
    this.message = message;
  }

  public void setMessage(Map message) {
    this.message = new JSONObject(message);
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MessageRoute that = (MessageRoute) o;
    return new EqualsBuilder()
            .append(profileDefinition, that.profileDefinition)
            .append(entity, that.entity)
            .append(message, that.message)
            .append(timestamp, that.timestamp)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(profileDefinition)
            .append(entity)
            .append(message)
            .append(timestamp)
            .toHashCode();
  }
}
