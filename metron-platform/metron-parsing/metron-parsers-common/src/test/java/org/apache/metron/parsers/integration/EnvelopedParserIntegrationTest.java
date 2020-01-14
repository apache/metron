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
package org.apache.metron.parsers.integration;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.message.metadata.MetadataUtil;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.parsers.integration.validation.ParserDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public abstract class EnvelopedParserIntegrationTest {

  /**
   *  {
   *    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
   *   ,"sensorTopic":"test"
   *   ,"rawMessageStrategy" : "ENVELOPE"
   *   ,"rawMessageStrategyConfig" : {
   *       "messageField" : "data"
   *   }
   *   ,"parserConfig": {
   *     "columns" : {
   *      "field1" : 0,
   *      "timestamp" : 1
   *     }
   *   }
   * }
   */
  @Multiline
  public static String parserConfig_default;

  public void testEnvelopedData(ParserDriver driver) throws IOException {
    Map<String, Object> inputRecord = new HashMap<String, Object>() {{
      put(Constants.Fields.ORIGINAL.getName(), "real_original_string");
      put("data", "field1_val,100");
      put("metadata_field", "metadata_val");
    }};
    ProcessorResult<List<byte[]>> results = driver.run(ImmutableList.of(JSONUtils.INSTANCE.toJSONPretty(inputRecord)));
    assertFalse(results.failed());
    List<byte[]> resultList = results.getResult();
    assertEquals(1, resultList.size());
    Map<String, Object> outputRecord = JSONUtils.INSTANCE.load(new String(resultList.get(0),
        StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER);
    assertEquals("field1_val", outputRecord.get("field1"));
    assertEquals(inputRecord.get(Constants.Fields.ORIGINAL.getName()), outputRecord.get(Constants.Fields.ORIGINAL.getName()));
    assertEquals(inputRecord.get(MetadataUtil.METADATA_PREFIX + ".metadata_field"), outputRecord.get("metadata_field"));

  }

  /**
   *  {
   *    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
   *   ,"sensorTopic":"test"
   *   ,"rawMessageStrategy" : "ENVELOPE"
   *   ,"rawMessageStrategyConfig" : {
   *       "messageField" : "data",
   *       "metadataPrefix" : ""
   *   }
   *   ,"parserConfig": {
   *     "columns" : {
   *      "field1" : 0,
   *      "timestamp" : 1
   *     }
   *   }
   * }
   */
  @Multiline
  public static String parserConfig_withPrefix;

  public void testEnvelopedData_withMetadataPrefix(ParserDriver driver) throws IOException {
    Map<String, Object> inputRecord = new HashMap<String, Object>() {{
      put(Constants.Fields.ORIGINAL.getName(), "real_original_string");
      put("data", "field1_val,100");
      put("metadata_field", "metadata_val");
    }};
    ProcessorResult<List<byte[]>> results = driver.run(ImmutableList.of(JSONUtils.INSTANCE.toJSONPretty(inputRecord)));
    assertFalse(results.failed());
    List<byte[]> resultList = results.getResult();
    assertEquals(1, resultList.size());
    Map<String, Object> outputRecord = JSONUtils.INSTANCE.load(new String(resultList.get(0),
        StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER);
    assertEquals("field1_val", outputRecord.get("field1"));
    assertEquals(inputRecord.get(Constants.Fields.ORIGINAL.getName()), outputRecord.get(Constants.Fields.ORIGINAL.getName()));
    assertEquals(inputRecord.get("metadata_field"), outputRecord.get("metadata_field"));

  }
/**
   *  {
   *    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
   *   ,"sensorTopic":"test"
   *   ,"rawMessageStrategy" : "ENVELOPE"
   *   ,"rawMessageStrategyConfig" : {
   *       "messageField" : "data"
   *   }
   *   ,"mergeMetadata" : false
   *   ,"parserConfig": {
   *     "columns" : {
   *      "field1" : 0,
   *      "timestamp" : 1
   *     }
   *   }
   * }
   */
  @Multiline
  public static String parserConfig_nomerge;

  public void testEnvelopedData_noMergeMetadata(ParserDriver driver) throws IOException {
    Map<String, Object> inputRecord = new HashMap<String, Object>() {{
      put(Constants.Fields.ORIGINAL.getName(), "real_original_string");
      put("data", "field1_val,100");
      put("metadata_field", "metadata_val");
    }};
    ProcessorResult<List<byte[]>> results = driver.run(ImmutableList.of(JSONUtils.INSTANCE.toJSONPretty(inputRecord)));
    assertFalse(results.failed());
    List<byte[]> resultList = results.getResult();
    assertEquals(1, resultList.size());
    Map<String, Object> outputRecord = JSONUtils.INSTANCE.load(new String(resultList.get(0),
        StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER);
    assertEquals("field1_val", outputRecord.get("field1"));
    assertEquals(inputRecord.get(Constants.Fields.ORIGINAL.getName()), outputRecord.get(Constants.Fields.ORIGINAL.getName()));
    assertFalse(outputRecord.containsKey(MetadataUtil.METADATA_PREFIX + ".metadata_field"));
  }

  /**
   * {
   *    "parserClassName" : "org.apache.metron.parsers.GrokParser"
   *   ,"sensorTopic" : "ciscoPix"

   *   , "parserConfig": {
   *      "grokPath": "/patterns/cisco_patterns",
   *      "patternLabel": "CISCO_PIX",
   *      "timestampField": "timestamp",
   *      "timeFields" : [ "timestamp" ],
   *      "dateFormat" : "MMM dd yyyy HH:mm:ss"
   *    }
   * }
   */
  @Multiline
  public static String ciscoPixSyslogConfig;

  /**
   * {
   *    "parserClassName" : "org.apache.metron.parsers.GrokParser"
   *   ,"sensorTopic" : "cisco302020"
   *   ,"rawMessageStrategy" : "ENVELOPE"
   *   ,"rawMessageStrategyConfig" : {
   *       "messageField" : "data",
   *       "metadataPrefix" : ""
   *   }
   *   , "parserConfig": {
   *      "grokPath": "/patterns/cisco_patterns",
   *      "patternLabel": "CISCOFW302020_302021"
   *    }
   * }
   */
  @Multiline
  public static String cisco302020Config;

  public void testCiscoPixEnvelopingCisco302020(ParserDriver syslogDriver, ParserDriver driver)
      throws Exception {
    byte[] envelopedData = null;
    String inputRecord = "Mar 29 2004 09:54:18: %PIX-6-302005: Built UDP connection for faddr 198.207.223.240/53337 gaddr 10.0.0.187/53 laddr 192.168.0.2/53";
    ProcessorResult<List<byte[]>> syslogResult = syslogDriver.run(ImmutableList.of(inputRecord.getBytes(
        StandardCharsets.UTF_8)));
    assertFalse(syslogResult.failed());
    List<byte[]> syslogResultList = syslogResult.getResult();
    envelopedData = syslogResultList.get(0);
    ProcessorResult<List<byte[]>> results = driver.run(ImmutableList.of(envelopedData));
    assertFalse(results.failed());
    List<byte[]> resultList = results.getResult();
    assertEquals(1, resultList.size());
    Map<String, Object> result = JSONUtils.INSTANCE.load(new String(resultList.get(0),
        StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER);
    assertEquals("UDP", result.get("protocol"));
    assertTrue((long) result.get("timestamp") > 1000);
  }
}
