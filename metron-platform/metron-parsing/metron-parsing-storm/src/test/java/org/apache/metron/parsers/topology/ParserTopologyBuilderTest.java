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

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.writer.NoopWriter;
import org.apache.metron.writer.kafka.KafkaWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ParserTopologyBuilder.class)
public class ParserTopologyBuilderTest {

  @Mock
  private ParserConfigurations configs;

  @Mock
  private KafkaWriter kafkaWriter;

  @Before
  public void setup() {
    spy(ParserTopologyBuilder.class);
    when(ParserTopologyBuilder.createKafkaWriter(Optional.of("brokerUrl"), "zookeeperUrl", Optional.of("securityProtocol")))
            .thenReturn(kafkaWriter);
  }

  @Test
  public void shouldCreateWriterConfig() {
    SensorParserConfig broConfig = new SensorParserConfig();
    broConfig.setSensorTopic("bro");
    when(configs.getSensorParserConfig("bro")).thenReturn(broConfig);
    KafkaWriter enrichmentWriter = mock(KafkaWriter.class);
    when(kafkaWriter.withTopic(Constants.ENRICHMENT_TOPIC)).thenReturn(enrichmentWriter);

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
    assertEquals(enrichmentWriter, writerConfigs.get("bro").getBulkMessageWriter());
    verify(enrichmentWriter, times(1)).configure(eq("bro"), any(ParserWriterConfiguration.class));
    verifyNoMoreInteractions(enrichmentWriter);
  }

  @Test
  public void shouldCreateWriterConfigWithSensorParserConfigOutputTopic() {
    SensorParserConfig snortConfig = new SensorParserConfig();
    snortConfig.setSensorTopic("snort");
    snortConfig.setOutputTopic("snort_topic");
    when(configs.getSensorParserConfig("snort")).thenReturn(snortConfig);
    KafkaWriter snortTestWriter = mock(KafkaWriter.class);
    when(kafkaWriter.withTopic("snort_topic")).thenReturn(snortTestWriter);

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
    assertEquals(snortTestWriter, writerConfigs.get("snort").getBulkMessageWriter());
    verify(snortTestWriter, times(1)).configure(eq("snort"), any(ParserWriterConfiguration.class));
    verifyNoMoreInteractions(snortTestWriter);
  }

  @Test
  public void shouldCreateWriterConfigWithSuppliedOutputTopic() {
    SensorParserConfig snortConfig = new SensorParserConfig();
    snortConfig.setSensorTopic("snort");
    when(configs.getSensorParserConfig("snort")).thenReturn(snortConfig);
    KafkaWriter suppliedTopicWriter = mock(KafkaWriter.class);
    when(kafkaWriter.withTopic("supplied_topic")).thenReturn(suppliedTopicWriter);

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
    assertEquals(suppliedTopicWriter, writerConfigs.get("snort").getBulkMessageWriter());
    verify(suppliedTopicWriter, times(1)).configure(eq("snort"), any(ParserWriterConfiguration.class));
    verifyNoMoreInteractions(suppliedTopicWriter);
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
