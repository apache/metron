/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.BasicIseParser;
import org.apache.metron.test.AbstractSchemaTest;
import org.junit.Assert;


/**
 * <ul>
 * <li>Title: Basic ISE Parser</li>
 * <li>Description: Junit Test Case for BasicISE Parser</li>
 * <li>Created: AUG 25, 2014</li>
 * </ul>
 * 
 * @version $Revision: 1.1 $
 */

public class BasicIseParserTest extends AbstractSchemaTest {
    /**
     * The inputStrings.
     */
     private static String[] inputStrings;   

	 /**
	 * The parser.
	 */
	private static BasicIseParser parser = null;


	/**
	 * Constructs a new <code>BasicIseParserTest</code> instance.
	 * 
	 * @param name
	 */

	public BasicIseParserTest(String name) {
		super(name);
	}

	/**
	 * 
	 * @throws java.lang.Exception
	 */
	protected static void setUpBeforeClass() throws Exception {
	}

	/**
	 * 
	 * @throws java.lang.Exception
	 */
	protected static void tearDownAfterClass() throws Exception {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */

	protected void setUp() throws Exception {
        super.setUp("org.apache.metron.parsing.test.BasicLancopeParserTest");
        setInputStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
        BasicIseParserTest.setIseParser(new BasicIseParser());
		
		URL schema_url = getClass().getClassLoader().getResource(
				"TestSchemas/IseSchema.json");
		 super.setSchemaJsonString(super.readSchemaFromFile(schema_url));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link org.apache.metron.parsing.parsers.BasicIseParser#parse(byte[])}.
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public void testParse() throws ParseException, IOException, Exception {
        for (String inputString : getInputStrings()) {
            JSONObject parsed = parser.parse(inputString.getBytes());
            assertNotNull(parsed);
        
            System.out.println(parsed);
            JSONParser parser = new JSONParser();

            Map<?, ?> json=null;
            try {
                json = (Map<?, ?>) parser.parse(parsed.toJSONString());
                Assert.assertEquals(true, validateJsonData(super.getSchemaJsonString(), json.toString()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
	}

	/**
	 * Returns the iseParser.
	 * 
	 * @return the iseParser.
	 */

	public BasicIseParser getIseParser() {
		return parser;
	}

	/**
	 * Sets the iseParser.
	 * 
	 * @param iseParser
	 */


	public static void setIseParser(BasicIseParser parser) {

		BasicIseParserTest.parser = parser;
	}
   /**
    * Returns the inputStrings.
    * @return the inputStrings.
    */
   
   public static String[] getInputStrings() {
       return inputStrings;
   }

   /**
    * Sets the inputStrings.
    * @param inputStrings the inputStrings.
    */
   
   public static void setInputStrings(String[] inputStrings) {
       BasicIseParserTest.inputStrings = inputStrings;
   }   



}
