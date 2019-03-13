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
package org.apache.metron.storm.common.bolt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfiguredParserBolt extends ConfiguredBolt<ParserConfigurations> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected final ParserConfigurations configurations = new ParserConfigurations();
  public ConfiguredParserBolt(String zookeeperUrl) {
    super(zookeeperUrl, "PARSERS");
  }

  protected SensorParserConfig getSensorParserConfig(String sensorType) {
    return getConfigurations().getSensorParserConfig(sensorType);
  }

  @Override
  public void cleanup() {
    // This method may not be called in production.
    // See https://storm.apache.org/releases/1.0.6/javadocs/org/apache/storm/task/IBolt.html#cleanup-- for more detail.
    super.cleanup();
    try {
      StellarFunctions.close();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
