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

/**
 * A MessageRoute defines the profile and entity that a telemetry message needs applied to.  This
 * allows a message to be routed to the profile and entity that needs it.
 *
 * One telemetry message may need multiple routes.  This is the case when a message is needed by
 * more than one profile.  In this case, there will be multiple MessageRoute objects for a single
 * message.
 */
public class MessageRoute {

  /**
   * The definition of the profile on this route.
   */
  private ProfileConfig profileDefinition;

  /**
   * The entity for this route.
   */
  private String entity;

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
