package org.apache.metron.parsers.asa;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class BasicAsaParserTest {

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
        //System.out.println(asaJson.toString());
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(isValidIpAddr((String) asaJson.get("ip_src_addr")));
        assertTrue(isValidIpAddr((String) asaJson.get("ip_dst_addr")));
        assertTrue(isValidPort((int) asaJson.get("ip_src_port")));
        assertTrue(isValidPort((int) asaJson.get("ip_dst_port")));
        assertTrue(isNotInFuture((long) asaJson.get("timestamp")));
    }

    @Test
    public void testCISCOFW106006() {
        String rawMessage = "<162>Aug 05 2016 01:02:25: %ASA-2-106006: Deny inbound UDP from 10.25.177.164/63279 to 10.2.52.71/161 on interface Inside";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes()).get(0);
        //System.out.println(asaJson.toString());
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(isValidIpAddr((String) asaJson.get("ip_src_addr")));
        assertTrue(isValidIpAddr((String) asaJson.get("ip_dst_addr")));
        assertTrue(isValidPort((int) asaJson.get("ip_src_port")));
        assertTrue(isValidPort((int) asaJson.get("ip_dst_port")));
        assertTrue(isNotInFuture((long) asaJson.get("timestamp")));
    }

    //TODO: Move validation code into main parser

    private boolean isValidPort(int portNumber) {
        if (portNumber > 1 && portNumber < 65536)
            return true;
        else
            return false;
    }

    private boolean isValidIpAddr(String ipAddress) {
        String pattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ipAddress);
        if (m.matches())
            return true;
        else
            return false;
    }

    private boolean isNotInFuture(long timestamp) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        if (now.toEpochSecond() > timestamp)
            return true;
        else
            return false;
    }
}
