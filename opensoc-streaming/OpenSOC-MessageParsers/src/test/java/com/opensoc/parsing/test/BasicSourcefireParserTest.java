/**
 * 
 */
package com.opensoc.parsing.test;



import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.opensoc.parsing.parsers.BasicSourcefireParser;

/**
 * <ul>
 * <li>Title: Test For SourceFireParser</li>
 * <li>Description: </li>
 * <li>Created: July 8, 2014</li>
 * </ul>
 * @version $Revision: 1.0 $
 */
public class BasicSourcefireParserTest extends TestCase
	{

	private  static String sourceFireString = "";
	private BasicSourcefireParser sourceFireParser=null;



	/**
	 * @throws java.lang.Exception
	 */
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	public static void tearDownAfterClass() throws Exception {
		setSourceFireString("");
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
		setSourceFireString("SFIMS: [Primary Detection Engine (a7213248-6423-11e3-8537-fac6a92b7d9d)][MTD Access Control] Connection Type: Start, User: Unknown, Client: Unknown, Application Protocol: Unknown, Web App: Unknown, Firewall Rule Name: MTD Access Control, Firewall Rule Action: Allow, Firewall Rule Reasons: Unknown, URL Category: Unknown, URL_Reputation: Risk unknown, URL: Unknown, Interface Ingress: s1p1, Interface Egress: N/A, Security Zone Ingress: Unknown, Security Zone Egress: N/A, Security Intelligence Matching IP: None, Security Intelligence Category: None, {TCP} 72.163.0.129:60517 -> 10.1.128.236:443");		assertNotNull(getSourceFireString());
		sourceFireParser = new BasicSourcefireParser();		
	}

	/**
	 * 	
	 * 	
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
		sourceFireParser = null;
	}

	/**
	 * Test method for {@link com.opensoc.parsing.parsers.BasicSourcefireParser#parse(java.lang.String)}.
	 */
	@SuppressWarnings({ "rawtypes", "unused" })
	public void testParse() {
		JSONObject parsed = sourceFireParser.parse(getSourceFireString().getBytes());
		assertNotNull(parsed);
		
		System.out.println(parsed);
		JSONParser parser = new JSONParser();

		Map json=null;
		try {
			json = (Map) parser.parse(parsed.toJSONString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Iterator iter = json.entrySet().iterator();
			

		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();
            String value = (String) json.get("original_string").toString();
			assertNotNull(value);
		}
	}

	/**
	 * Returns SourceFire Input String
	 */
	public static String getSourceFireString() {
		return sourceFireString;
	}

		
	/**
	 * Sets SourceFire Input String
	 */	
	public static void setSourceFireString(String sourceFireString) {
		BasicSourcefireParserTest.sourceFireString = sourceFireString;
	}
}