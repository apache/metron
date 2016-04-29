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
package org.apache.metron.parsers;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GrokParserTest {

  private JSONObject expectedParsed;

  @Before
  public void parseJSON() throws ParseException {
    JSONParser jsonParser = new JSONParser();
    expectedParsed = (JSONObject) jsonParser.parse(getExpectedParsedString());
  }

  @Test
  public void test() throws IOException, ParseException {
    String metronHdfsHome = "";
    GrokParser grokParser = new GrokParser(getGrokPath(), getGrokPatternLabel());
    String[] timeFields = getTimeFields();
    if (timeFields != null) {
      grokParser.withTimeFields(getTimeFields());
    }
    String dateFormat = getDateFormat();
    if (dateFormat != null) {
      grokParser.withDateFormat(getDateFormat());
    }
    grokParser.withTimestampField(getTimestampField());
    grokParser.init();
    byte[] rawMessage = getRawMessage().getBytes();
    List<JSONObject> parsedList = grokParser.parse(rawMessage);
    Assert.assertEquals(1, parsedList.size());
    compare(expectedParsed, parsedList.get(0));
  }

  public boolean compare(JSONObject expected, JSONObject actual) {
    MapDifference mapDifferences = Maps.difference(expected, actual);
    if (mapDifferences.entriesOnlyOnLeft().size() > 0) Assert.fail("Expected JSON has extra parameters: " + mapDifferences.entriesOnlyOnLeft());
    if (mapDifferences.entriesOnlyOnRight().size() > 0) Assert.fail("Actual JSON has extra parameters: " + mapDifferences.entriesOnlyOnRight());
    Map actualDifferences = new HashMap();
    if (mapDifferences.entriesDiffering().size() > 0) {
      Map differences = Collections.unmodifiableMap(mapDifferences.entriesDiffering());
      for (Object key : differences.keySet()) {
        Object expectedValueObject = expected.get(key);
        Object actualValueObject = actual.get(key);
        if (expectedValueObject instanceof Long || expectedValueObject instanceof Integer) {
          Long expectedValue = Long.parseLong(expectedValueObject.toString());
          Long actualValue = Long.parseLong(actualValueObject.toString());
          if (!expectedValue.equals(actualValue)) {
            actualDifferences.put(key, differences.get(key));
          }
        } else {
          actualDifferences.put(key, differences.get(key));
        }
      }
    }
    if (actualDifferences.size() > 0) Assert.fail("Expected and Actual JSON values don't match: " + actualDifferences);
    return true;
  }

  public abstract String getRawMessage();
  public abstract String getExpectedParsedString();
  public abstract String getGrokPath();
  public abstract String getGrokPatternLabel();
  public abstract String[] getTimeFields();
  public abstract String getDateFormat();
  public abstract String getTimestampField();
}
