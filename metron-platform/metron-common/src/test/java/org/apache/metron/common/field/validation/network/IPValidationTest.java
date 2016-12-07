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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.apache.metron.common.utils.StellarProcessorUtils.runPredicate;

public class IPValidationTest extends BaseValidationTest {
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
             ,"validation" : "IP"
            }
                         ]
   }
   */
  @Multiline
  public static String validWithSingleField;
  public static String validWithSingleField_MQL = "IS_IP(field1)";
  /**
   {
    "fieldValidations" : [
            {
              "input" : [ "field1", "field2" ]
             ,"validation" : "IP"
             ,"config" : {
                  "type" : "IPV4"
                         }
            }
                         ]
   }
   */
  @Multiline
  public static String validWithMultipleFields;
  public static String validWithMultipleFields_MQL = "IS_IP(field1, 'IPV4') && IS_IP(field2, 'IPV4')";

  /**
   {
   "fieldValidations" : [
   {
   "input" : [ "field1", "field2" ]
   ,"validation" : "IP"
   ,"config" : {
   "type" : ["IPV4","IPV6"]
   }
   }
   ]
   }
   */
  @Multiline
  public static String validWithMultipleFieldsMultipleTypes;
  public static String validWithMultipleFieldsMultipleTypes_MQL = "IS_IP(field1, '[IPV4,IPV6]') && IS_IP(field2, '[IPV4,IPV6]')";



  @Test
  public void positiveTest_single() throws IOException {
    Assert.assertTrue(execute(validWithSingleField, ImmutableMap.of("field1", "127.0.0.1")));
    Assert.assertTrue(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "127.0.0.1")));
  }
  @Test
  public void negativeTest_single() throws IOException {
    Assert.assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", "2014/05/01")));
    Assert.assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", "2014/05/01")));
    Assert.assertFalse(execute(validWithSingleField, ImmutableMap.of("field1", 2.3f)));
    Assert.assertFalse(runPredicate(validWithSingleField_MQL, ImmutableMap.of("field1", 2.3f)));
  }
  @Test
  public void positiveTest_multiple() throws IOException {
    Assert.assertTrue(execute(validWithMultipleFields, ImmutableMap.of("field1", "192.168.0.1", "field2", "127.0.0.2")));
    Assert.assertTrue(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", "192.168.0.1", "field2", "127.0.0.2")));
  }

  @Test
  public void negativeTest_multiple() throws IOException {
    Assert.assertFalse(execute(validWithMultipleFields, ImmutableMap.of("field1", 1, "field2", "192.168.1")));
    Assert.assertFalse(runPredicate(validWithMultipleFields_MQL, ImmutableMap.of("field1", 1, "field2", "192.168.1")));
  }

  @Test
  public void positiveTest_multiplex2() throws IOException {
    Assert.assertTrue(execute(validWithMultipleFieldsMultipleTypes, ImmutableMap.of("field1", "192.168.0.1", "field2", "127.0.0.2")));
    Assert.assertTrue(runPredicate(validWithMultipleFieldsMultipleTypes_MQL, ImmutableMap.of("field1", "192.168.0.1", "field2", "127.0.0.2")));
  }

  @Test
  public void negativeTest_multiplex2() throws IOException {
    Assert.assertFalse(execute(validWithMultipleFieldsMultipleTypes, ImmutableMap.of("field1", 1, "field2", "192.168.1")));
    Assert.assertFalse(runPredicate(validWithMultipleFieldsMultipleTypes_MQL, ImmutableMap.of("field1", 1, "field2", "192.168.1")));
  }
}
