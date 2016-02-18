/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsing.test;



import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.BasicSourcefireParser;
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
public class BasicSourcefireParserTest extends AbstractConfigTest
{
     /**
     * The sourceFireStrings.
     */    
    private static String[] sourceFireStrings;
    
     /**
     * The sourceFireParser.
     */
    private BasicSourcefireParser sourceFireParser=null;


    /**
     * Constructs a new <code>BasicSourcefireParserTest</code> instance.
     * @throws Exception
     */
     
    public BasicSourcefireParserTest() throws Exception {
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
		setSourceFireStrings(null);
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
        super.setUp("org.apache.metron.parsing.test.BasicSoureceFireParserTest");
        setSourceFireStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
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
	 * Test method for {@link org.apache.metron.parsing.parsers.BasicSourcefireParser#parse(byte[])}.
	 */
	@SuppressWarnings({ "rawtypes", "unused" })
	public void testParse() {
		for (String sourceFireString : getSourceFireStrings()) {
		    byte[] srcBytes = sourceFireString.getBytes();
			JSONObject parsed = sourceFireParser.parse(sourceFireString.getBytes()).get(0);
			Assert.assertNotNull(parsed);
		
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
				Assert.assertNotNull(value);
			}
		}
	}

	/**
	 * Returns SourceFire Input String
	 */
	public static String[] getSourceFireStrings() {
		return sourceFireStrings;
	}

		
	/**
	 * Sets SourceFire Input String
	 */	
	public static void setSourceFireStrings(String[] strings) {
		BasicSourcefireParserTest.sourceFireStrings = strings;
	}
    /**
    * Returns the sourceFireParser.
    * @return the sourceFireParser.
    */
   
   public BasicSourcefireParser getSourceFireParser() {
       return sourceFireParser;
   }

   /**
    * Sets the sourceFireParser.
    * @param sourceFireParser the sourceFireParser.
    */
   
   public void setSourceFireParser(BasicSourcefireParser sourceFireParser) {
   
       this.sourceFireParser = sourceFireParser;
   }	
}
