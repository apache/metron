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
package org.apache.metron.parsers.asa;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class BasicAsaParserTest {   //TODO: add additional unit tests for, malformed IPs, IPv6, garbage data

    private static BasicAsaParser asaParser;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        asaParser = new BasicAsaParser();
        asaParser.init();
    }

    @Test
    public void testCISCOFW106023() {
        String rawMessage = "<164>Aug 05 2016 01:01:34: %ASA-4-106023: Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes()).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.30.9.121"));
        assertTrue(asaJson.get("ip_dst_addr").equals("192.168.135.51"));
        assertTrue(asaJson.get("ip_src_port").equals(new Integer(54580)));
        assertTrue(asaJson.get("ip_dst_port").equals(new Integer(42028)));
        assertTrue((long) asaJson.get("timestamp") == 1470358894000L);
    }

    @Test
    public void testCISCOFW106006() {
        String rawMessage = "<162>Aug 05 2016 01:02:25: %ASA-2-106006: Deny inbound UDP from 10.25.177.164/63279 to 10.2.52.71/161 on interface Inside";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes()).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.25.177.164"));
        assertTrue(asaJson.get("ip_dst_addr").equals("10.2.52.71"));
        assertTrue(asaJson.get("ip_src_port").equals(new Integer(63279)));
        assertTrue(asaJson.get("ip_dst_port").equals(new Integer(161)));
        assertTrue((long) asaJson.get("timestamp") == 1470358945000L);
    }

    @Test
    public void testShortTimestamp() {
        String rawMessage = "<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes()).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.22.8.110"));
        assertTrue(asaJson.get("ip_dst_addr").equals("192.111.72.8"));
        assertTrue(asaJson.get("ip_src_port").equals(new Integer(49886)));
        assertTrue(asaJson.get("ip_dst_port").equals(new Integer(8612)));
        assertTrue((long) asaJson.get("timestamp") == 1452005555000L);
    }

    @Test
    public void testBulkInputFromFile() {
        InputStream logFile = this.getClass().getClassLoader().getResourceAsStream("sample_asa_events.txt");
        try {
            LineIterator logLines = IOUtils.lineIterator(logFile, "UTF-8");
            while (logLines.hasNext()) {
                String line = logLines.nextLine();
                JSONObject asaJson = asaParser.parse(line.getBytes()).get(0);
                assertEquals(asaJson.get("original_string"), line);
                assertNotNull(asaJson.get("timestamp"));
                assertNotNull(asaJson.get("ciscotag"));
                assertNotNull(asaJson.get("syslog_severity"));
                assertNotNull(asaJson.get("syslog_facility"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(logFile);
        }
    }
}
