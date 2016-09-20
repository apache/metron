package org.apache.metron.parsers.asa;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

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
