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
package com.opensoc.alerts.adapters;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import com.opensoc.test.AbstractConfigTest;
import com.opensoc.alerts.adapters.AllAlertAdapter;

 /**
 * <ul>
 * <li>Title: AllAlertAdapterTest</li>
 * <li>Description: Tests for AllAlertAdapter</li>
 * <li>Created: Oct 8, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class AllAlertAdapterTest extends AbstractConfigTest {

     /**
     * The allAlertAdapter.
     */
    private static AllAlertAdapter allAlertAdapter=null;
    
     /**
     * The connected.
     */
    private static boolean connected=false;

    /**
     * Constructs a new <code>AllAlertAdapterTest</code> instance.
     * @param name
     */
    public AllAlertAdapterTest(String name) {
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

    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
          super.setUp("com.opensoc.alerts.adapters.AllAlertAdapter");
          Properties prop = super.getTestProperties();
          assertNotNull(prop);   
       // this.setMode("global");
        if(skipTests(this.getMode())){
            System.out.println(getClass().getName()+" Skipping Tests !!Local Mode");
            return;//skip tests
       }else{      
           Map<String, String> settings = super.getSettings();
           @SuppressWarnings("rawtypes")
        Class loaded_class = Class.forName("com.opensoc.alerts.adapters.AllAlertAdapter");
           @SuppressWarnings("rawtypes")
        Constructor constructor = loaded_class.getConstructor(new Class[] { Map.class});
           
           AllAlertAdapterTest.allAlertAdapter = (AllAlertAdapter) constructor.newInstance(settings);
            // AllAlertAdapterTest.allAlertAdapter = new AllAlertAdapter(settings)
      }
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Test method for {@link com.opensoc.alerts.adapters.AlllterAdapter#initialize()}.
     */
    public void testInitializeAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{        
           
        boolean initialized =AllAlertAdapterTest.getAllAlertAdapter().initialize();
        assertTrue(initialized);
       }
    }
    
    /**
     * Test method for containsAlertId(@link  com.opensoc.alerts.adapters.AlllterAdapter#containsAlertId()}.
     */
    public void testContainsAlertId(){
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{          
            boolean containsAlert =AllAlertAdapterTest.getAllAlertAdapter().containsAlertId("test");
            assertFalse(containsAlert);
       }
    }
 
   

    /**
     * Returns the allAlertAdapter.
     * @return the allAlertAdapter.
     */
    
    public static AllAlertAdapter getAllAlertAdapter() {
        return allAlertAdapter;
    }

    /**
     * Sets the allAlertAdapter.
     * @param allAlertAdapter the allAlertAdapter.
     */
    
    public static void setAllAlertAdapter(AllAlertAdapter allAlertAdapter) {
    
        AllAlertAdapterTest.allAlertAdapter = allAlertAdapter;
    }
    /**
     * Returns the connected.
     * @return the connected.
     */
    
    public static boolean isConnected() {
        return connected;
    }

    /**
     * Sets the connected.
     * @param connected the connected.
     */
    
    public static void setConnected(boolean connected) {
    
        AllAlertAdapterTest.connected = connected;
    }    
}

