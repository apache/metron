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

package org.apache.metron.parsers.checkpointsyslog;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BasicCheckPointSyslogParserTest {

    @Test
    public void testParseWithProcessId() {
        BasicCheckPointSyslogParser basicCheckPointSyslogParser = new BasicCheckPointSyslogParser();
        String testString = "<133>xpand[22939]: admin localhost t +volatile:mrma:users:user:socbackup:role:radius-group-any t";

        List<JSONObject> result = null;
        try {
            result = basicCheckPointSyslogParser.parse(testString.getBytes());
        } catch (Exception e) {
            fail();
        }
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority"), "133");
        assertEquals(parsedJSON.get("processName"), "xpand");
        assertEquals(parsedJSON.get("processId"), "22939");
        assertEquals(parsedJSON.get("message"), "admin localhost t +volatile:mrma:users:user:socbackup:role:radius-group-any t");
    }

    @Test
    public void testParseWithoutProcessId() {
        BasicCheckPointSyslogParser basicCheckPointSyslogParser = new BasicCheckPointSyslogParser();

        String testString = "<86>cp_radius_helper_1: Non-local user 'socbackup' given role 'radius-group-any' (if that exists)";

        List<JSONObject> result = null;
        try {
            result = basicCheckPointSyslogParser.parse(testString.getBytes());
        } catch (Exception e) {
            fail();
        }
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority"), "86");
        assertEquals(parsedJSON.get("processName"), "cp_radius_helper_1");
        assertEquals(parsedJSON.get("message"), "Non-local user 'socbackup' given role 'radius-group-any' (if that exists)");
    }

    @Test
    public void testParseWithoutProcess() {
        BasicCheckPointSyslogParser basicCheckPointSyslogParser = new BasicCheckPointSyslogParser();

        String testString = "<27>last message repeated 6 times";

        List<JSONObject> result = null;
        try {
            result = basicCheckPointSyslogParser.parse(testString.getBytes());
        } catch (Exception e) {
            fail();
        }
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority"), "27");
        assertEquals(parsedJSON.get("message"), "last message repeated 6 times");
    }

    @Test
    public void tetsParseMalformedOtherLine() {
        BasicCheckPointSyslogParser basicCheckPointSyslogParser = new BasicCheckPointSyslogParser();

        String testString = "<86cp_radius_helper_1 Non-local user 'socbackup' given role 'radius-group-any' (if that exists)";
        List<JSONObject> result = null;
        boolean hitException = false;
        try {
            result = basicCheckPointSyslogParser.parse(testString.getBytes());
        } catch (Exception e) {
            hitException = true;
        }
        assertTrue(hitException);
    }

    @Test
    public void testParseEmptyLine() {
        BasicCheckPointSyslogParser basicCheckPointSyslogParser = new BasicCheckPointSyslogParser();

        String testString = "";
        List<JSONObject> result = null;
        boolean hitException = false;
        try {
            result = basicCheckPointSyslogParser.parse(testString.getBytes());
        } catch (Exception e) {
            hitException = true;
        }
        assertTrue(hitException);
    }
}
