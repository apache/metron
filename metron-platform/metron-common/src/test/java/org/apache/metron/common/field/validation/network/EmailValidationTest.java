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

package org.apache.metron.common.field.validation.network;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.field.validation.BaseValidationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailValidationTest extends BaseValidationTest {
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
             ,"validation" : "EMAIL"
            }
                         ]
   }
   */
  @Multiline
  public static String validWithSingleField;
  public static String validWithSingleField_MQL = "IS_EMAIL(field1)";

  /**
   {
    "fieldValidations" : [
            {
              "input" : [ "field1", "field2" ]
             ,"validation" : "EMAIL"
            }
                         ]
   }
   */
  @Multiline
  public static String validWithMultipleFields;
  public static String validWithMultipleFields_MQL = "IS_EMAIL(field1) and IS_EMAIL(field2)";

  @Test
  public void positiveTest_single() throws IOException {
    assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "me@caseystella.com")));
    assertTrue(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "me@caseystella.com")));
    assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "me@www.hotmail.co.uk")));
    assertTrue(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "me@www.hotmail.co.uk")));
  }
  @Test
  public void negativeTest_single() throws IOException {
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "me@foo")));
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "me@foo")));
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "caseystella.turtle")));
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "caseystella.turtle")));
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "caseystella.com")));
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "caseystella.com")));
    assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", 2.7f)));
    assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", 2.7f)));
  }
  @Test
  public void negativeTest_empty() throws IOException {
    assertFalse(runPredicate("IS_EMAIL()", Collections.emptyMap()));
    assertFalse(runPredicate("IS_EMAIL('')", Collections.emptyMap()));
  }
  @Test
  public void positiveTest_multiple() throws IOException {
    assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field1", "me@www.gmail.com", "field2", "me@www.hotmail.com")));
    assertTrue(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", "me@www.gmail.com", "field2", "me@www.hotmail.com")));
  }

  @Test
  public void negativeTest_multiple() throws IOException {
    assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field2", "me@hotmail.edu")));
    assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field2", "me@hotmail.edu")));
    assertFalse(execute(validWithMultipleFields, ImmutableMap.of("field1", "", "field2", "me@gmail.com")));
    assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", "", "field2", "me@gmail.com")));
  }
}
