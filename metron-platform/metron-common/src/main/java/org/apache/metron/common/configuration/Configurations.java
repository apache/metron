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
package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.log4j.Logger;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Configurations implements Serializable {

  private static final Logger LOG = Logger.getLogger(Configurations.class);

  protected ConcurrentMap<String, Object> configurations = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public Map<String, Object> getGlobalConfig() {
    return (Map<String, Object>) configurations.get(ConfigurationType.GLOBAL.getName());
  }

  public void updateGlobalConfig(byte[] data) throws IOException {
    if (data == null) throw new IllegalStateException("global config data cannot be null");
    updateGlobalConfig(new ByteArrayInputStream(data));
  }

  public void updateGlobalConfig(InputStream io) throws IOException {
    Map<String, Object> globalConfig = JSONUtils.INSTANCE.load(io, new TypeReference<Map<String, Object>>() {
    });
    updateGlobalConfig(globalConfig);
  }

  public void updateGlobalConfig(Map<String, Object> globalConfig) {
    configurations.put(ConfigurationType.GLOBAL.getName(), globalConfig);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Configurations that = (Configurations) o;
    return configurations.equals(that.configurations);
  }

  @Override
  public int hashCode() {
    return configurations.hashCode();
  }

  @Override
  public String toString() {
    return configurations.toString();
  }
}
