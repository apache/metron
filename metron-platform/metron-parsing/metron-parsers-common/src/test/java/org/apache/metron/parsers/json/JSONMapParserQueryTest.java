/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JSONMapParserQueryTest {

  /**
   * {
   * "foo" :
   * [
   * { "name" : "foo1", "value" : "bar", "number" : 1.0 },
   * { "name" : "foo2", "value" : "baz", "number" : 2.0 }
   * ]
   * }
   */
  @Multiline
  static String JSON_LIST;

  /**
   * { "name" : "foo1", "value" : "bar", "number" : 1.0 }
   */
  @Multiline
  static String JSON_SINGLE;

  /**
   * { "name" : "foo2", "value" : "baz", "number" : 2.0 }
   */
  @Multiline
  static String JSON_SINGLE2;

  @Test
  public void testHappyPath() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$.foo");
    }});
    List<JSONObject> output = parser.parse(JSON_LIST.getBytes(StandardCharsets.UTF_8));
    assertEquals(2, output.size());
    JSONObject message = output.get(0);
    // account for timestamp field in the size
    assertEquals(4, message.size());
    assertEquals("foo1", message.get("name"));
    assertEquals("bar", message.get("value"));
    assertEquals(1.0, message.get("number"));
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
    assertNotNull(message.get("number"));
    assertTrue(message.get("number") instanceof Number);
    assertThat("original_string should be handled external to the parser by default",
        message.containsKey(Fields.ORIGINAL.getName()), equalTo(false));

    message = output.get(1);
    // account for timestamp field in the size
    assertEquals(4, message.size());
    assertEquals("foo2", message.get("name"));
    assertEquals("baz", message.get("value"));
    assertEquals(2.0, message.get("number"));
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
    assertNotNull(message.get("number"));
    assertTrue(message.get("number") instanceof Number);
    assertThat("original_string should be handled external to the parser by default",
        message.containsKey(Fields.ORIGINAL.getName()), equalTo(false));
  }

  @Test
  public void testOriginalStringHandledByParser() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$.foo");
      put(JSONMapParser.OVERRIDE_ORIGINAL_STRING, true);
    }});
    List<JSONObject> output = parser.parse(JSON_LIST.getBytes(StandardCharsets.UTF_8));
    assertEquals(2, output.size());

    JSONObject message = output.get(0);
    // account for timestamp field in the size
    assertEquals(5, message.size());
    assertEquals("foo1", message.get("name"));
    assertEquals("bar", message.get("value"));
    assertEquals(1.0, message.get("number"));
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
    assertNotNull(message.get("number"));
    assertTrue(message.get("number") instanceof Number);
    assertThat("original_string should have been handled by the parser",
        message.get(Fields.ORIGINAL.getName()), equalTo("{\"name\":\"foo1\",\"number\":1.0,\"value\":\"bar\"}"));

    message = output.get(1);
    // account for timestamp field in the size
    assertEquals(5, message.size());
    assertEquals("foo2", message.get("name"));
    assertEquals("baz", message.get("value"));
    assertEquals(2.0, message.get("number"));
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
    assertNotNull(message.get("number"));
    assertTrue(message.get("number") instanceof Number);
    assertThat("original_string should have been handled by the parser",
        message.get(Fields.ORIGINAL.getName()), equalTo("{\"name\":\"foo2\",\"number\":2.0,\"value\":\"baz\"}"));
  }

  @Test
  public void testInvalidJSONPathThrows() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$$..$$SDSE$#$#.");
    }});
    assertThrows(IllegalStateException.class, () -> parser.parse(JSON_LIST.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testNoMatchesNoExceptions() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$.foo");
    }});
    List<JSONObject> output = parser.parse(JSON_SINGLE.getBytes(StandardCharsets.UTF_8));
    assertEquals(0, output.size());
  }

  /**
   * {
   * "foo" :
   * [
   * {
   * "collection" : { "blah" : 7, "blah2" : "foo", "bigblah" : { "innerBlah" : "baz", "reallyInnerBlah" : { "color" : "grey" }}}
   * },
   * {
   * "collection" : { "blah" : 8, "blah2" : "bar", "bigblah" : { "innerBlah" : "baz2", "reallyInnerBlah" : { "color" : "blue" }}}
   * }
   * ]
   * }
   */
  @Multiline
  static String collectionHandlingJSON;

  @Test
  public void testCollectionHandlingDrop() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$.foo");
    }});
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 2);

    //don't forget the timestamp field!
    assertEquals(output.get(0).size(), 1);

    JSONObject message = output.get(0);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);

    message = output.get(1);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingError() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ERROR.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.FATAL);
    assertThrows(IllegalStateException.class, () -> parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8)));
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.ERROR);
  }


  @Test
  public void testCollectionHandlingAllow() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ALLOW.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 2);
    assertEquals(output.get(0).size(), 2);
    JSONObject message = output.get(0);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);

    assertEquals(output.get(1).size(), 2);
    message = output.get(1);
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingUnfold() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.UNFOLD.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes(StandardCharsets.UTF_8));
    assertEquals(output.size(), 2);
    assertEquals(output.get(0).size(), 5);
    JSONObject message = output.get(0);
    assertEquals(message.get("collection.blah"), 7);
    assertEquals(message.get("collection.blah2"), "foo");
    assertEquals(message.get("collection.bigblah.innerBlah"), "baz");
    assertEquals(message.get("collection.bigblah.reallyInnerBlah.color"), "grey");
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);

    assertEquals(output.get(1).size(), 5);
    message = output.get(1);
    assertEquals(message.get("collection.blah"), 8);
    assertEquals(message.get("collection.blah2"), "bar");
    assertEquals(message.get("collection.bigblah.innerBlah"), "baz2");
    assertEquals(message.get("collection.bigblah.reallyInnerBlah.color"), "blue");
    assertNotNull(message.get("timestamp"));
    assertTrue(message.get("timestamp") instanceof Number);
  }
}
