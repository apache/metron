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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class QueryValidationTest extends BaseValidationTest{
  /**
   {
    "fieldValidations" : [
          {
           "validation" : "STELLAR"
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
           "validation" : "STELLAR"
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
           "validation" : "STELLAR"
          ,"config" : {
              "condition" : "exi and "
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
           "validation" : "STELLAR"
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
    assertTrue(execute(validQueryConfig, ImmutableMap.of("field1", "foo")));
  }

  @Test
  public void testPositive_map() throws IOException {
    assertTrue(execute(validQueryConfig_map, ImmutableMap.of("dc", "la")));
  }
  @Test
  public void testNegative_map() throws IOException {
    assertFalse(execute(validQueryConfig_map, ImmutableMap.of("dc", "nyc")));
    assertFalse(execute(validQueryConfig_map, ImmutableMap.of("foo", "nyc")));
  }
  @Test
  public void testNegative() throws IOException {
    assertFalse(execute(validQueryConfig, ImmutableMap.of("field2", "foo")));
  }

  @Test
  public void testInvalidConfig_missingConfig() {
    assertThrows(IllegalStateException.class, () -> getConfiguration(invalidQueryConfig1));
  }

  @Test
  public void testInvalidConfig_invalidQuery() {
    assertThrows(IllegalStateException.class, () -> getConfiguration(invalidQueryConfig2));
  }


}
