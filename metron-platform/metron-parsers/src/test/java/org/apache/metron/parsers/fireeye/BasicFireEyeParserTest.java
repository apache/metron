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
package org.apache.metron.parsers.fireeye;



import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsers.AbstractConfigTest;
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
        super.setUp("org.apache.metron.parsers.fireeye.BasicFireEyeParserTest");
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
	 * Test method for
	 *
	 *
	 *
	 *
	 *
	 * {@link BasicFireEyeParser#parse(byte[])}.
	 */
	@SuppressWarnings({ "rawtypes"})
	public void testParse() {
		for (String inputString : getInputStrings()) {
			JSONObject parsed = parser.parse(inputString.getBytes()).get(0);
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
