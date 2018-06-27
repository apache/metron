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
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class RegexSelectTransformationTest {
  /**
   {
    "fieldTransformations" : [
          {
            "input" : "in_field"
          , "output" : "out_field"
          , "transformation" : "REGEX_SELECT"
          , "config" : {
              "option_1" : ".*foo.*",
              "option_2" : [ ".*metron.*", ".*mortron.*" ]
             }
          }
                      ]
   }
   */
  @Multiline
  public static String routeSingleInSingleOut;

  private String transform(String in, String config) throws Exception {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(config));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);
    JSONObject input = new JSONObject(new HashMap<String, Object>() {{
      put("in_field", in);
      put("dummy_field", "dummy"); //this is added to ensure that it looks like something approaching a real message
    }});
    handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
    return (String) input.get("out_field");
  }

  @Test
  public void smoketest() throws Exception{
    Assert.assertEquals("option_1", transform("foo", routeSingleInSingleOut));
    Assert.assertNull(transform("bar", routeSingleInSingleOut));
  }

  @Test
  public void testListOfRegexes() throws Exception {
    Assert.assertEquals("option_2", transform("I am mortron", routeSingleInSingleOut));
    Assert.assertEquals("option_2", transform("metron is for smelling", routeSingleInSingleOut));
  }
  @Test
  public void testPrecedence() throws Exception {
    Assert.assertEquals("option_1", transform("metron is for foorensic cybersecurity", routeSingleInSingleOut));
  }

  /**
   {
    "fieldTransformations" : [
          {
           "output" : "out_field"
          , "transformation" : "REGEX_SELECT"
          , "config" : {
              "option_1" : ".*foo.*",
              "option_2" : [ ".*metron.*", ".*mortron.*" ]
             }
          }
                      ]
   }
   */
  @Multiline
  public static String routeMissingInput;

  @Test
  public void testMissingInput() throws Exception {
    Assert.assertNull(transform("metron", routeMissingInput));
  }

  /**
   {
    "fieldTransformations" : [
          {
           "input" : "in_field"
          , "transformation" : "REGEX_SELECT"
          , "config" : {
              "option_1" : ".*foo.*",
              "option_2" : [ ".*metron.*", ".*mortron.*" ]
             }
          }
                      ]
   }
   */
  @Multiline
  public static String routeMissingOutput;

  @Test
  public void testMissingOutput() throws Exception {
    Assert.assertNull(transform("metron", routeMissingOutput));
  }

  /**
   {
    "fieldTransformations" : [
          {
           "input" : "in_field"
          ,"output" : [ "out_field", "baz_field" ]
          , "transformation" : "REGEX_SELECT"
          , "config" : {
              "option_1" : ".*foo.*",
              "option_2" : [ ".*metron.*", ".*mortron.*" ]
             }
          }
                      ]
   }
   */
  @Multiline
  public static String routeMultiOutput;

  @Test
  public void testMultiOutput() throws Exception{
    Assert.assertEquals("option_1", transform("foo", routeMultiOutput));
    Assert.assertNull(transform("bar", routeMultiOutput));
  }

  /**
   {
    "fieldTransformations" : [
          {
           "input" : "in_field"
          ,"output" : "out_field"
          , "transformation" : "REGEX_SELECT"
          , "config" : {
              "option_1" : "[a-z",
              "option_2" : [ ".*metron.*", ".*mortron.*" ]
             }
          }
                      ]
   }
   */
  @Multiline
  public static String routeBadRegex;

  @Test
  public void testBadRegex() throws Exception{
    Assert.assertEquals("option_2", transform("metron", routeBadRegex));
  }
}
