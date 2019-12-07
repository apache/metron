/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.storm.common.message;

import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageGettersTest {
  @Test
  public void bytesFromPositionShouldReturnBytes() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getBinary(1)).thenReturn("bytes".getBytes(UTF_8));

    MessageGetStrategy messageGetStrategy = MessageGetters.BYTES_FROM_POSITION.get("1");
    assertEquals("bytes", new String((byte[]) messageGetStrategy.get(tuple), UTF_8));
  }

  @Test
  public void jsonFromPositionShouldReturnJSON() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getBinary(1)).thenReturn("{\"field\":\"value\"}".getBytes(UTF_8));

    JSONObject expected = new JSONObject();
    expected.put("field", "value");
    MessageGetStrategy messageGetStrategy = MessageGetters.JSON_FROM_POSITION.get("1");
    assertEquals(expected, messageGetStrategy.get(tuple));
  }

  @Test
  public void jsonFromPositionShouldThrowException() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getBinary(1)).thenReturn("{\"field\":".getBytes(UTF_8));

    MessageGetStrategy messageGetStrategy = MessageGetters.JSON_FROM_POSITION.get("1");
    assertThrows(IllegalStateException.class, () -> messageGetStrategy.get(tuple));
  }

  @Test
  public void jsonFromFieldShouldReturnJSON() {
    JSONObject actual = new JSONObject();
    actual.put("field", "value");
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField("tuple_field")).thenReturn(actual);

    JSONObject expected = new JSONObject();
    expected.put("field", "value");
    MessageGetStrategy messageGetStrategy = MessageGetters.JSON_FROM_FIELD.get("tuple_field");
    assertEquals(expected, messageGetStrategy.get(tuple));
  }

  @Test
  public void objectFromFieldShouldReturnObject() {
    Object actual = "object";
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField("tuple_field")).thenReturn(actual);

    Object expected = "object";
    MessageGetStrategy messageGetStrategy = MessageGetters.OBJECT_FROM_FIELD.get("tuple_field");
    assertEquals(expected, messageGetStrategy.get(tuple));
  }

  @Test
  public void defaultBytesFromPositionShouldReturnBytes() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getBinary(0)).thenReturn("bytes".getBytes(UTF_8));

    MessageGetStrategy messageGetStrategy = MessageGetters.DEFAULT_BYTES_FROM_POSITION.get();
    assertEquals("bytes", new String((byte[]) messageGetStrategy.get(tuple), UTF_8));
  }

  @Test
  public void defaultJSONFromPositionShouldReturnJSON() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getBinary(0)).thenReturn("{\"field\":\"value\"}".getBytes(UTF_8));

    JSONObject expected = new JSONObject();
    expected.put("field", "value");
    MessageGetStrategy messageGetStrategy = MessageGetters.DEFAULT_JSON_FROM_POSITION.get();
    assertEquals(expected, messageGetStrategy.get(tuple));
  }

  @Test
  public void defaultJSONFromFieldShouldReturnJSON() {
    JSONObject actual = new JSONObject();
    actual.put("field", "value");
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField("message")).thenReturn(actual);

    JSONObject expected = new JSONObject();
    expected.put("field", "value");
    MessageGetStrategy messageGetStrategy = MessageGetters.DEFAULT_JSON_FROM_FIELD.get();
    assertEquals(expected, messageGetStrategy.get(tuple));
  }

  @Test
  public void defaultObjectFromFieldShouldReturnObject() {
    Object actual = "object";
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField("message")).thenReturn(actual);

    Object expected = "object";
    MessageGetStrategy messageGetStrategy = MessageGetters.DEFAULT_OBJECT_FROM_FIELD.get();
    assertEquals(expected, messageGetStrategy.get(tuple));
  }
}
