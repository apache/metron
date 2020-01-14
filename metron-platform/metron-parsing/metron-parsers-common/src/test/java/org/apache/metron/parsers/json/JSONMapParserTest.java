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
package org.apache.metron.parsers.json;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.log4j.Level;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JSONMapParserTest {

  private JSONMapParser parser;

  @BeforeEach
  public void setup() {
    parser = new JSONMapParser();
  }

  /**
   {
     "foo" : "bar"
    ,"blah" : "blah"
    ,"number" : 2.0
   }
   */
   @Multiline
   static String happyPathJSON;

  @Test
  public void testHappyPath() {
    List<JSONObject> output = parser.parse(happyPathJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    assertEquals(output.get(0).size(), 4);
    JSONObject message = output.get(0);
    assertEquals("bar", message.get("foo"));
    assertEquals("blah", message.get("blah"));
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
    assertNotNull(message.get("number"));
    assertTrue(message.get("number") instanceof Number);
  }

  /**
   {
    "collection" : { "blah" : 7, "blah2" : "foo", "bigblah" : { "innerBlah" : "baz", "reallyInnerBlah" : { "color" : "grey" }}}
   }
   */
   @Multiline
   static String collectionHandlingJSON;

  /**
    {
     "collection" : {
        "key" : "value"
      },
     "key" : "value"
    }
   */
  @Multiline
  static String mixCollectionHandlingJSON;

  @Test
  public void testCollectionHandlingDrop() {
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    assertEquals(output.get(0).size(), 1);
    JSONObject message = output.get(0);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingError() {
    parser.configure(ImmutableMap.of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ERROR.name()));
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.FATAL);
    assertThrows(IllegalStateException.class, () -> parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8)));
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.ERROR);
  }


  @Test
  public void testCollectionHandlingAllow() {
    parser.configure(ImmutableMap.of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ALLOW.name()));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    assertEquals(output.get(0).size(), 2);
    JSONObject message = output.get(0);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingUnfold() {
    parser.configure(ImmutableMap.of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.UNFOLD.name()));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    assertEquals(output.get(0).size(), 5);
    JSONObject message = output.get(0);
    assertEquals(message.get("collection.blah"), 7);
    assertEquals(message.get("collection.blah2"), "foo");
    assertEquals(message.get("collection.bigblah.innerBlah"),"baz");
    assertEquals(message.get("collection.bigblah.reallyInnerBlah.color"),"grey");
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testMixedCollectionHandlingUnfold() {
    parser.configure(ImmutableMap.of(JSONMapParser.MAP_STRATEGY_CONFIG,JSONMapParser.MapStrategy.UNFOLD.name()));
      List<JSONObject> output = parser.parse(mixCollectionHandlingJSON.getBytes(
              StandardCharsets.UTF_8));
    assertEquals(output.get(0).size(), 3);
    JSONObject message = output.get(0);
    assertEquals(message.get("collection.key"), "value");
    assertEquals(message.get("key"),"value");
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number );
  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
