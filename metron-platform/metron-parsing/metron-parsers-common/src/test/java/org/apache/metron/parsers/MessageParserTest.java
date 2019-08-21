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

package org.apache.metron.parsers;

import java.nio.charset.StandardCharsets;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MessageParserTest {

  abstract class TestMessageParser implements MessageParser<JSONObject> {
    @Override
    public void init() {
    }

    @Override
    public boolean validate(JSONObject message) {
      return false;
    }

    @Override
    public void configure(Map<String, Object> config) {

    }
  }

  @Test
  public void testNullable() throws Exception {
    MessageParser parser = new TestMessageParser() {
      @Override
      public List<JSONObject> parse(byte[] rawMessage) {
        return null;
      }
    };
    Assert.assertNotNull(parser.parseOptionalResult(null));
    Assert.assertFalse(parser.parseOptionalResult(null).isPresent());
  }

  @Test
  public void testNotNullable() throws Exception {
    MessageParser<JSONObject> parser = new TestMessageParser() {
      @Override
      public List<JSONObject> parse(byte[] rawMessage) {
        return new ArrayList<>();
      }
    };
    Assert.assertNotNull(parser.parseOptionalResult(null));
    Optional<MessageParserResult<JSONObject>> ret = parser.parseOptionalResult(null);
    Assert.assertTrue(ret.isPresent());
    Assert.assertEquals(0, ret.get().getMessages().size());
  }

  @Test
  public void testParse() {
    JSONObject message = new JSONObject();
    MessageParser<JSONObject> parser = new TestMessageParser() {
      @Override
      public List<JSONObject> parse(byte[] rawMessage) {
        return Collections.singletonList(message);
      }
    };
    Optional<MessageParserResult<JSONObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    Assert.assertTrue(ret.isPresent());
    Assert.assertEquals(1, ret.get().getMessages().size());
    Assert.assertEquals(message, ret.get().getMessages().get(0));
  }

  @Test
  public void testParseOptional() {
    JSONObject message = new JSONObject();
    MessageParser<JSONObject> parser = new TestMessageParser() {
      @Override
      public Optional<List<JSONObject>> parseOptional(byte[] rawMessage) {
        return Optional.of(Collections.singletonList(message));
      }
    };
    Optional<MessageParserResult<JSONObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    Assert.assertTrue(ret.isPresent());
    Assert.assertEquals(1, ret.get().getMessages().size());
    Assert.assertEquals(message, ret.get().getMessages().get(0));
  }

  @Test
  public void testParseException() {
    MessageParser<JSONObject> parser = new TestMessageParser() {
      @Override
      public List<JSONObject> parse(byte[] rawMessage) {
        throw new RuntimeException("parse exception");
      }
    };
    Optional<MessageParserResult<JSONObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    Assert.assertTrue(ret.isPresent());
    Assert.assertTrue(ret.get().getMasterThrowable().isPresent());
    Assert.assertEquals("parse exception", ret.get().getMasterThrowable().get().getMessage());
  }

  @Test
  public void testParseOptionalException() {
    MessageParser<JSONObject> parser = new TestMessageParser() {
      @Override
      public Optional<List<JSONObject>> parseOptional(byte[] rawMessage) {
        throw new RuntimeException("parse exception");
      }
    };
    Optional<MessageParserResult<JSONObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    Assert.assertTrue(ret.isPresent());
    Assert.assertTrue(ret.get().getMasterThrowable().isPresent());
    Assert.assertEquals("parse exception", ret.get().getMasterThrowable().get().getMessage());
  }

}
