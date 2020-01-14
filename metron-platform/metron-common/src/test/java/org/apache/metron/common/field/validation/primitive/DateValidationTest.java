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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateValidationTest extends BaseValidationTest{
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
             ,"validation" : "DATE"
             ,"config" : {
                  "format" : "yyyy-MM-dd"
                         }
            }
                         ]
   }
   */

  @Multiline
  public static String validWithSingleField;
  public static String validWithSingleField_MQL = "IS_DATE(field1, 'yyyy-MM-dd')";

  /**
   {
    "fieldValidations" : [
            {
              "input" : [ "field1", "field2" ]
             ,"validation" : "DATE"
             ,"config" : {
                  "format" : "yyyy-MM-dd"
                         }
            }
                         ]
   }
   */
  @Multiline
  public static String validWithMultipleFields;
  public static String validWithMultipleFields_MQL = "IS_DATE(field1, 'yyyy-MM-dd') && IS_DATE(field2, 'yyyy-MM-dd')";

  @Test
  public void positiveTest_single() throws IOException {
    assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "2014-05-01")));
    assertTrue(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "2014-05-01")));
  }
  @Test
  public void negativeTest_single() throws IOException {
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", 2.3f)));
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "2014/05/01")));
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "2014/05/01")));
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", 2.3f)));
    //invalid month
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "2014-25-01")));
    //invalid date
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "2014-05-32")));
  }
  @Test
  public void positiveTest_multiple() throws IOException {
    assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field1", "2014-06-01", "field2", "2014-06-02")));
    assertTrue(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", "2014-06-01", "field2", "2014-06-02")));
  }

  @Test
  public void negativeTest_multiple() throws IOException {

    assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field2", "2014-06-02")));
    assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field2", "2014-06-02")));
    assertFalse(execute(validWithMultipleFields, ImmutableMap.of("field1", 1, "field2", "2014-06-02")));
    assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", 1, "field2", "2014-06-02")));
    assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field3", "2014-06-02")));
    assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field3", "2014-06-02")));
  }
}
