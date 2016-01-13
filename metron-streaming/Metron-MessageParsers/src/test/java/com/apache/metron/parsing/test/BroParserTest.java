package com.apache.metron.parsing.test;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.apache.metron.parsing.parsers.BasicBroParser;
import com.apache.metron.test.AbstractConfigTest;

/**
 * <ul>
 * <li>Title: Test For BroParser</li>
 * <li>Description: </li>
 * <li>Created: July 8, 2014</li>
 * </ul>
 * @version $Revision: 1.0 $
 */

 /**
 * <ul>
 * <li>Title: </li>
 * <li>Description: </li>
 * <li>Created: Feb 20, 2015 </li>
 * </ul>
 * @author $Author: $
 * @version $Revision: 1.1 $
 */
public class BroParserTest extends AbstractConfigTest {
	
	
	/**
	 * The inputStrings.
	 */
	private static String[] inputStrings;

     /**
     * The parser.
     */
    private BasicBroParser parser=null;
	
    /**
     * Constructs a new <code>BroParserTest</code> instance.
     * @throws Exception 
     */
    public BroParserTest() throws Exception {
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
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
        super.setUp("com.apache.metron.parsing.test.BroParserTest");
        setInputStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
        parser = new BasicBroParser();  
	}
	
	/**
	 * @throws ParseException
	 * Tests for Parse Method
	 * Parses Static json String and checks if any spl chars are present in parsed string.
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	public void testParse() throws ParseException {

		for (String inputString : getInputStrings()) {
			JSONObject cleanJson = parser.parse(inputString.getBytes());
			assertNotNull(cleanJson);
			System.out.println(cleanJson);

			Pattern p = Pattern.compile("[^\\._a-z0-9 ]",
					Pattern.CASE_INSENSITIVE);

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

	}

	/**
	 * Returns Input String
	 */
	public static String[] getInputStrings() {
		return inputStrings;
	}

	/**
	 * Sets SourceFire Input String
	 */
	public static void setInputStrings(String[] strings) {
		BroParserTest.inputStrings = strings;
	}
	
    /**
     * Returns the parser.
     * @return the parser.
     */
    
    public BasicBroParser getParser() {
        return parser;
    }


    /**
     * Sets the parser.
     * @param parser the parser.
     */
    
    public void setParser(BasicBroParser parser) {
    
        this.parser = parser;
    }	
}
