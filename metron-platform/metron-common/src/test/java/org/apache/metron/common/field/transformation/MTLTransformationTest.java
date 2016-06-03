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

import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class MTLTransformationTest {
  /**
   {
    "fieldTransformations" : [
          {
           "transformation" : "MTL"
          ,"output" : "utc_timestamp"
          ,"config" : {
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', 'UTC')"
                      }
          }
                      ]
   }
   */
  @Multiline
  public static String mtlConfig;

  @Test
  public void testMTL() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(mtlConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      put("timestamp", "2016-01-05 17:02:30");
    }});
    handler.transformAndUpdate(input, new HashMap<>());
    long expected = 1452013350000L;
    Assert.assertEquals(expected, input.get("utc_timestamp"));
    Assert.assertTrue(input.containsKey("timestamp"));
  }
  @Test
  public void testMTL_negative() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(mtlConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
    }});
    handler.transformAndUpdate(input, new HashMap<>());
    Assert.assertFalse(input.containsKey("utc_timestamp"));
    Assert.assertTrue(input.isEmpty());
  }

  /**
   {
    "fieldTransformations" : [
          {
           "transformation" : "MTL"
          ,"output" : [ "utc_timestamp", "url_host", "url_protocol" ]
          ,"config" : {
            "utc_timestamp" : "TO_EPOCH_TIMESTAMP(timestamp, 'yyyy-MM-dd HH:mm:ss', MAP_GET(dc, dc2tz, 'UTC') )"
           ,"url_host" : "URL_TO_HOST(url)"
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
  public static String mtlConfig_multi;

  @Test
  public void testMTL_multi() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(mtlConfig_multi));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
        put("dc", "portland");
      }});
      handler.transformAndUpdate(input, c.getParserConfig());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
        put("dc", "london");
      }});
      handler.transformAndUpdate(input, c.getParserConfig());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("timestamp", "2016-01-05 17:02:30");
        put("url", "https://caseystella.com/blog");
      }});
      handler.transformAndUpdate(input, c.getParserConfig());
      long expected = 1452013350000L;
      Assert.assertEquals(expected, input.get("utc_timestamp"));
      Assert.assertEquals("caseystella.com", input.get("url_host"));
      Assert.assertEquals("https", input.get("url_protocol"));
      Assert.assertTrue(input.containsKey("timestamp"));
      Assert.assertTrue(input.containsKey("url"));
    }
  }
}
