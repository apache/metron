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

import org.apache.metron.common.configuration.profiler.ProfileConfig;

import java.io.Serializable;

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
   * Create a {@link MessageRoute}.
   *
   * @param profileDefinition The profile definition.
   * @param entity The entity.
   */
  public MessageRoute(ProfileConfig profileDefinition, String entity) {
    this.entity = entity;
    this.profileDefinition = profileDefinition;
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
}
