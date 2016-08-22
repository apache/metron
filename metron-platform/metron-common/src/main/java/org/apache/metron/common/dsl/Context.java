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
package org.apache.metron.common.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Context {
  public interface Capability {
    Object get();
  }
  public enum Capabilities {
      HBASE_PROVIDER
    , ZOOKEEPER_CLIENT
    , SERVICE_DISCOVERER;

  }

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
    public Context build() {

      return new Context(capabilityMap);
    }
  }
  public static Context EMPTY_CONTEXT() {
    return
    new Context(new HashMap<>()){
      @Override
      public Optional<Object> getCapability(String capability) {
        return Optional.empty();
      }
    };
  }
  private Map<String, Capability> capabilities;
  private Context( Map<String, Capability> capabilities
                 )
  {
    this.capabilities = capabilities;
  }
  public Optional<Object> getCapability(Enum<?> capability) {
    return getCapability(capability.toString());
  }
  public Optional<Object> getCapability(String capability) {
    Capability c = capabilities.get(capability);
    if(c == null) {
      throw new IllegalStateException("Unable to find capability " + capability + "; it may not be available in your context.");
    }
    return Optional.ofNullable(c.get());
  }

  public void addCapability(String s, Capability capability) {
    this.capabilities.put(s, capability);
  }

  public void addCapability(Enum<?> s, Capability capability) {
    this.capabilities.put(s.toString(), capability);
  }
}
