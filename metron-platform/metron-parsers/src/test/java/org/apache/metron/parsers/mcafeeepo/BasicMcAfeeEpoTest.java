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

package org.apache.metron.parsers.mcafeeepo;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class BasicMcAfeeEpoTest {

    private McAfeeEpoParser bmap = new McAfeeEpoParser();

    public BasicMcAfeeEpoTest() throws Exception {
        super();

    }

    @Test
    public void test1() {
        String testString = "<13> computer.website.com \"2016-04-11 14:20:15\" timestamp=\"2016-04-11 14:20:15.69\", AutoID=\"136424372\", signature=\"WRITE_DENIED\", threat_type=\"none\", signature_id=\"20719\", category=\"cc.file.block\", severity_id=\"3\", event_description=\"File Write Denied\", detected_timestamp=\"2016-04-11 13:29:09.0\", file_name=\"c:\\windows\\system32\\folder\\file\", detection_method=\"NULL\", vendor_action=\"deny write\", threat_handled=\"1\", logon_user=\"NT AUTHORITY\\NETWORK SERVICE\", user=\"abc123\", dest_nt_domain=\"APL\", dest_dns=\"IMCAVA12345\", dest_nt_host=\"IMCAVA12345\", fqdn=\"IMCAVA12345.something.website.com\", dest_ip=\"100.170.200.100\", dest_mac=\"000000000000\", os=\"Windows 7\", sp=\"Service Pack 1\", os_version=\"6.1\", os_build=\"7601\", timezone=\"Eastern Standard Time\", src_dns=\"NULL\", src_ip=\"200.23.55.70\", src_mac=\"NULL\", process=\"NULL\", url=\"NULL\", source_logon_user=\"NULL\", is_laptop=\"1\", product=\"Solidifier\", product_version=\"6.1.3.436\", engine_version=\"NULL\", dat_version=\"NULL\", vse_dat_version=\"8130.0000\", vse_engine64_version=\"5800.7501\", vse_engine_version=\"5800.7501\", vse_hotfix=\"5\", vse_product_version=\"8.8.0.1385\"";

        List<JSONObject> result = bmap.parse(testString.getBytes());

        JSONObject jo = result.get(0);



        assertEquals(jo.get("priority"), "13");


        assertEquals(jo.get("timestamp"), 1460384415690L);
        assertEquals(jo.get("AutoID"), "136424372");
        assertEquals(jo.get("signature"), "WRITE_DENIED");
        assertEquals(jo.get("threat_type"), "none");
        assertEquals(jo.get("signature_id"), "20719");
        assertEquals(jo.get("category"), "cc.file.block");
        assertEquals(jo.get("severity_id"), "3");
        assertEquals(jo.get("event_description"), "File Write Denied");
        assertEquals(jo.get("detected_timestamp"), "2016-04-11 13:29:09.0");
        assertEquals(jo.get("file_name"), "c:\\windows\\system32\\folder\\file");
        assertEquals(jo.get("vendor_action"), "deny write");
        assertEquals(jo.get("threat_handled"), "1");
        assertEquals(jo.get("logon_user"), "NT AUTHORITY\\NETWORK SERVICE");
        assertEquals(jo.get("user"), "abc123");
        assertEquals(jo.get("dest_nt_domain"), "APL");
        assertEquals(jo.get("dest_dns"), "IMCAVA12345");
        assertEquals(jo.get("dest_nt_host"), "IMCAVA12345");
        assertEquals(jo.get("fqdn"), "IMCAVA12345.something.website.com");
        assertEquals(jo.get("ip_dst_addr"), "100.170.200.100");
        assertEquals(jo.get("dest_mac"), "000000000000");
        assertEquals(jo.get("os"), "Windows 7");
        assertEquals(jo.get("sp"), "Service Pack 1");
        assertEquals(jo.get("os_version"), "6.1");
        assertEquals(jo.get("os_build"), "7601");
        assertEquals(jo.get("timezone"), "Eastern Standard Time");
        assertEquals(jo.get("ip_src_addr"), "200.23.55.70");
        assertEquals(jo.get("is_laptop"), "1");
        assertEquals(jo.get("product"), "Solidifier");
        assertEquals(jo.get("product_version"), "6.1.3.436");
        assertEquals(jo.get("vse_dat_version"), "8130.0000");
        assertEquals(jo.get("vse_engine64_version"), "5800.7501");
        assertEquals(jo.get("vse_engine_version"), "5800.7501");
        assertEquals(jo.get("vse_hotfix"), "5");
        assertEquals(jo.get("vse_product_version"), "8.8.0.1385");




        System.out.println(result);
    }


}
