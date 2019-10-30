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
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveTransformationTest {
  /**
   {
    "fieldTransformations" : [
          {
            "input" : "field1"
          , "transformation" : "REMOVE"
          }
                      ]
   }
   */
  @Multiline
  public static String removeUnconditionalConfig;

  @Test
  public void testUnconditionalRemove() throws Exception{
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(removeUnconditionalConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      put("field1", "foo");
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    assertFalse(input.containsKey("field1"));
  }

  /**
   {
    "fieldTransformations" : [
          {
            "output" : "field1"
          , "transformation" : "REMOVE"
          , "config" : {
              "condition" : "exists(field2) and field2 == 'foo'"
                       }
          }
                      ]
   }
   */
  @Multiline
  public static String removeConditionalConfig;
  @Test
  public void testConditionalRemove() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(removeConditionalConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("field1", "foo");
      }});
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
      //no removal happened because field2 does not exist
      assertTrue(input.containsKey("field1"));
      assertFalse(input.containsKey("field2"));
    }
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("field1", "foo");
        put("field2", "bar");
      }});
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
      //no removal happened because field2 != bar
      assertTrue(input.containsKey("field1"));
      assertTrue(input.containsKey("field2"));
    }
    {
      JSONObject input = new JSONObject(new HashMap<String, Object>() {{
        put("field1", "bar");
        put("field2", "foo");
      }});
      //removal of field1 happens because field2 exists and is 'bar'
      handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
      assertFalse(input.containsKey("field1"));
      assertTrue(input.containsKey("field2"));
    }
  }
}
