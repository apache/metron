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
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class JSONMapParserWrappedQueryTest {

  /**
   * { "name" : "foo1", "value" : "bar", "number" : 1.0 },
   * { "name" : "foo2", "value" : "baz", "number" : 2.0 }
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
      put(JSONMapParser.WRAP_JSON,true);
      put(JSONMapParser.WRAP_ENTITY_NAME,"foo");
      put(JSONMapParser.JSONP_QUERY, "$.foo");
    }});
    List<JSONObject> output = parser.parse(JSON_LIST.getBytes());
    Assert.assertEquals(output.size(), 2);
    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 5);
    JSONObject message = output.get(0);
    Assert.assertEquals("foo1", message.get("name"));
    Assert.assertEquals("bar", message.get("value"));
    Assert.assertEquals(1.0, message.get("number"));
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
    Assert.assertNotNull(message.get("number"));
    Assert.assertTrue(message.get("number") instanceof Number);

    message = output.get(1);
    Assert.assertEquals("foo2", message.get("name"));
    Assert.assertEquals("baz", message.get("value"));
    Assert.assertEquals(2.0, message.get("number"));
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
    Assert.assertNotNull(message.get("number"));
    Assert.assertTrue(message.get("number") instanceof Number);

  }

  @Test(expected = IllegalStateException.class)
  public void testInvalidJSONPathThrows() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$$..$$SDSE$#$#.");
    }});
    List<JSONObject> output = parser.parse(JSON_LIST.getBytes());

  }

  @Test
  public void testNoMatchesNoExceptions() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(new HashMap<String, Object>() {{
      put(JSONMapParser.JSONP_QUERY, "$.foo");
    }});
    List<JSONObject> output = parser.parse(JSON_SINGLE.getBytes());
    Assert.assertEquals(0, output.size());
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
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 2);

    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 2);

    JSONObject message = output.get(0);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);

    message = output.get(1);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test(expected = IllegalStateException.class)
  public void testCollectionHandlingError() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ERROR.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.FATAL);
    parser.parse(collectionHandlingJSON.getBytes());
    UnitTestHelper.setLog4jLevel(BasicParser.class, Level.ERROR);
  }


  @Test
  public void testCollectionHandlingAllow() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.ALLOW.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 2);
    Assert.assertEquals(output.get(0).size(), 3);
    JSONObject message = output.get(0);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);

    Assert.assertEquals(output.get(1).size(), 3);
    message = output.get(1);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingUnfold() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap
        .of(JSONMapParser.MAP_STRATEGY_CONFIG, JSONMapParser.MapStrategy.UNFOLD.name(),
            JSONMapParser.JSONP_QUERY, "$.foo"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 2);
    Assert.assertEquals(output.get(0).size(), 6);
    JSONObject message = output.get(0);
    Assert.assertEquals(message.get("collection.blah"), 7);
    Assert.assertEquals(message.get("collection.blah2"), "foo");
    Assert.assertEquals(message.get("collection.bigblah.innerBlah"), "baz");
    Assert.assertEquals(message.get("collection.bigblah.reallyInnerBlah.color"), "grey");
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);

    Assert.assertEquals(output.get(1).size(), 6);
    message = output.get(1);
    Assert.assertEquals(message.get("collection.blah"), 8);
    Assert.assertEquals(message.get("collection.blah2"), "bar");
    Assert.assertEquals(message.get("collection.bigblah.innerBlah"), "baz2");
    Assert.assertEquals(message.get("collection.bigblah.reallyInnerBlah.color"), "blue");
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }
}
