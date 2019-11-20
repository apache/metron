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

package org.apache.metron.parsers.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.writer.NoopWriter;
import org.apache.metron.writer.kafka.KafkaWriter;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ParserTopologyBuilderTest {

  private static ParserConfigurations configs;
  private static KafkaWriter kafkaWriter;

  @BeforeAll
  public static void setupAll() {
      configs = mock(ParserConfigurations.class);
      kafkaWriter = mock(KafkaWriter.class);
  }

  @Test
  public void shouldCreateWriterConfig() {
    SensorParserConfig broConfig = new SensorParserConfig();
    broConfig.setSensorTopic("bro");
    when(configs.getSensorParserConfig("bro")).thenReturn(broConfig);

    Map<String, SensorParserConfig> sensorTypeToParserConfig = new HashMap<String, SensorParserConfig>() {{
      put("bro", broConfig);
    }};

    Map<String, WriterHandler> writerConfigs = ParserTopologyBuilder
            .createWriterConfigs("zookeeperUrl",
                    Optional.of("brokerUrl"),
                    sensorTypeToParserConfig,
                    Optional.of("securityProtocol"),
                    configs,
                    Optional.empty());

    assertEquals(1, writerConfigs.size());

    // Can't directly verify against mocks because this is all static. However, knowing that we have a KafkaWriter
    // and the appropriate topic lets us know we've created the underlying config.
    BulkMessageWriter writer = writerConfigs.get("bro").getBulkMessageWriter();
    assertTrue(writer instanceof KafkaWriter);
    assertEquals(Constants.ENRICHMENT_TOPIC, ((KafkaWriter) writer).getKafkaTopic(new JSONObject()).get());
  }

  @Test
  public void shouldCreateWriterConfigWithSensorParserConfigOutputTopic() {
    SensorParserConfig snortConfig = new SensorParserConfig();
    snortConfig.setSensorTopic("snort");
    snortConfig.setOutputTopic("snort_topic");
    when(configs.getSensorParserConfig("snort")).thenReturn(snortConfig);

    Map<String, SensorParserConfig> sensorTypeToParserConfig = new HashMap<String, SensorParserConfig>() {{
      put("snort", snortConfig);
    }};

    Map<String, WriterHandler> writerConfigs = ParserTopologyBuilder
            .createWriterConfigs("zookeeperUrl",
                    Optional.of("brokerUrl"),
                    sensorTypeToParserConfig,
                    Optional.of("securityProtocol"),
                    configs,
                    Optional.empty());

    assertEquals(1, writerConfigs.size());

    // Can't directly verify against mocks because this is all static. However, knowing that we have a KafkaWriter
    // and the appropriate topic lets us know we've created the underlying config.
    BulkMessageWriter writer = writerConfigs.get("snort").getBulkMessageWriter();
    assertTrue(writer instanceof KafkaWriter);
    assertEquals("snort_topic", ((KafkaWriter) writer).getKafkaTopic(new JSONObject()).get());
  }

  @Test
  public void shouldCreateWriterConfigWithSuppliedOutputTopic() {
    SensorParserConfig snortConfig = new SensorParserConfig();
    snortConfig.setSensorTopic("snort");
    when(configs.getSensorParserConfig("snort")).thenReturn(snortConfig);

    Map<String, SensorParserConfig> sensorTypeToParserConfig = new HashMap<String, SensorParserConfig>() {{
      put("snort", snortConfig);
    }};

    Map<String, WriterHandler> writerConfigs = ParserTopologyBuilder
            .createWriterConfigs("zookeeperUrl",
                    Optional.of("brokerUrl"),
                    sensorTypeToParserConfig,
                    Optional.of("securityProtocol"),
                    configs,
                    Optional.of("supplied_topic"));

    assertEquals(1, writerConfigs.size());
    // Can't directly verify against mocks because this is all static. However, knowing that we have a KafkaWriter
    // and the appropriate topic lets us know we've created the underlying config.
    BulkMessageWriter writer = writerConfigs.get("snort").getBulkMessageWriter();
    assertTrue(writer instanceof KafkaWriter);
    assertEquals("supplied_topic", ((KafkaWriter) writer).getKafkaTopic(new JSONObject()).get());
  }

  @Test
  public void shouldCreateWriterConfigWithWriterClassName() {
    SensorParserConfig yafConfig = new SensorParserConfig();
    yafConfig.setSensorTopic("yaf");
    yafConfig.setWriterClassName("org.apache.metron.writer.NoopWriter");
    when(configs.getSensorParserConfig("yaf")).thenReturn(yafConfig);

    Map<String, SensorParserConfig> sensorTypeToParserConfig = new HashMap<String, SensorParserConfig>() {{
      put("yaf", yafConfig);
    }};

    Map<String, WriterHandler> writerConfigs = ParserTopologyBuilder
            .createWriterConfigs("zookeeperUrl",
                    Optional.of("brokerUrl"),
                    sensorTypeToParserConfig,
                    Optional.of("securityProtocol"),
                    configs,
                    Optional.empty());

    assertEquals(1, writerConfigs.size());
    assertTrue(writerConfigs.get("yaf").getBulkMessageWriter() instanceof NoopWriter);
  }

}
