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

package org.apache.metron.common.field.validation;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.FieldValidator;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class QueryValidationTest extends BaseValidationTest{
  /**
   {
    "fieldValidations" : [
          {
           "validation" : "MQL"
          ,"config" : {
                "condition" : "exists(field1)"
                      }
          }
                         ]
   }
   */
  @Multiline
  public static String validQueryConfig;

  /**
   {
    "fieldValidations" : [
          {
           "validation" : "MQL"
          ,"config" : {
                      }
          }
                         ]
   }
   */
  @Multiline
  public static String invalidQueryConfig1;

  /**
   {
    "fieldValidations" : [
          {
           "validation" : "MQL"
          ,"config" : {
              "condition" : "exi"
                      }
          }
                         ]
   }
   */
  @Multiline
  public static String invalidQueryConfig2;
  /**
   {
    "fieldValidations" : [
          {
           "validation" : "MQL"
          ,"config" : {
                "condition" : "MAP_EXISTS(dc, dc2tz)"
                ,"dc2tz" : {
                          "la" : "PST"
                           }
                      }
          }
                         ]
   }
   */
  @Multiline
  public static String validQueryConfig_map;

  @Test
  public void testPositive() throws IOException {
    Assert.assertTrue(execute(validQueryConfig, ImmutableMap.of("field1", "foo")));
  }

  @Test
  public void testPositive_map() throws IOException {
    Assert.assertTrue(execute(validQueryConfig_map, ImmutableMap.of("dc", "la")));
  }
  @Test
  public void testNegative_map() throws IOException {
    Assert.assertFalse(execute(validQueryConfig_map, ImmutableMap.of("dc", "nyc")));
    Assert.assertFalse(execute(validQueryConfig_map, ImmutableMap.of("foo", "nyc")));
  }
  @Test
  public void testNegative() throws IOException {
    Assert.assertFalse(execute(validQueryConfig, ImmutableMap.of("field2", "foo")));
  }

  @Test(expected=IllegalStateException.class)
  public void testInvalidConfig_missingConfig() throws IOException {
    getConfiguration(invalidQueryConfig1);
  }

  @Test(expected=IllegalStateException.class)
  public void testInvalidConfig_invalidQuery() throws IOException {
    getConfiguration(invalidQueryConfig2);
  }


}
