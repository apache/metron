package org.apache.metron.parsers.json;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class JSONMapParserTest {

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
    JSONMapParser parser = new JSONMapParser();
    List<JSONObject> output = parser.parse(happyPathJSON.getBytes());
    Assert.assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 5);
    JSONObject message = output.get(0);
    Assert.assertEquals("bar", message.get("foo"));
    Assert.assertEquals("blah", message.get("blah"));
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
    Assert.assertNotNull(message.get("number"));
    Assert.assertTrue(message.get("number") instanceof Number);
  }

  /**
   {
    "collection" : { "blah" : 7, "blah2" : "foo" }
   }
   */
   @Multiline
   static String collectionHandlingJSON;

  @Test
  public void testCollectionHandlingDrop() {
    JSONMapParser parser = new JSONMapParser();
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 2);
    JSONObject message = output.get(0);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test(expected=IllegalStateException.class)
  public void testCollectionHandlingError() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap.of("mapStrategy", "ERROR"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
  }


  @Test
  public void testCollectionHandlingAllow() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap.of("mapStrategy", "ALLOW"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 3);
    JSONObject message = output.get(0);
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }

  @Test
  public void testCollectionHandlingUnfold() {
    JSONMapParser parser = new JSONMapParser();
    parser.configure(ImmutableMap.of("mapStrategy", "UNFOLD"));
    List<JSONObject> output = parser.parse(collectionHandlingJSON.getBytes());
    Assert.assertEquals(output.size(), 1);
    //don't forget the timestamp field!
    Assert.assertEquals(output.get(0).size(), 4);
    JSONObject message = output.get(0);
    Assert.assertEquals(message.get("collection.blah"), 7);
    Assert.assertEquals(message.get("collection.blah2"), "foo");
    Assert.assertNotNull(message.get("timestamp"));
    Assert.assertTrue(message.get("timestamp") instanceof Number);
  }
}
