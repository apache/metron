package com.opensoc.parsing.test;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import junit.framework.TestCase;

import com.opensoc.parsing.parsers.BasicBroParser;

/**
 * <ul>
 * <li>Title: Test For BroParser</li>
 * <li>Description: </li>
 * <li>Created: July 8, 2014</li>
 * </ul>
 * @version $Revision: 1.0 $
 */
public class BroParserTest extends TestCase {
	
	private static String broJsonString="";
	private static BasicBroParser broParser=null;
	
    /**
     * Constructs a new <code>BroParserTest</code> instance.
     */
    public BroParserTest() {
        super();
    }	


	/**
	 * @throws java.lang.Exception
	 */
	public static void setUpBeforeClass() throws Exception {
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	public static void tearDownAfterClass() throws Exception {
		setBroJsonString("");
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
	    setBroJsonString("{\"http\":{\"ts\":1402307733473,\"uid\":\"CTo78A11g7CYbbOHvj\",\"id.orig_h\":\"192.249.113.37\",\"id.orig_p\":58808,\"id.resp_h\":\"72.163.4.161\",\"id.resp_p\":80,\"trans_depth\":1,\"method\":\"GET\",\"host\":\"www.cisco.com\",\"uri\":\"/\",\"user_agent\":\"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3\",\"request_body_len\":0,\"response_body_len\":25523,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"FJDyMC15lxUn5ngPfd\"],\"resp_mime_types\":[\"text/html\"]}}");	    
		assertNotNull(getBroJsonString());
		BroParserTest.setBroParser(new BasicBroParser());		
	}
	
	/**
	 * @throws ParseException
	 * Tests for Parse Method
	 * Parses Static json String and checks if any spl chars are present in parsed string.
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	public void testParse() throws ParseException {


		BasicBroParser broparser = new BasicBroParser();
		assertNotNull(getBroJsonString());
		JSONObject cleanJson = broparser.parse(getBroJsonString().getBytes());
        assertNotNull(cleanJson);		
		System.out.println(cleanJson);


		Pattern p = Pattern.compile("[^\\._a-z0-9 ]", Pattern.CASE_INSENSITIVE);

		JSONParser parser = new JSONParser();

		Map json = (Map) cleanJson;
		Map output = new HashMap();
		Iterator iter = json.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();

			Matcher m = p.matcher(key);
			boolean b = m.find();
			// Test False
			assertFalse(b);
		}

	}
    /**
     * Returns the instance of BroParser
     */
	public static BasicBroParser getBroParser() {
		return broParser;
	}
    /**
     * Sets the instance of BroParser
     */
	public static void setBroParser(BasicBroParser broParser) {
		BroParserTest.broParser = broParser;
	}
    /**
     * Return BroPaser JSON String
     */
	public static String getBroJsonString() {
		return BroParserTest.broJsonString;
	}

    /**
     * Sets BroPaser JSON String
     */
	public static void setBroJsonString(String broJsonString) {
		BroParserTest.broJsonString = broJsonString;
	}	
}
