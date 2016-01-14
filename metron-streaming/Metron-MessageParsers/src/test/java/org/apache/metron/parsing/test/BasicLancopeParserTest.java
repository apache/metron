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

import org.apache.metron.parsing.parsers.BasicLancopeParser;
import org.apache.metron.test.AbstractSchemaTest;

 /**
 * <ul>
 * <li>Title: Junit for LancopeParserTest</li>
 * <li>Description: </li>
 * <li>Created: Aug 25, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class BasicLancopeParserTest extends AbstractSchemaTest {
    
    /**
     * The inputStrings.
     */
     private static String[] inputStrings;    


    /**
     * The parser.
     */
    private static BasicLancopeParser parser=null;   

    /**
     * Constructs a new <code>BasicLancopeParserTest</code> instance.
     * @param name
     */

    public BasicLancopeParserTest(String name) {
        super(name);
    }

    /**
     
     * @throws java.lang.Exception
     */
    protected static void setUpBeforeClass() throws Exception {        
    }

    /**
     
     * @throws java.lang.Exception
     */
    protected static void tearDownAfterClass() throws Exception {
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */

    protected void setUp() throws Exception {
        super.setUp("org.apache.metron.parsing.test.BasicLancopeParserTest");
        setInputStrings(super.readTestDataFromFile(this.getConfig().getString("logFile")));
        BasicLancopeParserTest.setParser(new BasicLancopeParser());   
        
        URL schema_url = getClass().getClassLoader().getResource(
            "TestSchemas/LancopeSchema.json");
        super.setSchemaJsonString(super.readSchemaFromFile(schema_url));      
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.apache.metron.parsing.parsers.BasicLancopeParser#parse(byte[])}.
     * @throws Exception 
     * @throws IOException 
     */
    public void testParse() throws IOException, Exception {
        
        for (String inputString : getInputStrings()) {
            JSONObject parsed = parser.parse(inputString.getBytes());
            assertNotNull(parsed);
        
            System.out.println(parsed);
            JSONParser parser = new JSONParser();

            Map<?, ?> json=null;
            try {
                json = (Map<?, ?>) parser.parse(parsed.toJSONString());
                assertEquals(true, validateJsonData(super.getSchemaJsonString(), json.toString()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
    * Returns the parser.
    * @return the parser.
    */
   
   public static BasicLancopeParser getParser() {
       return parser;
   }

   /**
    * Sets the parser.
    * @param parser the parser.
    */
   
   public static void setParser(BasicLancopeParser parser) {
   
       BasicLancopeParserTest.parser = parser;
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
   
       BasicLancopeParserTest.inputStrings = inputStrings;
   }   
}

