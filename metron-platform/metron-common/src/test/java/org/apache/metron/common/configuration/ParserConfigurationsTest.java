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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.Test;

public class ParserConfigurationsTest {

  /**
   * {
   *  "parserClassName" : "parser-class",
   *  "filterClassName" : "filter-class",
   *  "sensorTopic" : "sensor-topic",
   *  "outputTopic" : "output-topic",
   *  "errorTopic" : "error-topic",
   *  "writerClassName" : "writer-class",
   *  "errorWriterClassName" : "error-writer-class",
   *  "readMetadata" : true,
   *  "mergeMetadata" : true,
   *  "numWorkers" : 40,
   *  "numAckers" : 40,
   *  "spoutParallelism" : 40,
   *  "spoutNumTasks" : 40,
   *  "parserParallelism" : 40,
   *  "parserNumTasks" : 40,
   *  "errorWriterParallelism" : 40,
   *  "errorWriterNumTasks" : 40,
   *  "securityProtocol" : "security-protocol",
   *  "spoutConfig" : {
   *    "foo" : "bar"
   *  },
   *  "stormConfig" : {
   *    "storm" : "config"
   *  },
   *  "cacheConfig" : {
   *    "stellar.cache.maxSize" : 20000
   *  },
   *  "parserConfig" : {
   *    "parser" : "config"
   *  },
   *  "fieldTransformations" : [
   *    {
   *      "input" : "input-field",
   *      "transformation" : "REMOVE"
   *    }
   *  ]
   * }
   */
  @Multiline
  private static String parserConfig;

  @Test
  public void sensorParserConfig_properties_populated_by_JSON_configuration() throws IOException {
    ParserConfigurations parserConfigs = new ParserConfigurations();
    parserConfigs.updateSensorParserConfig("test-sensor", parserConfig.getBytes(
        StandardCharsets.UTF_8));
    SensorParserConfig actualSensorConfig = parserConfigs.getSensorParserConfig("test-sensor");
    assertThat(actualSensorConfig.getParserClassName(), equalTo("parser-class"));
    assertThat(actualSensorConfig.getFilterClassName(), equalTo("filter-class"));
    assertThat(actualSensorConfig.getSensorTopic(), equalTo("sensor-topic"));
    assertThat(actualSensorConfig.getOutputTopic(), equalTo("output-topic"));
    assertThat(actualSensorConfig.getErrorTopic(), equalTo("error-topic"));
    assertThat(actualSensorConfig.getWriterClassName(), equalTo("writer-class"));
    assertThat(actualSensorConfig.getErrorWriterClassName(), equalTo("error-writer-class"));
    assertThat(actualSensorConfig.getReadMetadata(), equalTo(true));
    assertThat(actualSensorConfig.getMergeMetadata(), equalTo(true));
    assertThat(actualSensorConfig.getNumWorkers(), equalTo(40));
    assertThat(actualSensorConfig.getNumAckers(), equalTo(40));
    assertThat(actualSensorConfig.getSpoutParallelism(), equalTo(40));
    assertThat(actualSensorConfig.getSpoutNumTasks(), equalTo(40));
    assertThat(actualSensorConfig.getParserParallelism(), equalTo(40));
    assertThat(actualSensorConfig.getParserNumTasks(), equalTo(40));
    assertThat(actualSensorConfig.getErrorWriterParallelism(), equalTo(40));
    assertThat(actualSensorConfig.getErrorWriterNumTasks(), equalTo(40));
    assertThat(actualSensorConfig.getSecurityProtocol(), equalTo("security-protocol"));
    assertThat(actualSensorConfig.getSpoutConfig(), not(new HashMap<>()));
    assertThat(actualSensorConfig.getSpoutConfig().get("foo"), equalTo("bar"));
    assertThat(actualSensorConfig.getStormConfig(), not(new HashMap<>()));
    assertThat(actualSensorConfig.getStormConfig().get("storm"), equalTo("config"));
    assertThat(actualSensorConfig.getCacheConfig(), not(new HashMap<>()));
    assertThat(actualSensorConfig.getCacheConfig().get("stellar.cache.maxSize"), equalTo(20000));
    assertThat(actualSensorConfig.getParserConfig(), not(new HashMap<>()));
    assertThat(actualSensorConfig.getParserConfig().get("parser"), equalTo("config"));
    assertThat(actualSensorConfig.getFieldTransformations(), not(new ArrayList<>()));
    assertThat(actualSensorConfig.getFieldTransformations().get(0), not(nullValue()));
    assertThat(
        ((FieldTransformer) actualSensorConfig.getFieldTransformations().get(0)).getInput().size(),
        equalTo(1));
    assertThat(
        ((FieldTransformer) actualSensorConfig.getFieldTransformations().get(0)).getInput().get(0),
        equalTo("input-field"));
    assertThat(((FieldTransformer) actualSensorConfig.getFieldTransformations().get(0))
        .getTransformation(), equalTo("REMOVE"));
  }

}
