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
package org.apache.metron.management;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.metron.common.Constants.ErrorFields.*;
import static org.apache.metron.common.Constants.Fields.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StellarParserRunnerTest {

    /**
     * {
     *  "dns": {
     *  "ts":1402308259.609,
     *  "uid":"CuJT272SKaJSuqO0Ia",
     *  "id.orig_h":"10.122.196.204",
     *  "id.orig_p":33976,
     *  "id.resp_h":"144.254.71.184",
     *  "id.resp_p":53,
     *  "proto":"udp",
     *  "trans_id":62418,
     *  "query":"www.cisco.com",
     *  "qclass":1,
     *  "qclass_name":"C_INTERNET",
     *  "qtype":28,
     *  "qtype_name":"AAAA",
     *  "rcode":0,
     *  "rcode_name":"NOERROR",
     *  "AA":true,
     *  "TC":false,
     *  "RD":true,
     *  "RA":true,
     *  "Z":0,
     *  "answers":["www.cisco.com.akadns.net","origin-www.cisco.com","2001:420:1201:2::a"],
     *  "TTLs":[3600.0,289.0,14.0],
     *  "rejected":false
     *  }
     * }
     */
    @Multiline
    public String broMessage;

    /**
     * {
     *  "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
     *  "filterClassName":"org.apache.metron.parsers.filters.StellarFilter",
     *  "sensorTopic":"bro"
     * }
     */
    @Multiline
    private String broParserConfig;

    @Test
    public void testParseMessage() {
        List<String> toParse = new ArrayList<>();
        toParse.add(broMessage);
        toParse.add(broMessage);
        toParse.add(broMessage);

        // parse the messages
        StellarParserRunner runner = new StellarParserRunner("bro")
                .withParserConfiguration(broParserConfig)
                .withContext(Context.EMPTY_CONTEXT());
        List<JSONObject> messages = runner.parse(toParse);

        // expect 3 successfully parsed message
        assertEquals(3, messages.size());
        for(JSONObject message: messages) {
            assertEquals("bro", message.get(Constants.SENSOR_TYPE));
            assertTrue(message.containsKey(Constants.GUID));
            assertEquals("10.122.196.204", message.get(SRC_ADDR.getName()));
            assertEquals(33976L, message.get(SRC_PORT.getName()));
            assertEquals("144.254.71.184", message.get(DST_ADDR.getName()));
            assertEquals(53L, message.get(DST_PORT.getName()));
            assertEquals("dns", message.get("protocol"));
        }
    }

    @Test
    public void testParseInvalidMessage() {
        List<String> toParse = new ArrayList<>();
        toParse.add("{DAS}");

        // parse the messages
        StellarParserRunner runner = new StellarParserRunner("bro")
                .withParserConfiguration(broParserConfig)
                .withContext(Context.EMPTY_CONTEXT());
        List<JSONObject> messages = runner.parse(toParse);

        // expect an error message to be returned
        JSONObject error = messages.get(0);
        assertEquals(toParse.get(0), error.get("raw_message"));
        assertEquals(Constants.ERROR_TYPE, error.get(Constants.SENSOR_TYPE));
        assertEquals("parser_error", error.get(ERROR_TYPE.getName()));
        assertTrue(error.containsKey(MESSAGE.getName()));
        assertTrue(error.containsKey(EXCEPTION.getName()));
        assertTrue(error.containsKey(STACK.getName()));
        assertTrue(error.containsKey(ERROR_HASH.getName()));
        assertTrue(error.containsKey(Constants.GUID));
    }

    @Test
    public void testToString() {
        List<String> toParse = new ArrayList<>();
        toParse.add(broMessage);
        toParse.add("{DAS}");

        // parse the messages
        StellarParserRunner runner = new StellarParserRunner("bro")
                .withParserConfiguration(broParserConfig)
                .withContext(Context.EMPTY_CONTEXT());
        List<JSONObject> messages = runner.parse(toParse);

        // toString() should tally the number of successes and failures
        assertEquals("Parser{1 successful, 1 error(s)}", runner.toString());
    }
}
