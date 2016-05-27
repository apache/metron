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

package org.apache.metron.common.field.validation.primitive;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.field.validation.BaseValidationTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RegexValidationTest extends BaseValidationTest {
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
             ,"validation" : "REGEX_MATCH"
             ,"config" : {
                  "pattern" : "fo.*"
                         }
            }
                         ]
   }
   */

  @Multiline
  public static String validWithSingleField;

  /**
   {
    "fieldValidations" : [
            {
              "input" : [ "field1", "field2" ]
             ,"validation" : "REGEX_MATCH"
             ,"config" : {
                  "pattern" : "fo.*"
                         }
            }
                         ]
   }
   */
  @Multiline
  public static String validWithMultipleFields;

  @Test
  public void positiveTest_single() throws IOException {
    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "foo")));
    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "fop")));
    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "fo")));
  }
  @Test
  public void negativeTest_single() throws IOException {
    Assert.assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "flo")));
    Assert.assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", 2.3f)));
  }
  @Test
  public void positiveTest_multiple() throws IOException {
    Assert.assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field1", "fooo", "field2", "foll")));
  }

  @Test
  public void negativeTest_multiple() throws IOException {

    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field2", "foo")));
    Assert.assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", 1, "field2", "foo")));
    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field3", "foo")));
  }
}

