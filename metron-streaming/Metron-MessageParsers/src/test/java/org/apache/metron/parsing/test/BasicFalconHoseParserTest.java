package org.apache.metron.parsing.test;

import java.util.Map;

import junit.framework.TestCase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.BasicFalconHoseParser;

public class BasicFalconHoseParserTest extends TestCase {

    private BasicFalconHoseParser falconHoseParser = null;
    private JSONParser jsonParser = null;

    public BasicFalconHoseParserTest() throws Exception {
        falconHoseParser = new BasicFalconHoseParser();
        jsonParser = new JSONParser();
    }

    public void testLoginAuditEvent() throws ParseException {
        String rawMessage = "{\"metadata\":{\"offset\":3302,\"eventType\":\"LoginAuditEvent\"},\"event\":{\"LoginTime\":1444160709766,\"UserId\":\"tyler.baker@customer.rackspace.com\",\"UserIp\":\"50.56.228.73\",\"OperationName\":\"UserAuthenticate\",\"ServiceName\":\"TokenApi\",\"Success\":true}}";

        Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
        JSONObject rawJson = (JSONObject) rawMessageMap.get("event");

        JSONObject fhJson = falconHoseParser.parse(rawMessage.getBytes());

        assertEquals(Long.parseLong(fhJson.get("timestamp").toString()), Long.parseLong(rawJson.get("LoginTime").toString()) * 1000);
        assertEquals(fhJson.get("ip_src_addr").toString(), rawJson.get("UserIp").toString());
        assertEquals(fhJson.get("ip_dst_addr").toString(), rawJson.get("UserIp").toString());
        assertEquals(fhJson.get("ip_src_port").toString(), "0");
        assertEquals(fhJson.get("ip_dst_port").toString(), "0");
        assertEquals(fhJson.get("protocol").toString(), "http");
        assertTrue(fhJson.containsKey("original_string"));
        assertTrue(fhJson.containsKey("timestamp"));
    }

    public void testDetectionSummaryEvent() throws ParseException {
        String rawMessage = "{\"metadata\":{\"offset\":3304,\"eventType\":\"DetectionSummaryEvent\"},\"event\":{\"ProcessStartTime\":1444168443,\"ProcessEndTime\":0,\"ProcessId\":288437472047,\"ParentProcessId\":288435542004,\"ComputerName\":\"619027-DAPPP083\",\"UserName\":\"mxaon_admin\",\"DetectName\":\"Suspicious Activity\",\"DetectDescription\":\"An administrative/reconnaissance tool was spawned under an IIS worker process\",\"Severity\":2,\"SeverityName\":\"Low\",\"FileName\":\"regsvr32.exe\",\"FilePath\":\"\\Device\\HarddiskVolume1\\Windows\\SysWOW64\",\"CommandLine\":\"regsvr32.exe /u /s C:\\Windows\\system32\\dxtmsft.dll\",\"SHA256String\":\"890c1734ed1ef6b2422a9b21d6205cf91e014add8a7f41aa5a294fcf60631a7b\",\"MD5String\":\"432be6cf7311062633459eef6b242fb5\",\"SHA1String\":\"N/A\",\"MachineDomain\":\"619027-DAPPP083\",\"FalconHostLink\":\"https://falcon.crowdstrike.com/detects/-2623836595666801992\",\"SensorId\":\"97264ff9a8b548749f41871e09c6856e\"}}";

        Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
        JSONObject rawJson = (JSONObject) rawMessageMap.get("event");

        JSONObject fhJson = falconHoseParser.parse(rawMessage.getBytes());

        assertEquals(Long.parseLong(fhJson.get("timestamp").toString()), Long.parseLong(rawJson.get("ProcessStartTime").toString()));
        assertEquals(fhJson.get("ip_src_addr").toString(), rawJson.get("ComputerName").toString());
        assertEquals(fhJson.get("ip_dst_addr").toString(), rawJson.get("ComputerName").toString());
        assertEquals(fhJson.get("ip_src_port").toString(), "0");
        assertEquals(fhJson.get("ip_dst_port").toString(), "0");
        assertEquals(fhJson.get("protocol").toString(), "http");
        assertTrue(fhJson.containsKey("original_string"));
        assertTrue(fhJson.containsKey("timestamp"));
    }

    public void testUserActivityAuditEvent() throws ParseException {
        String rawMessage = "{\"metadata\":{\"offset\":3326,\"eventType\":\"UserActivityAuditEvent\"},\"event\":{\"UserId\":\"jason.blagg@customer.rackspace.com\",\"UserIp\":\"50.56.228.68\",\"OperationName\":\"UpdateDetectState\",\"ServiceName\":\"Detects\",\"Success\":true,\"AuditKeyValues\":[{\"Key\":\"detects\",\"ValueString\":\"6574431533307329744\"},{\"Key\":\"new_state\",\"ValueString\":\"in_progress\"}]}}";

        Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
        JSONObject rawJson = (JSONObject) rawMessageMap.get("event");

        JSONObject fhJson = falconHoseParser.parse(rawMessage.getBytes());

        assertEquals(fhJson.get("ip_src_addr").toString(), rawJson.get("UserIp").toString());
        assertEquals(fhJson.get("ip_dst_addr").toString(), rawJson.get("UserIp").toString());
        assertEquals(fhJson.get("ip_src_port").toString(), "0");
        assertEquals(fhJson.get("ip_dst_port").toString(), "0");
        assertEquals(fhJson.get("protocol").toString(), "http");
        assertTrue(fhJson.containsKey("original_string"));
        assertTrue(fhJson.containsKey("timestamp"));
    }
}
