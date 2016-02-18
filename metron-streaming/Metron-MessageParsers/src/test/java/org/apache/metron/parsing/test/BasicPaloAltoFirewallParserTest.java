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

import org.apache.metron.parsing.parsers.BasicPaloAltoFirewallParser;
import org.apache.metron.test.AbstractConfigTest;
import org.junit.Assert;

public class BasicPaloAltoFirewallParserTest extends AbstractConfigTest {
    /**
    * The inputStrings.
    */
   private static String[] inputStrings;

    /**
     * Constructs a new <code>BasicPaloAltoFirewallParserTest</code> instance.
     * @throws Exception
     */ 
    public BasicPaloAltoFirewallParserTest() throws Exception {
        super();        
    }

     /**
     * Sets the inputStrings.
     * @param inputStrings the inputStrings.
     */
        
    public static void setInputStrings(String[] inputStrings) {
    
        BasicPaloAltoFirewallParserTest.inputStrings = inputStrings;
    }

     /**
     * The paParser.
     */
    private BasicPaloAltoFirewallParser paParser=null;

		/**
		 * @throws java.lang.Exception
		 */
		public static void setUpBeforeClass() throws Exception {
		}

		/**
		 * @throws java.lang.Exception
		 */
		public static void tearDownAfterClass() throws Exception {
			setPAStrings(null);
		}

		/**
		 * @throws java.lang.Exception
		 */
		public void setUp() throws Exception {
	          super.setUp("org.apache.metron.parsing.test.BasicPaloAltoFirewallParserTest");
	          setPAStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
	          paParser = new BasicPaloAltoFirewallParser();           
		}

		/**
		 * 	
		 * 	
		 * @throws java.lang.Exception
		 */
		public void tearDown() throws Exception {
			paParser = null;
		}

		/**
		 * Test method for
		 * {@link org.apache.metron.parsing.parsers.BasicSourcefireParser#parse(byte[])}.
		 */
		@SuppressWarnings({ "rawtypes" })
		public void testParse() {
			for (String inputString : getInputStrings()) {
				JSONObject parsed = paParser.parse(inputString.getBytes()).get(0);
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
					String value = (String) json.get(key).toString();
					Assert.assertNotNull(value);
				}
			}
		}

		/**
		 * Returns  Input String
		 */
		public static String[] getInputStrings() {
			return inputStrings;
		}

			
		/**
		 * Sets  Input String
		 */	
		public static void setPAStrings(String[] strings) {
			BasicPaloAltoFirewallParserTest.inputStrings = strings;
		}
        
        /**
         * Returns the paParser.
         * @return the paParser.
         */
        public BasicPaloAltoFirewallParser getPaParser() {
            return paParser;
        }

        /**
         * Sets the paParser.
         * @param paParser the paParser.
         */
        
        public void setPaParser(BasicPaloAltoFirewallParser paParser) {
        
            this.paParser = paParser;
        }

	}
