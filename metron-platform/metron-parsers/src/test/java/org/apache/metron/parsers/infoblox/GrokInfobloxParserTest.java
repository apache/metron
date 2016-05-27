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

package org.apache.metron.parsers.infoblox;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GrokInfobloxParserTest {

    private final String grokPath = "../metron-parsers/src/main/resources/patterns/infoblox";
    private final String grokLabel = "INFOBLOX";
    private final String dateFormat = "yyyy MMM dd HH:mm:ss";
    private final String timestampField = "timestamp";

    @Test
    public void testParseQueryLine() {

        //Set up parser, parse message
        GrokInfobloxParser parser = new GrokInfobloxParser(grokPath, grokLabel);
        parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 101.26.165.24#59335" +
                " (asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("ip_address"), "10.26.9.26");
        assertEquals(parsedJSON.get("process_id"), "19446");
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("dns_record_type"), "IN,A");
        assertEquals(parsedJSON.get("dns_query"), "asdf-network-ach-server.data.org");
        assertEquals(parsedJSON.get("priority"), "30");
        assertEquals(parsedJSON.get("dns_result"), "success");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 101.26.165.24#59335 (asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)");
        assertEquals(parsedJSON.get("dns_server_interface"), "120.126.219.25");
        assertEquals(parsedJSON.get("dns_action_type"), "query");
        assertEquals(parsedJSON.get("dns_bind_parameters"), "+");
        assertEquals(parsedJSON.get("ip_src_addr"), "101.26.165.24");
        assertEquals(parsedJSON.get("ip_src_port"), "59335");
        assertEquals(parsedJSON.get("timestamp"), "1459432137000");
    }

    @Test
    public void testParseErrorLine() {

    }


}
