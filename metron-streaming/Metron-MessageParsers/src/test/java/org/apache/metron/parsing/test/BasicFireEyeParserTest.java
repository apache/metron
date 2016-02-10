/**
 * 
 */
package org.apache.metron.parsing.test;



import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.BasicFireEyeParser;
import org.apache.metron.test.AbstractConfigTest;
import org.junit.Assert;

/**
 * <ul>
 * <li>Title: Test For SourceFireParser</li>
 * <li>Description: </li>
 * <li>Created: July 8, 2014</li>
 * </ul>
 * @version $Revision: 1.0 $
 */
public class BasicFireEyeParserTest extends AbstractConfigTest
{
   /**
    * The inputStrings.
    */
    private static String[] inputStrings;
 
   /**
    * The parser.
    */
    private BasicFireEyeParser parser=null;

	
   /**
    * Constructs a new <code>BasicFireEyeParserTest</code> instance.
    * @throws Exception
    */ 
    public BasicFireEyeParserTest() throws Exception {
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
        super.setUp("org.apache.metron.parsing.test.BasicFireEyeParserTest");
        setInputStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
        parser = new BasicFireEyeParser();  
	}

	/**
	 * 	
	 * 	
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
		parser = null;
        setInputStrings(null);		
	}

	/**
	 * Test method for {@link org.apache.metron.parsing.parsers.BasicFireEyeParser#parse(java.lang.String)}.
	 */
	@SuppressWarnings({ "rawtypes"})
	public void testParse() {
		for (String inputString : getInputStrings()) {
			JSONObject parsed = parser.parse(inputString.getBytes());
			Assert.assertNotNull(parsed);
		
			JSONParser parser = new JSONParser();

			Map json=null;
			try {
				json = (Map) parser.parse(parsed.toJSONString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			Iterator iter = json.entrySet().iterator();
			
			Assert.assertNotNull(json);
			Assert.assertFalse(json.isEmpty());
			

			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				String value = (String) json.get(key).toString();
				Assert.assertNotNull(value);
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
		BasicFireEyeParserTest.inputStrings = strings;
	}
	
    /**
     * Returns the parser.
     * @return the parser.
     */
    public BasicFireEyeParser getParser() {
        return parser;
    }

    /**
     * Sets the parser.
     * @param parser the parser.
     */
     public void setParser(BasicFireEyeParser parser) {
    
        this.parser = parser;
     }
}