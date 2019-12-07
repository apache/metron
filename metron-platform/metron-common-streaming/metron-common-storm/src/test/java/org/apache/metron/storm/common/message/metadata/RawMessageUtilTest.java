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
package org.apache.metron.storm.common.message.metadata;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.message.metadata.EnvelopedRawMessageStrategy;
import org.apache.metron.common.message.metadata.MetadataUtil;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.message.metadata.RawMessageStrategies;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RawMessageUtilTest {

  private static Tuple createTuple(Map<String, Object> kafkaFields, String metadata) {
    List<Map.Entry<String, Object>> fields = new ArrayList<>();
    for(Map.Entry<String, Object> kv : kafkaFields.entrySet()) {
      fields.add(kv);
    }

    Tuple t = mock(Tuple.class);
    Fields f = mock(Fields.class);
    when(f.size()).thenReturn(fields.size()+2);

    for(int i = 0;i < fields.size();++i) {
      when(f.get(eq(i + 2))).thenReturn(fields.get(i).getKey());
      when(t.getValue(eq(i + 2))).thenReturn(fields.get(i).getValue());
    }

    when(t.getFields()).thenReturn(f);
    when(t.getBinary(eq(RawMessageUtil.KEY_INDEX))).thenReturn(metadata.getBytes(
            StandardCharsets.UTF_8));
    return t;
  }

  private void checkKafkaMetadata(RawMessage m, boolean isEmpty) {
    if(!isEmpty) {
      assertEquals("kafka_meta_1_val", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".kafka_meta_1"));
      assertEquals("kafka_meta_2_val", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".kafka_meta_2"));
    }
    else {
      assertFalse(m.getMetadata().containsKey(MetadataUtil.METADATA_PREFIX + ".kafka_meta_1"));
      assertFalse(m.getMetadata().containsKey(MetadataUtil.METADATA_PREFIX + ".kafka_meta_2"));
    }
  }

  private void checkAppMetadata(RawMessage m, boolean isEmpty) {
    if(!isEmpty) {
      assertEquals("app_meta_1_val", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".app_meta_1"));
      assertEquals("app_meta_2_val", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".app_meta_2"));
    }
    else {
      assertFalse(m.getMetadata().containsKey(MetadataUtil.METADATA_PREFIX + ".app_meta_1"));
      assertFalse(m.getMetadata().containsKey(MetadataUtil.METADATA_PREFIX + ".app_meta_2"));
    }
  }

  public static Map<String, Object> kafkaMetadata = ImmutableMap.of("kafka_meta_1", "kafka_meta_1_val", "kafka_meta_2", "kafka_meta_2_val");

  /**
   * {
   *   "app_meta_1" : "app_meta_1_val",
   *   "app_meta_2" : "app_meta_2_val"
   * }
   */
  @Multiline
  public static String appMetadata;

  @Test
  public void testDefaultStrategy_withKafkaMetadata_withAppMetadata() {
    Tuple t = createTuple( kafkaMetadata
                         , appMetadata);
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), true, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkKafkaMetadata(m, false);
      checkAppMetadata(m, false);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), false, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testDefaultStrategy_withKafkaMetadata_withoutAppMetadata() {
    Tuple t = createTuple(kafkaMetadata
                         ,"{}");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), true, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkKafkaMetadata(m, false);
      checkAppMetadata(m, true);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), false, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testDefaultStrategy_withoutKafkaMetadata_withAppMetadata() {
    Tuple t = createTuple(new HashMap<>() ,appMetadata);
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), true, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkKafkaMetadata(m, true);
      checkAppMetadata(m, false);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), false, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testDefaultStrategy_withoutKafkaMetadata_withoutAppMetadata() {
    Tuple t = createTuple(new HashMap<>() , "{}");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), true, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkKafkaMetadata(m, true);
      checkAppMetadata(m, true);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage(RawMessageStrategies.DEFAULT, t, "raw_message".getBytes(
          StandardCharsets.UTF_8), false, new HashMap<>());
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  /**
   * {
   *   "data" : "raw_message",
   *   "original_string" : "real_original_string",
   *   "enveloped_metadata_field_1" : "enveloped_metadata_val_1",
   *   "enveloped_metadata_field_2" : "enveloped_metadata_val_2"
   * }
   */
  @Multiline
  public static String envelopedData;

  public static JSONObject envelopedMessage = new JSONObject() {{
    put("message_field1", "message_val1");
    put(Constants.Fields.ORIGINAL.getName(), "envelope_message_val");
  }};

  private void checkEnvelopeMetadata(RawMessage m) {
    assertEquals("real_original_string", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + "." + Constants.Fields.ORIGINAL.getName()));
    assertEquals("enveloped_metadata_val_1", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".enveloped_metadata_field_1"));
    assertEquals("enveloped_metadata_val_2", m.getMetadata().get(MetadataUtil.METADATA_PREFIX + ".enveloped_metadata_field_2"));
  }

  private void checkMergedData(RawMessage m) {
    JSONObject message = new JSONObject(envelopedMessage);
    RawMessageStrategies.ENVELOPE.mergeMetadata(message, m.getMetadata(), true, new HashMap<String, Object>() {});
    if(m.getMetadata().containsKey(MetadataUtil.METADATA_PREFIX + "." +Constants.Fields.ORIGINAL.getName())) {
      assertEquals(m.getMetadata().get(MetadataUtil.METADATA_PREFIX + "." + Constants.Fields.ORIGINAL.getName()), message.get(Constants.Fields.ORIGINAL.getName()));
    }
    assertEquals("message_val1", message.get("message_field1"));
  }

  @Test
  public void testEnvelopeStrategy_withKafkaMetadata_withAppMetadata() {
    Tuple t = createTuple( kafkaMetadata
                         , appMetadata);
    Map<String, Object> config = ImmutableMap.of(EnvelopedRawMessageStrategy.MESSAGE_FIELD_CONFIG, "data");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), true, config);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertFalse(m.getMetadata().containsKey("data"));
      checkEnvelopeMetadata(m);
      checkMergedData(m);
      checkKafkaMetadata(m, false);
      checkAppMetadata(m, false);

    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), false, config);
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertFalse(m.getMetadata().containsKey("data"));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testEnvelopeStrategy_withKafkaMetadata_withoutAppMetadata() {
    Tuple t = createTuple(kafkaMetadata
                         ,"{}");
    Map<String, Object> config = ImmutableMap.of(EnvelopedRawMessageStrategy.MESSAGE_FIELD_CONFIG, "data");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), true, config);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      checkEnvelopeMetadata(m);
      checkKafkaMetadata(m, false);
      checkAppMetadata(m, true);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), false, config);
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testEnvelopeStrategy_withoutKafkaMetadata_withAppMetadata() {
    Tuple t = createTuple(new HashMap<>() ,appMetadata);
    Map<String, Object> config = ImmutableMap.of(EnvelopedRawMessageStrategy.MESSAGE_FIELD_CONFIG, "data");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), true, config);
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkEnvelopeMetadata(m);
      checkKafkaMetadata(m, true);
      checkAppMetadata(m, false);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), false, config);
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

  @Test
  public void testEnvelopeStrategy_withoutKafkaMetadata_withoutAppMetadata() {
    Tuple t = createTuple(new HashMap<>() , "{}");
    Map<String, Object> config = ImmutableMap.of(EnvelopedRawMessageStrategy.MESSAGE_FIELD_CONFIG, "data");
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), true, config);
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      checkEnvelopeMetadata(m);
      checkKafkaMetadata(m, true);
      checkAppMetadata(m, true);
    }
    {
      RawMessage m = RawMessageUtil.INSTANCE.getRawMessage( RawMessageStrategies.ENVELOPE, t, envelopedData.getBytes(
          StandardCharsets.UTF_8), false, config);
      assertFalse(m.getMetadata().containsKey("data"));
      checkMergedData(m);
      assertEquals("raw_message", new String(m.getMessage(), StandardCharsets.UTF_8));
      assertTrue(m.getMetadata().isEmpty());
    }
  }

}
