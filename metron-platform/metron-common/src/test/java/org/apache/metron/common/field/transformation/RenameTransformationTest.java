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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RenameTransformationTest {
  /**
   {
    "fieldTransformations" : [
          {
            "transformation" : "RENAME",
            "config" : {
              "old_field1" : "new_field1",
              "old_field2" : "new_field2"
                      }
          }
                             ]
   }
   */
  @Multiline
  public static String smoketestConfig;

  @Test
  public void smokeTest() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(smoketestConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      for(int i = 1;i <= 10;++i) {
        put("old_field" + i, "f" + i);
      }
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    assertEquals("f1", input.get("new_field1"));
    assertEquals("f2", input.get("new_field2"));
    for(int i = 3;i <= 10;++i) {
      assertEquals("f" + i, input.get("old_field" + i));
    }
    assertFalse(input.containsKey("old_field1"));
    assertFalse(input.containsKey("old_field2"));
    assertEquals(10, input.size());
  }

  /**
   {
    "fieldTransformations" : [
          {
            "transformation" : "RENAME",
            "config" : {
              "old_field1" : "new_field1"
                      }
          }
                             ]
   }
   */
  @Multiline
  public static String renameMissingField;
  @Test
  public void renameMissingField() throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(renameMissingField));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      for(int i = 2;i <= 10;++i) {
        put("old_field" + i, "f" + i);
      }
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    assertFalse(input.containsKey("new_field1"));
    for(int i = 2;i <= 10;++i) {
      assertEquals("f" + i, input.get("old_field" + i));
    }
    assertEquals(9, input.size());
  }
}
