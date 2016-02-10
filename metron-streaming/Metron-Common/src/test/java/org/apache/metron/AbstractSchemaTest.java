
 
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
package org.apache.metron;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

 /**
 * <ul>
 * <li>Title: </li>
 * <li>Description: The class <code>AbstractSchemaTest</code> is
 * an abstract base class for implementing JUnit tests that need to load a
 * Json Schema. The <code>setup</code> method will attempt to
 * load a properties from a file, located in src/test/resources,
 * with the same name as the class.</li>
 * <li>Created: Aug 7, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class AbstractSchemaTest  extends AbstractConfigTest{
        
        
         /**
         * The schemaJsonString.
         */
        private String schemaJsonString = null;
        /**
         * Any Object for mavenMode
         * @parameter
         *   expression="${mode}"
         *   default-value="local"
         */
         private Object mode="local";        

        /**
         * Constructs a new <code>AbstractTestContext</code> instance.
         * @throws Exception 
         */
        public AbstractSchemaTest() throws Exception {
            super.setUp();
        }

        /**
         * Constructs a new <code>AbstractTestContext</code> instance.
         * @param name the name of the test case.
         */
        public AbstractSchemaTest(String name) {
            super(name);
            try{
                if(System.getProperty("mode")!=null){
                    setMode(System.getProperty("mode") );                
                }else
                {
                    setMode("local");
                }
            }catch(Exception ex){
                setMode("local");
            }            
        }

        /*
         * (non-Javadoc)
         * @see junit.framework.TestCase#setUp()
         */
        @Override
        protected void setUp() throws Exception {
            super.setUp();
            
        }

        /*
         * (non-Javadoc)
         * @see junit.framework.TestCase#tearDown()
         */
        @Override
        protected void tearDown() throws Exception {

        }

        
         /**
         * validateJsonData
         * @param jsonSchema
         * @param jsonData
         * @return
         * @throws Exception
         */
         
        protected boolean validateJsonData(final String jsonSchema, final String jsonData)
            throws Exception {
    
            final JsonNode d = JsonLoader.fromString(jsonData);
            final JsonNode s = JsonLoader.fromString(jsonSchema);
    
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonValidator v = factory.getValidator();
    
            ProcessingReport report = v.validate(s, d);
            System.out.println(report);
            
            return report.toString().contains("success");
        }
        
        protected String readSchemaFromFile(URL schema_url) throws Exception {
            BufferedReader br = new BufferedReader(new FileReader(
                    schema_url.getFile()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                sb.append(line);
            }
            br.close();

            String schema_string = sb.toString().replaceAll("\n", "");
            schema_string = schema_string.replaceAll(" ", "");

            System.out.println("Read in schema: " + schema_string);

            return schema_string;

        }        
        
       /**
        * Skip Tests
        */
       public boolean skipTests(Object mode){
           if(mode.toString().equals("local")){
               return true;
           }else {
               return false;
           }
       }
       
       /**
        * Returns the mode.
        * @return the mode.
        */
       
       public Object getMode() {
           return mode;
       }

       /**
        * Sets the mode.
        * @param mode the mode.
        */
       
       public void setMode(Object mode) {
       
           this.mode = mode;
       }

    
     /**
     
     * @param readSchemaFromFile
     */
     
    public void setSchemaJsonString(String schemaJsonString) {
        this.schemaJsonString=schemaJsonString;
    }

    
     /**
     
     * @return
     */
     
    public String getSchemaJsonString() {
       return this.schemaJsonString;
    }
     
}


