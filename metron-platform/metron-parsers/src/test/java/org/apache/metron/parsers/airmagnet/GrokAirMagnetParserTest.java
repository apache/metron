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

package org.apache.metron.parsers.airmagnet;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GrokAirMagnetParserTest {

    private Map<String, Object> parserConfig;

    @Before
    public void setup() {
        parserConfig = new HashMap<>();
        parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/airmagnet");
        parserConfig.put("patternLabel", "AIRMAGNET");
        parserConfig.put("timestampField", "timestamp_string");
        parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
    }

    @Test
    public void testParse() throws Exception {

        //Set up parser, parse message
        GrokAirMagnetParser parser = new GrokAirMagnetParser();
        parser.configure(parserConfig);
        String testString = "<116>Apr 27 00:18:57 TYRION-ABC04013 TYRION-ABC04013 Alert: Device unprotected by WPA-TKIP from sensor MORBDGANS33, " +
                "Location: /Branches/FRA/ACS/ACS, Description: AP CISCO-LINKSYS:5A:62:D1 (SSID : Neisha-guest) is not using WPA-TKIP (Temporal Key Integrity Protocol) protection.  " +
                "WLAN traffic encrypted with TKIP defeats packet forgery, and replay attack.  TKIP is immune to the weakness introduced by crackable WEP IV key and attacks stemming " +
                "from key reuse.  The latest IEEE 802.11i pre-standard draft includes TKIP as one of the recommended data privacy protocols along with CCMP and WRAP.  Wi-Fi Alliance " +
                "also recommends TKIP in its WPA (Wireless Protected Access) standard.  Some WLAN equipment vendors have added TKIP support in their latest firmware and driver.  " +
                "Unlike AES based CCMP encryption, TKIP typically does not require hardware upgrade.  Please consider configuring this WLAN device to enable TKIP to prevent security " +
                "vulnerabilities such as packet forgery, replay, and WEP key recovery., Source MAC: 58:6D:8F:5A:62:D1-gn, Channel: 1";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("hostname"), "TYRION-ABC04013");
        assertEquals(parsedJSON.get("source_MAC_address"), "58:6D:8F:5A:62:D1");
        assertEquals(parsedJSON.get("alert"), "Device unprotected by WPA-TKIP from sensor MORBDGANS33");
        assertEquals(parsedJSON.get("description"), "AP CISCO-LINKSYS:5A:62:D1 (SSID : Neisha-guest) is not using WPA-TKIP (Temporal Key Integrity Protocol) protection.  " +
                "WLAN traffic encrypted with TKIP defeats packet forgery, and replay attack.  TKIP is immune to the weakness introduced by crackable WEP IV key and attacks " +
                "stemming from key reuse.  The latest IEEE 802.11i pre-standard draft includes TKIP as one of the recommended data privacy protocols along with CCMP and WRAP.  " +
                "Wi-Fi Alliance also recommends TKIP in its WPA (Wireless Protected Access) standard.  Some WLAN equipment vendors have added TKIP support in their latest " +
                "firmware and driver.  Unlike AES based CCMP encryption, TKIP typically does not require hardware upgrade.  Please consider configuring this WLAN device to enable " +
                "TKIP to prevent security vulnerabilities such as packet forgery, replay, and WEP key recovery.");
        assertEquals(parsedJSON.get("wifi_channel"), 1);
        assertEquals(parsedJSON.get("location"), "/Branches/FRA/ACS/ACS");
        assertEquals(parsedJSON.get("priority"), 116);
        assertEquals(parsedJSON.get("timestamp"), 1461716337000L);
    }

    @Test
    public void testParseMalformed() throws Exception {

        //Set up parser, attempt to parse malformed message
        GrokAirMagnetParser parser = new GrokAirMagnetParser();
        parser.configure(parserConfig);
        String testString = "<116>Apr 27 00:18:57 TYRION-ABC04013 TYRION-ABC04013 Alert: Device unprotected by WPA-TKIP from sensor MORBDGANS33, " +
                "Description: AP CISCO-LINKSYS:5A:62:D1 (SSID : Neisha-guest) is not using WPA-TKIP (Temporal Key Integrity Protocol) protection.  " +
                "WLAN traffic encrypted with TKIP defeats packet forgery, and replay attack.  TKIP is immune to the weakness introduced by crackable WEP IV key and attacks stemming " +
                "from key reuse.  The latest IEEE 802.11i pre-standard draft includes TKIP as one of the recommended data privacy protocols along with CCMP and WRAP.  Wi-Fi Alliance " +
                "also recommends TKIP in its WPA (Wireless Protected Access) standard.  Some WLAN equipment vendors have added TKIP support in their latest firmware and driver.  " +
                "Unlike AES based CCMP encryption, TKIP typically does not require hardware upgrade.  Please consider configuring this WLAN device to enable TKIP to prevent security " +
                "vulnerabilities such as packet forgery, replay, and WEP key recovery., Source MAC: 58:6D:8F:5A:62:D1-gn, Channel: 1";
        List<JSONObject> result = parser.parse(testString.getBytes());
        assertNull(result);
    }

}
