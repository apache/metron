package org.apache.metron.parsers.infoblox;

import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by zra564 on 5/26/16.
 */
public class GrokInfobloxParserTest {

    private final String grokPath = "../metron-parsers/src/main/resources/patterns/infoblox";
    private final String grokLabel = "INFOBLOX";
    private final String dateFormat = "yyyy MMM dd HH:mm:ss";
    private final String timestampField = "timestamp_string";

    @Test
    public void testParseLoginLine() throws Exception {

        //Set up parser, parse message
        GrokWebSphereParser parser = new GrokWebSphereParser(grokPath, grokLabel);
        parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 10.26.65.240#59335 " +
                "(stmt-filenet-nch-server.uscards.cof): query: stmt-filenet-nch-server.uscards.cof IN A + (10.26.9.25)";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);
        System.out.println(parsedJSON.toJSONString());

        //Compare fields
        assertEquals(parsedJSON.get("process_id"), 19446);
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("dns_record_type"), "IN,A");
        assertEquals(parsedJSON.get("dns_query"), "asdf-network-ach-server.data.org");
        assertEquals(parsedJSON.get("source.type"), "Infoblox");
        assertEquals(parsedJSON.get("ip_address"), "10.26.9.26");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("dns_result"), "success");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 101.26.165.24#59335 (asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)");
        assertEquals(parsedJSON.get("ip_src_port"), 59335);
        assertEquals(parsedJSON.get("dns_server_interface"), "120.126.219.25");
        assertEquals(parsedJSON.get("dns_action_type"), "query");
        assertEquals(parsedJSON.get("dns_bind_parameters"), "+");
        assertEquals(parsedJSON.get("ip_src_addr"), "101.26.165.24");
        assertEquals(parsedJSON.get("timestamp"), 1459432137000L);
    }

}
