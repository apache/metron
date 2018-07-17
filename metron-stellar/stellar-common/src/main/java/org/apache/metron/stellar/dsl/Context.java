/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.stellar.dsl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context implements Serializable {

  public interface Capability {
    Object get();
  }
  
  public enum Capabilities {
      HBASE_PROVIDER
    ,
    /**
     * This capability indicates that the global config is available.
     */
    GLOBAL_CONFIG
    ,
    /**
     * This capability indicates that a zookeeper client (i.e. a Curator client, specifically) is available.
     */
    ZOOKEEPER_CLIENT
    ,
    /**
     * This capability indicates that a MaaS service discoverer is available.
     */
    SERVICE_DISCOVERER
    ,
    /**
     * This capability indicates that a map configuring stellar is available.  Generally this is done within the global config
     * inside of storm, but may be sourced elsewhere (e.g. the CLI when running the REPL).
     */
    STELLAR_CONFIG
    ,
    /**
     * This capability indicates that the Console object is available.  This is available when run via the CLI (e.g. from the REPL).
     */
    CONSOLE
    ,
    /**
     * This capability indicates that shell variables are available.  This is available when run via the CLI (e.g. from the REPL).
     */
    SHELL_VARIABLES
    ,
    /**
     * This capability indicates that the StellarProcessor should use a Caffeine cache to cache expression -{@literal >} results.  If an expression
     * is in the cache, then the cached result will be returned instead of recomputing.
     */
    CACHE
  }

  public enum ActivityType {
    VALIDATION_ACTIVITY,
    PARSE_ACTIVITY
  }

  private static ThreadLocal<ActivityType> _activityType = ThreadLocal.withInitial(() ->
      null);

  public static class Builder {

    private Map<String, Capability> capabilityMap = new HashMap<>();

    public Builder with(String s, Capability capability) {
      capabilityMap.put(s, capability);
      return this;
    }

    public Builder with(Enum<?> s, Capability capability) {
      capabilityMap.put(s.toString(), capability);
      return this;
    }
    
    public Builder withAll(Map<String, Object> externalConfig) {
      for(Map.Entry<String, Object> entry : externalConfig.entrySet()) {

        capabilityMap.put(entry.getKey(), () -> entry.getValue());
      }
      return this;
    }

    public Context build() {
      return new Context(capabilityMap);
    }
  }

  public static Context EMPTY_CONTEXT() {
    return new Context(new HashMap<>()){};
  }

  private Map<String, Capability> capabilities;

  private Context( Map<String, Capability> capabilities) {
    this.capabilities = capabilities;
  }

  public Optional<Object> getCapability(Enum<?> capability) {
    return getCapability(capability, true);
  }

  public Optional<Object> getCapability(Enum<?> capability, boolean errorIfNotThere) {
    return getCapability(capability.toString(), errorIfNotThere);
  }

  public Optional<Object> getCapability(String capability) {
    return getCapability(capability, true);
  }

  public Optional<Object> getCapability(String capability, boolean errorIfNotThere) {
    Capability c = capabilities.get(capability);
    if(c == null && errorIfNotThere) {
      throw new IllegalStateException("Unable to find capability " + capability + "; it may not be available in your context.");
    }
    else if(c == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(c.get());
  }

  public void addCapability(String s, Capability capability) {
    this.capabilities.put(s, capability);
  }

  public void addCapability(Enum<?> s, Capability capability) {
    this.capabilities.put(s.toString(), capability);
  }

  public ActivityType getActivityType() {
    return _activityType.get();
  }

  public void setActivityType(ActivityType activityType) {
    _activityType.set(activityType);
  }
}
