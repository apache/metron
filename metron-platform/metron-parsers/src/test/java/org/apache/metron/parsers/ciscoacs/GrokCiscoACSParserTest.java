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

package org.apache.metron.parsers.ciscoacs;

import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GrokCiscoACSParserTest {

    private final String grokPath = "../metron-parsers/src/main/resources/patterns/ciscoacs";
    private final String grokLabel = "CISCOACS";
    private final String dateFormat = "yyyy MMM dd HH:mm:ss";
    private final String timestampField = "timestamp_string";


        public GrokCiscoACSParserTest() throws Exception {
            super();
        }

        @Override
        public void testParseRealLine() {
            // We are only testing the first line
            String arubaStrings = getRealStrings()[0];

            JSONObject parsed = parser.parse(arubaStrings.getBytes());
            assertNotNull(parsed);
            assertFalse(parsed.isEmpty());

            JSONParser parser = new JSONParser();

            Map<String,Object> json = null;
            try {
                json = (Map<String,Object>) parser.parse(parsed.toJSONString());
            } catch (ParseException e) {
                LOGGER.error("ParserException when trying to parse JSONObject");
            }
            // Ensure json is not null
            if (null == json) {
                LOGGER.error("JSON is null");
                assertNotNull(json);
            }

            // Ensure json is not empty
            if (json.isEmpty()) {
                LOGGER.error("JSON is empty");
                fail();
            }

            Iterator<Entry<String,Object>> iter = json.entrySet().iterator();

            // ensure we dont have any null keys
            while (iter.hasNext()) {
                Entry<String,Object> entry = iter.next();
                assertNotNull(entry);

                String key = (String) entry.getKey();
                assertNotNull(key);
            }

            try {
                assertEquals(json.get("timestamp"), dateFormat.parse("2015-11-05 09:32:46").getTime() + (ParserConfig.getArubaTimezoneOffset() * 3600000));
                assertEquals(json.get("device_generated_timestamp"), dateFormat.parse("2015-11-05 09:32:46").getTime());
            } catch (java.text.ParseException e) {
                LOGGER.error("ParserException - Unable to parse date");
                fail();
            }

            assertEquals(json.get("ip_src_addr"), "10.0.0.140");
            assertEquals(json.get("message_type"), "Aruba-ClearPass");
            assertEquals(json.get("message_subtype"), "Session_Logs-Roadblock001");
            assertEquals(json.get("message_hex"), "11553520");
            assertEquals(json.get("unknown_1"), "2");
            assertEquals(json.get("unknown_2"), "0");
            assertEquals(json.get("Common_Username"), "host/MUSAVA198084.cof.ds.capitalone.com");
            assertEquals(json.get("Common_Roles"), "[Machine Authenticated]");

        }

        @Override
        public void testParserGeneratesCorrectSchema() {
            for (String arubaString : getRealStrings()) {
                JSONObject parsed = parser.parse(arubaString.getBytes());
                JSONParser parser = new JSONParser();
                Map<String,Object> json = null;

                try {
                    json = (Map<String,Object>) parser.parse(parsed.toJSONString());
                } catch (ParseException e) {
                    LOGGER.error("ParserException when trying to parse JSONObject");
                    fail();
                }

                // Ensure schema is correct
                assertTrue(isSchemaCorrect(parsed)); // checks if json contains keys original_string and timestamp
                boolean hasUrlKey = json.containsKey("url");
                boolean hasIpKey = json.containsKey("ip_src_addr");
                assertTrue((hasUrlKey && !hasIpKey) || (!hasUrlKey && hasIpKey)); // contains key url xor ip_src_addr
                assertTrue(json.containsKey("message_type")); // contains key type
                if (json.containsKey("sub_type")) {
                    assertTrue(!"".equals(json.get("message_subtype"))); // if  key sub_type exists, it is not ""
                }

                assertTrue(!json.containsKey("UNWANTED")); // does not contain UNWANTED
            }
        }

    }