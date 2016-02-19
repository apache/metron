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
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

 /**
 * <ul>
 * <li>Title: </li>
 * <li>Description: The class <code>AbstractTestContext</code> is
 * an abstract base class for implementing JUnit tests that need to load a
 * test properties. The <code>setup</code> method will attempt to
 * load a properties from a file, located in src/test/resources,
 * with the same name as the class.</li>
 * <li>Created: Aug 7, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class AbstractTestContext {
         /**
         * The testProps.
         */
        protected File testPropFile=null;

        /**
         * The properties loaded for test.
         */
        protected Properties testProperties=new Properties();
        
        /**
         * Any Object for mavenMode
         * @parameter
         *   expression="${mode}"
         *   default-value="global"
         */
         private Object mode="local";        

        /**
         * Constructs a new <code>AbstractTestContext</code> instance.
         */
        public AbstractTestContext() {
            super();
        }

        /**
         * Constructs a new <code>AbstractTestContext</code> instance.
         * @param name the name of the test case.
         */
        public AbstractTestContext(String name) {
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
        protected void setUp() throws Exception {
            InputStream input=null;
            File directory = new File("src/test/resources");
            if (!directory.isDirectory()) {
                return;
            }
            File file = new File(directory, getClass().getSimpleName() + ".properties");
            if (!file.canRead()) {
                return;
            }
            setTestPropFile(file);
            try{
                input=new FileInputStream(file);
                testProperties.load(input);
            }catch(IOException ex){
                ex.printStackTrace();
                throw new Exception("failed to load properties");
            }
            
            
        }

        /*
         * (non-Javadoc)
         * @see junit.framework.TestCase#tearDown()
         */
        @After
        protected void tearDown() throws Exception {

        }

        /**
         * Returns the testProperties.
         * @return the testProperties.
         */
        
        public Properties getTestProperties() {
            return testProperties;
        }

        /**
         * Sets the testProperties.
         * @param testProperties the testProperties.
         */
        
        public void setTestProperties(Properties testProperties) {
        
            this.testProperties = testProperties;
        }    
        /**
        * Returns the testPropFile.
        * @return the testPropFile.
        */
       
       public File getTestPropFile() {
           return testPropFile;
       }

       /**
        * Sets the testPropFile.
        * @param testPropFile the testPropFile.
        */
       
       public void setTestPropFile(File testPropFile) {
       
           this.testPropFile = testPropFile;
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

       protected void assertNotNull() {}
       protected void assertNotNull(Object o) {}
     
    }


