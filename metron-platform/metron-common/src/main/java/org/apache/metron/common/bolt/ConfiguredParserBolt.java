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
package org.apache.metron.common.bolt;

import org.apache.log4j.Logger;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;

import java.io.IOException;

public abstract class ConfiguredParserBolt extends ConfiguredBolt<ParserConfigurations> {

  private static final Logger LOG = Logger.getLogger(ConfiguredEnrichmentBolt.class);


  protected final ParserConfigurations configurations = new ParserConfigurations();
  private String sensorType;
  public ConfiguredParserBolt(String zookeeperUrl, String sensorType) {
    super(zookeeperUrl);
    this.sensorType = sensorType;
  }

  protected SensorParserConfig getSensorParserConfig() {
    return getConfigurations().getSensorParserConfig(sensorType);
  }

  @Override
  protected ParserConfigurations defaultConfigurations() {
    return new ParserConfigurations();
  }

  public String getSensorType() {
    return sensorType;
  }
  @Override
  public void loadConfig() {
    try {
      ConfigurationsUtils.updateParserConfigsFromZookeeper(getConfigurations(), client);
    } catch (Exception e) {
      LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...");
    }
  }

  @Override
  public void updateConfig(String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);
      if (path.startsWith(ConfigurationType.PARSER.getZookeeperRoot())) {
        getConfigurations().updateSensorParserConfig(name, data);
        reloadCallback(name, ConfigurationType.PARSER);
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        getConfigurations().updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }
}
