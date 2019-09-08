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
package org.apache.metron.parsers.integration.validation;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.parsers.ParserRunner;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParserDriver implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(ParserDriver.class);

  protected ParserConfigurations config;
  protected String sensorType;
  protected ParserRunner parserRunner;

  public ParserDriver(String sensorType, String parserConfig, String globalConfig)
      throws IOException {
    SensorParserConfig sensorParserConfig = SensorParserConfig.fromBytes(parserConfig.getBytes(
        StandardCharsets.UTF_8));
    this.sensorType = sensorType == null ? sensorParserConfig.getSensorTopic() : sensorType;
    config = new ParserConfigurations();
    config.updateSensorParserConfig(this.sensorType,
        SensorParserConfig.fromBytes(parserConfig.getBytes(StandardCharsets.UTF_8)));
    config.updateGlobalConfig(JSONUtils.INSTANCE.load(globalConfig, JSONUtils.MAP_SUPPLIER));

    parserRunner = new ParserRunnerImpl(new HashSet<String>() {{
      add(sensorType);
    }});
  }

  abstract public ProcessorResult<List<byte[]>> run(Iterable<byte[]> in);

  public String getSensorType() {
    return sensorType;
  }
}
