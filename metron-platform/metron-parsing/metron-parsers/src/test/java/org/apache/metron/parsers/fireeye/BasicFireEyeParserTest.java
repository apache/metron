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
package org.apache.metron.parsers.fireeye;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import org.apache.metron.parsers.AbstractParserConfigTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BasicFireEyeParserTest extends AbstractParserConfigTest {

  @Before
  public void setUp() throws Exception {
    inputStrings = super.readTestDataFromFile("src/test/resources/logData/FireEyeParserTest.txt");
    parser = new BasicFireEyeParser();
  }

  @SuppressWarnings({"rawtypes"})
  @Test
  public void testParse() throws ParseException {
    for (String inputString : inputStrings) {
      JSONObject parsed = parser.parse(inputString.getBytes(StandardCharsets.UTF_8)).get(0);
      Assert.assertNotNull(parsed);

      JSONParser parser = new JSONParser();

      Map json = (Map) parser.parse(parsed.toJSONString());

      Assert.assertNotNull(json);
      Assert.assertFalse(json.isEmpty());

      for (Object o : json.entrySet()) {
        Entry entry = (Entry) o;
        String key = (String) entry.getKey();
        String value = json.get(key).toString();
        Assert.assertNotNull(value);
      }
    }
  }

  private final static String fireeyeMessage = "<164>Mar 19 05:24:39 10.220.15.15 fenotify-851983.alert: CEF:0|FireEye|CMS|7.2.1.244420|DM|domain-match|1|rt=Feb 09 2015 12:28:26 UTC dvc=10.201.78.57 cn3Label=cncPort cn3=53 cn2Label=sid cn2=80494706 shost=dev001srv02.example.com proto=udp cs5Label=cncHost cs5=mfdclk001.org dvchost=DEVFEYE1 spt=54527 dvc=10.100.25.16 smac=00:00:0c:07:ac:00 cn1Label=vlan cn1=0 externalId=851983 cs4Label=link cs4=https://DEVCMS01.example.com/event_stream/events_for_bot?ev_id\\=851983 dmac=00:1d:a2:af:32:a1 cs1Label=sname cs1=Trojan.Generic.DNS";

  @SuppressWarnings("rawtypes")
  @Test
  public void testTimestampParsing() throws ParseException {
    JSONObject parsed = parser.parse(fireeyeMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    JSONParser parser = new JSONParser();
    Map json = (Map) parser.parse(parsed.toJSONString());
    long expectedTimestamp = ZonedDateTime.of(Year.now(ZoneOffset.UTC).getValue(), 3, 19, 5, 24, 39, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
    Assert.assertEquals(expectedTimestamp, json.get("timestamp"));
  }
}
