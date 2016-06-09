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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GrokCiscoACSParserTest {

    private final String grokPath = "../metron-parsers/src/main/resources/patterns/ciscoacs";
    private final String grokLabel = "CISCOACS";
    private final String dateFormat = "MMM dd HH:mm:ss";
    private final String timestampField = "timestamp_string";

    private static final Logger LOGGER = LoggerFactory.getLogger(GrokCiscoACSParserTest.class);

    public GrokCiscoACSParserTest() throws Exception {
        super();
    }

    private Map<String, Object> parserConfig;

    @Before
    public void setup() {
        parserConfig = new HashMap<>();
        parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/ciscoacs");
        parserConfig.put("patternLabel", "CISCOACS");
        parserConfig.put("timestampField", "timestamp");
        parserConfig.put("dateFormat", "MMM dd HH:mm:ss");
    }

    @Test
    public void testParseLoginLine() throws Exception {

        //Set up parser, parse message
        GrokCiscoACSParser parser = new GrokCiscoACSParser();
        parser.configure(parserConfig);
        String testString = "<181>May 26 09:05:55 MDCNMSACS002 CSCOacs_TACACS_Accounting 0107266148 1 0 2016-05-26 09:05:55.151 +01:00 1384512861 3300 NOTICE Tacacs-Accounting: TACACS+ Accounting with Command, ACSVersion=acs-5.8.0.32-B.442.x86_64, ConfigVersionId=2134, Device IP Address=10.53.18.31, CmdSet=[ CmdAV=dir system: <cr> ], RequestLatency=0, Type=Accounting, Privilege-Level=15, Service=Login, User=hpna, Port=tty1, Remote-Address=10.24.0.17, Authen-Method=TacacsPlus, AVPair=task_id=11254, AVPair=timezone=GMT, AVPair=start_time=1464267955, AVPair=priv-lvl=15, AcctRequest-Flags=Stop, Service-Argument=shell, AcsSessionID=MDCNMSACS002/242802909/105517228, SelectedAccessService=TACACS, Step=13006 , Step=15008 , Step=15004 , Step=15012 , Step=13035 , NetworkDeviceName=nash-sw1, NetworkDeviceGroups=Location:All Locations:VZB, NetworkDeviceGroups=Device Type:All Device Types:Cisco IOS, Response={Type=Accounting; AcctReply-Status=Success; }";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        System.out.println("result.get(0): " + result.get(0));

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "181");
        assertEquals(parsedJSON.get("hostname"), "MDCNMSACS002");
        assertEquals(parsedJSON.get("severity"), "NOTICE");
        assertEquals(parsedJSON.get("category"), "CSCOacs_Passed_Authentications");
        assertEquals(parsedJSON.get("messageID"), 93197809);
        assertEquals(parsedJSON.get("totalSegments"), 2);
        assertEquals(parsedJSON.get("AuthenticationIdentityStore"), "InternalUsers");
    }
}