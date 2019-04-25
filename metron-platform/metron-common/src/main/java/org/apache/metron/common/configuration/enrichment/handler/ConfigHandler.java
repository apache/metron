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
package org.apache.metron.common.configuration.enrichment.handler;

import java.util.*;

/**
 * This is the core logic of how to configure enrichments.  The default type of enrichment configuration is a simple list
 * however more complex enrichment adapters require more complex configuration (e.g. stellar).
 */
public class ConfigHandler {
  private Object config;
  private Configs type = Configs.LIST;

  /**
   * Constructs new instance.
   *
   * @param enrichment The enrichment being handled
   * @param obj Map that contains the config and may contain the type of enrichment,
   *     (e.g. Stellar). Otherwise, use it will use the uppercased enrichment as the type.
   */
  public ConfigHandler(String enrichment, Map<String, Object> obj) {
    config = obj.get("config");
    if(obj.containsKey("type")) {
      type = Configs.valueOf((String) obj.get("type"));
    }
    else {
      //TODO: make this more adaptable
      type = Configs.valueOf(enrichment.toUpperCase());
    }
  }

  public ConfigHandler(List<String> obj) {
    config = new HashMap<>();
    type = Configs.LIST;
  }
  public Object getConfig() {
    return config;
  }

  public void setConfig(Object config) {
    this.config = config;
  }

  public Configs getType() {
    return type;
  }

  public void setType(Configs retriever) {
    this.type = retriever;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigHandler that = (ConfigHandler) o;

    if (getConfig() != null ? !getConfig().equals(that.getConfig()) : that.getConfig() != null) return false;
    return getType() != null ? getType().equals(that.getType()) : that.getType() == null;

  }

  @Override
  public int hashCode() {
    int result = getConfig() != null ? getConfig().hashCode() : 0;
    result = 31 * result + (getType() != null ? getType().hashCode() : 0);
    return result;
  }
}
