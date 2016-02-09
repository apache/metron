package org.apache.metron.parsing.test;

import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.GrokAsaParser;
import org.apache.metron.test.AbstractConfigTest;
import org.junit.Assert;


/**
 * <ul>
 * <li>Title: </li>
 * <li>Description: </li>
 * <li>Created: Feb 17, 2015 by: </li>
 * </ul>
 * @author $Author:  $
 * @version $Revision: 1.1 $
 */
public class GrokAsaParserTest extends AbstractConfigTest{
     /**
     * The grokAsaStrings.
     */
    private static String[] grokAsaStrings=null;
 
     /**
     * The grokAsaParser.
     */
     
    private GrokAsaParser grokAsaParser=null;
    
     /**
     * Constructs a new <code>GrokAsaParserTest</code> instance.
     * @throws Exception
     */
     
    public GrokAsaParserTest() throws Exception {
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
		setGrokAsaStrings(null);
	}

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
	public void setUp() throws Exception {
          super.setUp("org.apache.metron.parsing.test.GrokAsaParserTest");
          setGrokAsaStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
          grokAsaParser = new GrokAsaParser();		
	}

		/**
		 * 	
		 * 	
		 * @throws java.lang.Exception
		 */
		public void tearDown() throws Exception {
			grokAsaParser = null;
		}

		/**
		 * Test method for {@link org.apache.metron.parsing.parsers.BasicSourcefireParser#parse(java.lang.String)}.
		 */
		@SuppressWarnings({ "rawtypes" })
		public void testParse() {
		    
			for (String grokAsaString : getGrokAsaStrings()) {
				JSONObject parsed = grokAsaParser.parse(grokAsaString.getBytes());
				Assert.assertNotNull(parsed);
			
				System.out.println(parsed);
				JSONParser parser = new JSONParser();

				Map json=null;
				try {
					json = (Map) parser.parse(parsed.toJSONString());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				//Ensure JSON returned is not null/empty
				Assert.assertNotNull(json);
				
				Iterator iter = json.entrySet().iterator();
				

				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					Assert.assertNotNull(entry);
					
					String key = (String) entry.getKey();
					Assert.assertNotNull(key);
					
					String value = (String) json.get("CISCO_TAGGED_SYSLOG").toString();
					Assert.assertNotNull(value);
				}
			}
		}

		/**
		 * Returns GrokAsa Input String
		 */
		public static String[] getGrokAsaStrings() {
			return grokAsaStrings;
		}

			
		/**
		 * Sets GrokAsa Input String
		 */	
		public static void setGrokAsaStrings(String[] strings) {
			GrokAsaParserTest.grokAsaStrings = strings;
		}
	    
	    /**
	     * Returns the grokAsaParser.
	     * @return the grokAsaParser.
	     */
	    
	    public GrokAsaParser getGrokAsaParser() {
	        return grokAsaParser;
	    }


	    /**
	     * Sets the grokAsaParser.
	     * @param grokAsaParser the grokAsaParser.
	     */
	    
	    public void setGrokAsaParser(GrokAsaParser grokAsaParser) {
	    
	        this.grokAsaParser = grokAsaParser;
	    }
		
	}