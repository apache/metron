package org.apache.metron.parsers.mcafeeepo;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.metron.parsers.bluecoat.BasicBluecoatParser;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * Created by bnp617 on 5/16/16.
 */

public class BasicMcAfeeEpoTest {

    private BasicBluecoatParser bmap = new BasicBluecoatParser();

    public BasicMcAfeeEpoTest() throws Exception {
        super();

    }

    @Test
    public void test1() {
        String testString = "<13> computer.website.com \"2016-04-11 14:20:15\" timestamp=\"2016-04-11 14:20:15.693\", AutoID=\"136424372\", signature=\"WRITE_DENIED\", threat_type=\"none\", signature_id=\"20719\", category=\"cc.file.block\", severity_id=\"3\", event_description=\"File Write Denied\", detected_timestamp=\"2016-04-11 13:29:09.0\", file_name=\"c:\\windows\\system32\\folder\\file\", detection_method=\"NULL\", vendor_action=\"deny write\", threat_handled=\"1\", logon_user=\"NT AUTHORITY\\NETWORK SERVICE\", user=\"abc123\", dest_nt_domain=\"APL\", dest_dns=\"IMCAVA12345\", dest_nt_host=\"IMCAVA12345\", fqdn=\"IMCAVA12345.something.website.com\", dest_ip=\"100.170.200.100\", dest_mac=\"000000000000\", os=\"Windows 7\", sp=\"Service Pack 1\", os_version=\"6.1\", os_build=\"7601\", timezone=\"Eastern Standard Time\", src_dns=\"NULL\", src_ip=\"200.23.55.70\", src_mac=\"NULL\", process=\"NULL\", url=\"NULL\", source_logon_user=\"NULL\", is_laptop=\"1\", product=\"Solidifier\", product_version=\"6.1.3.436\", engine_version=\"NULL\", dat_version=\"NULL\", vse_dat_version=\"8130.0000\", vse_engine64_version=\"5800.7501\", vse_engine_version=\"5800.7501\", vse_hotfix=\"5\", vse_product_version=\"8.8.0.1385\"\n";

        List<JSONObject> result = bmap.parse(testString.getBytes());

        JSONObject jo = result.get(0);
        System.out.println(jo.toJSONString());


        /*
        assertEquals(jo.get("event_type"), "authentication failure");
        assertEquals(jo.get("event_code"), "250017");
        assertEquals(jo.get("realm"), "AD_ldap");
        assertEquals(jo.get("priority"), "29");
        assertEquals(jo.get("designated_host"), "10.118.29.228");
        */

        System.out.println(result);
    }


}
