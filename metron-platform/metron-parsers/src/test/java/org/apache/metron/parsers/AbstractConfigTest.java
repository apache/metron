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
package org.apache.metron.parsers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

/**
 * <ul>
 * <li>Title: </li>
 * <li>Description: The class <code>AbstractConfigTest</code> is
 * an abstract base class for implementing JUnit tests that need to use
 * config to connect to ZooKeeper and HBase. The <code>setup</code> method will attempt to
 * load a properties from a file, located in src/test/resources,
 * with the same name as the class.</li>
 * <li>Created: Oct 10, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class AbstractConfigTest  extends AbstractTestContext {
         /**
         * The configPath.
         */
        protected String configPath=null;   
        
        /**
        * The configName.
        */
       protected String configName=null;           

        /**
         * The config.
         */
        private Configuration config=null;
        
         /**
         * The settings.
         */
        Map<String, String> settings=null;       

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
         * Constructs a new <code>AbstractConfigTest</code> instance.
         * @throws Exception 
         */
        public AbstractConfigTest() throws Exception {
            super.setUp();
        }

        /**
         * Constructs a new <code>AbstractTestContext</code> instance.
         * @param name the name of the test case.
         */
        public AbstractConfigTest(String name) {
            super(name);
        }

        /*
         * (non-Javadoc)
         * @see junit.framework.TestCase#setUp()
         */
        protected void setUp(String configName) throws Exception {
            super.setUp();
            this.setConfigPath("src/test/resources/config/"+getClass().getSimpleName()+".config");
            try {
                this.setConfig(new PropertiesConfiguration(this.getConfigPath()));
               
                Map configOptions= SettingsLoader.getConfigOptions((PropertiesConfiguration)this.config, configName+"=");
                this.setSettings(SettingsLoader.getConfigOptions((PropertiesConfiguration)this.config, configName + "."));
                this.getSettings().put(configName, (String) configOptions.get(configName));
            } catch (ConfigurationException e) {
                e.printStackTrace();
                throw new Exception("Config not found !!"+e);
            }
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
  
        protected String[] readTestDataFromFile(String test_data_url) throws Exception {
            BufferedReader br = new BufferedReader(new FileReader(
                    new File(test_data_url)));
            ArrayList<String> inputDataLines = new ArrayList<String>();
           
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                inputDataLines.add(line.toString().replaceAll("\n", ""));
            }
            br.close();
            String[] inputData = new String[inputDataLines.size()];
            inputData = inputDataLines.toArray(inputData);

            return inputData;
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
        
        /**
        * Returns the configPath.
        * @return the configPath.
        */
       public String getConfigPath() {
           return configPath;
       }
    
       /**
        * Sets the configPath.
        * @param configPath the configPath.
        */
       public void setConfigPath(String configPath) {
           this.configPath = configPath;
       }    
       /**
        * Returns the config.
        * @return the config.
        */
       
       public Configuration getConfig() {
           return config;
       }
    
       /**
        * Sets the config.
        * @param config the config.
        */
       
       public void setConfig(Configuration config) {
       
           this.config = config;
       }  
       /**
        * Returns the settings.
        * @return the settings.
        */
       
       public Map<String, String> getSettings() {
           return settings;
       }

       /**
        * Sets the settings.
        * @param settings the settings.
        */
       
       public void setSettings(Map<String, String> settings) {
           this.settings = settings;
       }   
       /**
       * Returns the configName.
       * @return the configName.
       */
      public String getConfigName() {
          return configName;
      }

      /**
       * Sets the configName.
       * @param configName the configName.
       */
      public void setConfigName(String configName) {  
          this.configName = configName;
      }       
}


