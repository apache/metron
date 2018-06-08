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

package org.apache.metron.common.field.transformation;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

@RunWith(Parameterized.class)
public class StellarTransformationTest {
  Context context;
  public StellarTransformationTest(Cache<CachingStellarProcessor.Key, Object> cache) {
    if(cache == null) {
      context = Context.EMPTY_CONTEXT();
    }
    else {
      context = new Context.Builder().with(Context.Capabilities.CACHE, () -> cache).build();
    }
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
            new Object[][] {
                     { CachingStellarProcessor.createCache(ImmutableMap.of(CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 10)) }
                   , { CachingStellarProcessor.createCache(ImmutableMap.of(CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 1)) }
                   , { CachingStellarProcessor.createCache(ImmutableMap.of(CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 0)) }
                   , { null }
                           }
                        );
  }

  /**
   {
    "fieldTransformations" : [
     {
       "transformation" : "STELLAR"
      ,"output" : [ "full_hostname", "domain_without_subdomains" ]
      ,"config" : {
         "full_hostname" : "URL_TO_HOST(123)"
        ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
                  }
     }
                      ]
   }
   */
  @Multiline
  public static String badConfig;

  /**
   { "fieldTransformations" : [
        { "transformation" : "STELLAR"
        ,"output" : [ "new_field"]
        ,"config" : {
          "new_field" : "MAP_GET('source.type', _)"
                    }
        }
                                ]
      }
   */
 @Multiline
 public static String configAll;

  @Test
  public void testConfigAll() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(configAll));
    JSONObject input = new JSONObject();
    input.put("source.type", "test");
    for (FieldTransformer handler : c.getFieldTransformations()) {
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    }
    Assert.assertEquals(2, input.size());
    Assert.assertTrue(input.containsKey("new_field"));
    Assert.assertEquals("test", input.get("new_field"));
  }

  /** { "fieldTransformations" : [
        { "transformation" : "STELLAR"
        ,"output" : [ "new_field", "new_field2", "old_field", "old_field2"]
        ,"config" : {
          "new_field" : "old_field"
         ,"new_field2" : "old_field2"
         ,"old_field" : "null"
         ,"old_field2" : "null"
                    }
        }
                                ]
      }
   */
 @Multiline
 public static String configRename;

 @Test
 public void testStellarRename() throws Exception {

   SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(configRename));
   {
     JSONObject input = new JSONObject();
     input.put("old_field", "val");
     input.put("old_field2", "val2");
     for (FieldTransformer handler : c.getFieldTransformations()) {
       handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
     }
     Assert.assertEquals(2, input.size());
     Assert.assertTrue(input.containsKey("new_field"));
     Assert.assertEquals("val", input.get("new_field"));
     Assert.assertEquals("val2", input.get("new_field2"));
     Assert.assertTrue(!input.containsKey("old_field"));
     Assert.assertTrue(!input.containsKey("old_field2"));
   }
   {
     JSONObject input = new JSONObject();
     input.put("old_field", "val");
     for (FieldTransformer handler : c.getFieldTransformations()) {
       handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
     }

     Assert.assertEquals(1, input.size());
     Assert.assertTrue(input.containsKey("new_field"));
     Assert.assertEquals("val", input.get("new_field"));
   }
 }

  /** { "fieldTransformations" : [
        { "transformation" : "STELLAR"
        ,"output" : [ "full_hostname", "domain_without_subdomains" ]
        ,"config" : {
          "full_hostname" : "URL_TO_HOST('http://1234567890123456789012345678901234567890123456789012345678901234567890/index.html')"
          ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
                    }
        }
                                ]
      }
   */
  @Multiline
  public static String configNumericDomain;

  @Test
  public void testStellarNumericDomain() throws Exception {
    /*
    Despite the domain being weird, URL_TO_HOST should allow it to pass through.
    However, because it does NOT form a proper domain (no TLD), DOMAIN_REMOVE_SUBDOMAINS returns
    null indicating that the input is semantically incorrect.
     */
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(configNumericDomain));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject();
    handler.transformAndUpdate(input,  Context.EMPTY_CONTEXT());
    Assert.assertTrue(input.containsKey("full_hostname"));
    Assert.assertEquals("1234567890123456789012345678901234567890123456789012345678901234567890", input.get("full_hostname"));
    Assert.assertFalse(input.containsKey("domain_without_subdomains"));

  }

  @Test(expected=IllegalStateException.class)
  public void testStellarBadConfig() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(badConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject();
    try {
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    }
    catch(IllegalStateException ex) {
      Assert.assertTrue(ex.getMessage().contains("URL_TO_HOST"));
      Assert.assertTrue(ex.getMessage().contains("123"));
      throw ex;
    }

  }

  /**
   {
    "fieldTransformations" : [
          {
           "transformation" : "STELLAR"
          ,"output" : "utc_timestamp"
          ,"config" : {
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', 'UTC')"
                      }
          }
                      ]
   }
   */
  @Multiline
  public static String stellarConfig;

  /**
   {
    "fieldTransformations" : [
          {
           "transformation" : "STELLAR"
          ,"output" : "final_value"
          ,"config" : {
            "value1" : "1"
           ,"value2" : "value1 + 1"
           ,"final_value" : "value2 + 1"
                      }
          }
                      ]
   }
   */
  @Multiline
  public static String intermediateValuesConfig;

  @Test
  public void testIntermediateValues() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(intermediateValuesConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    int expected = 3;
    Assert.assertEquals(expected, input.get("final_value"));
    Assert.assertFalse(input.containsKey("value1"));
    Assert.assertFalse(input.containsKey("value2"));
  }

  /**
   {
    "fieldTransformations" : [
          {
            "transformation" : "STELLAR"
          ,"output" : ["newStellarField","utc_timestamp"]
          ,"config" : {
            "newStellarField" : "'<<??>>'",
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', 'UTC')"
                      }
          }
                             ]
   }
   */
  @Multiline
  public static String stellarConfigEspecial;


  @Test
  public void testStellarSpecialCharacters() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(stellarConfigEspecial));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      put("timestamp", "2016-01-05 17:02:30");
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    long expected = 1452013350000L;
    Assert.assertEquals(expected, input.get("utc_timestamp"));
    Assert.assertTrue(input.containsKey("timestamp"));
    Assert.assertTrue(input.containsKey("newStellarField"));
  }

  /**
   * Test the happy path.  This ensures that a simple transformation, converting a timestamp in a yyyy-MM-dd HH:mm:ss
   * format can be converted to the expected UTC MS since Epoch.
   */
  @Test
  public void testStellar() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(stellarConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      put("timestamp", "2016-01-05 17:02:30");
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    long expected = 1452013350000L;
    Assert.assertEquals(expected, input.get("utc_timestamp"));
    Assert.assertTrue(input.containsKey("timestamp"));
  }

  /**
   * Ensures that if we try to transform with a field which does not exist, it does not
   * 1. throw an exception
   * 2. do any transformation.
   */
  @Test
  public void testStellar_negative() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(stellarConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    //no input fields => no transformation
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
    }});
    handler.transformAndUpdate(input,  Context.EMPTY_CONTEXT());
    Assert.assertFalse(input.containsKey("utc_timestamp"));
    Assert.assertTrue(input.isEmpty());
  }

  /**
   {
    "fieldTransformations" : [
          {
           "transformation" : "STELLAR"
          ,"output" : [ "utc_timestamp", "url_host", "url_protocol" ]
          ,"config" : {
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', MAP_GET(dc, dc2tz, 'UTC') )"
           ,"url_host" : "TO_LOWER(URL_TO_HOST(url))"
           ,"url_protocol" : "URL_TO_PROTOCOL(url)"
                      }
          }
                      ]
   ,"parserConfig" : {
      "dc2tz" : {
                "nyc" : "EST"
               ,"la" : "PST"
               ,"london" : "UTC"
                }
    }
   }
   */
  @Multiline
  public static String stellarConfig_multi;

  /**
   * A more complicated test where we are transforming multiple fields:
   * 1. Convert a timestamp field in yyyy-MM-dd HH:mm:ss format to unix epoch while
   *    looking up the timezone based on a second field, dc, in a map being kept in the parser config.
   *    If the data center isn't in the map, then the default is UTC
   * 2. Extract the host from a URL field and convert to lowercase
   * 3. Extract the protocol of the URL field
   **/
  @Test
  public void testStellar_multi() throws Exception {

    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(stellarConfig_multi));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    {
      //We need a timestamp field, a URL field and a data center field
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
        //looking up the data center in portland, which doesn't exist in the map, so we default to UTC
        put("dc", "portland");
      }});
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
    {
      //now we see what happens when we change the data center to london, which is in the map
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
        put("dc", "london");
      }});
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT(), c.getParserConfig());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
    //now we ensure that because we don't have a data center field at all, it's defaulted to UTC.
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
      }});
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT(), c.getParserConfig());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
  }
}
