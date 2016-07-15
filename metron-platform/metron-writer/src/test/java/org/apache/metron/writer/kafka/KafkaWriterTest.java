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

package org.apache.metron.writer.kafka;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KafkaWriterTest {


  public static final String SENSOR_TYPE = "test";
  public WriterConfiguration createConfiguration(final Map<String, Object> parserConfig) {
    ParserConfigurations configurations = new ParserConfigurations();
    configurations.updateSensorParserConfig( SENSOR_TYPE
                                           , new SensorParserConfig() {{
                                              setParserConfig(parserConfig);
                                                                      }}
                                           );
    return new ParserWriterConfiguration(configurations);
  }

  @Test
  public void testHappyPathGlobalConfig() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("kafka.brokerUrl" , "localhost:6667");
              put("kafka.topic" , SENSOR_TYPE);
              put("kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    Map<String, Object> producerConfigs = writer.createProducerConfigs();
    Assert.assertEquals(producerConfigs.get("bootstrap.servers"), "localhost:6667");
    Assert.assertEquals(producerConfigs.get("key.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    Assert.assertEquals(producerConfigs.get("value.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    Assert.assertEquals(producerConfigs.get("request.required.acks"), 1);
    Assert.assertEquals(producerConfigs.get("key1"), 1);
    Assert.assertEquals(producerConfigs.get("key2"), "value2");
  }

  @Test
  public void testHappyPathGlobalConfigWithPrefix() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    writer.withConfigPrefix("prefix");
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("prefix.kafka.brokerUrl" , "localhost:6667");
              put("prefix.kafka.topic" , SENSOR_TYPE);
              put("prefix.kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    Map<String, Object> producerConfigs = writer.createProducerConfigs();
    Assert.assertEquals(producerConfigs.get("bootstrap.servers"), "localhost:6667");
    Assert.assertEquals(producerConfigs.get("key.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    Assert.assertEquals(producerConfigs.get("value.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    Assert.assertEquals(producerConfigs.get("request.required.acks"), 1);
    Assert.assertEquals(producerConfigs.get("key1"), 1);
    Assert.assertEquals(producerConfigs.get("key2"), "value2");
  }
}
